package endpointprojection;

import extraction.network.*;

import java.util.HashMap;

public class Merging {
    static class MergingException extends RuntimeException{
        public MergingException(String s){
            super(s);
        }
    }

    static Behaviour merge(Behaviour left, Behaviour right){
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
            case Behaviour.NoneBehaviour nb -> Behaviour.NoneBehaviour.instance;
            case Behaviour.BreakBehaviour bb -> Behaviour.BreakBehaviour.instance;
            default -> throw new IllegalArgumentException("Behaviours of type " + left.getClass().getName() + " are not supported for merging");
        };

    }

    private static Behaviour merge(Send left, Send right){
        if (!left.receiver.equals(right.receiver) || !left.expression.equals(right.expression))
            throw new MergingException("Cant merge "+ left.receiver + " and " + right.receiver);
        var m = merge(left.getContinuation(), right.getContinuation());
        return new Send(left.receiver, left.expression, m);
    }

    private static Behaviour merge(Receive left, Receive right){
        if (!left.sender.equals(right.sender))
            throw new MergingException("Can't merge " + left.sender + " and " + right.sender);
        var m = merge(left.getContinuation(), right.getContinuation());
        return new Receive(left.sender, m);
    }

    private static Behaviour merge(Selection left, Selection right){
        if (!left.receiver.equals(right.receiver) || !left.label.equals(right.label))
            throw new MergingException("Can't merge " + left.receiver+"+"+left.label+" and "+right.receiver+"+"+right.label);
        var m = merge(left.getContinuation(), right.getContinuation());
        return new Selection(left.receiver, left.label, m);
    }

    private static Behaviour merge(Offering left, Offering right){
        if (!left.sender.equals(right.sender))
            throw new MergingException("Can't merge "+left.sender+" and "+right.sender);

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

        return new Offering(left.sender, labels);
    }

    private static Behaviour merge(Condition left, Condition right){
        Behaviour leftCondition = merge(left.thenBehaviour, right.thenBehaviour);
        Behaviour rightCondition = merge(left.elseBehaviour, right.elseBehaviour);
        Behaviour continuation = merge(left.continuation, right.continuation);

        if (!left.expression.equals(right.expression))
            throw new MergingException("Can't merge conditions "+leftCondition+" and "+rightCondition);
        return new Condition(left.expression, leftCondition, rightCondition, continuation);
    }

    private static Behaviour merge(ProcedureInvocation left, ProcedureInvocation right){
        if (left.procedure.equals(right.procedure) && left.getParameters().equals(right.getParameters()))
            return new ProcedureInvocation(left.procedure, left.getParameters(), merge(left.continuation, right.continuation));
        else
            throw new MergingException("Can't merge procedures "+left.procedure+" and "+right.procedure);
    }
}
