package utility.networkStatistics;

import extraction.network.*;
import extraction.network.utils.TreeVisitor;

import java.util.ArrayList;

public class NetworkProcessActionsPerProcedure implements TreeVisitor<Integer, Behaviour> {
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
                return sum;
            }
            case PROCESS_TERM:
                var host = (ProcessTerm)hostNode;
                int sum = 0;
                for (var procedure : host.procedures.values()){
                    sum += procedure.accept(this);
                }
                return sum;
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

    ArrayList<Integer> getLength(ProcessTerm term){
        var actionsProcedures = new ArrayList<Integer>();
        term.procedures.forEach((__, procedure) -> actionsProcedures.add(procedure.accept(this)));
        return actionsProcedures;
    }
}
