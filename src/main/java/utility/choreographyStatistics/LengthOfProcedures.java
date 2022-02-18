package utility.choreographyStatistics;

import extraction.choreography.*;
import extraction.network.utils.TreeVisitor;

import java.util.ArrayList;

public class LengthOfProcedures implements TreeVisitor<Integer, ChoreographyASTNode> {
    @Override
    public Integer Visit(ChoreographyASTNode hostNode){
        switch (hostNode.getType()){
            case CONDITION:{
                var host = (Condition)hostNode;
                return host.thenChoreography.accept(this) + host.elseChoreography.accept(this);
            }
            case PROCEDURE_DEFINITION:{
                var host = (ProcedureDefinition)hostNode;
                return host.body.accept(this);
            }

            case COMMUNICATION:
            case SELECTION:{
                var host = (ChoreographyBody.Interaction)hostNode;
                return host.getContinuation().accept(this) + 1;
            }

            case TERMINATION:
            case PROCEDURE_INVOCATION:
                return 1;
            case CHOREOGRAPHY:
            case PROGRAM:
            default:
                throw new UnsupportedOperationException();
        }
    }

    public static ArrayList<Integer> getLength(Choreography chorRoot) {
        var stat = new ArrayList<Integer>();
        var lengthCounter = new LengthOfProcedures();
        chorRoot.procedures.forEach(procedure -> stat.add(procedure.accept(lengthCounter)));
        return stat;
    }
}
