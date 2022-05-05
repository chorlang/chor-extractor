package utility.choreographyStatistics;

import extraction.choreography.*;
import extraction.network.utils.TreeVisitor;

import java.util.HashSet;
import java.util.Set;

public class UsedProcedures implements TreeVisitor<Set<String>, ChoreographyASTNode> {
    @Override
    public Set<String> Visit(ChoreographyASTNode hostNode) {
        switch (hostNode.getType()){
            case CONDITION: {
                Condition host = (Condition) hostNode;
                Set<String> mentionedProcedures = host.thenChoreography.accept(this);
                mentionedProcedures.addAll(host.elseChoreography.accept(this));
                mentionedProcedures.addAll(host.continuation.accept(this));
                return mentionedProcedures;
            }
            case MULTICOM: {
                Multicom host = (Multicom) hostNode;
                return host.getContinuation().accept(this);
            }
            case SPAWN:{
                Spawn host = (Spawn) hostNode;
                return host.getContinuation().accept(this);
            }
            case INTRODUCTION: {
                return ((Introduction) hostNode).continuation.accept(this);
            }
            case COMMUNICATION:
            case SELECTION: {
                var host = (ChoreographyBody.Interaction) hostNode;
                return host.getContinuation().accept(this);
            }
            case TERMINATION:
            case NONE:
                return new HashSet<>();
            case PROCEDURE_INVOCATION:
                return new HashSet<>(){{add(((ProcedureInvocation)hostNode).procedure);}};

            case PROCEDURE_DEFINITION:
            case CHOREOGRAPHY:
            case PROGRAM:
            default:
                throw new UnsupportedOperationException("Unexpected choreography AST node type "+hostNode.getType());
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