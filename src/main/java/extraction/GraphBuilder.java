package extraction;

import extraction.Node.ConcreteNode;
import extraction.network.*;
import org.jgrapht.graph.DirectedPseudograph;

import java.util.*;

public class GraphBuilder {
    private final Prospector prospector;
    private final DirectedPseudograph<Node, Label> graph = new DirectedPseudograph<>(Label.class);
    private final HashMap<String, ArrayList<ConcreteNode>> choicePaths = new HashMap<>();
    private final HashMap<Integer, ArrayList<ConcreteNode>> nodeHashes = new HashMap<>();
    private final Set<String> services;
    private int badLoopCounter = 0;
    private int nextNodeID = 0;
    enum BuildGraphResult{
        OK, BAD_LOOP, FAIL
    }

    private GraphBuilder(Strategy extractionStrategy, Set<String> services){
        prospector = new Prospector(extractionStrategy, this);
        this.services = services;
    }

    public static record SEGContainer (DirectedPseudograph<Node, Label> graph, ConcreteNode rootNode,
                                       BuildGraphResult buildGraphResult, int badLoopCounter) {}

    /**
     * Builds a Symbolic ExecutionGraph (SEG) over the execution paths that a network may take.
     * @param network The network to build an SEG for.
     * @return A container with the graph, its root, the success status of the construction, and
     * how many times a failed attempt to create a loop in the graph was made.
     */
    public static SEGContainer buildSEG(Network network, Strategy strategy) { return buildSEG(network, Set.of(), strategy); }
    /**
     * Builds a Symbolic ExecutionGraph (SEG) over the execution paths that a network may take.
     * @param network The network to build an SEG for.
     * @param services Set of names of processes that are allowed to be starved.
     * @return A container with the graph, its root, the success status of the construction, and
     * how many times a failed attempt to create a loop in the graph was made.
     */
    public static SEGContainer buildSEG(Network network, Set<String> services, Strategy strategy){
        var builder = new GraphBuilder(strategy, services);
        return builder.buildSEG(network, services);

    }
    private SEGContainer buildSEG(Network network, Set<String> services){
        var marking = new HashMap<String, Boolean>(network.processes.size());
        network.processes.forEach((processName, __) ->
            marking.put(processName, services.contains(processName))
        );
        var root = new ConcreteNode(network, "-", nextNodeID++, 0, marking);
        graph.addVertex(root);
        addToChoicePathsMap(root);
        addToNodeHashes(root);

        BuildGraphResult result = prospector.prospect(root);

        return new SEGContainer(graph, root, result, badLoopCounter);

    }

    /**
     * Builds the graph depth-first on a possible advancement of the network. If advancement contains the
     * details of a conditional, two branches are build, one for each branch.
     * If this function does not return OK, the graph is not modified.
     * @param advancement Container with the target Network for the next node, and the Label for the action that
     *                    created the target Network.
     *                    If elseLabel and elseNetwork is not null, then label and network are considered the
     *                    corresponding thenLabel and thenNetwork.
     * @param currentNode The node currently being build from.
     * @return OK on success, BAD_LOOP if a different action is needed to build on the graph, or FAIL if
     * the network is not extractable.
     */
    BuildGraphResult buildGraph(Network.Advancement advancement, ConcreteNode currentNode){

        var targetMarking = new HashMap<>(currentNode.marking);
        var label = advancement.label();
        var targetNetwork = advancement.network();
        var createdNewNode = false;

        advancement.actors().forEach(name -> targetMarking.put(name, Boolean.TRUE));
        if (!targetMarking.containsValue(false))
            flipAndResetMarking(label, targetMarking, targetNetwork);

        //If a matching node already exists in the graph, add a new edge to it.

        Procedure proc = findNodeInGraph(targetNetwork, targetMarking, currentNode);
        ConcreteNode targetNode;
        if (proc != null){
            targetNode = proc.top;
            label.becomes = proc.parameters;
            //If that fails, the edge creates a bad loop, and the algorithm must backtrack.
            if (!addEdgeToGraph(currentNode, targetNode, label))
                return BuildGraphResult.BAD_LOOP;
        }
        //If no matching node exists, add it to the graph and create an edge to it
        else{
            createdNewNode = true;
            targetNode = createNode(targetNetwork, label, currentNode, targetMarking); //currentNode.formNode(targetNetwork, label, nextNodeID++, targetMarking);
            addNodeAndEdgeToGraph(currentNode, targetNode, label);
            //Build the graph out from the new node. Remove the new node and abort on failure
            BuildGraphResult result = prospector.prospect(targetNode);
            if (result != BuildGraphResult.OK){
                removeNodeFromGraph(targetNode);
                return result;
            }
        }

        //If not working with a conditional, return success
        if (advancement.elseLabel() == null)
            return BuildGraphResult.OK;

        //Now the then branch has successfully been build
        //Repeat for the else branch
        var elseLabel = advancement.elseLabel();
        var elseNetwork = advancement.elseNetwork();
        elseLabel.flipped = label.flipped;

        //If a matching node already exists in the graph, add a new edge to it.
        proc = findNodeInGraph(elseNetwork, targetMarking, currentNode);
        ConcreteNode elseNode;
        if (proc != null){
            elseNode = proc.top;
            elseLabel.becomes = proc.parameters;
            //If that fails, the edge creates a bad loop, and the algorithm must backtrack.
            if (!addEdgeToGraph(currentNode, elseNode, elseLabel)){
                //Remove the then branch of the graph
                if (createdNewNode)
                    removeNodesFromGraph(targetNode.choicePath);
                else
                    graph.removeEdge(currentNode, targetNode);
                return BuildGraphResult.BAD_LOOP;
            }
        }
        //If no matching node exists, add it to the graph and create an edge to it
        else{
            elseNode = createNode(elseNetwork, elseLabel, currentNode, targetMarking);
            addNodeAndEdgeToGraph(currentNode, elseNode, elseLabel);
            //Build the graph out from the new node. Remove the new node and abort on failure
            BuildGraphResult result = prospector.prospect(elseNode);
            if (result != BuildGraphResult.OK){
                removeNodeFromGraph(elseNode);
                //Remove the then branch of the graph
                if (createdNewNode)
                    removeNodesFromGraph(targetNode.choicePath);
                else
                    graph.removeEdge(currentNode, targetNode);
                return result;
            }
        }

        return BuildGraphResult.OK;
    }

