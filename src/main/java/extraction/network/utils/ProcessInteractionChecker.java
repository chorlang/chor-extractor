package extraction.network.utils;

import extraction.network.*;

import java.util.HashSet;
import java.util.Map;

/**
 * This tree visitor visits a ProcessTerm in a Network (but not the Network itself) to find the names of
 * all processes that directly interacts with the process in question.
 */
public class ProcessInteractionChecker implements TreeVisitor<HashSet<String>, NetworkASTNode> {

    @Override
    public HashSet<String> Visit(NetworkASTNode hostNode) {
        switch (hostNode) {
            case ProcessTerm term: {
                var processes = new HashSet<>(term.runtimeMain().accept(this));
                term.procedures.forEach((__, behaviour) -> processes.addAll(behaviour.accept(this)));
                return processes;
            }
            case Spawn spawn: {
                //The spawned process is in the same set as its parent, and it cannot learn of a process outside
                //its parents set, so no need to check the child.
                return spawn.getContinuation().accept(this);
            }
            case Introduce introducer: {
                var interacting = new HashSet<String>();
                interacting.add(introducer.leftReceiver);
                interacting.add(introducer.rightReceiver);
                interacting.addAll(introducer.getContinuation().accept(this));
                return interacting;
            }
            case Introductee introductee: {
                var interacting = new HashSet<String>();
                interacting.add(introductee.processID);
                interacting.add(introductee.sender);
                interacting.addAll(introductee.getContinuation().accept(this));
                return interacting;
            }
            case Condition condition: {
                var interacting = new HashSet<String>();
                interacting.addAll(condition.thenBehaviour.accept(this));
                interacting.addAll(condition.elseBehaviour.accept(this));
                interacting.addAll(condition.getContinuation().accept(this));
                return interacting;
            }
            case Offering offer: {
                var interacting = new HashSet<String>();
                interacting.add(offer.sender);
                offer.branches.forEach((s, behaviour) -> interacting.addAll(behaviour.accept(this)));
                interacting.addAll(offer.getContinuation().accept(this));
                return interacting;
            }
            case Selection selector: {
                var interacting = new HashSet<String>();
                interacting.add(selector.receiver);
                interacting.addAll(selector.getContinuation().accept(this));
                return interacting;
            }
            case Receive receiver: {
                var interacting = new HashSet<String>();
                interacting.add(receiver.sender);
                interacting.addAll(receiver.getContinuation().accept(this));
                return interacting;
            }
            case Send sender: {
                var interacting = new HashSet<String>();
                interacting.add(sender.receiver);
                interacting.addAll(sender.getContinuation().accept(this));
                return interacting;
            }
            case ProcedureInvocation invocation: {
                HashSet<String> interacting = new HashSet<>(invocation.getParameters());
                interacting.addAll(invocation.getContinuation().accept(this));
                return interacting;
            }
            case Termination t:
                return new HashSet<>();
            case Behaviour.BreakBehaviour b:
                //The continuation will be checked when checking the ancestor branching behaviour
                return new HashSet<>();
            case Network n:{}
            default:
                throw new UnsupportedOperationException("While splitting network, searching for which processes communicate, encountered behaviour of type " + hostNode.getClass().getName() + " which is not applicable");
        }
    }

    /**
     * Goes through all the terms of the main behaviour of a process, and returns a set of all
     * process variable names that it finds. Procedures are unfolded recursively.
     */
    //Ideally, this would not consider procedure definition parameters part of the used variables,
    //but I'm too lazy to work that in that now.
    public static HashSet<String> CheckUsedVariables(ProcessTerm term){
        var varChecker = new VariableUsageChecker(term.procedures);
        return varChecker.Visit(term.rawMain());
    }

    /**
     * Intended for finding out which variables are used by a process in a loop.
     * Goes through the behaviour, and all its continuations and procedure invocations, and
     * creates a set of all process names/variables it uses.
     */
    public static class VariableUsageChecker implements TreeVisitor<HashSet<String>, NetworkASTNode> {
        private final Map<String, Behaviour> procedures;
        private final HashSet<String> checkedProcedures = new HashSet<>();
        public VariableUsageChecker(Map<String, Behaviour> procedures){
            this.procedures = procedures;
        }

        @Override
        public HashSet<String> Visit(NetworkASTNode hostNode) {
            switch (hostNode) {
                case Spawn spawn: {
                    //The spawned process is in the same set as its parent, and it cannot learn of a process outside
                    //its parents set, so no need to check the child.
                    return spawn.getContinuation().accept(this);
                }
                case Introduce introducer: {
                    var interacting = new HashSet<String>();
                    interacting.add(introducer.leftReceiver);
                    interacting.add(introducer.rightReceiver);
                    interacting.addAll(introducer.getContinuation().accept(this));
                    return interacting;
                }
                case Introductee introductee: {
                    var interacting = new HashSet<String>();
                    interacting.add(introductee.processID);
                    interacting.add(introductee.sender);
                    interacting.addAll(introductee.getContinuation().accept(this));
                    return interacting;
                }
                case Condition condition: {
                    var interacting = new HashSet<String>();
                    interacting.addAll(condition.thenBehaviour.accept(this));
                    interacting.addAll(condition.elseBehaviour.accept(this));
                    interacting.addAll(condition.getContinuation().accept(this));
                    return interacting;
                }
                case Offering offer: {
                    var interacting = new HashSet<String>();
                    interacting.add(offer.sender);
                    offer.branches.forEach((s, behaviour) -> interacting.addAll(behaviour.accept(this)));
                    interacting.addAll(offer.getContinuation().accept(this));
                    return interacting;
                }
                case Selection selector: {
                    var interacting = new HashSet<String>();
                    interacting.add(selector.receiver);
                    interacting.addAll(selector.getContinuation().accept(this));
                    return interacting;
                }
                case Receive receiver: {
                    var interacting = new HashSet<String>();
                    interacting.add(receiver.sender);
                    interacting.addAll(receiver.getContinuation().accept(this));
                    return interacting;
                }
                case Send sender: {
                    var interacting = new HashSet<String>();
                    interacting.add(sender.receiver);
                    interacting.addAll(sender.getContinuation().accept(this));
                    return interacting;
                }
                case ProcedureInvocation invocation: {
                    HashSet<String> interacting = new HashSet<>(invocation.getParameters());
                    if (!checkedProcedures.contains(invocation.procedure)){
                        checkedProcedures.add(invocation.procedure);
                        interacting.addAll(procedures.get(invocation.procedure).accept(this));
                    }
                    interacting.addAll(invocation.getContinuation().accept(this));
                    return interacting;
                }
                case Termination t:
                    return new HashSet<>();
                case Behaviour.BreakBehaviour b:
                    return new HashSet<>();
                //Network and ProcessTerm intentionally left out
                default:
                    throw new UnsupportedOperationException("While finding used variable names, encountered term with behaviour of type " + hostNode.getClass().getName() + " which is not applicable");
            }
        }
    }
}
