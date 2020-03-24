package extraction;

import network.*;
import extraction.Node.*;
import org.jgrapht.graph.DirectedPseudograph;

import java.util.*;

/**
 * Class for extracting a graph from a Network using a specific extraction strategy.
 * The graph symbolises the networks states throughout execution, and the communication between the network's processes.
 * The graph is intended to be used for choreography extraction.
 */
public class GraphBuilder {
    private Strategy strategy;
    private GraphExpander expander;

    enum BuildGraphResult{
        OK, BADLOOP, FAIL
    }

    /**
     * Instantiates a new GraphBuilder
     * @param extractionStrategy The Strategy used to extract a graph from the Network with networkGraphBuilder().
     */
    public GraphBuilder(Strategy extractionStrategy){
        strategy = extractionStrategy;
    }

    /**
     * Constructs a graph symbolizing the execution of the Network AST parsed to this method.
     * @param n The network to construct a graph from.
     * @param services A list of processes that are allowed to be livelocked.
     * @return A directed graph where each vertex symbolizes a state of the network, and the edges the interactions that change from the state in one vertex to the other.
     */
    public DirectedPseudograph<Node, Label> networkGraphBuilder(Network n, Set<String> services){
        var marking = new HashMap<String, Boolean>();
        n.processes.forEach((processName, processTerm) -> marking.put(processName, processTerm.main.getAction() == Behaviour.Action.termination || services.contains(processName)));
        var node = new ConcreteNode(n,"0", 0, new HashSet<>(), marking);
        expander = new GraphExpander(services, this, node);

        BuildGraphResult buildResult = buildGraph(node);
        System.out.println(buildResult);

        return expander.getGraph();
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

                var result = expander.buildCommunication(targetNetwork, label, currentNode);
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

                var result = expander.buildConditional(thenNetwork, thenLabel, elseNetwork, elseLabel, currentNode);
                if (result == BuildGraphResult.BADLOOP)
                    continue;
                return result;
            }
        }
        if (allTerminated(processes))
            return BuildGraphResult.OK;

        return BuildGraphResult.FAIL;
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

    /**
     * If the ProcessTerm's main action is conditional, returns labels and networks resulting from the oconditional action, both for the then case, and else case.
     * @param processes The map of ProcessTerms the graph is being build from
     * @param processName The name of the process currently being prospected for conditional action.
     * @param processTerm The ProcessTerm currently being prospected for conditional action.
     * @return A ConditionContainer with labels and Networks resulting from the then case, and else case.
     */
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

    private HashMap<String, ProcessTerm> copyProcesses(HashMap<String, ProcessTerm> processes){
        var copy = new HashMap<String, ProcessTerm>(processes.size());
        processes.forEach((processName, processTerm) -> copy.put(processName, processTerm.copy()));
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

    /**
     * If the ProcessTerm's main Behaviour is ProcedureInvocation, the main Behaviour is replaced by its procedure definition recursively, until it is no longer a ProcedureInvocation.
     * @param processTerm The term to unfold.
     * @return true if the ProcessTerm was unfolded, false if the ProcessTerm is not ProcedureInvocation.
     */
    private boolean unfold(ProcessTerm processTerm){
        Behaviour main = processTerm.main;

        if (main.getAction() != Behaviour.Action.procedureInvocation)
            return false;

        ProcedureInvocation mainProcedure = (ProcedureInvocation)main;
        String procedureName = mainProcedure.procedure;
        Behaviour procedureBehaviour = processTerm.procedures.get(procedureName);
        if (procedureBehaviour == null)
            throw new IllegalStateException("Cannot unfold the process. Procedure definition for " + procedureName + " do not exists");

        processTerm.main = procedureBehaviour.copy();

        if (procedureBehaviour.getAction() == Behaviour.Action.procedureInvocation){
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
        return strategy.copyAndSort(node.network.processes);
    }
}
