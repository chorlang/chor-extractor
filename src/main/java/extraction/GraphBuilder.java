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
    private final HashMap<String, ConcreteNode> conditionalAncestry = new HashMap<>();
    private final Set<String> services;
    private int badLoopCounter = 0;//Currently broken, since I'm unsure what counts at attempting to form a loop anymore.
    private int nextNodeID = 0;

    private GraphBuilder(Strategy extractionStrategy, Set<String> services){
        prospector = new Prospector(extractionStrategy, this);
        this.services = services;
    }

    public record SEGContainer (DirectedPseudograph<Node, Label> graph, ConcreteNode rootNode,
                                       BuildGraphResult buildGraphResult, int badLoopCounter) {}

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

        //currentNode is a conditional node, and should be added to the map from choice paths
        //to the conditional unique to that path.
        conditionalAncestry.put(currentNode.choicePath, currentNode);

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

    /**
     * Attempts to do one of the following:<br>
     * 1. Adds an edge from currentNode to a different existing node in the graph, such that the existing node
     * contains the same marking and equivalent network to the marking and network parameters. Then
     * returns and object with the found node, and the BuildGraphResult OK<br>
     * 2. Attempts 1, but finds that such and edge would create and invalid loop. Returns an object
     * containing the BuildGraphResult BAD_LOOP.<br>
     * 3. Adds a new node to the graph with the provided network and marking, and then adds an edge from currentNode to
     * that new node. Then attempts to build the graph out from that new node. Returns an object with the new node,
     * the result of building the graph out from that node, and boolean being true to indicate a new node was added.<br>
     *
     * The provided label is stored in the new edge in all cases. The returned object may also contain
     * the BuildGraphResult FAIL in case it detects a resource leak.
     *
     * @param network The network to either add to a new node, or check a node with an equivalent network already exists.
     * @param marking The marking of the network.
     * @param label The label to store in the created edge.
     * @param currentNode The node previously added to the graph, which is the origin of the new edge.
     * @return An object denoting the target node of the new edge, the success of extending the graph, and a
     * boolean indicating if a new node was added to the graph or not.
     */
    private extensionResult extendGraph(Network network, HashMap<String, Boolean> marking, Label label, ConcreteNode currentNode){
        //**Try to see if a loop can be formed**

        //Calculate the flip counter of the hypothetical next node.
        int flipCounter = currentNode.flipCounter + (label.flipped ? 1 : 0);

        //Get a list of nodes with the same network and marking hash, and the same choicePath.
        List<ConcreteNode> viableNodes = nodeHashes.getOrDefault(hashMarkedNetwork(network, marking), new ArrayList<>());//TODO remove marking

        //We do not want to add an edge to a terminated node. It created ugly choreographies.
        if (network.allTerminated())
            viableNodes = List.of();

        //Iterate though the nodes with the same hash, and see if they have equivalent behaviour.
        for (ConcreteNode otherNode : viableNodes){

            if (currentNode.choicePath.startsWith(otherNode.choicePath) && flipCounter > otherNode.flipCounter && detectResourceLeak(network, otherNode.network)) {
                System.err.println("Resource leak detected. Extraction not possible");
                return new extensionResult(null, BuildGraphResult.FAIL);   //Fail on resource leak.
            }

            //Try to generate a bijective mapping (proving behavioural equivalence),
            //and try the next viable node if no such mapping exists
            var parameters = findBijectiveMapping(network, otherNode.network);
            if (parameters == null)
                continue;

            //Compare markings
            if (marking.keySet().stream().filter(pname -> !network.processes.get(pname).isTerminated()).anyMatch(
                    pname -> !marking.get(pname) &&//If current marking is false, but the other nodes marking is true, then the process will not reduce in a loop.
                            otherNode.marking.get(parameters.getOrDefault(pname,pname))))
                continue;   //Markings are incompatible, try the next viable node

            //The current network and state is equivalent to a previous node, so a loop can be formed, maybe.
            //Store the mapping to generate parameters for the choreography invocation
            label.becomes = parameters;
            //Try to add the loop to the graph.
            //Return BAD_LOOP if not every process reduced in the loop.
            if (addEdgeToGraph(currentNode, otherNode, label))
                return new extensionResult(otherNode, BuildGraphResult.OK);
            //if otherNode is a build-ancestor, this is a bad loop
            else if (currentNode.choicePath.startsWith(otherNode.choicePath)) {
                return new extensionResult(otherNode, BuildGraphResult.BAD_LOOP);
            }
            //If otherNode is of a different branch, then this is not a bad loop
        }

        //**A loop cannot be formed. Create a new node for the graph.**
        ConcreteNode newNode = createNode(network, label, currentNode, marking);
        label.becomes = Map.of();//Reset in case it was set before a failed edge creation.
        addNodeAndEdgeToGraph(currentNode, newNode, label);
        //Try to expand the graph from the new node
        BuildGraphResult result = prospector.prospect(newNode);
        if (result != BuildGraphResult.OK)//TODO Probably just need to be equal to BAD_LOOP
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
     * Checks if a resource leak has happened in the network. Specifically checks for infinite spawning
     * of processes that doesn't terminate.<br>
     * Returns true if for every process in currentNetwork, a process with identical behaviour is in previousNetwork,
     * and currentNetwork has more processes than previousNetwork, ignoring terminated processes.
     */
    //I'm assuming here that the variables do not need to be checked. Honestly they probably do :(
    //Maybe merge it with findBijectiveMapping?
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
        return true;//Should all processes of prevNetwork be in currentNetwork also?
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
        for (var fromName : unmatchedFrom){
            var fromTerm = fromProcesses.get(fromName);
            for (var toName : unmatchedTo){
                var toTerm = toProcesses.get(toName);
                if (toTerm.equals(fromTerm)){
                    unmatchedTo.remove(toName);
                    map.put(fromName, toName);
                    break;
                }
            }
            //Check if the process could be matched. Fail if not
            if (!map.containsKey(fromName))
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
                    //else if (fromValue.equals(toValue)) {
                        //Creates a set of all variable names used by the process since the start of the loop
                        //(Procedure parameter vars may be considered variable names when they shouldn't.
                        //See the function source for more information)
                        Set<String> usedVars = ProcessInteractionChecker.CheckUsedVariables(toTerm);
                        //If the problematic variable is not in the loop, its assignment does not affect correctness.
                        if (!usedVars.contains(varName))
                            continue;
                    //}
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
                        //If the existing and alt mapping, maps to processes with identical behaviour, and the target
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
     * Checks if adding a new edge from source to target with the label would result in a bad loop.
     * @param source The hypothetical edge source.
     * @param target The hypothetical edge target.
     * @param label The label that is to be stored in the hypothetical edge.
     * @return true, if the label is flipped, or the target.flipCounter < source.flipCounter
     */
    private boolean checkLoop(ConcreteNode source, ConcreteNode target, Label label){
        if (label.flipped) {
            return true;
        }
        if (target == source) {
            return false;
        }
        ConcreteNode choiceAncestor = getLowestCommonChoiceAncestor(source, target);
        return source.flipCounter > choiceAncestor.flipCounter;
    }

    /**
     * Finds and returns the lowest common choice ancestor node in the graph, of the two nodes provided.
     * It is assumed lower is NOT a build-ancestor of higher (Meaning higher must have been
     * added to the graph first).
     * If higher is a build-ancestor of lower, then higher is returned. Otherwise, the lowest
     * conditional node that is a build ancestor of both nodes is returned.
     */
    private ConcreteNode getLowestCommonChoiceAncestor(ConcreteNode lower, ConcreteNode higher){
        if (lower.choicePath.startsWith(higher.choicePath))
            return higher;
        int shortest = Math.min(lower.choicePath.length(), higher.choicePath.length());
        int i = 0;
        for (; i < shortest; i++){
            if (lower.choicePath.charAt(i) != higher.choicePath.charAt(i))
                break;
        }//All choice-paths start with "-", so i should be at least 1.
        return conditionalAncestry.get(lower.choicePath.substring(0, i));
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
    private static Integer hashMarkedNetwork(Network n, HashMap<String, Boolean> marking){
        var unique = new HashSet<ProcessTerm>(n.processes.size());
        final int[] hash = {0};     //lambdas do not allow direct modifications of ints.
        n.processes.forEach((key, term) -> {
            if (unique.add(term) && !term.isTerminated())  //Returns false if the element is already in the set
                hash[0] += term.hashCode() * 31 ;//+ marking.get(key).hashCode();
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
        //nodeHashes.remove(hashMarkedNetwork(node.network, node.marking));
    }

    private void removeFromChoicePathsMap(ConcreteNode node){
        var nodeList = choicePaths.get(node.choicePath);
        if (nodeList != null){
            nodeList.remove(node);
        }
    }


}
