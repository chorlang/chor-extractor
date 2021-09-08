package extraction;

import extraction.Node.ConcreteNode;
import extraction.network.*;
import org.jgrapht.graph.DirectedPseudograph;
import extraction.GraphBuilder.BuildGraphResult;

import java.util.*;

/**
 * This class is highly coupled with GraphBuilder, and should not be interfaced with by anything else. Always interface with GraphBuilder instead.
 *
 * This class creates, contains, and modifies the extraction.network graph, so that GraphBuilder can specialize in prospecting, and abstractly manage the graph building process.
 */
public class GraphExpander {
    private DirectedPseudograph<Node, Label> graph;
    private HashMap<String, ArrayList<ConcreteNode>> choicePaths = new HashMap<>();
    private HashMap<Integer, ArrayList<ConcreteNode>> nodeHashes = new HashMap<>();
    private Set<String> services;
    private GraphBuilder parent;
    private int nextNodeID;
    int badLoopCounter = 0;


    /**
     * GraphExpander is solely intended as an auxiliary for GraphBuilder.
     * This class should not be instantiated outside of GraphBuilder.
     * Instantiating GraphBuilder internally creates a GraphExpander instance.
     * @param services The names of all processes which are services. Services are allowed to be livelocked.
     * @param parent The GraphBuilder instance which uses this instance. Parse <i>this</i> when instantiating from GraphBuilder.
     * @param rootNode The Node containing the initial extraction.network.
     */
    GraphExpander(Set<String> services, GraphBuilder parent, ConcreteNode rootNode){
        this.services = services;
        this.parent = parent;
        nextNodeID = rootNode.ID + 1;
        graph = new DirectedPseudograph<>(Label.class);

        graph.addVertex(rootNode);
        addToChoicePathsMap(rootNode);
        addToNodeHashes(rootNode);
    }

    /**
     * Returns a reference to the graph stored within this instance. Intended to use after graph generation has completed, and should not be modified before then.
     * @return Reference to internally stored graph.
     */
    DirectedPseudograph<Node, Label> getGraph(){
        return graph;
    }


    /* ====================================================================
        Code for building communications
       ==================================================================== */

    /**
     * Expands the graph with a communication, storing label in the created edge.
     * It creates a marking, targetMarking, which is a copy of currentNode's marking, but where the label's sender and receiver are marked,
     * and flips the marking if all processes are marked. It then tries to find a node already in the graph that has the same
     * Network as targetNetwork, marking as targetMarking, and which begins with the same choicePath as currentNode.
     * If successful, it adds an edge between the two nodes. Otherwise, it creates a new ConcreteNode, and add that
     * as well as a new edge to the graph.
     *
     * If a new node was successfully created, buildGraph is called on that node, and its return value is this methods return value.
     *
     * @param targetNetwork The extraction.network resulting from the interaction.
     * @param label The label denoting the interaction, and to be stored in the new edge.
     * @param currentNode The node that the new edge originates from, and that contains a previous state of the extraction.network.
     * @return OK if a new edge and/or node was added; otherwise, the graph is unchanged. BADLOOP if the new edge and/or node results in a bad loop, and the algorithm must backtrack. FAIL if a choreography cannot be extracted.
     */
    BuildGraphResult buildCommunication(Network targetNetwork, Label.InteractionLabel label, ConcreteNode currentNode){
        //Mark involved processes, and flip the marking if everything is marked
        HashMap<String, Boolean> targetMarking = new HashMap<>(currentNode.marking);
        targetMarking.put(label.sender, true);
        targetMarking.put(label.receiver, true);
        if (!targetMarking.containsValue(false)){
            flipAndResetMarking(label, targetMarking, targetNetwork);
        }

        //If the node already exists in the graph, add a new edge.
        //If that fails, the edge creates a bad loop, and the algorithm must backtrack.
        ConcreteNode node = findNodeInGraph(targetNetwork, targetMarking, currentNode);
        if (node != null){
            if (addEdgeToGraph(currentNode, node, label))
                return BuildGraphResult.OK;
            else
                return BuildGraphResult.BAD_LOOP;

        }

        //Create a new node, and add it, and an edge to the graph.
        //If it fails, remove the node before returning.
        node = createNode(targetNetwork, label, currentNode, targetMarking);
        addNodeAndEdgeToGraph(currentNode, node, label);
        BuildGraphResult result = parent.buildGraph(node);
        if (result != BuildGraphResult.OK)
            removeNodeFromGraph(node);
        return result;
    }


