package extraction.network;

import extraction.network.Behaviour.*;
import extraction.network.utils.TreeVisitor;

import javax.naming.OperationNotSupportedException;
import java.util.*;

public class WellFormedness{
    /**
     * Returns true if the Network is both well-formed and guarded.
     * Well-formed means a process does not communicate with itself, or a process not in the network,
     * and that no process invokes a procedure that is not defined for that process.
     * Guarded means all loops (of procedures) perform useful work, e.i. contains a term other than ProcedureInvocation.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isWellFormed(Network network){
        for (var process : network.processes.entrySet()){
            String name = process.getKey();
            ProcessTerm term = process.getValue();
            var processes = new HashSet<>(network.processes.keySet());
            if (!new WellFormedChecker(processes, term, name).Visit(term.main)) {
                System.out.println("The following process is not well-formed: " + name + " " + term);
                return false;
            }
            for (var procedure : term.procedures.entrySet()) {
                if (!isGuarded(Set.of(procedure.getKey()), procedure.getValue(), term.procedures)){
                    System.out.println("The procedure " + procedure.getKey() + " in process " + name + " is not well guarded.");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * This check ensures there are no procedure invocations that call themselves, or loop of procedure calls calling
     * each other indefinitely without performing other actions. For example, a network containing "def {X=X} in X" or
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
     * Class used to traverse a Behaviour tree to check for self communications, communications outside the network,
     * and
     */
    private static class WellFormedChecker implements TreeVisitor<Boolean, NetworkASTNode> {
        private final SnitchSet otherProcesses;   //Known process names
        private final HashSet<String> checkedProcedures;//Procedure definitions that have already been checked
        private final ProcessTerm checkTerm;              //The term of the process currently being checked
        private String checkName;
        WellFormedChecker(HashSet<String> otherProcesses, ProcessTerm checkTerm, String checkName){
            this(otherProcesses, checkTerm, checkName, new HashSet<>());
        }
        private WellFormedChecker(HashSet<String> otherProcesses, ProcessTerm checkTerm, String checkName, HashSet<String> checkedProcedures) {
            this.otherProcesses = new SnitchSet(otherProcesses);
            this.otherProcesses.parent = this;
            this.checkTerm = checkTerm;
            this.checkedProcedures = checkedProcedures;
            this.checkName = checkName;
        }
        //The SnitchSet ensures that if the process' own name gets used as a variable it no longer counts as self communication.
        private static class SnitchSet extends HashSet<String>{
            public WellFormedChecker parent = null;
            public SnitchSet(Collection<? extends String> c) {
                super(c);
            }
            //Is used internally by addAll in its superclass
            @Override
            public boolean add(String e){
                //The null check is because add is called when calling the superclass constructor
                if (parent != null && parent.checkName.equals(e))
                    parent.checkName = "";
                return super.add(e);
            }
        }

        //Copies this checker
        private WellFormedChecker copy() {
            return new WellFormedChecker(new HashSet<>(otherProcesses), checkTerm, checkName, new HashSet<>(checkedProcedures));
        }
        //Copies this checker, but change the name of the process to check. Used when spawning
        private WellFormedChecker copy(String newCheckheckName) {
            return new WellFormedChecker(new HashSet<>(otherProcesses), checkTerm, newCheckheckName, new HashSet<>(checkedProcedures));
        }

        /**
         * Returns false if a communication communicates with itself.
         * Returns false if communicating with an unknown process.
         * Variables are taken into account.
         * Child processes are checked, with the information inherited form its parent.
         */
        @Override
        public Boolean Visit(NetworkASTNode hostNode){
            switch (hostNode.action){
                case PROCEDURE_INVOCATION:
                    ProcedureInvocation procedureInvocation = ((ProcedureInvocation)hostNode);
                    String procedure = procedureInvocation.procedure;
                    if (!checkTerm.procedures.containsKey(procedure))
                        return false;   //Procedure does not exist
                    if (!otherProcesses.containsAll(procedureInvocation.parameters))
                        return false;   //If the parameter values are unknown: fail.
                    if (checkedProcedures.contains(procedure))
                        return true;    //If it has already been checked in this branch.
                    else {
                        //This procedure will now be checked, so add it to the set of checked procedures
                        checkedProcedures.add(procedure);
                        //Add the parameters to list of known other processes.
                        otherProcesses.addAll(checkTerm.parameters.get(procedure));
                        return checkTerm.procedures.get(procedure).accept(this);
                    }
                case TERMINATION:
                    return true;

                case SEND:
                case SELECTION:
                    var sender = (Sender)hostNode;
                    return !checkName.equals(sender.receiver) && otherProcesses.contains(sender.receiver) && sender.continuation.accept(this);
                case INTRODUCE:
                    var introducer = (Introduce) hostNode;
                    return !checkName.equals(introducer.leftReceiver) && !checkName.equals(introducer.rightReceiver) &&
                            otherProcesses.contains(introducer.leftReceiver) && otherProcesses.contains(introducer.rightReceiver)
                            && introducer.continuation.accept(this);

                case INTRODUCTEE:
                    var introductee = (Introductee) hostNode;
                    //Check it's not self communication, in case it introduces itself to itself. The SnitchSet could get fooled otherwise
                    if (checkName.equals(introductee.sender))
                        return false;
                    otherProcesses.add(introductee.processID);  //Introduced process is now known
                case RECEIVE:
                    var receiver = (Receiver)hostNode;
                    return !checkName.equals(receiver.sender) && otherProcesses.contains(receiver.sender) && receiver.continuation.accept(this);
                case OFFERING:
                    var offer = (Offering)hostNode;
                    if (checkName.equals(offer.sender) || !otherProcesses.contains(offer.sender))
                        return false;
                    for (Behaviour procedureBehaviour : offer.branches.values())
                        if (!procedureBehaviour.accept(this))
                            return false;
                    return true;

                case SPAWN:
                    var spawner = (Spawn) hostNode;
                    otherProcesses.add(spawner.variable);
                    return this.copy(spawner.variable).Visit(spawner.processBehaviour) && spawner.continuation.accept(this);

                case CONDITION:
                    var conditional = (Condition)hostNode;
                    return copy().Visit(conditional.thenBehaviour) && copy().Visit(conditional.elseBehaviour);

                case NETWORK:
                case PROCESS_TERM:
                default:
                    throw new RuntimeException(new OperationNotSupportedException("While checking for well-formedness in the extraction.network AST, an object of type " + hostNode.getClass().getName() + " was visited which suggest a degenerate tree."));
            }
        }
    }
}
