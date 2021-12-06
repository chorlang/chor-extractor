package extraction;

import extraction.Node.ConcreteNode;
import extraction.network.*;
import extraction.network.utils.ProcessInteractionChecker;
import org.jgrapht.graph.DirectedPseudograph;

import java.util.*;
import java.util.function.Function;

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
    BuildGraphResult buildGraph(Network.Advancement advancement, ConcreteNode currentNode) {

        var targetMarking = new HashMap<>(currentNode.marking);
        var label = advancement.label();
        var targetNetwork = advancement.network();
        var createdNewNode = false;

        advancement.actors().forEach(name -> targetMarking.put(name, Boolean.TRUE));
        if (!targetMarking.containsValue(false))
            flipAndResetMarking(label, targetMarking, targetNetwork);

        //Attempts to expand on the graph using the advancement of the prospector.
        //Either creates a loop, or adds edge and new node to the graph, and builds upon the new node
        extensionResult extension = extendGraph(targetNetwork, targetMarking, label, currentNode);

        //Expanding the graph using the provided advancement might not be possible.
        //Return BAD_LOOP if an invalid loop would be formed, or FAIL if the network cannot be extracted.
        if (extension.buildResult != BuildGraphResult.OK)
            return extension.buildResult;

        //If there is no else branch (the advancement is not a conditional) return success.
        if (advancement.elseLabel() == null)
            return BuildGraphResult.OK;

        //If there is an else branch, the above code build the then branch. Now to build the else branch.
        Label elseLabel = advancement.elseLabel();
        Network elseNetwork = advancement.elseNetwork();
        elseLabel.flipped = label.flipped;

        //The function cleans up after itself on failure, so we only need the result status
        BuildGraphResult elseResult = extendGraph(elseNetwork, targetMarking, elseLabel, currentNode).buildResult;

        //If both branches was successfully build, return OK
        if (elseResult == BuildGraphResult.OK)
            return BuildGraphResult.OK;

        //The else branch could not be build. Remove the then branch, and return the failure status.
        if (extension.createdNewNode)
            removeGraphBranch(extension.targetNode.choicePath);
        else
            graph.removeEdge(currentNode, extension.targetNode);
        return elseResult;
    }



