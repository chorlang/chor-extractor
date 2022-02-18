package utility.networkStatistics;

import extraction.network.*;
import extraction.network.utils.TreeVisitor;

import java.util.ArrayList;

public class NetworkProcessActionsPerProcedure implements TreeVisitor<Integer, NetworkASTNode> {
    public Integer Visit(NetworkASTNode hostNode){
        switch (hostNode){
            case Condition host:{
                return host.thenBehaviour.accept(this) + host.elseBehaviour.accept(this) + host.continuation.accept(this);
            }
            case Offering host: {
                int sum = 0;
                for (var branch : host.branches.values())
                    sum += branch.accept(this);
                return sum + host.continuation.accept(this);
            }
            case ProcessTerm host:
                int sum = 0;
                for (var procedure : host.procedures.values()){
                    sum += procedure.accept(this);
                }
                return sum;
            case Receive host:
                return ((Receive)hostNode).getContinuation().accept(this)+1;
            case Selection host:
                return ((Selection)hostNode).getContinuation().accept(this)+1;
            case Send host:
                return ((Send)hostNode).getContinuation().accept(this)+1;

            case ProcedureInvocation host: {return 0;}
            case Termination host: {return 0;}
            case Behaviour.BreakBehaviour b: {return 0;}
            case Network host:{}
            default:
                throw new UnsupportedOperationException("Invalid Network AST of type "+hostNode.getClass().getName());
        }
    }

    ArrayList<Integer> getLength(ProcessTerm term){
        var actionsProcedures = new ArrayList<Integer>();
        term.procedures.forEach((__, procedure) -> actionsProcedures.add(procedure.accept(this)));
        return actionsProcedures;
    }
}