    /* ============================
        Helper Functions
       ============================ */

    /**
     * Removes all nodes from the graph originating from a specific choice path.
     * @param choicePathPrefix All nodes in the graph whose choice path begins with this string are removed.
     */
    private void removeNodesFromGraph(String choicePathPrefix){
        choicePaths.forEach((path, nodeList) -> {
            if (path.startsWith(choicePathPrefix)){
                nodeList.forEach(node -> {
                    graph.removeVertex(node);
                    removeFromNodeHashes(node);
                });
                nodeList.clear();
            }
        });
    }

    private void removeNodeFromGraph(ConcreteNode node){
        graph.removeVertex(node);
        removeFromNodeHashes(node);
        removeFromChoicePathsMap(node);
    }

    private void addNodeAndEdgeToGraph(ConcreteNode currentNode, ConcreteNode newNode, Label label){
        graph.addVertex(newNode);
        graph.addEdge(currentNode, newNode, label);
        addToChoicePathsMap(newNode);
        addToNodeHashes(newNode);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean addEdgeToGraph(ConcreteNode source, ConcreteNode target, Label label){
        if (checkLoop(source, target, label))
            return graph.addEdge(source, target, label);
        badLoopCounter++;
        return false;
    }

    /**
     * Creates a new ConcreteNode for the graph
     * @param network The node's Network
     * @param label The label intended for the edge going to this node
     * @param predecessor The node that will have an outgoing edge to the returned node
     * @param marking The process marking for this mode
     * @return A new ConcreteNode instance
     */
    private ConcreteNode createNode(Network network, Label label, ConcreteNode predecessor, HashMap<String, Boolean> marking){
        String choicePath = predecessor.choicePath;
        if (label instanceof Label.ConditionLabel.ThenLabel)
            choicePath += "1";
        else if (label instanceof Label.ConditionLabel.ElseLabel)
            choicePath += "0";

        int flipCounter = predecessor.flipCounter;
        if (label.flipped)
            flipCounter++;

        return new ConcreteNode(network, choicePath, nextNodeID++, flipCounter, marking);
    }

    /**
     * Searches the graph for a Node with same Network, marking, and which begins with the same
     * choicePath as the currentNode parameter.
     * @param network The Network the matching Node should have.
     * @param marking The marking the matching Node should have
     * @param currentNode The Node with a choicePath beginning with the choicePath of the Node we are searching for.
     * @return A matching Node or null if none found.
     */
    private Procedure findNodeInGraph(Network network, HashMap<String, Boolean> marking, ConcreteNode currentNode){
        List<ConcreteNode> viableNodes = nodeHashes.get(hashMarkedNetwork(network, marking));
        if (viableNodes == null){
            return null;
        }
        for (ConcreteNode otherNode : viableNodes){
            if (!currentNode.choicePath.startsWith(otherNode.choicePath)){
                continue;
            }
            var parameters = findSurjectiveMapping(network, otherNode.network);
            if (parameters == null || parameters.size() != parameters.values().stream().distinct().count()){
                //The second half of the conditional ensures the mapping can be used with procedure invocation.
                //It works by ensuring the actual map is bijective.
                continue;
            }
            boolean fail = false;
            for (String processName : marking.keySet()){
                String otherName = parameters.getOrDefault(processName, processName); //Get the mapped value if it exists
                if (marking.get(processName) != otherNode.marking.get(otherName)){
                //if (!marking.get(processName) && otherNode.marking.get(otherName)){
                    fail = true;
                    break;
                }
            }
            if (fail)
                continue;
            return new Procedure(otherNode, parameters);
        }
        return null;
    }
    private record Procedure(ConcreteNode top, Map<String, String> parameters){}

    /**
     * Generates a partial mapping from process names in fromNetwork, to process names in toNetwork,
     * such that the corresponding ProcessTerms are equal, but their names different.
     * The mapping is only generated if the following holds true for all processes in the Networks,
     * regardless of if the processes are in the map:
     * - There is at least one identical ProcessTerm in the other network.
     * - If there are two (or more) identical ProcessTerm in toNetwork, then there must also be at least
     * as many identical processes in fromNetwork.
     * It is guaranteed that there exists a mapping to every process name in toNetwork, even if there
     * are multiple identical ProcessTerm in toNetwork, unless there is an identical process with the
     * same name in fromNetwork. That particular mapping is then omitted.
     * @return A partial surjective Map if one could be made, or null otherwise.
     */
    private Map<String, String> findSurjectiveMapping(Network fromNetwork, Network toNetwork){
        var fromProc = fromNetwork.processes;
        var toProc = toNetwork.processes;
        var unmatchedNames = new HashSet<>(fromProc.keySet());  //Keys not yet in the map
        var map = new HashMap<String, String>();
        //Ensure every process name in toNetwork is in the map
        for (String name : toProc.keySet()){
            var toTerm = toProc.get(name);
            var fromTerm = fromProc.get(name);
            //There is a better than random chance that a process maps to itself
            if (fromTerm != null && fromTerm.equals(toTerm)){
                //Mappings to itself is omitted.
                unmatchedNames.remove(name);
                continue;
            }
            //If not, look for a match
            boolean matched = false;
            for (String match : unmatchedNames){
                fromTerm = fromProc.get(match);
                if (toTerm.equals(fromTerm)){
                    map.put(match, name);
                    unmatchedNames.remove(match);
                    matched = true;
                    break;
                }
            }
            //Fail if no match
            if (!matched)
                return null;
        }
        //Terminated processes can be ignored.
        unmatchedNames.removeIf(name -> fromProc.get(name).isTerminated());
        //Now there is a mapping to every process name in toNetwork.
        //Add mappings from the so-far unused process names in fromNetwork
        for (String name : unmatchedNames){
            var fromTerm = fromProc.get(name);
            //Try every process in toNetwork
            boolean matched = false;
            for (String match : toProc.keySet()){
                var toTerm = fromProc.get(match);
                if (toTerm.equals(fromTerm)){
                    map.put(name, match);
                    matched = true;
                    break;
                }
            }
            //Fail if no match
            if (!matched)
                return null;
        }
        //A surjective mapping was made.
        return map;
    }

    /**
     * Checks if adding a new edge from source to target with the label would result in a bad loop.
     * @param source The hypothetical edge source.
     * @param target The hypothetical edge target.
     * @param label The label that is to be stored in the hypothetical edge.
     * @return true, if the label is flipped, or the target.flipCounter < source.flipCounter
     */
    private boolean checkLoop(ConcreteNode source, ConcreteNode target, Label label){
        if (label.flipped)
            return true;
        if (target == source)
            return false;
        return source.flipCounter > target.flipCounter;
    }

    /**
     * Sets the Label's flipped field to true, and sets all values in marking to false,
     * except for processes which has terminated, and services which is set to true.
     * Should be called when all values in marking have been set to true.
     * @param label The label for the interaction that marked the last process.
     * @param network The network where all processes have been marked.
     */
    private void flipAndResetMarking(Label label, HashMap<String, Boolean> marking, Network network){
        label.flipped = true;
        marking.replaceAll((name, __) -> network.processes.get(name).isTerminated() || services.contains(name));

    }

    private void addToChoicePathsMap(ConcreteNode node){
        ArrayList<ConcreteNode> nodesWithPath = choicePaths.computeIfAbsent(node.choicePath, k -> new ArrayList<>());
        nodesWithPath.add(node);
    }

    private Integer hashMarkedNetwork(Network n, HashMap<String, Boolean> marking){
        var unique = new HashSet<ProcessTerm>(n.processes.size());
        final int[] hash = {0};     //lambdas do not allow direct modifications of ints.
        n.processes.forEach((key, term) -> {
            if (unique.add(term))  //Returns false, if the element is already in the set
                hash[0] += term.hashCode() * 31 + marking.get(key).hashCode();
        });
        return hash[0];
        //return n.hashCode() + 31 * marking.hashCode();
    }

    private void addToNodeHashes(ConcreteNode node){
        Integer hash = hashMarkedNetwork(node.network, node.marking);
        ArrayList<ConcreteNode> nodesWithHash = nodeHashes.computeIfAbsent(hash, k -> new ArrayList<>());
        nodesWithHash.add(node);
    }

    //TODO: This removes the entire list. Shouldn't it just remove an element from the list?
    private void removeFromNodeHashes(ConcreteNode node){
        nodeHashes.remove(hashMarkedNetwork(node.network, node.marking));
    }

    private void removeFromChoicePathsMap(ConcreteNode node){
        var nodeList = choicePaths.get(node.choicePath);
        if (nodeList != null){
            nodeList.remove(node);
        }
    }


}
