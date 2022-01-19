package utility.fuzzing;

import extraction.network.*;
import extraction.network.utils.TreeVisitor;

public class ProcessSize implements TreeVisitor<Integer, NetworkASTNode> {
    @Override
    public Integer Visit(NetworkASTNode hostNode){
        switch (hostNode){
            case Condition con:
                return con.thenBehaviour.accept(this) + con.elseBehaviour.accept(this) + 1;
            case Offering offering:
                int sum = 0;
                for (Behaviour branch : ((Offering)hostNode).branches.values()){
                    sum += branch.accept(this);
                }
                return sum + 1;
            case Receive r:
                return ((Receive)hostNode).getContinuation().accept(this) + 1;
            case Selection s:
                return ((Selection)hostNode).getContinuation().accept(this) + 1;
            case Send s:
                return ((Send)hostNode).getContinuation().accept(this) + 1;
            case ProcedureInvocation pi:
                return 1;
            case Termination t:
                return 0;
            default:
                throw new UnsupportedOperationException("ERROR: Unable to get process-size because of invalid Network AST. Cannot visit type " + hostNode.getClass().getName());
        }
    }

    public static Integer compute(Behaviour rootNode){
        return new ProcessSize().Visit(rootNode);
    }
}
