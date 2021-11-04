package extraction.choreography;

import extraction.Label;
import extraction.network.utils.TreeVisitor;

import java.util.*;
import java.util.function.Function;

public class Purger {

    /**
     * Returns a new choreography where redundant interactions are removed.
     * Redundant meaning processes that interact among themselves in a loop, never letting
     * information escape.
     * @param choreography The choreography to purge. This parameter is unmodified by the operation.
     * @return A new choreography, where redundant processes have been removed.
     */
    public static Choreography purgeIsolated(Choreography choreography){
        //Find the parameters that does redundant interactions.
        //(aka they do not interact with the rest of the network)
        HashMap<ProcedureDefinition, Set<String>> isolated = findIsolated(choreography.procedures);

        //Find the index of all the parameters that does redundant interactions.
        HashMap<String, Set<Integer>> obsoleteIndexes = new HashMap<>();
        isolated.forEach((procedure, parameters) -> {
            var indexes = new HashSet<Integer>();
            parameters.forEach(parameter -> {
                indexes.add(procedure.parameters.indexOf(parameter));
            });
            obsoleteIndexes.put(procedure.name, indexes);
        });

        var procedures = new ArrayList<>(choreography.procedures);

        for (int i = 0; i < procedures.size(); i++){
            ProcedureDefinition procedure = procedures.get(i);
            var remover = new RedundantRemover(isolated.get(procedure), obsoleteIndexes);
            var newProcedure = new ProcedureDefinition(procedure.name, new ArrayList<>(procedure.parameters), remover.Visit(procedure.body));
            newProcedure.parameters.removeAll(isolated.get(procedure));
            procedures.set(i, newProcedure);
        }

        var remover = new RedundantRemover(Set.of(), obsoleteIndexes);
        var newMain = remover.Visit(choreography.main);
        return new Choreography(newMain, procedures, choreography.processes);
    }

    private static class RedundantRemover implements TreeVisitor<ChoreographyBody, ChoreographyASTNode>{
        private final Set<String> redundantParameters;
        private final HashMap<String, Set<Integer>> redundantParametersIndexes;
        RedundantRemover(Set<String> redundantParameters, HashMap<String, Set<Integer>> obsoleteIndexes){
            this.redundantParameters = redundantParameters;
            this.redundantParametersIndexes = obsoleteIndexes;
        }

        @Override
        public ChoreographyBody Visit(ChoreographyASTNode hostNode){
            switch (hostNode.getType()){
                case COMMUNICATION -> {
                    var com = (Communication)hostNode;
                    if (redundantParameters.contains(com.sender))
                        return com.continuation.accept(this);
                    return new Communication(com.sender, com.receiver, com.expression, com.continuation.accept(this));
                }
                case CONDITION -> {
                    var cond = (Condition)hostNode;
                    return new Condition(cond.process, cond.expression, cond.thenChoreography.accept(this), cond.elseChoreography.accept(this));
                }
                case INTRODUCTION -> {
                    var intro = (Introduction)hostNode;
                    if (redundantParameters.contains(intro.introducer))
                        return intro.continuation.accept(this);
                    return new Introduction(intro.introducer, intro.process1, intro.process2, intro.continuation.accept(this));
                }
                case MULTICOM -> {
                    var mult = (Multicom)hostNode;
                    var communications = new ArrayList<>(mult.communications);
                    communications.removeIf(com -> redundantParameters.contains(com.sender));
                    return new Multicom(communications, mult.continuation.accept(this));
                }
                case PROCEDURE_INVOCATION -> {
                    var inv = (ProcedureInvocation)hostNode;
                    var parameters = new ArrayList<>(inv.parameters);
                    var removeIndexes = redundantParametersIndexes.get(inv.procedure);
                    for (Integer index : removeIndexes){
                        parameters.remove(inv.parameters.get(index));
                    }
                    return new ProcedureInvocation(inv.procedure, parameters);
                }
                case SELECTION -> {
                    var sel = (Selection)hostNode;
                    if (redundantParameters.contains(sel.sender))
                        return sel.continuation.accept(this);
                    return new Selection(sel.sender, sel.receiver, sel.label, sel.continuation.accept(this));
                }
                case SPAWN -> {
                    var spawn = (Spawn)hostNode;
                    return new Spawn(spawn.spawner, spawn.spawned, spawn.continuation.accept(this));
                }
                case TERMINATION -> {
                    return Termination.getInstance();
                }
                default ->
                        throw new UnsupportedOperationException("Attempted to remove redundant process from a Choreography node that was not a choreography body");
            }
        }

    }

