package extraction.network;

import extraction.network.Behaviour.*;
import extraction.network.utils.TreeVisitor;

import javax.naming.OperationNotSupportedException;
import java.util.*;

import static extraction.network.NetAnalyser.ContinueStatus.*;

/**
 * Analyses network structures recursively to check for certain conditions,
 * such as self-communication and unreachable code.
 */
public class NetAnalyser {
    /**
     * Returns true if the Network is safe to use in extraction. This includes;<br>
     *  - Well-formedness: No processes communicates with itself, or a process not in the network.
     *  Furthermore, a processes only invokes procedures defined for that process, and only using
     *  parameters defined at the time of invocation.<br>
     *  - Guarded: All recursive procedures perform useful work, i.e. contains a term other than ProcedureInvocation.<br>
     *  - Reachable: All behaviour terms are reachable. Conditionals, offerings, and procedure invocations can
     *  have a "continuation" behaviour which is effectively appended to the end of any non-terminating branch
     *  of execution. This checks that the continuation is used for at least one of those branches. It also
     *  checks that any branch that needs a continuation has one.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isSafe(Network network){
        if (!isWellFormed(network))
            return false;
        for (var process : network.processes.entrySet()){
            String name = process.getKey();
            ProcessTerm term = process.getValue();
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
    private static boolean isGuarded(Set<String> procedureNames, Behaviour behaviour, Map<String, Behaviour> procedures){
        if (!(behaviour instanceof ProcedureInvocation invocation))
            return true;
        var newProcedureNames = new HashSet<>(procedureNames);
        newProcedureNames.add(invocation.procedure);
        return !procedureNames.contains(invocation.procedure) && isGuarded(newProcedureNames, procedures.get(invocation.procedure), procedures);
    }

    private static boolean isWellFormed(Network network){
        var processNames = new HashSet<>(network.processes.keySet());
        String processName = null;
        ProcessTerm term = null;
        try {
            for (var entry : network.processes.entrySet()) {
                processName = entry.getKey();
                term = entry.getValue();
                new WellFormedChecker(processNames, term, processName).Visit(term.rawMain());
            }
        } catch (UndefinedProcedureException | SelfCommunicationException | NoSuchProcessException e){
            System.err.printf("Process %s is not well formed. The well-formedness check resulted in the following error: %s%nThe process is defined as %s%n",
                    processName, e.getMessage(), term);
        }
        return true;
    }

    /**
     * Thrown to indicate a process in a Network tries to invoke a procedure that is not defined for that process.
     */
    private static class UndefinedProcedureException extends RuntimeException{
        UndefinedProcedureException(String message) { super(message); }
    }
    /**
     * Thrown to indicate a process in a Network tries to perform an interaction with itself.
     */
    private static class SelfCommunicationException extends RuntimeException{
        SelfCommunicationException(String message) { super(message); }
        SelfCommunicationException(Behaviour term) {
            this("Term "+term+" is a self-communication.");
        }
    }
    /**
     * Thrown to indicate a process in a Network tries to communicate with a process that is not defined for that Network.
     */
    private static class NoSuchProcessException extends RuntimeException{
        NoSuchProcessException(String message) { super(message); }
    }

