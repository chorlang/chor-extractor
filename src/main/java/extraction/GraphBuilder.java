package extraction;

import network.*;
import extraction.Node.*;
import org.jgrapht.graph.DirectedPseudograph;

import java.util.*;

public class GraphBuilder {
    private Strategy strategy;
    private GraphExpander expander;

    enum BuildGraphResult{
        OK, BADLOOP, FAIL
    }

    public GraphBuilder(Strategy extractionStrategy){
        strategy = extractionStrategy;
    }

    public DirectedPseudograph<Node, Label> networkGraphGenerator(Network n, Set<String> services){
        var marking = new HashMap<String, Boolean>();
        n.processes.forEach((processName, processTerm) -> {
            marking.put(processName, processTerm.main.getAction() == Behaviour.Action.termination || services.contains(processName));
        });
        var node = new ConcreteNode(n,"0", 0, new HashSet<>(), marking);
        expander = new GraphExpander(services, this, node);

        BuildGraphResult buildResult = buildGraph(node);
        System.out.println(buildResult);

        return expander.getGraph();
    }

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
        for (String processName : unfoldedProcesses){
            targetNetwork.processes.get(processName).main = node.network.processes.get(processName).main.copy();
        }
        //unfoldedProcesses.forEach(process -> targetNetwork.processes.get(process).main = node.network.processes.get(process).main.copy());
    }

    private LinkedHashMap<String, ProcessTerm> copyAndSortProcesses(Node.ConcreteNode node){
        return strategy.copyAndSort(node.network.processes);
    }
}
