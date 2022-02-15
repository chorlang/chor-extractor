package utility.choreographyStatistics;

import extraction.choreography.ChoreographyASTNode;
import extraction.choreography.ChoreographyBody;
import extraction.choreography.Condition;
import extraction.choreography.ProcedureInvocation;
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
                return host.getContinuation().accept(this);
            }
            case TERMINATION:
                return new HashSet<>();
            case PROCEDURE_INVOCATION:
                return new HashSet<>(){{add(((ProcedureInvocation)hostNode).procedure);}};

            case PROCEDURE_DEFINITION:
            case CHOREOGRAPHY:
            case PROGRAM:
            default:
                throw new UnsupportedOperationException("Invalid choreography AST");
        }
    }

    /**
     * Finds all procedures that are invoked within a ChoreographyBody.
     * @param hostNode The ChoreographyBody to examine.
     * @return A set containing the names of all found ProcedureInvocations
     */
    public static Set<String> usedProcedures(ChoreographyBody hostNode){
        return hostNode.accept(new UsedProcedures());
    }
}