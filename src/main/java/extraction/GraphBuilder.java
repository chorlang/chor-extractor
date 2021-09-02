package extraction;

import extraction.network.*;
import extraction.Node.*;
import org.jgrapht.graph.DirectedPseudograph;

import java.util.*;

/**
 * Class for extracting a graph from a Network using a specific extraction strategy.
 * The graph symbolises the networks states throughout execution, and the communication between the extraction.network's processes.
 * The graph is intended to be used for choreography extraction.
 */
public class GraphBuilder {
    private Strategy strategy;
    private GraphExpander expander;

    enum BuildGraphResult{
        OK, BAD_LOOP, FAIL
    }

    /**
     * Instantiates a new GraphBuilder
     * @param extractionStrategy The Strategy used to extract a graph from the Network with networkGraphBuilder().
     */
    GraphBuilder(Strategy extractionStrategy){
        strategy = extractionStrategy;
    }

    /**
     * Constructs a graph symbolizing the execution of the Network AST parsed to this method.
     * @param n The extraction.network to construct a graph from.
     * @param services A list of processes that are allowed to be livelocked.
     * @return A directed graph where each vertex symbolizes a state of the extraction.network, and the edges the interactions that change from the state in one vertex to the other. It also returns the root node of the graph.
     */
    public ExecutionGraphResult buildExecutionGraph(Network n, Set<String> services){
        var marking = new HashMap<String, Boolean>();
        n.processes.forEach((processName, processTerm) -> marking.put(processName, processTerm.main.getAction() == Behaviour.Action.TERMINATION || services.contains(processName)));
        var node = new ConcreteNode(n,"0", 0, new HashSet<>(), marking);
        expander = new GraphExpander(services, this, node);

        BuildGraphResult buildResult = buildGraph(node);
        System.out.println("Graph building result: " + buildResult);

        return new ExecutionGraphResult(expander.getGraph(), node, buildResult, expander.badLoopCounter);
    }

    public static class ExecutionGraphResult{
        public DirectedPseudograph<Node, Label> graph;
        public ConcreteNode rootNode;
        public BuildGraphResult buildGraphResult;
        public int badLoopCounter;
        public ExecutionGraphResult(DirectedPseudograph<Node, Label> graph, ConcreteNode rootNode, BuildGraphResult buildGraphResult, int badLoopCounter){
            this.graph = graph;
            this.rootNode = rootNode;
            this.buildGraphResult = buildGraphResult;
            this.badLoopCounter = badLoopCounter;
        }
    }

    /**
     * Builds the graph recursively from the node parsed to this method.
     * This method is not intended to be called externally. Use networkGraphBuilder() instead.
     * @param currentNode The node to build a branch of the graph from.
     * @return OK if successful, BADLOOP if adding the node would create a loop where not all processes are marked (used), or FAIL if the graph could not be build, making choreography extraction impossible.
     */
    BuildGraphResult buildGraph(ConcreteNode currentNode){
        HashSet<String> unfoldedProcesses = new HashSet<>();
        LinkedHashMap<String, ProcessTerm> processes = copyAndSortProcesses(currentNode);

        processes.forEach((processName, processTerm) -> {
            if (unfold(processTerm))
                unfoldedProcesses.add(processName);
        });

        /* === Considerations ===
        If the main action is communication, but the co-communicator do not have a matching main action, then
        findCommunication returns null, which makes it check the process for a conditional even though it is a communication.
        Perhaps refactor after implementing multicom.

        Use pattern matching instead of enums. Upgrade to java 17 to use in switch statements
         */

        //For each process, ordered depending on extraction strategy.
        for (Map.Entry<String, ProcessTerm> entry : processes.entrySet()){
            String processName = entry.getKey();
            ProcessTerm processTerm = entry.getValue();
            HashSet<String> unfoldedProcessesCopy = new HashSet<>(unfoldedProcesses);


            //Check if the next action of the process is to send/receive/select/offer
            //and that the next action of the other process of the interaction matches the communication
            CommunicationContainer communication = findCommunication(processes, processName, processTerm);
            if (communication != null){
                Network targetNetwork = communication.targetNetwork;
                var label = communication.label;
                unfoldedProcessesCopy.remove(label.sender);
                unfoldedProcessesCopy.remove(label.receiver);
                fold(unfoldedProcessesCopy, targetNetwork, currentNode);

                var result = expander.buildCommunication(targetNetwork, label, currentNode);
                if (result == BuildGraphResult.BAD_LOOP)
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

                var result = expander.buildConditional(thenNetwork, thenLabel, elseNetwork, elseLabel, currentNode);
                if (result == BuildGraphResult.BAD_LOOP)
                    continue;
                return result;
            }
        }
        if (allTerminated(processes))
            return BuildGraphResult.OK;

        return BuildGraphResult.FAIL;
    }

