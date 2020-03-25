package extraction.choreography;

import network.*;

import javax.naming.OperationNotSupportedException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WellFormedness{
    public static Boolean compute(Network n){
        for (var process: n.processes.entrySet()){
            String processName = process.getKey();
            ProcessTerm term = process.getValue();
            if (!checkWellFormedness(term.main, processName))
                return false;
            for (var procedure : term.procedures.entrySet()){
                String procedureName = procedure.getKey();
                Behaviour procedureBehaviour = procedure.getValue();
                if (!isGuarded(Set.of(procedureName), procedureBehaviour, term.procedures) || !checkWellFormedness(procedureBehaviour, processName))
                    return false;
            }
        }
        return true;
    }

    private static Boolean checkWellFormedness(Behaviour behaviour, String processName){
        return behaviour.accept(new WellFormedChecker(processName));
    }

    private static Boolean isGuarded(Set<String> procedureNames, Behaviour behaviour, Map<String, Behaviour> procedures){
        if (behaviour.getAction() != Behaviour.Action.PROCEDURE_INVOCATION)
            return true;
        var invocation = (network.ProcedureInvocation)behaviour;
        var newProcedureNames = new HashSet<>(procedureNames);
        newProcedureNames.add(invocation.procedure);
        return !procedureNames.contains(invocation.procedure) && isGuarded(newProcedureNames, procedures.get(invocation.procedure), procedures);
    }

    private static class WellFormedChecker implements TreeVisitor<Boolean, Behaviour>{
        //The name of the process currently being checked
        private String checkingProcessName;
        public WellFormedChecker(String processName){
            checkingProcessName = processName;
        }

        @Override
        public Boolean Visit(Behaviour hostNode) {
            switch (hostNode.getAction()){
                case PROCEDURE_INVOCATION:
                case TERMINATION:
                    return true;

                case CONDITION:
                    var conditional = (network.Condition)hostNode;
                    return conditional.thenBehaviour.accept(this) && conditional.elseBehaviour.accept(this);

                case OFFERING:
                    var offer = (Offering)hostNode;
                    if (offer.sender.equals(checkingProcessName))
                        return false;
                    for (Behaviour procedureBehaviour : offer.branches.values())
                        if (!procedureBehaviour.accept(this))
                            return false;
                    return true;

                case RECEIVE:
                    var receiver = (Receive)hostNode;
                    return !receiver.sender.equals(checkingProcessName) && receiver.continuation.accept(this);

                case SELECTION:
                    var selector = (network.Selection)hostNode;
                    return !selector.receiver.equals(checkingProcessName) && selector.continuation.accept(this);

                case SEND:
                    var sender = (Send)hostNode;
                    return !sender.receiver.equals(checkingProcessName) && sender.continuation.accept(this);
                case NETWORK:
                case PROCESS_TERM:
                default:
                    throw new RuntimeException(new OperationNotSupportedException("While checking for well-formedness in the network AST, an object of type " + hostNode.getClass().getName() + " was visited which is not supported."));
            }
        }
    }
}
