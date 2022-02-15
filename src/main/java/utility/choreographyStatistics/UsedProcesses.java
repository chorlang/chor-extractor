package utility.choreographyStatistics;

import extraction.choreography.Choreography;
import extraction.choreography.ChoreographyASTNode;
import extraction.choreography.ChoreographyBody;
import extraction.choreography.Condition;
import extraction.network.utils.TreeVisitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UsedProcesses implements TreeVisitor<Set<String>, ChoreographyASTNode> {
    @Override
    public Set<String> Visit(ChoreographyASTNode hostNode) {
        switch (hostNode.getType()){
            case CONDITION: {
                Condition host = (Condition) hostNode;
                Set<String> freeProcesses = host.thenChoreography.accept(this);
                freeProcesses.addAll(host.elseChoreography.accept(this));
                freeProcesses.add(host.process);
                return freeProcesses;
            }
            case COMMUNICATION:
            case SELECTION: {
                var host = (ChoreographyBody.Interaction) hostNode;
                Set<String> freeProcesses = host.getContinuation().accept(this);
                freeProcesses.add(host.getSender());
                freeProcesses.add(host.getReceiver());
                return freeProcesses;
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

    /**
     * Finds all process names that are mentioned in a ChoreographyBody.
     * If the body invokes another procedure, the processes in that procedure is not counted in.
     * @param node The ChoreographyBody to analyse.
     * @return A set of all process names explicitly mentioned.
     */
    private static Set<String> freeProcessesNames(ChoreographyASTNode node){
        return node.accept(new UsedProcesses());
    }

    /**
     * Goes through the procedures of a choreography, and finds out which processes are used in each procedure.
     * If a procedure invokes another procedure, then the processes in the second procedure is considered to
     * be in the first procedure as well.
     * @param choreography The choreography whose procedures should be checked.
     * @return A mapping from the name of each procedure, to a set if the names of processes used in that procedure.
     */
    public static Map<String, Set<String>> usedProcesses(Choreography choreography){
        //Find which procedures call other procedures.
        var calls = new HashMap<String, Set<String>>();
        choreography.procedures.forEach(procedure ->
                calls.put(procedure.name, UsedProcedures.usedProcedures(procedure.body)));

        //Find out which processes are explicitly mentioned in a procedure definition
        var oldUsedProcesses = new HashMap<String, Set<String>>();
        var newUsedProcesses = new HashMap<String, Set<String>>();
        choreography.procedures.forEach(procedure -> {
            newUsedProcesses.put(procedure.name, freeProcessesNames(procedure.body));
        });

        //I think this adds processes used by an invoked procedure, to a procedures list of processes.
        while (!oldUsedProcesses.equals(newUsedProcesses)){
            choreography.procedures.forEach(procedure ->
                    oldUsedProcesses.put(procedure.name, newUsedProcesses.get(procedure.name)));
            choreography.procedures.forEach(procedure ->{
                calls.get(procedure.name).forEach(call ->{
                        newUsedProcesses.get(procedure.name).addAll(oldUsedProcesses.get(call));
                });
            });
        }
        return oldUsedProcesses;
    }
}