    /**
     * If the ProcessTerm's main action is conditional, returns labels and networks resulting from the oconditional action, both for the then case, and else case.
     * @param processes The map of ProcessTerms the graph is being build from
     * @param processName The name of the process currently being prospected for conditional action.
     * @param processTerm The ProcessTerm currently being prospected for conditional action.
     * @return A ConditionContainer with labels and Networks resulting from the then case, and else case.
     */
    private ConditionContainer findConditional(HashMap<String, ProcessTerm> processes, String processName, ProcessTerm processTerm){
        if (processTerm.main.getAction() != Behaviour.Action.CONDITION)
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

    /**
     * Simple class to store the networks "thenNetwork" and "elseNetwork" resulting from a conditional, as well as the
     * corresponding labels "thenLabel" and "elseLabel".
     */
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

    /**
     * Checks if the entire network has terminated.
     * @param network The network to check.
     * @return true if all processes has terminated. false otherwise.
     */
    private boolean allTerminated(HashMap<String, ProcessTerm> network){
        for (ProcessTerm process : network.values()) {
            if (process.main.getAction() != Behaviour.Action.TERMINATION)
                return false;
        }
        return true;
    }

    /**
     * If the ProcessTerm's main action is information exchange between two processes, returns a label for such action,
     * and the Network that would result from that action.
     * @param processes The map of ProcessTerms the graph is being build from.
     * @param processName The name of the process currently being prospected for communication.
     * @param processTerm The processTerm for the process currently being prospected for communication
     * @return A CommunicationContainer with the Label and Network resulting from executing the next action in the ProcessTerm, or null if that action is not related to communication.
     */
    private CommunicationContainer findCommunication(HashMap<String, ProcessTerm> processes, String processName, ProcessTerm processTerm){
        Behaviour main = processTerm.main;
        switch (main.getAction()) {
            case SEND -> {
                String recipientProcessName = ((Send) main).receiver;
                ProcessTerm receiveTerm = processes.get(recipientProcessName);
                if (receiveTerm.main.getAction() == Behaviour.Action.RECEIVE &&
                        ((Receive) receiveTerm.main).sender.equals(processName)) {
                    return consumeCommunication(processes, processTerm, receiveTerm);
                }
            }
            case RECEIVE -> {
                String sendingProcessName = ((Receive) main).sender;
                ProcessTerm senderTerm = processes.get(sendingProcessName);
                if (senderTerm.main.getAction() == Behaviour.Action.SEND &&
                        ((Send) senderTerm.main).receiver.equals(processName)) {
                    return consumeCommunication(processes, senderTerm, processTerm);
                }
            }
            case SELECTION -> {
                String offeringProcessName = ((Selection) main).receiver;
                ProcessTerm offerTerm = processes.get(offeringProcessName);
                if (offerTerm.main.getAction() == Behaviour.Action.OFFERING &&
                        ((Offering) offerTerm.main).sender.equals(processName)) {
                    return consumeSelection(processes, offerTerm, processTerm);
                }
            }
            case OFFERING -> {
                String selectingProcessName = ((Offering) main).sender;
                ProcessTerm selectionTerm = processes.get(selectingProcessName);
                if (selectionTerm.main.getAction() == Behaviour.Action.SELECTION &&
                        ((Selection) selectionTerm.main).receiver.equals(processName)) {
                    return consumeSelection(processes, processTerm, selectionTerm);
                }
            }
        }
        return null;
    }

    /**
     * Takes a send / receive pair of processes, and returns the network after the communication has occurred, and the graph label of the communication.
     * @param processes The processTerms of the current network
     * @param sendTerm The processTerm of the process with the send action.
     * @param receiveTerm The processTerm of the process with the receive action.
     * @return Object storing the label of the communication, and the network resulting form the communication.
     */
    private CommunicationContainer consumeCommunication(HashMap<String, ProcessTerm> processes, ProcessTerm sendTerm, ProcessTerm receiveTerm){
        var processesCopy = copyProcesses(processes);
        Send sender = (Send)sendTerm.main;
        Receive receiver = (Receive)receiveTerm.main;

        //Retrieve the name of the sender from the receiver. Reverse for the other statement.
        processesCopy.replace(receiver.sender, new ProcessTerm(sendTerm.procedures, sender.continuation));
        processesCopy.replace(sender.receiver, new ProcessTerm(receiveTerm.procedures, receiver.continuation));

        var label = new Label.InteractionLabel.CommunicationLabel(receiver.sender, sender.receiver, sender.expression);

        return new CommunicationContainer(new Network(processesCopy), label);
    }

    /**
     * Takes a select / offer pair of processes, and returns the network after the selection has occurred, and the graph label of the selection.
     * @param processes The processTerms of the current network
     * @param offerTerm The processTerm of the process with the offer action.
     * @param selectTerm The processTerm of the process with the select action.
     * @return Object storing the label of the selection, and the network resulting form the selection.
     */
    private CommunicationContainer consumeSelection(HashMap<String, ProcessTerm> processes, ProcessTerm offerTerm, ProcessTerm selectTerm){
        var processesCopy = copyProcesses(processes);
        Selection selector = (Selection)selectTerm.main;
        Offering offer = (Offering)offerTerm.main;

        Behaviour offeringBehaviour = offer.branches.get(selector.label);
        if (offeringBehaviour == null)
            return null;

        processesCopy.replace(selector.receiver, new ProcessTerm(offerTerm.procedures, offeringBehaviour));
        processesCopy.replace(offer.sender, new ProcessTerm(selectTerm.procedures, selector.continuation));

        var label = new Label.InteractionLabel.SelectionLabel(selector.receiver, offer.sender, selector.label);

        return new CommunicationContainer(new Network(processesCopy), label);
    }

    /**
     * Returns a deep copy of the HashMap
     */
    private HashMap<String, ProcessTerm> copyProcesses(HashMap<String, ProcessTerm> processes){
        var copy = new HashMap<String, ProcessTerm>(processes.size());
        processes.forEach((processName, processTerm) -> copy.put(processName, processTerm.copy()));
        return copy;
    }

    /**
     * Simple class to store a network "n" and a label "l"
     */
    private static class CommunicationContainer{
        Network targetNetwork;
        Label.InteractionLabel label;
        public CommunicationContainer(Network n, Label.InteractionLabel l){
            targetNetwork = n;
            label = l;
        }
    }

    /**
     * If the ProcessTerm's main Behaviour is ProcedureInvocation, the main Behaviour is replaced by its procedure definition recursively, until it is no longer a ProcedureInvocation.
     * @param processTerm The term to unfold.
     * @return true if the ProcessTerm was unfolded, false if the ProcessTerm is not ProcedureInvocation.
     */
    private boolean unfold(ProcessTerm processTerm){
        Behaviour main = processTerm.main;

        if (main.getAction() != Behaviour.Action.PROCEDURE_INVOCATION)
            return false;

        ProcedureInvocation mainProcedure = (ProcedureInvocation)main;
        String procedureName = mainProcedure.procedure;
        Behaviour procedureBehaviour = processTerm.procedures.get(procedureName);
        if (procedureBehaviour == null)
            throw new IllegalStateException("Cannot unfold the process. Procedure definition for " + procedureName + " do not exists");

        processTerm.main = procedureBehaviour.copy();

        if (procedureBehaviour.getAction() == Behaviour.Action.PROCEDURE_INVOCATION){
            unfold(processTerm);
        }
        return true;
    }

    /**
     * Folds back unfolded processes.
     * From a list of processes which have been unfolded, in the targetNetwork,
     * replace the main Behaviours of each ProcessTerm with a copy of the main Behaviour of the ProcessTerm in the Network of the node.
     * @param unfoldedProcesses The set of process names for which their corresponding ProcessTerm has been unfolded.
     * @param targetNetwork The Network for which to fold back its ProcessTerms, if they are listed in unfoldedProcesses.
     * @param node A node containing a copy of the Network from before the ProcessTerms was unfolded.
     */
    private void fold(HashSet<String> unfoldedProcesses, Network targetNetwork, ConcreteNode node){
        for (String processName : unfoldedProcesses){
            targetNetwork.processes.get(processName).main = node.network.processes.get(processName).main.copy();
        }
    }

    /**
     * Returns a ordered HashMap of the processes of the Network in the node, sorted after the strategy this GraphBuilder uses, such that higher priority ProcessTerms are earlier in the HashMap.
     * The ProcessTerms in the HashMap are copies of the original, so changes to the elements in the original HashMap do not affect the copy and vice versa.
     * @param node The node containing the Network with a list of ProcessTerms to be sorted
     * @return A ordered HashMap sorted after this instances extraction strategy.
     */
    private LinkedHashMap<String, ProcessTerm> copyAndSortProcesses(Node.ConcreteNode node){
        return strategy.copyAndSort(node);
    }
}
