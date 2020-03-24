package extraction;

import extraction.Node.ConcreteNode;
import network.*;
import org.jgrapht.graph.DirectedPseudograph;
import extraction.GraphBuilder.BuildGraphResult;

import java.util.*;

public class GraphExpander {
    private DirectedPseudograph<Node, Label> graph;
    private HashMap<String, ArrayList<ConcreteNode>> choicePaths = new HashMap<>();
    private HashMap<Integer, ArrayList<ConcreteNode>> nodeHashes = new HashMap<>();
    private Set<String> services;
    private GraphBuilder parent;
    private int nextNodeID;


    GraphExpander(Set<String> services, GraphBuilder parent, ConcreteNode rootNode){
        this.services = services;
        this.parent = parent;
        nextNodeID = rootNode.ID + 1;
        graph = new DirectedPseudograph<>(Label.class);

        graph.addVertex(rootNode);
        addToChoicePathsMap(rootNode);
        addToNodeHashes(rootNode);
    }

    DirectedPseudograph<Node, Label> getGraph(){
        return graph;
    }


    /* ====================================================================
        Code for building communications
       ==================================================================== */

    /**
     * Expands the graph with a new node, and an edge noting communication.
     * @param targetNetwork
     * @param label
     * @param currentNode
     * @return
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
                return BuildGraphResult.BADLOOP;

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
                return BuildGraphResult.BADLOOP;
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
                return BuildGraphResult.BADLOOP;
            }
        }

        return BuildGraphResult.OK;
    }

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


    /* ====================================================================
        Helper functions
       ==================================================================== */

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
        return false;
    }

    private boolean checkLoop(ConcreteNode source, ConcreteNode target, Label label){
        if (label.flipped)
            return true;
        if (target == source)
            return false;
        return !source.badNodes.contains(target.ID);
    }

    /**
     * If all processes in the Network of a node have been accounted for, the node can form a
     * good loop. To indicate this. all markings are removed. The Label is furthermore
     * "flipped" to indicate this.
     * @param l
     * @param marking
     * @param n
     */
    private void flipAndResetMarking(Label l, HashMap<String, Boolean> marking, Network n){
        l.flipped = true;
        for (String key : marking.keySet()){
            marking.put(key, isTerminated(n.processes.get(key).main, n.processes.get(key).procedures) || services.contains(key));
        }
        /*marking.replaceAll((processName, v) -> isTerminated(n.processes.get(processName).main, n.processes.get(processName).procedures)
                || services.contains(processName));*/
    }

    static boolean isTerminated(Behaviour b, HashMap<String, Behaviour> procedures){
        if (b.getAction() == Behaviour.Action.termination)
            return true;
        else if (b.getAction() == Behaviour.Action.procedureInvocation)
            return isTerminated(procedures.get(((ProcedureInvocation)b).procedure), procedures);
        return false;
    }

    /**
     * Searches the graph for a node with same Network, marking, and which begins with the same choicePath.
     * There should be at most one matching node, as it would otherwise not be added to the graph.
     * @param network The network we want to compare to
     * @param marking The marking we want to compare to
     * @param node The node with a choicePath beginning with the choicePath of the node we are searching for.
     * @return A matching concreteNode or null if none found.
     */
    private ConcreteNode findNodeInGraph(Network network, HashMap<String, Boolean> marking, ConcreteNode node){
        List<ConcreteNode> viableNodes = nodeHashes.get(hashMarkedNetwork(network, marking));
        if (viableNodes == null){
            return null;
        }
        for (ConcreteNode otherNode : viableNodes){
            boolean path = node.choicePath.startsWith(otherNode.choicePath);
            if (node.choicePath.startsWith(otherNode.choicePath) &&
                    otherNode.network.equals(network) &&
                    otherNode.marking.equals(marking))
                return otherNode;
        }
        return null;
    }

    private boolean equalMarking(HashMap<String, Boolean> firstMarking, HashMap<String, Boolean> secondMarking){
        for (String processName : firstMarking.keySet()){
            if (!firstMarking.get(processName).equals(secondMarking.get(processName)))
                return false;
        }
        return true;
    }

    /**
     * Creates a new ConcreteNode to add to the graph (this function will not do that for you)
     * @param network The nodes network
     * @param label The label intended for the edge to this node
     * @param predecessor The node that will have an edge to the new node
     * @param marking The process marking for this mode
     * @return
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