    /**
     * Class used to traverse a Behaviour tree to check for self communications, communications outside the network,
     * and that all invoked procedures are defined.
     */
    private static class WellFormedChecker implements TreeVisitor<Void, NetworkASTNode> {
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
        public Void Visit(NetworkASTNode hostNode){
            if (!(hostNode instanceof Behaviour))
                throw new IllegalStateException("Attempted to perform well-formedness check on a Network AST node which is not a Behaviour. This suggest either incorrect usage, or a degenerate Network tree.");
            switch (hostNode){
                case ProcedureInvocation procedureInvocation:
                    String procedure = procedureInvocation.procedure;
                    if (!checkTerm.procedures.containsKey(procedure))   //Procedure does not exist
                        throw new UndefinedProcedureException("Attempted to invoke procedure \""+procedure+"\" which is not defined for this process.");
                    if (!otherProcesses.containsAll(procedureInvocation.parameters)){   //Parameters are not defined
                        var unknown = new HashSet<String>();
                        procedureInvocation.parameters.stream().filter(otherProcesses::contains).forEach(unknown::add);
                        throw new UndefinedProcedureException("Attempted to invoke \"%s(%s)\", but parameters %s are either not processes in the network, or variables undefined by the process at the time of invocation.".formatted(procedure,
                                procedureInvocation.parameters.toString().replace('[', '(').replace(']', ')'),
                                unknown.toString().replace('[', '{').replace(']', '}')));
                    }
                    if (!checkedProcedures.contains(procedure)) {
                        //This procedure will now be checked, so add it to the set of checked procedures
                        checkedProcedures.add(procedure);
                        //Add the parameters to list of known other processes.
                        otherProcesses.addAll(checkTerm.parameters.get(procedure));
                        checkTerm.procedures.get(procedure).accept(this);
                        procedureInvocation.continuation.accept(this);
                    }
                    return null;    //Process have been checked
                case Termination t:
                    return null;
                case BreakBehaviour b:
                    return null;
                case Introduce introducer:
                    if (checkName.equals(introducer.leftReceiver) || checkName.equals(introducer.rightReceiver)) //Check for self communications
                        throw new SelfCommunicationException(introducer);
                    if (!otherProcesses.contains(introducer.leftReceiver) || !otherProcesses.contains(introducer.rightReceiver))   //Check that the processes it introduces are defined
                        throw new NoSuchProcessException("Process(es)"+
                                (!otherProcesses.contains(introducer.leftReceiver) ? " "+introducer.leftReceiver : "")+
                                (!otherProcesses.contains(introducer.rightReceiver) ? " "+introducer.rightReceiver : "")+
                                " are not in the network, or are undefined variables when invoking "+introducer+".");
                    return introducer.continuation.accept(this);
                case Sender sender:
                    if (checkName.equals(sender.receiver))
                        throw new SelfCommunicationException(sender);
                    if (!otherProcesses.contains(sender.receiver))
                        throw new NoSuchProcessException("Process "+sender.receiver+" is not in the network, or are an undefined variable when executing term "+sender);
                    return sender.continuation.accept(this);
                case Offering offer:
                    if (checkName.equals(offer.sender))
                        throw new SelfCommunicationException(offer);
                    if (!otherProcesses.contains(offer.sender))
                        throw new NoSuchProcessException("Process "+offer.sender+" is not in the network, or are an undefined variable when executing term "+offer);
                    for (Behaviour procedureBehaviour : offer.branches.values())
                        procedureBehaviour.accept(this);
                    return offer.continuation.accept(this);
                case Receiver receiver:
                    if (receiver instanceof Introductee introductee){
                        //Check it's not self communication, in case it introduces itself to itself. The SnitchSet could get fooled otherwise
                        if (checkName.equals(introductee.sender))
                            throw new SelfCommunicationException(introductee);
                        otherProcesses.add(introductee.processID);  //Introduced process is now known
                    }
                    if (checkName.equals(receiver.sender))
                        throw new SelfCommunicationException(receiver);
                    if (!otherProcesses.contains(receiver.sender))
                        throw new NoSuchProcessException("Process "+receiver.sender+" is not in the network, or are an undefined variable when executing term "+receiver);
                    return receiver.continuation.accept(this);
                case Spawn spawner:
                    otherProcesses.add(spawner.variable);
                    copy(spawner.variable).Visit(spawner.processBehaviour); //Check for spawned process
                    return spawner.continuation.accept(this);
                case Condition conditional:
                    //Check both branches individually, using separate data
                    WellFormedChecker thenBranch = copy();
                    WellFormedChecker elseBranch = copy();
                    thenBranch.Visit(conditional.thenBehaviour);
                    elseBranch.Visit(conditional.elseBehaviour);
                    //Check the continuation, using the data from both branches.
                    thenBranch.Visit(conditional.continuation);
                    elseBranch.Visit(conditional.continuation);
                    return null;
                default:
                    throw new RuntimeException(new OperationNotSupportedException("Unexpected Behaviour encountered while checking for well-formedness. The Behaviour is of the type " + hostNode.getClass().getName()));
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
         * Checks if the continue-status of the branch(es) and the continuation are compatible.
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
                    throw new IncompleteBehaviourException("A branch in process %s wants to continue to an ancestor Behaviours continuation, but not such ancestor exists.%nThe problematic process is defined as %s%n".formatted(entry.getKey(), entry.getValue()));
                }
            }
        }


        /**
         * Explores a Behaviour tree to check for unreachable code, and finding branching behaviours that expect
         * a continuation from their ancestor branching behaviour where there is none.
         * @param hostNode The next node to check
         * @return If the called behaviour Cannot have, Can have, or Must have, a branching ancestor with a continuation to return to.
         */
        @Override
        public ContinueStatus Visit(NetworkASTNode hostNode) throws UnreachableBehaviourException{
            if (!(hostNode instanceof Behaviour))
                throw new IllegalStateException("Attempted to  check for code reachability on a Network AST node which is not a Behaviour. This suggest either incorrect usage, or a degenerate Network tree.");
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
                    throw new IllegalStateException("Unexpected node type when checking code-completeness: " + hostNode + "");
            }


        }
    }
}
