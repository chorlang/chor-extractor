package utility.fuzzing;

import extraction.network.*;
import extraction.network.utils.TreeVisitor;

public class ProcessSize implements TreeVisitor<Integer, NetworkASTNode> {
    @Override
    public Integer Visit(NetworkASTNode hostNode){
        switch (hostNode.action){
            case CONDITION:
                var con = (Condition)hostNode;
                return con.thenBehaviour.accept(this) + con.elseBehaviour.accept(this) + 1;
            case OFFERING:
                int sum = 0;
                for (Behaviour branch : ((Offering)hostNode).branches.values()){
                    sum += branch.accept(this);
                }
                return sum + 1;
            case RECEIVE:
                return ((Receive)hostNode).continuation.accept(this) + 1;
            case SELECTION:
                return ((Selection)hostNode).continuation.accept(this) + 1;
            case SEND:
                return ((Send)hostNode).continuation.accept(this) + 1;
            case PROCEDURE_INVOCATION:
                return 1;
            case TERMINATION:
                return 0;
            case NETWORK:
            case PROCESS_TERM:
            default:
                throw new UnsupportedOperationException("ERROR: Unable to get process-size because of invalid Network AST. Cannot visit type " + hostNode.getClass().getName());
        }
    }

    public static Integer compute(Behaviour rootNode){
        return new ProcessSize().Visit(rootNode);
    }
}
