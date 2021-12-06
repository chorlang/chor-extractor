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
        switch (hostNode.action) {
            case PROCESS_TERM: {
                var term = (ProcessTerm) hostNode;
                var processes = new HashSet<>(term.runtimeMain().accept(this));
                term.procedures.forEach((__, behaviour) -> processes.addAll(behaviour.accept(this)));
                return processes;
            }
            case SPAWN: {
                //The spawned process is in the same set as its parent, and it cannot learn of a process outside
                //its parents set, so no need to check the child.
                return ((Spawn) hostNode).continuation.accept(this);
            }
            case INTRODUCE: {
                var acquaint = (Introduce) hostNode;
                var interacting = new HashSet<String>();
                interacting.add(acquaint.leftReceiver);
                interacting.add(acquaint.rightReceiver);
                interacting.addAll(acquaint.continuation.accept(this));
                return interacting;
            }
            case INTRODUCTEE: {
                var familiarize = (Introductee) hostNode;
                var interacting = new HashSet<String>();
                interacting.add(familiarize.processID);
                interacting.add(familiarize.sender);
                interacting.addAll(familiarize.continuation.accept(this));
                return interacting;
            }
            case CONDITION: {
                var condition = (Condition) hostNode;
                var interacting = new HashSet<String>();
                interacting.addAll(condition.thenBehaviour.accept(this));
                interacting.addAll(condition.elseBehaviour.accept(this));
                return interacting;
            }
            case OFFERING: {
                var offer = (Offering) hostNode;
                var interacting = new HashSet<String>();
                interacting.add(offer.sender);
                offer.branches.forEach((s, behaviour) -> interacting.addAll(behaviour.accept(this)));
                return interacting;
            }
            case SELECTION: {
                var selector = (Selection) hostNode;
                var interacting = new HashSet<String>();
                interacting.add(selector.receiver);
                interacting.addAll(selector.continuation.accept(this));
                return interacting;
            }
            case RECEIVE: {
                var receiver = (Receive) hostNode;
                var interacting = new HashSet<String>();
                interacting.add(receiver.sender);
                interacting.addAll(receiver.continuation.accept(this));
                return interacting;
            }
            case SEND: {
                var sender = (Send) hostNode;
                var interacting = new HashSet<String>();
                interacting.add(sender.receiver);
                interacting.addAll(sender.continuation.accept(this));
                return interacting;
            }
            case TERMINATION:
                return new HashSet<>();
            case PROCEDURE_INVOCATION: {
                var inv = (ProcedureInvocation) hostNode;
                return new HashSet<>(inv.getParameters());
            }
            case NETWORK:
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
            switch (hostNode.action) {
                case SPAWN: {
                    //The spawned process is in the same set as its parent, and it cannot learn of a process outside
                    //its parents set, so no need to check the child.
                    return ((Spawn) hostNode).continuation.accept(this);
                }
                case INTRODUCE: {
                    var acquaint = (Introduce) hostNode;
                    var interacting = new HashSet<String>();
                    interacting.add(acquaint.leftReceiver);
                    interacting.add(acquaint.rightReceiver);
                    interacting.addAll(acquaint.continuation.accept(this));
                    return interacting;
                }
                case INTRODUCTEE: {
                    var familiarize = (Introductee) hostNode;
                    var interacting = new HashSet<String>();
                    interacting.add(familiarize.processID);
                    interacting.add(familiarize.sender);
                    interacting.addAll(familiarize.continuation.accept(this));
                    return interacting;
                }
                case CONDITION: {
                    var condition = (Condition) hostNode;
                    var interacting = new HashSet<String>();
                    interacting.addAll(condition.thenBehaviour.accept(this));
                    interacting.addAll(condition.elseBehaviour.accept(this));
                    return interacting;
                }
                case OFFERING: {
                    var offer = (Offering) hostNode;
                    var interacting = new HashSet<String>();
                    interacting.add(offer.sender);
                    offer.branches.forEach((s, behaviour) -> interacting.addAll(behaviour.accept(this)));
                    return interacting;
                }
                case SELECTION: {
                    var selector = (Selection) hostNode;
                    var interacting = new HashSet<String>();
                    interacting.add(selector.receiver);
                    interacting.addAll(selector.continuation.accept(this));
                    return interacting;
                }
                case RECEIVE: {
                    var receiver = (Receive) hostNode;
                    var interacting = new HashSet<String>();
                    interacting.add(receiver.sender);
                    interacting.addAll(receiver.continuation.accept(this));
                    return interacting;
                }
                case SEND: {
                    var sender = (Send) hostNode;
                    var interacting = new HashSet<String>();
                    interacting.add(sender.receiver);
                    interacting.addAll(sender.continuation.accept(this));
                    return interacting;
                }
                case TERMINATION:
                    return new HashSet<>();
                case PROCEDURE_INVOCATION: {
                    var inv = (ProcedureInvocation) hostNode;
                    HashSet<String> interacting = new HashSet<>(inv.getParameters());
                    if (!checkedProcedures.contains(inv.procedure)){
                        checkedProcedures.add(inv.procedure);
                        interacting.addAll(procedures.get(inv.procedure).accept(this));
                    }
                    return interacting;
                }
                case NETWORK:
                case PROCESS_TERM:
                default:
                    throw new UnsupportedOperationException("While finding used variable names, encountered term with behaviour of type " + hostNode.getClass().getName() + " which is not applicable");
            }
        }
    }
}
