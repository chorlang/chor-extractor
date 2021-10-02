package utility.networkStatistics;

import extraction.network.*;
import extraction.network.utils.TreeVisitor;

public class NetworkProcessConditionals implements TreeVisitor<Integer, NetworkASTNode> {
    public Integer Visit(NetworkASTNode hostNode){
        switch (hostNode.action){
            case CONDITION:{
                var host = (Condition)hostNode;
                return 1 + host.thenBehaviour.accept(this) + host.elseBehaviour.accept(this);
            }
            case OFFERING: {
                var host = (Offering) hostNode;
                int sum = 0;
                for (var branch : host.branches.values())
                    sum += branch.accept(this);
                double ratio = (double) sum / host.branches.size();
                return (int) ratio;
            }
            case PROCESS_TERM:
                return ((ProcessTerm)hostNode).main().accept(this);
            case RECEIVE:
                return ((Receive)hostNode).continuation.accept(this);
            case SELECTION:
                return ((Selection)hostNode).continuation.accept(this);
            case SEND:
                return ((Send)hostNode).continuation.accept(this);

            case PROCEDURE_INVOCATION:
            case TERMINATION:
                return 0;
            case NETWORK:
            default:
                throw new UnsupportedOperationException("Invalid Network AST");
        }
    }
}
