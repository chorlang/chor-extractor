package extraction.network;

import extraction.network.Behaviour.*;
import extraction.network.utils.TreeVisitor;

import javax.naming.OperationNotSupportedException;
import java.util.*;

import static extraction.network.WellFormedness.ContinueStatus.*;

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
            if (!new WellFormedChecker(processes, term, name).Visit(term.rawMain())) {
                System.err.println("The following process is not well-formed: " + name + " " + term);
                return false;
            }
            for (var procedure : term.procedures.entrySet()) {
                if (!isGuarded(Set.of(procedure.getKey()), procedure.getValue(), term.procedures)){
                    System.err.println("The procedure " + procedure.getKey() + " in process " + name + " is not well guarded.");
                    return false;
                }
            }
        }
        try{
            ReachableCodeChecker.checkReachability(network);
        } catch (UnreachableBehaviourException e){
            System.err.printf("A Behaviour in the input network cannot be executed:%n\t%s%n", e.getMessage());
            return false;
        } catch (IncompleteBehaviourException e){
            System.err.printf("A Behaviour in the input network is incomplete:%n\t%s%n", e.getMessage());
            return false;
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
        private WellFormedChecker copy(String newCheckName) {
            return new WellFormedChecker(new HashSet<>(otherProcesses), checkTerm, newCheckName, new HashSet<>(checkedProcedures));
        }

        /**
         * Returns false if a communication communicates with itself.
         * Returns false if communicating with an unknown process.
         * Variables are taken into account.
         * Child processes are checked, with the information inherited form its parent.
         */
        @Override
        public Boolean Visit(NetworkASTNode hostNode){
            switch (hostNode){
                case ProcedureInvocation procedureInvocation:
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
                        return checkTerm.procedures.get(procedure).accept(this)
                                && procedureInvocation.continuation.accept(this);
                    }
                case Termination t:
                    return true;
                case BreakBehaviour b:
                    return true;
                case Introduce introducer:
                    return !checkName.equals(introducer.leftReceiver) && !checkName.equals(introducer.rightReceiver) &&
                            otherProcesses.contains(introducer.leftReceiver) && otherProcesses.contains(introducer.rightReceiver)
                            && introducer.continuation.accept(this);
                case Sender sender:
                    return !checkName.equals(sender.receiver) && otherProcesses.contains(sender.receiver) && sender.continuation.accept(this);
                case Offering offer:
                    if (checkName.equals(offer.sender) || !otherProcesses.contains(offer.sender))
                        return false;
                    for (Behaviour procedureBehaviour : offer.branches.values())
                        if (!procedureBehaviour.accept(this))
                            return false;
                    return offer.continuation.accept(this);
                case Receiver receiver:
                    if (receiver instanceof Introductee introductee){
                        //Check it's not self communication, in case it introduces itself to itself. The SnitchSet could get fooled otherwise
                        if (checkName.equals(introductee.sender))
                            return false;
                        otherProcesses.add(introductee.processID);  //Introduced process is now known
                    }
                    return !checkName.equals(receiver.sender) && otherProcesses.contains(receiver.sender) && receiver.continuation.accept(this);
                case Spawn spawner:
                    otherProcesses.add(spawner.variable);
                    return this.copy(spawner.variable).Visit(spawner.processBehaviour) && spawner.continuation.accept(this);

                case Condition conditional:
                    WellFormedChecker thenBranch = copy();
                    WellFormedChecker elseBranch = copy();
                            //Check both branches individually, using separate data
                    return thenBranch.Visit(conditional.thenBehaviour) && elseBranch.Visit(conditional.elseBehaviour)
                            //Check the continuation, using the data from both branches.
                            && thenBranch.Visit(conditional.continuation) && elseBranch.Visit(conditional.continuation);

                default:
                    throw new RuntimeException(new OperationNotSupportedException("While checking for well-formedness in the extraction.network AST, an object of type " + hostNode.getClass().getName() + " was visited which suggest a degenerate tree."));
            }
        }
    }

    /**
     * Thrown when a Behaviour that branches has a continuation, but the continuation will never be
     * executed in the network.
     */
    private static class UnreachableBehaviourException extends IllegalStateException{
        public UnreachableBehaviourException(String message){
            super(message);
        }
    }

    /**
     * Thrown when a Behaviour needs to return to an ancestor branching Behaviour's continuation,
     * (because it neither terminates nor loops) but no ancestor has a continuation to return to.
     */
    private static class IncompleteBehaviourException extends IllegalStateException {
        public IncompleteBehaviourException(String message){
            super(message);
        }
    }

    enum ContinueStatus { MUST, CAN, WONT }
    private static class ReachableCodeChecker implements TreeVisitor<ContinueStatus, NetworkASTNode>{
        private static ContinueStatus strongest(ContinueStatus ... statuses){
            boolean can = false;
            for (var status : statuses){
                if (status == MUST)
                    return MUST;
                if (status == CAN)
                    can = true;
            }
            if (can)
                return CAN;
            return WONT;
        }
        /**
         * Checks if the continue status of the branch(es) and the continuation are compatible.
         * This is used with behaviours that branch (Conditionals, ProcedureInvocations, Offerings)
         * with an optional continuation.
         * Throws IllegalStateException if the branch(es) cannot have a continuation, but there is one.
         * @param branchStatus If the branch can/must/wont have a continuation (aggregate multiple branches with strongest() first if needed)
         * @param continuation The behaviour that is the continuation of the behaviour being checked.
         * @return WONT if no continuation is allowed. CAN if a continuation is possible, but not needed. MUST if a continuation must be supplied by a parent behaviour.
         */
        private ContinueStatus checkBranchAndContinue(ContinueStatus branchStatus, Behaviour continuation) throws UnreachableBehaviourException{
            ContinueStatus continuationStatus = continuation.accept(this);
            boolean hasContinuation = !(continuation instanceof Behaviour.BreakBehaviour);
            //If the conditional has a (reducible) continuation, but no branches can continue, the continuation will never be executed.
            if (branchStatus == WONT && hasContinuation)
                throw new UnreachableBehaviourException("A branching Behaviour had no branches that could continue, but had a continuation anyway.");
            //If a continuation is guaranteed, the branches are automatically satisfied.
            if (hasContinuation)
                return continuationStatus;
            //If the conditional took on a continuation, it would be unreachable if the branches don't break out
            if (branchStatus == WONT)
                return WONT;
            //At least one branch can or must have a continuation.
            return strongest(branchStatus, continuationStatus);
        }

        private final HashMap<String, Behaviour> procedures;
        private final HashMap<String, ContinueStatus> procedureStatus = new HashMap<>();
        private ReachableCodeChecker(HashMap<String, Behaviour> procedures){
            this.procedures = procedures;
        }

        /**
         * Checks if the main Behaviour of all processes satisfies:<br>
         * - All descendant Behaviour can be executed by at least one branch.<br>
         * - If a branching Behaviour except to return to an ancestors' continuation,
         * there is a continuation for it to do so.<br>
         *
         * @param network The network to check
         * @throws IncompleteBehaviourException If the Behaviour from a branch needs to continue as an
         * ancestor branching Behaviour's continuation, but no such ancestor exists.
         * @throws UnreachableBehaviourException If a branching Behaviour has a continuation, but none
         * of its branches Behaviours continue as it.
         */
        public static void checkReachability(Network network) throws UnreachableBehaviourException, IncompleteBehaviourException{
            for (Map.Entry<String, ProcessTerm> entry : network.processes.entrySet()){
                ProcessTerm process = entry.getValue();
                //Construct a checker with this process' procedures
                var checker = new ReachableCodeChecker(process.procedures);
                //Check if there are any unsatisfied continuations
                if (checker.Visit(process.rawMain()) == MUST){
                    throw new IncompleteBehaviourException("A branch in process %s wants to continue to an ancestor Behaviours continuation, but not such ancestor exists.".formatted(entry.getKey()));
                }
            }
        }


        /**
         * Only for internal use. use checkReachability() instead.
         * <br><br>
         * Explores a Behaviour tree to check for unreachable code, and finding branching behaviours that expect
         * a continuation from their ancestor branching behaviour where there is none.
         * @param hostNode The next node to check
         * @return If the called behaviour Cannot have, Can have, or Must have, a branching ancestor with a continuation to return to.
         */
        @Override
        public ContinueStatus Visit(NetworkASTNode hostNode) throws UnreachableBehaviourException{
            switch (hostNode){
                case NoneBehaviour n: return CAN;
                case BreakBehaviour bb: return MUST;
                case Termination t: return WONT;
                case Condition condition: {
                    ContinueStatus branches = strongest(condition.thenBehaviour.accept(this), condition.elseBehaviour.accept(this));
                    //Check if the branches and continuation are compatible, and return the continue status of this conditional
                    return checkBranchAndContinue(branches, condition.continuation);
                }
                case ProcedureInvocation invocation: {
                    String procedure = invocation.procedure;
                    if (procedureStatus.containsKey(procedure)){
                        //If the status is in the map, but set to null, this is a loop, which won't accept continuations
                        procedureStatus.putIfAbsent(procedure, WONT);
                    }
                    else{
                        //Add a null-entry so it can detect if the procedure loops
                        procedureStatus.put(procedure, null);
                        //Find the continue status for the procedure body, and add it to the map.
                        ContinueStatus status = procedures.get(procedure).accept(this);
                        procedureStatus.put(procedure, status);
                    }
                    //The procedure status is now non-null and can be retrieved.
                    ContinueStatus branch = procedureStatus.get(procedure);
                    //Check for compatibility against the continuation
                    return checkBranchAndContinue(branch, invocation.continuation);
                }
                case Offering offering: {
                    //Collect the continuestatus of all branches
                    ContinueStatus[] branchStatuses = offering.branches.values().stream().map(behaviour ->
                            behaviour.accept(this)).toArray(ContinueStatus[]::new);
                    //Aggregate the statues of the branches
                    ContinueStatus branchStatus= strongest(branchStatuses);
                    //Check branch and continuation compatibility
                    return checkBranchAndContinue(branchStatus, offering.continuation);
                }
                case Behaviour b:
                    return b.continuation.accept(this);
                default:
                    throw new IllegalStateException("Unexpected node type when checking code-completeness: " + hostNode + ". Not a Behaviour");
            }


        }
    }
}
