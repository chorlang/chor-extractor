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
                Set<String> freeProcesses = host.continuation.accept(this);
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

    private static Set<String> freeProcessesNames(ChoreographyASTNode node){
        return node.accept(new UsedProcesses());
    }

    public static Map<String, Set<String>> usedProcesses(Choreography choreography){
        var calls = new HashMap<String, Set<String>>();
        choreography.procedures.forEach(procedure ->
                calls.put(procedure.name, UsedProcedures.usedProcedures(procedure.body)));
        var oldUsedProcesses = new HashMap<String, Set<String>>();
        var newUsedProcesses = new HashMap<String, Set<String>>();
        choreography.procedures.forEach(procedure -> {
            newUsedProcesses.put(procedure.name, freeProcessesNames(procedure.body));
        });

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
