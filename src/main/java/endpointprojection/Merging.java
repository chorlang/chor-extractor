package endpointprojection;

import extraction.network.*;
import extraction.network.Behaviour.BreakBehaviour;
import extraction.network.Behaviour.NoneBehaviour;

import java.util.HashMap;

public class Merging {
    static class MergingException extends RuntimeException{
        public MergingException(String s){
            super(s);
        }
    }

    private final Behaviour continuation;
    public boolean consumedContinuation;
    //If the provided continuation has been appended. It must only be appended once
    private Merging(Behaviour continuation){
        this.continuation = continuation;
        consumedContinuation = continuation instanceof NoneBehaviour;
        //If the continuation is none, it doesn't matter if it gets appended or not.
    }

    public static Behaviour merge(Behaviour thenBehaviour, Behaviour elseBehaviour, Behaviour continuation){
        var merger =  new Merging(continuation);
        Behaviour result = merger.merge(thenBehaviour, elseBehaviour);
        if (!merger.consumedContinuation)
            throw new IllegalArgumentException("Unable to append a continuation to merged Behaviours." +
                    "Check your input.%nUnapendable continuation: %s%nThen Behaviour: %s%nElse Behaviour: %s%n"
                            .formatted(continuation, thenBehaviour, elseBehaviour));
        return result;
    }

    private Behaviour merge(Behaviour left, Behaviour right){
        if (!left.getClass().equals(right.getClass()))
            throw new MergingException("Can't merge " + left + " with " + right);

        return switch (left) {
            case Send s -> merge((Send) left, (Send) right);
            case Receive r -> merge((Receive) left, (Receive) right);
            case Termination t -> Termination.instance;
            case Selection s -> merge((Selection) left, (Selection) right);
            case Offering o -> merge((Offering) left, (Offering) right);
            case Condition c -> merge((Condition) left, (Condition) right);
            case ProcedureInvocation pi -> merge((ProcedureInvocation) left, (ProcedureInvocation) right);
            case Introductee leftIntroductee -> merge(leftIntroductee, (Introductee) right);
            case Introduce leftIntroduce -> merge(leftIntroduce, (Introduce) right);
            case Spawn leftSpawn -> merge(leftSpawn, (Spawn) right);
            case NoneBehaviour nb -> {
                if (consumedContinuation)
                    yield NoneBehaviour.instance;
                else {
                    consumedContinuation = true;
                    yield continuation;
                }
            }
            case BreakBehaviour bb -> {
                if (consumedContinuation)
                    yield BreakBehaviour.instance;
                else {
                    consumedContinuation = true;
                    yield continuation;
                }
            }
            default -> throw new IllegalArgumentException("Behaviours of type " + left.getClass().getName() + " are not supported for merging");
        };

    }
    /*
    When adding new branching behaviour, make sure to hande the continuation first.
    The order in which the continuation variable is handled is important for correctness.
     */

    private Behaviour merge(Introductee left, Introductee right){
        if (!left.sender.equals(right.sender) || !left.processID.equals(right.processID))
            throw new MergingException("Can't merge "+left+" and "+right);
        var m = merge(left.getContinuation(), right.getContinuation());
        return new Introductee(left.sender, left.processID, m);
    }

    private Behaviour merge(Introduce left, Introduce right){
        if (!left.leftReceiver.equals(right.leftReceiver) || !left.rightReceiver.equals(right.rightReceiver))
            throw new MergingException("Can't merge "+left+" and "+right);
        var m = merge(left.getContinuation(), right.getContinuation());
        return new Introduce(left.leftReceiver, left.rightReceiver, m);
    }

    private Behaviour merge(Spawn left, Spawn right){
        if (!left.variable.equals(right.variable) || !left.processBehaviour.equals(right.processBehaviour))
            throw new MergingException("Can't merge "+left+" and "+right);
        var m = merge(left.getContinuation(), right.getContinuation());
        return new Spawn(left.variable, left.processBehaviour, m);
    }

    private Behaviour merge(Send left, Send right){
        if (!left.receiver.equals(right.receiver) || !left.expression.equals(right.expression))
            throw new MergingException("Can't merge "+ left.receiver + " and " + right.receiver);
        var m = merge(left.getContinuation(), right.getContinuation());
        return new Send(left.receiver, left.expression, m);
    }