    /* ====================================================================
        Code for building conditionals
       ==================================================================== */

    /**
     * Expands the graph with a conditional, storing the respective labels in the two new edges respectively.
     * It creates a marking, targetMarking, which is a copy of currentNode's marking, but where the thenLabel's process is marked,
     * and flips the marking if all processes are marked. It then tries to find a node already in the graph that has the same
     * Network as thenNetwork, marking as targetMarking, and which begins with the same choicePath as currentNode.
     * If successful, it adds an edge between the two nodes. Otherwise, it creates a new ConcreteNode, and add that
     * as well as a new edge to the graph.
     *
     * If a new node was created, it calls buildGraph on it. If buildGraph returns OK, it tries to find a node for the elseNetwork
     * and continues on like with the thenNetwork, except it uses the same targetMarking.
     * If this also creates a new node, buildGraph is called on that too.
     *
     * If at any point the conditional cannot be build, either in this method, or because buildGraph do not return OK,
     * then the graph is reverted to be identical to wat it was before this method call.
     *
     * @param thenNetwork The extraction.network resulting from the then case.
     * @param thenLabel The label denoting the expression, and to be stored in the then edge.
     * @param elseNetwork The extraction.network resulting from the else case.
     * @param elseLabel The label denoting the expression, and to be stored in the else edge.
     * @param currentNode The node that the two new edges originate from, and that contains the previous state of the extraction.network, before the condition is evaluated.
     * @return OK if both edges and/or node(s) was added; otherwise the graph is unchanged. BADLOOP if adding either edges would result in a bad loop, and the algorithm must backtrack. FAIL if a choreography cannot be extracted.
     */
    BuildGraphResult buildConditional(Network thenNetwork, Label.ConditionLabel.ThenLabel thenLabel,
                                              Network elseNetwork, Label.ConditionLabel.ElseLabel elseLabel, ConcreteNode currentNode){
        var targetMarking = new HashMap<>(currentNode.marking);
        targetMarking.replace(thenLabel.process, true);
        if (!targetMarking.containsValue(false)){
            flipAndResetMarking(thenLabel, targetMarking, thenNetwork);
            flipAndResetMarking(elseLabel, targetMarking, elseNetwork);
        }

        var thenNode = findNodeInGraph(thenNetwork, targetMarking, currentNode);
        boolean createdNewThenNode = false;
        if (thenNode == null){
            createdNewThenNode = true;
            thenNode = createNode(thenNetwork, thenLabel, currentNode, targetMarking);
            addNodeAndEdgeToGraph(currentNode, thenNode,thenLabel);

            var result = parent.buildGraph(thenNode);
            if (result != BuildGraphResult.OK){
                removeNodeFromGraph(thenNode);
                return result;
            }
        }
        else{
            if (!addEdgeToGraph(currentNode, thenNode, thenLabel))
                return BuildGraphResult.BAD_LOOP;
        }



        var elseNode = findNodeInGraph(elseNetwork, targetMarking, currentNode);

        if (elseNode == null){
            elseNode = createNode(elseNetwork, elseLabel, currentNode, targetMarking);
            addNodeAndEdgeToGraph(currentNode, elseNode, elseLabel);

            var result = parent.buildGraph(elseNode);
            if (result != BuildGraphResult.OK){
                if (createdNewThenNode)
                    removeNodesFromGraph(thenNode.choicePath);
                else
                    graph.removeEdge(currentNode, thenNode);
                removeNodeFromGraph(elseNode);
                return result;
            }
        }
        else{
            if (!addEdgeToGraph(currentNode, elseNode, elseLabel)){
                if (createdNewThenNode)
                    removeNodesFromGraph(thenNode.choicePath);
                else
                    graph.removeEdge(currentNode, thenNode);
                return BuildGraphResult.BAD_LOOP;
            }
        }

        return BuildGraphResult.OK;
    }


