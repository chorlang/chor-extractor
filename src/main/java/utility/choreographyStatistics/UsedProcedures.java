package utility.choreographyStatistics;

import extraction.choreography.ChoreographyASTNode;
import extraction.choreography.ChoreographyBody;
import extraction.choreography.Condition;
import extraction.network.utils.TreeVisitor;

import java.util.HashSet;
import java.util.Set;

public class UsedProcedures implements TreeVisitor<Set<String>, ChoreographyASTNode> {
    @Override
    public Set<String> Visit(ChoreographyASTNode hostNode) {
        switch (hostNode.getType()){
            case CONDITION: {
                Condition host = (Condition) hostNode;
                Set<String> freeProcesses = host.thenChoreography.accept(this);
                freeProcesses.addAll(host.elseChoreography.accept(this));
                return freeProcesses;
            }
            case COMMUNICATION:
            case SELECTION: {
                var host = (ChoreographyBody.Interaction) hostNode;
                return host.continuation.accept(this);
            }
            case TERMINATION:
            case PROCEDURE_INVOCATION:
                return new HashSet<>();

            case PROCEDURE_DEFINITION:
            case CHOREOGRAPHY:
            case PROGRAM:
            default:
                throw new UnsupportedOperationException("Invalid choreography AST");
        }
    }

    public static Set<String> usedProcedures(ChoreographyASTNode hostNode){
        return hostNode.accept(new UsedProcedures());
    }
}