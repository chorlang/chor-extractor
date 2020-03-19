package extraction;

import network.*;
import extraction.Node.*;
import org.jgrapht.graph.DirectedPseudograph;

import java.util.*;

public class GraphBuilder {
    private Strategy strategy;
    private DirectedPseudograph<Node, Label> graph;
    private HashMap<String, ArrayList<ConcreteNode>> choicePaths = new HashMap<>();
    private HashMap<Integer, ArrayList<ConcreteNode>> nodeHashes = new HashMap<>();
    private Set<String> services;

    private enum BuildGraphResult{
        OK, BADLOOP, FAIL
    }

    public GraphBuilder(Strategy extractionStrategy){
        strategy = extractionStrategy;
    }

    DirectedPseudograph<Node, Label> makeGraph(Network n){
        graph = new DirectedPseudograph<>(Label.class);
        HashMap<String, Boolean> marking = new HashMap<>();
        ConcreteNode node = new ConcreteNode(n,"0", nextNodeID(), new HashSet<>(), marking);
        graph.addVertex(node);
        addToChoicePaths(node);

        BuildGraphResult buildResult = buildGraph(node);


        return graph;
    }

    private BuildGraphResult buildGraph(ConcreteNode currentNode){
        HashSet<String> unfoldedProcesses = new HashSet<>();
        LinkedHashMap<String, ProcessTerm> processes = copyAndSortProcesses(currentNode);

        processes.forEach((processName, processTerm) -> {
            if (unfold(processTerm))
                unfoldedProcesses.add(processName);

        });


        for (Map.Entry<String, ProcessTerm> entry : processes.entrySet()){
            String processName = entry.getKey();
            ProcessTerm processTerm = entry.getValue();
            HashSet<String> unfoldedProcessesCopy = new HashSet<>(unfoldedProcesses);


            CommunicationContainer communication = findCommunication(processes, processName, processTerm);
            if (communication != null){
                Network targetNetwork = communication.targetNetwork;
                var label = communication.label;
                unfoldedProcessesCopy.remove(label.sender);
                unfoldedProcessesCopy.remove(label.receiver);
                fold(unfoldedProcessesCopy, targetNetwork, currentNode);

                var result = buildCommunication(targetNetwork, label, currentNode);
                if (result == BuildGraphResult.BADLOOP)
                    continue;
                return result;
            }


            ConditionContainer conditional = findConditional(processes, processName, processTerm);
            if (conditional != null){
                Network thenNetwork = conditional.thenNetwork;
                Network elseNetwork = conditional.elseNetwork;
                var thenLabel = conditional.thenLabel;
                var elseLabel = conditional.elseLabel;

                unfoldedProcessesCopy.remove(thenLabel.process);
                fold(unfoldedProcessesCopy, thenNetwork, currentNode);
                fold(unfoldedProcessesCopy, elseNetwork, currentNode);

                var result = buildConditional(thenNetwork, thenLabel, elseNetwork, elseLabel, currentNode);
                if (result == BuildGraphResult.BADLOOP)
                    continue;
                return result;
            }
        }
        if (allTerminated(processes))
            return BuildGraphResult.OK;

        return BuildGraphResult.FAIL;
    }

    /**
     * Expands the graph with a new node, and an edge noting communication.
     * @param targetNetwork
     * @param label
     * @param currentNode
     * @return
     */
    private BuildGraphResult buildCommunication(Network targetNetwork, Label.InteractionLabel label, ConcreteNode currentNode){
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
        BuildGraphResult result = buildGraph(node);
        if (result != BuildGraphResult.OK)
            removeNodeFromGraph(node);
        return result;
    }



    /* ==================
        Helper functions
       ================== */

