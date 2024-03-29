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
                return host.thenChoreography.accept(this) + host.elseChoreography.accept(this) + host.continuation.accept(this) + 1;
            }

            case COMMUNICATION:
            case SELECTION:
                var host = (ChoreographyBody.Interaction)hostNode;
                return host.getContinuation().accept(this) + 1;
            case INTRODUCTION:
                return ((Introduction)hostNode).continuation.accept(this)+1;
            case SPAWN:
                return ((Spawn)hostNode).continuation.accept(this)+1;//Not sure if child behaviour should be counted

            case NONE:
                return 0;
            case TERMINATION:
            case PROCEDURE_INVOCATION:
                return 1;

            case PROCEDURE_DEFINITION:
            case CHOREOGRAPHY:
            case PROGRAM:
            default:
                throw new UnsupportedOperationException("Invalid AST or incorrect root node for this function. Has type: "+hostNode.getType());
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
