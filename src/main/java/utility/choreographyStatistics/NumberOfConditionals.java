package utility.choreographyStatistics;

import extraction.choreography.Choreography;
import extraction.choreography.ChoreographyASTNode;
import extraction.choreography.ChoreographyBody;
import extraction.choreography.Condition;
import extraction.network.utils.TreeVisitor;

public class NumberOfConditionals implements TreeVisitor<Integer, ChoreographyASTNode> {
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
                return host.continuation.accept(this);

            case TERMINATION:
            case PROCEDURE_INVOCATION:
                return 0;

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
