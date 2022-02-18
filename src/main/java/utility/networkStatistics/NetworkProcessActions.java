package utility.networkStatistics;

import extraction.network.*;
import extraction.network.utils.TreeVisitor;

public class NetworkProcessActions implements TreeVisitor<Integer, NetworkASTNode> {
    public Integer Visit(NetworkASTNode hostNode){
        switch (hostNode){
            case Condition host:{
                return host.thenBehaviour.accept(this) + host.elseBehaviour.accept(this) + host.continuation.accept(this);
            }
            case Offering host: {
                int sum = 0;
                for (var branch : host.branches.values())
                    sum += branch.accept(this);
                return sum+1;
            }
            case ProcessTerm host:
                //I'm assuming this is equivalent to the foldRight method in Kotlin
                int sum = 0;
                for (var procedure : host.procedures.values()){
                    sum += procedure.accept(this);
                }
                return sum + host.runtimeMain().accept(this);
            case Receive host:
                return ((Receive)hostNode).getContinuation().accept(this)+1;
            case Selection host:
                return ((Selection)hostNode).getContinuation().accept(this)+1;
            case Send host:
                return ((Send)hostNode).getContinuation().accept(this)+1;

            case ProcedureInvocation host: {return 0;}
            case Termination host: {return 0;}
            case Behaviour.BreakBehaviour b: {return 0;}
            case Network host: {}
            default:
                throw new UnsupportedOperationException("Invalid Network AST of type " + hostNode.getClass().getName() + " toString: "+hostNode);
        }
    }
}