    /* ====================================================================
        Code for building multicoms
       ==================================================================== */

    /**
     * Expands the graph with a multicom action, and then continues to build it recursively.
     * Marks all processes mentioned in the label, and resets the marking if all processes becomes marked.
     * If a node storing a network and marking identical to targetNetwork already exists, a new edge is added
     * to that node, using the label. If that would result in an invalid loop, the edge is not added, and the function
     * returns BAD_LOOP instead.
     * If no such node already exists, it creates a new one, and calls buildGraph() on that node. If the call does not
     * return OK, it removes the node (and its children) from the graph. In any case, it returns the result of the
     * call to buildGraph-
     * @param targetNetwork The network resulting from performing the multicom when in the network of currentNode
     * @param label The multicom label to store on the graph edge that will be added to the graph
     * @param currentNode The node of the graph, represetning the state of the network before this multicom
     * @return OK if the graph was extended. BAD_LOOP if it was not possible to expand on this multicom, or FAIL if the network is unextractable
     */
    BuildGraphResult buildMulticom(Network targetNetwork, Label.MulticomLabel label, ConcreteNode currentNode){
        //Mark involved processes, and flip the marking if everything is marked
        HashMap<String, Boolean> targetMarking = new HashMap<>(currentNode.marking);
        label.communications.forEach(com -> {targetMarking.put(com.receiver, true); targetMarking.put(com.sender, true);});
        if (!targetMarking.containsValue(false)){
            flipAndResetMarking(label, targetMarking, targetNetwork);
        }

        //If the node already exists in the graph, add a new edge.
        //If that fails, the edge creates a bad loop, and the algorithm must backtrack.
        ConcreteNode node = findNodeInGraph(targetNetwork, targetMarking, currentNode);
        if (node != null){
            if (addEdgeToGraph(currentNode, node, label))
                return BuildGraphResult.OK;
            else
                return BuildGraphResult.BAD_LOOP;

        }

        //Create a new node, and add it, and an edge to the graph.
        //If it fails, remove the node before returning.
        node = createNode(targetNetwork, label, currentNode, targetMarking);
        addNodeAndEdgeToGraph(currentNode, node, label);
        BuildGraphResult result = parent.buildGraph(node);
        if (result != BuildGraphResult.OK)
            removeNodeFromGraph(node);
        return result;
    }

    /* ====================================================================
        Helper functions
       ==================================================================== */

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

    private void removeFromNodeHashes(ConcreteNode node){
        nodeHashes.remove(hashMarkedNetwork(node.network, node.marking));
    }

    private boolean removeFromChoicePathsMap(ConcreteNode node){
        var nodeList = choicePaths.get(node.choicePath);
        if (nodeList != null){
            return nodeList.remove(node);
        }
        return false;
    }

    private boolean addNodeAndEdgeToGraph(ConcreteNode currentNode, ConcreteNode newNode, Label label){
        if (graph.addVertex(newNode)){
            if (graph.addEdge(currentNode, newNode, label)){
                addToChoicePathsMap(newNode);
                addToNodeHashes(newNode);
                return true;
            }
            else
                graph.removeVertex(newNode);
        }
        return false;
    }

    private boolean addEdgeToGraph(ConcreteNode source, ConcreteNode target, Label label){
        if (checkLoop(source, target, label))
            return graph.addEdge(source, target, label);
        badLoopCounter++;
        return false;
    }

    /**
     * Checks if adding a new edge from source to target with the label would result in a bad loop.
     * @param source The hypothetical edge source.
     * @param target The hypothetical edge target.
     * @param label The label that is to be stored in the hypothetical edge.
     * @return true, if the label is flipped, or the target node is not one of source's bad nodes.
     */
    private boolean checkLoop(ConcreteNode source, ConcreteNode target, Label label){
        if (label.flipped)
            return true;
        if (target == source)
            return false;
        return !source.badNodes.contains(target.ID);
    }

