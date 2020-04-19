package utility.choreographyStatistics;

import extraction.choreography.*;
import extraction.network.utils.TreeVisitor;

import java.util.ArrayList;

public class NumberOfActions implements TreeVisitor<Integer, ChoreographyASTNode>{
    @Override
    public Integer Visit(ChoreographyASTNode hostNode){
        switch (hostNode.getType()){
            case CONDITION:{
                var host = (Condition)hostNode;
                return host.thenChoreography.accept(this) + host.elseChoreography.accept(this) + 1;
            }

            case COMMUNICATION:
            case SELECTION:
                var host = (ChoreographyBody.Interaction)hostNode;
                return host.continuation.accept(this) + 1;

            case TERMINATION:
            case PROCEDURE_INVOCATION:
                return 1;

            case PROCEDURE_DEFINITION:
            case CHOREOGRAPHY:
            case PROGRAM:
            default:
                throw new UnsupportedOperationException("Invalid AST or incorrect root node for this function");
        }
    }

    public static int compute(Choreography choreography){
        int sum = 0;
        for (var procedure : choreography.procedures){
            sum += procedure.body.accept(new NumberOfActions());
        }

        return sum + choreography.main.accept(new NumberOfActions());
    }
}