    /**
     * Returns a mapping from each of the provided ProcedureDefinitions to a set of process names, where the set
     * contains all parameters that directly or indirectly interacts with a process not passed as a parameter.
     * @param procedures The procedures to search
     * @return A mapping from the provided procedures, to sets of parameters that could be omitted.
     */
    public static HashMap<ProcedureDefinition, Set<String>> findIsolated(Collection<ProcedureDefinition> procedures){
        var isolatedMap = new HashMap<ProcedureDefinition, Set<String>>(procedures.size());
        var procedureMap = new HashMap<String, ProcedureDefinition>();
        procedures.forEach(procedure -> procedureMap.put(procedure.name, procedure));
        for (var procedure : procedures){
            //Create an interaction checker, and look through the procedure body.
            var checker = new ExternalInteractionChecker(procedure.name, procedureMap);
            checker.Visit(procedure.body);
            //Get a list of all variables used in the procedure, and the procedures it calls.
            HashSet<String> visitedProcedures = checker.getVisitedProcedures();
            HashSet<String> variables = new HashSet<>();    //The variables used for this procedure call
            for (var procName : visitedProcedures){
                variables.addAll(procedureMap.get(procName).parameters);
            }

            var isolated = new HashSet<String>();
            var linkedProcesses = checker.getConnections();
            for (var parameter : procedure.parameters){
                //Find the processes that the "parameter" variable interacts with, except
                //other variables
                var allButVariables = new HashSet<>(linkedProcesses.get(parameter));
                allButVariables.removeAll(variables);

                //If it interacts only with other variables, add it to the isolated set
                if (allButVariables.isEmpty())
                    isolated.add(parameter);
            }
            //isolated is not a set of all parameters of this procedure, that only interacts with other variables.
            isolatedMap.put(procedure, isolated);
        }

        return isolatedMap;
    }

    /**
     * Stores a HashMap from process names to set of process names, where every process in a set can directly
     * or indirectly interact with each other.
     */
    private static class LinkedSets{
        final HashMap<String, HashSet<String>> linked = new HashMap<>();
        private final Function<String, HashSet<String>> newSet = s -> new HashSet<>(){{add(s);}};

        /**
         * Adds the two process names to the internal HashMap if not already present, then ensures that they
         * are in the same set. After this operation, the internal HashMap "linked" will be such that
         * linked.get(a) == linked.get(b) is true.
         * @param a First process name to add and link.
         * @param b Second process name to add and link.
         */
        public void link(String a, String b){
            var aSet = linked.computeIfAbsent(a, newSet);
            var bSet = linked.computeIfAbsent(a, newSet);
            if (aSet != bSet){
                aSet.addAll(bSet);
                linked.put(b, aSet);
            }
        }
    }

    /**
     * Used to run through a procedure (and any procedures it calls) to find which processes that interact.
     * After the Visit() function returns, call getConnections() which returns a hashmap mapping process names
     * to a set of all processes it influences though direct or indirect interactions. Note that some of the
     * process names are parameter variables. Including those of the procedure to examine.
     */
    private static class ExternalInteractionChecker implements TreeVisitor<Object,  ChoreographyASTNode> {
        private final LinkedSets connections = new LinkedSets();
        private final HashSet<String> visitedProcedures = new HashSet<>();
        private final HashMap<String, ProcedureDefinition> procedures;
        ExternalInteractionChecker(String procedure, HashMap<String, ProcedureDefinition> procedures){
            visitedProcedures.add(procedure);//The name of the procedure being checked.
            this.procedures = procedures;   //Procedure invocations in the choreography
        }
        public HashMap<String, HashSet<String>> getConnections(){
            return connections.linked;
        }
        public HashSet<String> getVisitedProcedures(){
            return visitedProcedures;
        }

        @Override
        public Object Visit(ChoreographyASTNode hostNode) {
            switch (hostNode.getType()){
                case COMMUNICATION -> {
                    var com = (Communication)hostNode;
                    connections.link(com.sender, com.receiver);
                    return com.continuation.accept(this);
                }
                case CONDITION -> {
                    var cond = (Condition)hostNode;
                    cond.thenChoreography.accept(this);
                    return cond.elseChoreography.accept(this);
                }
                case INTRODUCTION -> {
                    var intro = (Introduction)hostNode;
                    connections.link(intro.introducer, intro.process1);
                    connections.link(intro.introducer, intro.process2);
                    return intro.continuation.accept(this);
                }
                case MULTICOM -> {
                    var mult = (Multicom)hostNode;
                    for (var interaction : mult.communications){
                        connections.link(interaction.receiver, interaction.sender);
                        if (interaction instanceof Label.IntroductionLabel)
                            connections.link(interaction.sender, interaction.expression);
                    }
                    return mult.continuation.accept(this);
                }
                case PROCEDURE_INVOCATION -> {
                    var inv = (ProcedureInvocation) hostNode;
                    var def = (ProcedureDefinition) procedures.get(inv.procedure);
                    //Stop if we are about to repeat a loop
                    if (visitedProcedures.contains(inv.procedure))
                        return null;
                    visitedProcedures.add(inv.procedure);
                    //Link the parameter variables with its values, since they refer to the same process
                    //they should be in the same set.
                    for (int i = 0; i < inv.parameters.size(); i++)
                        connections.link(inv.parameters.get(i), def.parameters.get(i));
                    return def.body.accept(this);
                }
                case SELECTION -> {
                    var sel = (Selection)hostNode;
                    connections.link(sel.sender, sel.receiver);
                    return sel.continuation.accept(this);
                }
                case SPAWN -> {
                    var spawn = (Spawn)hostNode;
                    connections.link(spawn.spawner, spawn.spawned);
                    return spawn.continuation.accept(this);
                }
                case TERMINATION -> {
                    return null;
                }
                default ->
                        throw new UnsupportedOperationException("The ChoreographyASTNode is of the wrong type");
            }
        }
    }
}
