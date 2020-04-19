package utility.networkStatistics;

import extraction.network.*;
import extraction.network.utils.TreeVisitor;

public class NetworkProcessActions implements TreeVisitor<Integer, Behaviour> {
    public Integer Visit(Behaviour hostNode){
        switch (hostNode.getAction()){
            case CONDITION:{
                var host = (Condition)hostNode;
                return host.thenBehaviour.accept(this) + host.elseBehaviour.accept(this);
            }
            case OFFERING: {
                var host = (Offering) hostNode;
                int sum = 0;
                for (var branch : host.branches.values())
                    sum += branch.accept(this);
                return sum+1;
            }
            case PROCESS_TERM:
                var host = (ProcessTerm)hostNode;
                //I'm assuming this is equivalent to the foldRight method in Kotlin
                int sum = 0;
                for (var procedure : host.procedures.values()){
                    sum += procedure.accept(this);
                }
                return sum + host.main.accept(this);
            case RECEIVE:
                return ((Receive)hostNode).continuation.accept(this)+1;
            case SELECTION:
                return ((Selection)hostNode).continuation.accept(this)+1;
            case SEND:
                return ((Send)hostNode).continuation.accept(this)+1;

            case PROCEDURE_INVOCATION:
            case TERMINATION:
                return 0;
            case NETWORK:
            default:
                throw new UnsupportedOperationException("Invalid Network AST");
        }
    }
}