    private Behaviour merge(Receive left, Receive right){
        if (!left.sender.equals(right.sender))
            throw new MergingException("Can't merge " + left.sender + " and " + right.sender);
        var m = merge(left.getContinuation(), right.getContinuation());
        return new Receive(left.sender, m);
    }

    private Behaviour merge(Selection left, Selection right){
        if (!left.receiver.equals(right.receiver) || !left.label.equals(right.label))
            throw new MergingException("Can't merge " + left.receiver+"+"+left.label+" and "+right.receiver+"+"+right.label);
        var m = merge(left.getContinuation(), right.getContinuation());
        return new Selection(left.receiver, left.label, m);
    }

    private Behaviour merge(Offering left, Offering right){
        if (!left.sender.equals(right.sender))
            throw new MergingException("Can't merge "+left.sender+" and "+right.sender);

        Behaviour thisContinuation;
        //If this is the first offering or conditional, append the continuation to it.
        //Otherwise, merge the continuations
        if (consumedContinuation)
            thisContinuation = merge(left.continuation, right.continuation);
        else {
            Merging continuationMerger = new Merging(continuation);
            thisContinuation = continuationMerger.merge(left.continuation, right.continuation);
            if (!continuationMerger.consumedContinuation){
                throw new IllegalArgumentException("Unable to append a continuation to merged Behaviours." +
                        "Check your input.%nUnapendable continuation: %s%nLeft Behaviour: %s%nRight Behaviour: %s%n"
                                .formatted(continuation, left, right));
            }
            consumedContinuation = true;
        }

        var leftBranches = left.branches;
        var rightBranches = right.branches;
        var labels = new HashMap<String, Behaviour>();

        //For all keys in the left offering term
        for (var leftKey : leftBranches.keySet()){
            //If the right offering term contains the same label, merge the behaviours for that label.
            if (rightBranches.containsKey(leftKey)){
                labels.put(leftKey, merge(leftBranches.get(leftKey), rightBranches.get(leftKey)));
            }
            //Else, add the labeled behaviour from the left offering term.
            else{
                labels.put(leftKey, leftBranches.get(leftKey));
            }
        }

        //For all the labels of the right offering, if the left offering contains no such label,
        //add the labeled behaviour from the right offering term.
        for (var rightKey : rightBranches.keySet()){
            if (!leftBranches.containsKey(rightKey))
                labels.put(rightKey, rightBranches.get(rightKey));
        }


        return new Offering(left.sender, labels, thisContinuation);
    }

    private Behaviour merge(Condition left, Condition right){
        if (!left.expression.equals(right.expression))
            throw new MergingException("Can't merge conditions "+left+" and "+right);

        Behaviour thisContinuation;
        if (consumedContinuation)
            thisContinuation = merge(left.continuation, right.continuation);
        else {
            Merging continuationMerger = new Merging(continuation);
            thisContinuation = continuationMerger.merge(left.continuation, right.continuation);
            if (!continuationMerger.consumedContinuation){
                throw new IllegalArgumentException("Unable to append a continuation to merged Behaviours." +
                        "Check your input.%nUnapendable continuation: %s%nLeft Behaviour: %s%nRight Behaviour: %s%n"
                                .formatted(continuation, left, right));
            }
            consumedContinuation = true;
        }

        Behaviour leftCondition = merge(left.thenBehaviour, right.thenBehaviour);
        Behaviour rightCondition = merge(left.elseBehaviour, right.elseBehaviour);

        return new Condition(left.expression, leftCondition, rightCondition, thisContinuation);
    }

    private Behaviour merge(ProcedureInvocation left, ProcedureInvocation right){
        if (!(consumedContinuation || continuation instanceof NoneBehaviour))
            throw new UnsupportedOperationException("Appending continuations to procedure invocations have not been implemented yet");
        if (left.continuation != Behaviour.NoneBehaviour.instance || right.continuation != Behaviour.NoneBehaviour.instance)
            throw new UnsupportedOperationException("Merging procedure invocations with continuations has not been implemented yet");
        if (left.procedure.equals(right.procedure) && left.getParameters().equals(right.getParameters()))
            return new ProcedureInvocation(left.procedure, left.getParameters(), merge(left.continuation, right.continuation));
        else
            throw new MergingException("Can't merge procedures "+left.procedure+" and "+right.procedure);
    }
}
