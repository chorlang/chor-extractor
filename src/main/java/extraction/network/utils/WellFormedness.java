package extraction.network.utils;

import extraction.network.*;

import javax.naming.OperationNotSupportedException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WellFormedness{
    /**
     * Computes whether the extraction.network is both well-formed and guarded.
     * A extraction.network that is well-formed if no processes communicate with themselves
     * A extraction.network is guarded if no procedures call each other in an infinite loop with no other actions taken.
     * @param network The extraction.network to check.
     * @return true, if the extraction.network is both well-formed and guarded. False otherwise
     */
    public static Boolean compute(Network network){
        for (var process: network.processes.entrySet()){
            String processName = process.getKey();
            ProcessTerm term = process.getValue();
            if (!checkWellFormedness(term.main(), processName))
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

    private static Boolean checkWellFormedness(NetworkASTNode node, String processName){
        return node.accept(new WellFormedChecker(processName));
    }

    /**
     * This check ensures there are no procedure invocations that call themselves, or loop of procedure calls calling
     * each other indefinitely without performing other actions. For example, a extraction.network containing "def {X=X} in X" or
     * "def {X=Y, Y=X} in X" will make this method return false, but "def {X=X} in 0" or "def {X=Y, Y=q!; X} in X" will not.
     * @param procedureNames A list of the name of procedures that have been invoked. Initial non-recursive call should
     *                       simply be a set containing the name of the top procedure.
     * @param behaviour The Behaviour that is being tested for being guarded. Will always return true if it is not ProcedureInvocation.
     * @param procedures A list of procedures in the extraction.network, so this function can recursively check the guardedness.
     * @return true if the procedure do meaningful work. false if it only calls procedures in a loop.
     */
    private static Boolean isGuarded(Set<String> procedureNames, Behaviour behaviour, Map<String, Behaviour> procedures){
        if (!(behaviour instanceof ProcedureInvocation invocation))
            return true;
        var newProcedureNames = new HashSet<>(procedureNames);
        newProcedureNames.add(invocation.procedure);
        return !procedureNames.contains(invocation.procedure) && isGuarded(newProcedureNames, procedures.get(invocation.procedure), procedures);
    }

    /**
     * This tree visitor is intended to traverse Network AST's of a process to ensure that it is well-formed,
     * meaning the process does not interact with itself.
     * The choreography extraction algorithm assumes the extraction.network is well-formed.
     */
    private static class WellFormedChecker implements TreeVisitor<Boolean, NetworkASTNode> {
        //The name of the process currently being checked
        private final String checkingProcessName;
        public WellFormedChecker(String processName){
            checkingProcessName = processName;
        }

        @Override
        public Boolean Visit(NetworkASTNode hostNode) {
            switch (hostNode.action){
                case PROCEDURE_INVOCATION:
                case TERMINATION:
                    return true;

                case INTRODUCE:
                    var introducer = (Introduce) hostNode;
                    return !introducer.process1.equals(checkingProcessName) &&
                            !introducer.process2.equals(checkingProcessName) &&
                            introducer.continuation.accept(this);
                case INTRODUCTEE:
                    var introduced = (Introductee) hostNode;
                    return !introduced.sender.equals(checkingProcessName) &&
                            !introduced.processID.equals(checkingProcessName) &&
                            introduced.continuation.accept(this);

                case CONDITION:
                    var conditional = (extraction.network.Condition)hostNode;
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
                    var selector = (extraction.network.Selection)hostNode;
                    return !selector.receiver.equals(checkingProcessName) && selector.continuation.accept(this);

                case SEND:
                    var sender = (Send)hostNode;
                    return !sender.receiver.equals(checkingProcessName) && sender.continuation.accept(this);
                case NETWORK:
                case PROCESS_TERM:
                default:
                    throw new RuntimeException(new OperationNotSupportedException("While checking for well-formedness in the extraction.network AST, an object of type " + hostNode.getClass().getName() + " was visited which suggest a degenerate tree."));
            }
        }
    }
}