/*
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
        Label elseLabel = advancement.elseLabel();
        Network elseNetwork = advancement.elseNetwork();
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
                    removeGraphBranch(targetNode.choicePath);
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
                    removeGraphBranch(targetNode.choicePath);
                else
                    graph.removeEdge(currentNode, targetNode);
                return result;
            }
        }

        return BuildGraphResult.OK;
    }*/

    /**
     * Ensures that the graph contains a node with the provided network and state. Either it finds an existing node
     * and forms a loop, or it creates a new node (with an edge to that node) and builds recursively from that.<br>
     *
     * @param network
     * @param marking
     * @param label
     * @param currentNode
     * @return
     */
    //TODO figure out how to return the created node (if applicable)
    private extensionResult extendGraph(Network network, HashMap<String, Boolean> marking, Label label, ConcreteNode currentNode){
        //**Try to see if a loop can be formed**

        //Calculate the flip counter of the hypothetical next node.
        int flipCounter = currentNode.flipCounter + (label.flipped ? 1 : 0);

        //Get a list of nodes with the same network and marking hash, and the same choicePath.
        List<ConcreteNode> viableNodes = nodeHashes.getOrDefault(hashMarkedNetwork(network, marking), new ArrayList<>())
                .stream().filter(node -> currentNode.choicePath.startsWith(node.choicePath)).toList();

        //Iterate though the nodes with the same hash, and see if they have equivalent behaviour.
        viableIterator: for (ConcreteNode otherNode : viableNodes){
            if (flipCounter > otherNode.flipCounter && detectResourceLeak(network, otherNode.network)) {
                System.out.println("Resource leak detected. Extraction not possible");
                return new extensionResult(null, BuildGraphResult.FAIL);   //Fail on resource leak.
            }

            //Try to generate a bijective mapping (proving behavioural equivalence),
            //and try the next viable node if no such mapping exists
            var parameters = findBijectiveMapping(network, otherNode.network);
            if (parameters == null)
                continue;

            //Check that the marking corresponds as well
            for (String processName : marking.keySet()){
                String otherName = parameters.getOrDefault(processName, processName);
                if (    !network.processes.get(processName).isTerminated() &&
                        marking.get(processName) != otherNode.marking.get(otherName)){
                    continue viableIterator;
                }
            }

            //The current network and state is equivalent to a previous node, so a loop can be formed, maybe.
            //Store the mapping to generate parameters for the choreography invocation
            label.becomes = parameters;
            //Try to add the loop to the graph.
            //Return BAD_LOOP if not every process reduced in the loop.
            if (addEdgeToGraph(currentNode, otherNode, label))
                return new extensionResult(otherNode, BuildGraphResult.OK);
            else
                return new extensionResult(otherNode, BuildGraphResult.BAD_LOOP);
        }

        //**A loop cannot be formed. Create a new node for the graph.**
        ConcreteNode newNode = createNode(network, label, currentNode, marking);
        addNodeAndEdgeToGraph(currentNode, newNode, label);
        //Try to expand the graph from the new node
        BuildGraphResult result = prospector.prospect(newNode);
        if (result != BuildGraphResult.OK)
            removeNodeFromGraph(newNode);

        return new extensionResult(newNode, result, true);
    }
    private record extensionResult(ConcreteNode targetNode, BuildGraphResult buildResult, boolean createdNewNode){
        extensionResult(ConcreteNode targetNode, BuildGraphResult buildResult){
            this(targetNode, buildResult, false);
        }
    }

    /* ============================
        Helper Functions
       ============================ */

    /**
     * Removes all nodes from the graph, which are in a sub-branch of the graph.
     * @param choicePathPrefix All nodes in the graph whose choice path begins with this string are removed.
     */
    private void removeGraphBranch(String choicePathPrefix){
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

            //perhaps check that all processes reduced here as an optimization?

            //Check for resource leak if othernode has fewer non-terminated processes than the current network.

            var parameters = findSurjectiveMapping(network, otherNode.network);
            if (parameters == null || parameters.size() != parameters.values().stream().distinct().count()){
                //The second half of the conditional ensures the mapping can be used with procedure invocation.
                //It works by ensuring the actual map is bijective.
                continue;
            }
            boolean fail = false;
            for (String processName : marking.keySet()){
                String otherName = parameters.getOrDefault(processName, processName); //Get the mapped value if it exists
                if (!network.processes.get(processName).isTerminated() && marking.get(processName) != otherNode.marking.get(otherName)){
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
     * Checks if a resource leak has happened in the network. Specifically checks for infinite spawning
     * of processes that doesn't terminate.<br>
     * Returns true if for every process in currentNetwork, a process with identical behaviour is in previousNetwork,
     * and currentNetwork has more processes than previousNetwork, ignoring terminated processes.
     */
    //I'm assuming here that the variables do not need to be chekced.
    private boolean detectResourceLeak(Network currentNetwork, Network previousNetwork){
        var currentNonTerminated = currentNetwork.processes.values().stream().filter(term -> !term.isTerminated()).toList();
        var previousNonTerminated = previousNetwork.processes.values().stream().filter(term -> !term.isTerminated()).toList();
        if (currentNonTerminated.size() <= previousNonTerminated.size())
            return false;
        var previousBehaviours = new HashSet<>(previousNonTerminated);
        for (var term : currentNonTerminated){
            if (!previousBehaviours.contains(term))
                return false;
        }
        return true;
    }

    /**
     * Calculates a mapping from process names in fromNetwork to process names in toNetwork, such that:<br>
     * 1. The mapping is bijective.<br>
     * 2. The processes have identical process terms.<br>
     * 3. If a process gets mapped to a process of a different name, then any variables in toNetwork that refers
     * to the process it maps to, refers to the remapped process in fromNetwork.<br>
     * The returned map only contains mappings between processes of different names. Processes not in the
     * returned map maps to themselves.
     * @return A mapping of process names, representing a renaming that allows a loop to close.
     */
    private Map<String, String> findBijectiveMapping(Network fromNetwork, Network toNetwork){
        Function<Map<String, ProcessTerm>, HashMap<String, ProcessTerm>> filterTerminated = original-> {
            var newMap = new HashMap<String, ProcessTerm>();
            original.forEach((name, term)->{
                if (!term.isTerminated())
                    newMap.put(name, term);
            });
            return newMap;
        };
        var fromProcesses = filterTerminated.apply(fromNetwork.processes);
        var toProcesses = filterTerminated.apply(toNetwork.processes);
        if (fromProcesses.size() != toProcesses.size())
            return null;    //There can be no bijective mapping between a different number of processes
        var unmatchedFrom = new HashSet<>(fromProcesses.keySet());
        var unmatchedTo = new HashSet<>(toProcesses.keySet());
        var map = new HashMap<String, String>();

        //Ignore variable names for now, then try to fix the mapping if the variable mappings do not match.

        //Find all processes that maps to themselves. This makes for fewer parameters in the final
        //choreography, and this is probably the most common case.
        toProcesses.forEach((name, toTerm) ->{
            var fromTerm = fromProcesses.get(name);
            if (fromTerm != null && toTerm.equals(fromTerm)){
                unmatchedFrom.remove(name);
                unmatchedTo.remove(name);
            }
        });
        //Match up the remaining processes
        for (var toName : unmatchedTo){
            var toTerm = toProcesses.get(toName);
            for (var fromName : unmatchedFrom){
                var fromTerm = fromProcesses.get(fromName);
                if (toTerm.equals(fromTerm)){
                    unmatchedFrom.remove(fromName);
                    map.put(fromName, toName);
                    break;
                }
            }
            //Check if the process could be matched. Fail if not
            if (!map.containsKey(toName))
                return null;
        }

        //Now there is a bijective mapping of processes with identical behaviour.
        //Next step is to check the variable bindings, and reshuffle the map if necessary.

        //Run through the variables of each process. If they change binding, then check the mapping is
        //consistent with the variable assignments.
        //TODO Create tests for every case. Ensure 100% code coverage for this function

        //Map, mapping process names in fromProcesses to a set of process names from toProcesses, such that
        //every name in the set was a mapping that turned out to be incompatible with the variable assignments.
        var badMappings = new HashMap<String, HashSet<String>>();

        //Every time the mapping is changed, this loop repeats.
        //If all variables corresponds to the mapping, the mapping is returned.
        //Otherwise, it changes the map (if possible), notes the bad mapping in badMappings, and repeats the loop.
        checkVariables:
        while (true) {
            //For every process in fromNetwork
            for (var fromEntry : fromProcesses.entrySet()) {
                //Get the processes that correspond to each other in the mapping, and their variables
                String fromName = fromEntry.getKey();
                ProcessTerm fromTerm = fromEntry.getValue();
                String toName = map.getOrDefault(fromName, fromName);
                ProcessTerm toTerm = toProcesses.get(toName);

                Map<String, String> fromVars = fromTerm.getVariables();
                Map<String, String> toVars = toTerm.getVariables();

                //For every process in the toProcesses
                //(the from-process may have strictly more variables, but the additional variables can be ignored, since they will be assigned before being used)
                for (String varName : toVars.keySet()) {
                    String toValue = toVars.get(varName);
                    String fromValue = fromVars.get(varName);
                    //If the variable corresponds to the remapping, check the next variable
                    if (map.getOrDefault(fromValue, fromValue).equals(toValue))
                        continue;
                        //Otherwise, if the variable is unchanged, it might not be used in the loop, and thus can be ignored
                    else if (fromValue.equals(toValue)) {
                        //Creates a set of all variable names used by the process since the start of the loop
                        //(Procedure parameter vars may be considered variable names when they shouldn't.
                        //See the function source for more information)
                        Set<String> usedVars = ProcessInteractionChecker.CheckUsedVariables(toTerm);
                        //If the problematic variable is not in the loop, its assignment does not affect correctness.
                        if (!usedVars.contains(varName))
                            continue;
                    }
                    //The variables do not agree with the mapping
                    //Try to fix the mapping by swapping entries if possible, and then check again.

                    //Update the set of invalid assignments for this process
                    HashSet<String> invalidAssignments = badMappings.computeIfAbsent(fromName, __ -> new HashSet<>());
                    invalidAssignments.add(toName);

                    //Look for a mapping that maps to a process with identical behaviour to toTerm
                    for (var swappableMapping : map.entrySet()) {
                        String altToName = swappableMapping.getValue();
                        String altFromName = swappableMapping.getKey();
                        ProcessTerm altToTerm = toProcesses.get(altToName);
                        //If the existing and alt mapping maps to processes with identical behaviour, and the target
                        //of the alt mapping has not previously been assigned the current process, then swap them,
                        //and re-check the variable assignments for the new mapping.
                        if (altToTerm.equals(fromTerm) && !invalidAssignments.contains(altToName)) {
                            map.put(fromName, altToName);
                            map.put(altFromName, toName);
                            continue checkVariables;   //Completely restart the variable check.
                        }
                    }
                    //It was not possible to get the variables to correspond to any mapping
                    //between the two networks. Return null to indicate no valid mapping exists.
                    return null;
                }
            }

            //There exists a bijective-mapping with the same (relevant) variable assignment. Success.
            return map;
        }
    }

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
        //I used to remove terminated processes here, but it lead to infinite graphs in some networks.
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

    /**
     * Calculates a hashcode of a network's process terms along with that process' marking.
     * If there are multiple identical terms, only one is used for the hash. The rest are skipped.
     */
    private Integer hashMarkedNetwork(Network n, HashMap<String, Boolean> marking){
        var unique = new HashSet<ProcessTerm>(n.processes.size());
        final int[] hash = {0};     //lambdas do not allow direct modifications of ints.
        n.processes.forEach((key, term) -> {
            if (unique.add(term) && !term.isTerminated())  //Returns false if the element is already in the set
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

    private void removeFromNodeHashes(ConcreteNode node){
        ArrayList<ConcreteNode> viableNodes = nodeHashes.getOrDefault(hashMarkedNetwork(node.network, node.marking), new ArrayList<>());
        for (var viableNode : viableNodes){
            if (viableNode.network.equals(node.network)){
                viableNodes.remove(viableNode);
                break;
            }
        }
        nodeHashes.remove(hashMarkedNetwork(node.network, node.marking));
    }

    private void removeFromChoicePathsMap(ConcreteNode node){
        var nodeList = choicePaths.get(node.choicePath);
        if (nodeList != null){
            nodeList.remove(node);
        }
    }


}