    /**
     * When all processes have been involved in at least one graph expansion, (all are marked), the graph can form a valid loop.
     * This function marks the label as flipped (in a valid loop), and resets the marking to only be true for terminated or livelocked processes (services).
     * @param l The label to flip.
     * @param marking The marking to reset.
     * @param n The extraction.network containing information needed to expand ProcedureInvocations to see if the eventually lead to Termination.
     */
    private void flipAndResetMarking(Label l, HashMap<String, Boolean> marking, Network n){
        l.flipped = true;
        /*for (String key : marking.keySet()){
            marking.put(key, isTerminated(n.processes.get(key).main, n.processes.get(key).procedures) || services.contains(key));
        }*/
        marking.replaceAll((processName, __) -> isTerminated(n.processes.get(processName).main, n.processes.get(processName).procedures)
                || services.contains(processName));
    }

    /**
     * Checks if the Behaviour is Termination, or an ProcedureInvocation that expands to Termination.
     * @param b The Behaviour to check.
     * @param procedures The procedures in the extraction.network, in case the Behaviour is ProcedureInvocation, and it needs to be expanded (recursively) until it reach Termination or something else.
     * @return true if the Behaviour is Termination, or expands into Termination.
     */
    static boolean isTerminated(Behaviour b, HashMap<String, Behaviour> procedures){
        if (b.getAction() == Behaviour.Action.TERMINATION)
            return true;
        else if (b.getAction() == Behaviour.Action.PROCEDURE_INVOCATION)
            return isTerminated(procedures.get(((ProcedureInvocation)b).procedure), procedures);
        return false;
    }

    /**
     * Searches the graph for a node with same Network, marking, and which begins with the same choicePath as the node, of the parameters.
     * There should be at most one matching node, as it would otherwise not be added to the graph.
     * @param network The Network the matching node should have.
     * @param marking The marking the matching node should have
     * @param node The node with a choicePath beginning with the choicePath of the node we are searching for.
     * @return A matching ConcreteNode or null if none found.
     */
    private ConcreteNode findNodeInGraph(Network network, HashMap<String, Boolean> marking, ConcreteNode node){
        List<ConcreteNode> viableNodes = nodeHashes.get(hashMarkedNetwork(network, marking));
        if (viableNodes == null){
            return null;
        }
        for (ConcreteNode otherNode : viableNodes){
            if (node.choicePath.startsWith(otherNode.choicePath) &&
                    otherNode.network.equals(network) &&
                    otherNode.marking.equals(marking))
                return otherNode;
        }
        return null;
    }

    /**
     * Creates a new ConcreteNode to add to the graph (this method will not do that for you)
     * @param network The node's extraction.network
     * @param label The label intended for the edge going to this node
     * @param predecessor The node that will have an edge to the new node
     * @param marking The process marking for this mode
     * @return A new ConcreteNode instance
     */
    private ConcreteNode createNode(Network network, Label label, ConcreteNode predecessor, HashMap<String, Boolean> marking){
        String choicePath = predecessor.choicePath;
        if (label.labelType == Label.LabelType.THEN)
            choicePath += "1";
        else if (label.labelType == Label.LabelType.ELSE)
            choicePath += "0";

        HashSet<Integer> badNodes = new HashSet<>();
        if (!label.flipped){
            badNodes.addAll(predecessor.badNodes);
            badNodes.add(predecessor.ID);
        }

        return new ConcreteNode(network, choicePath, nextNodeID(), badNodes, marking);
    }

    private int nextNodeID(){
        return nextNodeID++;
    }

    private void addToChoicePathsMap(ConcreteNode node){
        ArrayList<ConcreteNode> nodesWithPath = choicePaths.computeIfAbsent(node.choicePath, k -> new ArrayList<>());
        nodesWithPath.add(node);
    }

    private Integer hashMarkedNetwork(Network n, HashMap<String, Boolean> marking){
        return n.hashCode() + 31 * marking.hashCode();
    }

    private void addToNodeHashes(ConcreteNode node){
        Integer hash = hashMarkedNetwork(node.network, node.marking);
        ArrayList<ConcreteNode> nodesWithHash = nodeHashes.computeIfAbsent(hash, k -> new ArrayList<>());
        nodesWithHash.add(node);
    }
}