    private BuildGraphResult buildConditional(Network thenNetwork, Label.ConditionLabel.ThenLabel thenLabel,
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

            var result = buildGraph(thenNode);
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

            var result = buildGraph(elseNode);
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

    private static class ConditionContainer{
        public Network thenNetwork, elseNetwork;
        public Label.ConditionLabel.ThenLabel thenLabel;
        public Label.ConditionLabel.ElseLabel elseLabel;
        public ConditionContainer(Network thenNetwork, Label.ConditionLabel.ThenLabel thenLabel, Network elseNetwork, Label.ConditionLabel.ElseLabel elseLabel){
            this.thenNetwork = thenNetwork;
            this.thenLabel = thenLabel;
            this.elseNetwork = elseNetwork;
            this.elseLabel = elseLabel;
        }
    }

    private ConditionContainer findConditional(HashMap<String, ProcessTerm> processes, String processName, ProcessTerm processTerm){
        if (processTerm.main.getAction() != Behaviour.Action.condition)
            return null;

        var thenProcessMap = new HashMap<>(processes);
        var elseProcessMap = new HashMap<>(processes);

        Condition conditional = (Condition)processTerm.main;
        thenProcessMap.replace(processName, new ProcessTerm(processTerm.procedures, conditional.thenBehaviour));
        elseProcessMap.replace(processName, new ProcessTerm(processTerm.procedures, conditional.elseBehaviour));

        return new ConditionContainer(
                new Network(thenProcessMap),
                new Label.ConditionLabel.ThenLabel(processName, conditional.expression),
                new Network(elseProcessMap),
                new Label.ConditionLabel.ElseLabel(processName, conditional.expression)
        );
    }

    private boolean allTerminated(HashMap<String, ProcessTerm> network){
        for (ProcessTerm process : network.values()) {
            if (process.main.getAction() != Behaviour.Action.termination)
                return false;
        }
        return true;
    }

    private CommunicationContainer findCommunication(HashMap<String, ProcessTerm> processes, String processName, ProcessTerm processTerm){
        Behaviour main = processTerm.main;
        switch (main.getAction()){
            case send:
                String recipientProcessName = ((Send)main).receiver;
                ProcessTerm receiveTerm  = processes.get(recipientProcessName);
                if (receiveTerm.main.getAction() == Behaviour.Action.receive &&
                        ((Receive)receiveTerm.main).sender.equals(processName)){
                    return consumeCommunication(processes, processTerm, receiveTerm);
                }
                break;
            case receive:
                String sendingProcessName = ((Receive)main).sender;
                ProcessTerm senderTerm = processes.get(sendingProcessName);
                if (senderTerm.main.getAction() == Behaviour.Action.send &&
                        ((Send)senderTerm.main).receiver.equals(processName)){
                    return consumeCommunication(processes, senderTerm, processTerm);
                }
                break;
            case selection:
                String offeringProcessName = ((Selection)main).receiver;
                ProcessTerm offerTerm = processes.get(offeringProcessName);
                if (offerTerm.main.getAction() == Behaviour.Action.offering &&
                        ((Offering)offerTerm.main).sender.equals(processName)){
                    return consumeSelection(processes, offerTerm, processTerm);
                }
                break;
            case offering:
                String selectingProcessName = ((Offering)main).sender;
                ProcessTerm selectionTerm = processes.get(selectingProcessName);
                if (selectionTerm.main.getAction() == Behaviour.Action.selection &&
                    ((Selection)selectionTerm.main).receiver.equals(processName)){
                    return consumeSelection(processes, processTerm, selectionTerm);
                }
                break;
        }
        return null;
    }

    private CommunicationContainer consumeCommunication(HashMap<String, ProcessTerm> processes, ProcessTerm sendTerm, ProcessTerm receiveTerm){
        var processesCopy = copyProcesses(processes);
        Send sender = (Send)sendTerm.main;
        Receive receiver = (Receive)receiveTerm.main;

        processesCopy.replace(receiver.sender, new ProcessTerm(sendTerm.procedures, sender.continuation));
        processesCopy.replace(sender.receiver, new ProcessTerm(receiveTerm.procedures, receiver.continuation));

        var label = new Label.InteractionLabel.CommunicationLabel(receiver.sender, sender.receiver, sender.expression);

        return new CommunicationContainer(new Network(processesCopy), label);
    }

    private CommunicationContainer consumeSelection(HashMap<String, ProcessTerm> processes, ProcessTerm offerTerm, ProcessTerm selectTerm){
        var processesCopy = copyProcesses(processes);
        Selection selector = (Selection)offerTerm.main;
        Offering offer = (Offering)selectTerm.main;

        Behaviour offeringBehaviour = offer.branches.get(selector.label);
        if (offeringBehaviour == null)
            return null;

        processesCopy.replace(selector.receiver, new ProcessTerm(offerTerm.procedures, offeringBehaviour));
        processesCopy.replace(offer.sender, new ProcessTerm(selectTerm.procedures, selector.continuation));

        var label = new Label.InteractionLabel.SelectionLabel(selector.receiver, offer.sender, selector.label);

        return new CommunicationContainer(new Network(processesCopy), label);
    }

    private HashMap<String, ProcessTerm> copyProcesses(HashMap<String, ProcessTerm> processes){
        HashMap<String, ProcessTerm> copy = new HashMap<>(processes.size());
        processes.forEach((processName, processTerm) -> {
            copy.put(processName, processTerm.copy());
        });
        return copy;
    }

    private static class CommunicationContainer{
        Network targetNetwork;
        Label.InteractionLabel label;
        public CommunicationContainer(Network n, Label.InteractionLabel l){
            targetNetwork = n;
            label = l;
        }
    }


    private void removeNodeFromGraph(ConcreteNode node){
        graph.removeVertex(node);
        removeFromNodeHashes(node);
        removeFromChoicePathMap(node);
    }

    private void removeFromNodeHashes(ConcreteNode node){
        nodeHashes.remove(hashMarkedNetwork(node.network, node.marking));
    }

    private boolean removeFromChoicePathMap(ConcreteNode node){
        var nodeList = choicePaths.get(node.choicePath);
        if (nodeList != null){
            return nodeList.remove(node);
        }
        return false;
    }

    private boolean addNodeAndEdgeToGraph(ConcreteNode currentNode, ConcreteNode newNode, Label label){
        if (graph.addVertex(currentNode)){
            if (graph.addEdge(currentNode, newNode, label)){
                addToChoicePaths(newNode);
                addToNodeHashes(newNode);
                return true;
            }
            else
                graph.removeVertex(newNode);
        }
        return false;
    }

    private boolean addEdgeToGraph(ConcreteNode currentNode, ConcreteNode newNode, Label label){
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
        marking.replaceAll((processName, v) -> isTerminated(n.processes.get(processName).main, n.processes.get(processName).procedures)
                || services.contains(processName));
    }

    private boolean isTerminated(Behaviour b, HashMap<String, Behaviour> procedures){
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
        return viableNodes.stream().filter(otherNode ->
                node.choicePath.startsWith(otherNode.choicePath) &&
                        otherNode.network.equals(network) &&
                        otherNode.marking.equals(marking)).findFirst().orElse(null);
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

    private boolean unfold(ProcessTerm processTerm){
        Behaviour main = processTerm.main;

        if (main.getAction() != Behaviour.Action.procedureInvocation)
            return false;

        ProcedureInvocation mainProcedure = (ProcedureInvocation)main;
        String procedureName = mainProcedure.procedure;
        Behaviour procedureBehaviour = processTerm.procedures.get(procedureName);
        if (procedureBehaviour == null){
            System.out.println("Cannot unfold the process. Procedure definition do not exists");
            System.exit(1);
        }
        processTerm.main = procedureBehaviour.copy();

        if (procedureBehaviour.getAction() == Behaviour.Action.procedureInvocation){
            unfold(processTerm);
        }
        return true;

    }

    private void fold(HashSet<String> unfoldedProcesses, Network targetNetwork, ConcreteNode node){
        unfoldedProcesses.forEach(process -> targetNetwork.processes.get(process).main = node.network.processes.get(process).main.copy());
    }

    private LinkedHashMap<String, ProcessTerm> copyAndSortProcesses(Node.ConcreteNode node){
        return strategy.copyAndSort(node.network.processes);
    }

    private int nextNodeID = 0;
    private int nextNodeID(){
        return nextNodeID++;
    }

    private void addToChoicePaths(ConcreteNode node){
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
