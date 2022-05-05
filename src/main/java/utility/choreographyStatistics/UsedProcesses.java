package utility.choreographyStatistics;

import extraction.Label;
import extraction.choreography.*;
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
                Set<String> mentionedProcesses = host.thenChoreography.accept(this);
                mentionedProcesses.addAll(host.elseChoreography.accept(this));
                mentionedProcesses.addAll(host.continuation.accept(this));
                mentionedProcesses.add(host.process);
                return mentionedProcesses;
            }
            case MULTICOM:{
                Multicom host = (Multicom) hostNode;
                Set<String> mentionedProcesses = host.getContinuation().accept(this);
                host.communications.forEach(interaction -> {
                    mentionedProcesses.add(interaction.sender);
                    mentionedProcesses.add(interaction.receiver);
                    if (interaction instanceof Label.IntroductionLabel)
                        mentionedProcesses.add(interaction.expression);
                });
                return mentionedProcesses;
            }
            case SPAWN:{
                Spawn host = (Spawn) hostNode;
                var mentionedProcesses = host.getContinuation().accept(this);
                mentionedProcesses.add(host.spawner);
                //The child process is intentionally omitted.
                return mentionedProcesses;
            }
            case INTRODUCTION:{
                Introduction host = (Introduction) hostNode;
                Set<String> mentionedProcesses = host.continuation.accept(this);
                mentionedProcesses.add(host.process1); mentionedProcesses.add(host.process2);
                mentionedProcesses.add(host.introducer);
                return mentionedProcesses;
            }
            case COMMUNICATION:
            case SELECTION: {
                var host = (ChoreographyBody.Interaction) hostNode;
                Set<String> mentionedProcesses = host.getContinuation().accept(this);
                mentionedProcesses.add(host.getSender());
                mentionedProcesses.add(host.getReceiver());
                return mentionedProcesses;
            }
            case NONE:
            case TERMINATION:
            case PROCEDURE_INVOCATION:
                return new HashSet<>();

            case PROCEDURE_DEFINITION:
            case CHOREOGRAPHY:
            case PROGRAM:
            default:
                throw new UnsupportedOperationException("Unsupported choreography AST node of type "+hostNode.getType());
        }
    }

    /**
     * Finds all process names that are mentioned in a ChoreographyBody.
     * If the body invokes another procedure, the processes in that procedure is not counted in.
     * @param node The ChoreographyBody to analyse.
     * @return A set of all process names explicitly mentioned.
     */
    private static Set<String> mentionedProcesses(ChoreographyASTNode node){
        return node.accept(new UsedProcesses());
    }

    /**
     * Goes through the procedures of a choreography, and finds out which processes are used in each procedure.
     * If a procedure invokes another procedure, then the processes in the second procedure is considered to
     * be used in the first procedure as well.
     * @param choreography The choreography whose procedures should be checked.
     * @return A mapping from the name of each procedure, to a set if the names of processes used in that procedure.
     */
    public static Map<String, Set<String>> usedProcesses(Choreography choreography){
        //Find which procedures call other procedures.
        var calls = new HashMap<String, Set<String>>();
        choreography.procedures.forEach(procedure ->{
            calls.put(procedure.name, UsedProcedures.usedProcedures(procedure.body));
        });

        //Find out which processes are explicitly mentioned in a procedure definition
        var allUsedProcesses = new HashMap<String, Set<String>>();
        var directlyUsedProcesses = new HashMap<String, Set<String>>();
        choreography.procedures.forEach(procedure -> {
            var processes = mentionedProcesses(procedure.body);
            directlyUsedProcesses.put(procedure.name, processes);
        });

        //I think this adds processes used by an invoked procedure, to a procedures list of processes.
        while (!allUsedProcesses.equals(directlyUsedProcesses)){
            choreography.procedures.forEach(procedure ->
                    allUsedProcesses.put(procedure.name, new HashSet<>(directlyUsedProcesses.get(procedure.name))));
            choreography.procedures.forEach(procedure ->{
                calls.get(procedure.name).forEach(calledProcedure ->{
                        directlyUsedProcesses.get(procedure.name).addAll(allUsedProcesses.get(calledProcedure));
                });
            });
        }
        return allUsedProcesses;
    }
}
