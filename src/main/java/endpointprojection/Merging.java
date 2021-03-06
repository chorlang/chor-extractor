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
        if (left.getAction() != right.getAction())
            throw new MergingException("Can't merge " + left + " with " + right);
        switch (left.getAction()){
            case SEND:
                return merge((Send)left, (Send)right);
            case RECEIVE:
                return merge((Receive)left, (Receive)right);
            case TERMINATION:
                return Termination.getTermination();
            case SELECTION:
                return merge((Selection)left, (Selection)right);
            case OFFERING:
                return merge((Offering)left, (Offering)right);
            case CONDITION:
                return merge((Condition)left, (Condition)right);
            case PROCEDURE_INVOCATION:
                return merge((ProcedureInvocation)left, (ProcedureInvocation)right);
            default:
                throw new IllegalArgumentException("Behaviours of type " + left.getClass().getName() + " are not supported for merging");
        }

    }

    private static Behaviour merge(Send left, Send right){
        if (!left.receiver.equals(right.receiver) || !left.expression.equals(right.expression))
            throw new MergingException("Cant merge "+ left.receiver + " and " + right.receiver);
        var m = merge(left.continuation, right.continuation);
        return new Send(left.receiver, left.expression, m);
    }

    private static Behaviour merge(Receive left, Receive right){
        if (!left.sender.equals(right.sender))
            throw new MergingException("Can't merge " + left.sender + " and " + right.sender);
        var m = merge(left.continuation, right.continuation);
        return new Receive(left.sender, m);
    }

    private static Behaviour merge(Selection left, Selection right){
        if (!left.receiver.equals(right.receiver) || !left.label.equals(right.label))
            throw new MergingException("Can't merge " + left.receiver+"+"+left.label+" and "+right.receiver+"+"+right.label);
        var m = merge(left.continuation, right.continuation);
        return new Selection(left.receiver, left.label, m);
    }

    private static Behaviour merge(Offering left, Offering right){
        if (!left.sender.equals(right.sender))
            throw new MergingException("Can't merge "+left.sender+" and "+right.sender);

        var leftBranches = left.branches;
        var rightBranches = right.branches;
        var labels = new HashMap<String, Behaviour>();

        for (var leftKey : leftBranches.keySet()){
            if (rightBranches.containsKey(leftKey)){
                labels.put(leftKey, merge(leftBranches.get(leftKey), rightBranches.get(leftKey)));
                rightBranches.remove(leftKey);
            } else{
                labels.put(leftKey, leftBranches.get(leftKey));
            }
        }
        for (var rightKey : rightBranches.keySet()){
            labels.put(rightKey, rightBranches.get(rightKey));
        }

        return new Offering(left.sender, labels);
    }

    private static Behaviour merge(Condition left, Condition right){
        var leftCondition = merge(left.thenBehaviour, right.thenBehaviour);
        var rightCondition = merge(left.elseBehaviour, right.elseBehaviour);
        if (!left.expression.equals(right.expression))
            throw new MergingException("Can't merge conditions "+leftCondition+" and "+rightCondition);
        return new Condition(left.expression, leftCondition, rightCondition);
    }

    private static Behaviour merge(ProcedureInvocation left, ProcedureInvocation right){
        if (left.procedure.equals(right.procedure))
            return new ProcedureInvocation(left.procedure);
        else
            throw new MergingException("Can't merge procedures "+left.procedure+" and "+right.procedure);
    }
}
