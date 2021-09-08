package extraction;

import extraction.network.*;
import extraction.Node.*;
import org.jgrapht.graph.DirectedPseudograph;

import java.util.*;

import static extraction.network.Behaviour.Action.SEND;

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

        No need to match receive and offering. They will be matched when their matching process is checked, so they
        can be removed from their sorting.
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

        //Not a 100% sure why multicom is down here, and not in the loop with Communication and Conditional
        //At least it allows the assumption that no synchronized communication is possible
        for (Map.Entry<String, ProcessTerm> entry : processes.entrySet()) {
            String processName = entry.getKey();
            HashSet<String> unfoldedProcessesCopy = new HashSet<>(unfoldedProcesses);
            /*TODO: Multicom across process unfolding is not possible.
            For example, "a{def X {b?; stop} main {b!<msg>; X}} | b { main {a!<msg2>; a?; stop}}" is won't extract*/


            MulticomContainer multicom = findMulticom(processes, processName);
            if (multicom == null)           //If this process does not start a valid multicom, try the next one
                continue;

            unfoldedProcessesCopy.removeAll(multicom.actors);                   //Fold back processes not part of the multicom
            fold(unfoldedProcessesCopy, multicom.targetNetwork, currentNode);

            var result = expander.buildMulticom(multicom.targetNetwork, multicom.label, currentNode);
            if (result == BuildGraphResult.BAD_LOOP)
                continue;
            return result;
        }

        //If not actions are applicable, the network is unextractable
        return BuildGraphResult.FAIL;
    }

    /**
     * Tries to create a multicom involving the main behaviour of a process.
     * Will only attempt if the main behaviour is Send or Selection.
     * @param processMap Map from all process names to process terms in the network
     * @param processName The name of the process whose main behaviour may be part of a multicom
     * @return A dataclass storing the label representing the multicom, the network resulting form the multicom,
     * and the names of all participating processes, or null if no multicom culd be created
     */
    private MulticomContainer findMulticom(HashMap<String, ProcessTerm> processMap, String processName) {
        var processes = copyProcesses(processMap);  //Copy safe to modify
        var processTerm = processes.get(processName);             //Initial interaction
        var type = processTerm.main.getAction();
        if (!(type == SEND || type == Behaviour.Action.SELECTION))              //Only start multicom by send or select
            return null;

        var actions = new ArrayList<Label.InteractionLabel>();  //List of multicom communications
        var actors = new HashSet<String>();                     //List of participating processes
        var waiting = new LinkedList<Label.InteractionLabel>(); //List of interactions to be processed
        waiting.add(createInteractionLabel(processName, processTerm.main));     //Initial queue interaction

        //Change network state so that the "send" part of the interaction has completed
        if (processTerm.main instanceof Send s)
            processTerm.main = s.continuation;
        else if (processTerm.main instanceof Selection s)
            processTerm.main = s.continuation;

        while (waiting.size() > 0) {                             //While there are interactions to process
            var next = waiting.remove();    //Get next interaction to process
            actions.add(next);              //Add to the list of actions of the multicom
            actors.add(next.sender);        //Add involved processes (used for when folding back unused processes)
            actors.add(next.receiver);

            //Add all send/selection actions of the receiving process, which comes before the receiving action
            //Abort if any other behaviours occur before.
            processTerm = processes.get(next.receiver);
            Behaviour blocking = processTerm.main;              //Behaviour blocking the receival of the communication
            while (!(blocking instanceof Receive || blocking instanceof Offering)){
                Behaviour continuation;
                //If blocking instanceof procedure invocation, unfold procedure.
                if (blocking instanceof Send send){
                    continuation = send.continuation;
                }
                else if (blocking instanceof Selection sel){
                    continuation = sel.continuation;
                }
                else{                                           //Multicom not possible for current set of actions
                    return null;
                }
                //Add the send/selection to list of interactions to be processed, then look at its continuation.

                //The sender is the receiver, because we are adding a send action that must be done before receiving
                var label = createInteractionLabel(next.receiver, blocking);
                if (!actions.contains(label))                   //Add blocking interaction, if not already in actions
                    waiting.add(label);                         //TODO Theoretically, this should always be the case
                blocking = continuation;                        //Look at the next interaction
                processTerm.main = continuation;                //Update network state to have already sent/selected

            }

            //Now all send/select actions before the receive/offer has been queued
            //Variable blocking is now the receive / offer action.
            //Update the network state to include having received/offered
            if (next instanceof Label.InteractionLabel.CommunicationLabel com && blocking instanceof Receive receive
                    && com.sender.equals(receive.sender))
                processTerm.main = receive.continuation;
            else if (next instanceof Label.InteractionLabel.SelectionLabel selection && blocking instanceof Offering offering
                    && selection.sender.equals(offering.sender))
                processTerm.main = offering.branches.get(selection.expression);
            else
                return null;    //Sender and receiver doesn't match
        }
        //Now actions contains all interactions of the multicom
        //Return the updated network, list of interactions, and involved processes
        return new MulticomContainer(new Network(processes), new Label.MulticomLabel(actions), actors);
    }

    /*private MulticomContainer findMulticom(HashMap<String, ProcessTerm> processes, String processName){
        var processTerm = processes.get(processName);
        var type = processTerm.main.getAction();
        if (!(type == SEND || type == Behaviour.Action.SELECTION))
            return null;
        var processesCopy = copyProcesses(processes);
        var actions = new ArrayList<Label.InteractionLabel>();  //List of multicom communications
        var actors = new HashSet<String>();                     //List of participating processes
        var waiting = new LinkedList<Label.InteractionLabel>(); //List of interactions to be processed
        waiting.add(createInteractionLabel(processName, processTerm.main));
        while (waiting.size() > 0){                             //While there are interactions to process
            var next = waiting.remove();   //Retrieve interaction to process
            var acting = processesCopy.get(next.sender);

            actions.add(next);
            actors.add(next.receiver);
            actors.add(next.sender);
            Behaviour blocking = processes.get(next.receiver).main;

            while (!(blocking instanceof Receive)){             //While receiving process is not ready to receive
                Behaviour continuation = null;
                if (blocking instanceof Send send){
                    continuation = send.continuation;
                }
                else if (blocking instanceof Selection sel){
                    continuation = sel.continuation;
                }
                else{                                           //Multicom not possible for current set of actions
                    return null;
                }
                //Add the send/selection to list of interactions to be processed, then look at its continuation.
                var label = createInteractionLabel(next.sender, blocking);
                if (!actions.contains(label))                   //Add blocking interaction, if not already in actions
                    waiting.add(label);
                blocking = continuation;
            }
            //If the recipient expects to receive from a different process, multicom is not possible.
            if (!((Receive) blocking).sender.equals(next.sender)) {
                return null;
            }
        }
        //Now actions should contain all interactions of the multicom

        //Replace process mains with continuations
        return new MulticomContainer(new Network(processesCopy), new Label.MulticomLabel(actions), actors);
    }*/

    /**
     * Helper function to create an InteractionLabel instance from a sending or selection Behaviour
     * @param sender The name of the process sending or selecting
     * @param interaction The Behaviour that is Send or Selection
     * @return An CommunicationLabel or SelectionLabel instance, depending on the instance of the interaction parameter
     */
    private Label.InteractionLabel createInteractionLabel(String sender, Behaviour interaction){
        if (interaction instanceof Send send){
            return new Label.InteractionLabel.CommunicationLabel(sender, send.receiver, send.expression);
        }
        else if (interaction instanceof Selection select){
            return new Label.InteractionLabel.SelectionLabel(sender, select.receiver, select.label);
        }
        else{
            throw new IllegalArgumentException("Function createInteractionLabel expects only Send and Selection Behaviours." +
                    " The behaviour " + interaction.toString() + " is of type " + interaction.getAction().toString());
        }
    }

    /**
     * Simple dataclass to store information about a possible multicom interaction.
     * Stores the resulting network, the multicom label, and a list of names of participating processes.
     */
    private static class MulticomContainer{
        public Network targetNetwork;
        public Label.MulticomLabel label;
        public Set<String> actors; //Set of names of processes that interact for this multicom
        public MulticomContainer(Network targetNetwork, Label.MulticomLabel label, Set<String> actors){
            this.targetNetwork = targetNetwork;
            this.label = label;
            this.actors = actors;
        }
    }

    /**
     * If the ProcessTerm's main action is conditional, returns labels and networks resulting from the conditional action, both for the then case, and else case.
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
                if (senderTerm.main.getAction() == SEND &&
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
