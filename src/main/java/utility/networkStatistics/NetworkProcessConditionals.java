package utility.networkStatistics;

import extraction.network.*;
import extraction.network.utils.TreeVisitor;

public class NetworkProcessConditionals implements TreeVisitor<Integer, NetworkASTNode> {
    public Integer Visit(NetworkASTNode hostNode){
        switch (hostNode){
            case Condition host:{
                return 1 + host.thenBehaviour.accept(this) + host.elseBehaviour.accept(this) + host.continuation.accept(this);
            }
            case Offering host: {
                int sum = 0;
                for (var branch : host.branches.values())
                    sum += branch.accept(this);
                double ratio = (double) sum / host.branches.size();
                return (int) ratio + host.continuation.accept(this);
            }
            case ProcessTerm host:
                return host.runtimeMain().accept(this);
            case Receive host:
                return host.getContinuation().accept(this);
            case Selection host:
                return host.getContinuation().accept(this);
            case Send host:
                return host.getContinuation().accept(this);
            case Introductee host:
                return host.getContinuation().accept(this);
            case Introduce host:
                return host.getContinuation().accept(this);
            case Behaviour.Interaction host:
                return host.getContinuation().accept(this);

            case ProcedureInvocation host: {return 0;}
            case Termination host: {return 0;}
            case Behaviour.BreakBehaviour host: {return 0;}
            case Network n:{}
            default:
                throw new UnsupportedOperationException("Unexpected network AST type "+hostNode.getClass().getName());
        }
    }
}
