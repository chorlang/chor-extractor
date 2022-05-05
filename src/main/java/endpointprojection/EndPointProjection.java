package endpointprojection;

import extraction.choreography.Choreography;
import extraction.choreography.Program;
import extraction.network.Behaviour;
import extraction.network.Network;
import extraction.network.ProcessTerm;
import parsing.Parser;

import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;

public class EndPointProjection {
    /**
     * Parses a choreography in string form to internal representation, then uses endpoint projection
     * to project it into a Network.
     * @param choreographyDescription String representation of the choreography to project
     * @return A Network instance projected from the provided choreography, or null on failure.
     */
    public static Network project(String choreographyDescription){
        var program = Parser.stringToProgram(choreographyDescription);
        if (program == null)
            return null;
        return project(program);
    }

    /**
     * Projects a choreography to a network.
     * @param program The choreography to project
     */
    public static Network project(Program program){
        var choreographyList = program.choreographies;
        var networkMap = new HashMap<String, ProcessTerm>();
        for (var choreography : choreographyList){
            for (var process : choreography.processes){
                try {
                    if (networkMap.containsKey(process))
                        throw new InputMismatchException("Parallel choreographies cannot share processes (process " + process + ")");
                    networkMap.put(process, BehaviourProjection.project(choreography, process));
                } catch (Merging.MergingException e){
                    var newE = new Merging.MergingException("Process " + process + " " + e.getMessage());
                    newE.setStackTrace(e.getStackTrace());
                    throw newE;
                }
            }
        }
        //Post-processing needed to fix spawning.
        //This step adds the spawned processes' procedures to their parents, and changes procedure invocations
        //of spawned processes to use the correct procedures. (Parent and child procedures may use the same
        //names, hence this fix)

        //Map to store processes, for which spawnin is fixed
        var fixedMap = new HashMap<String, ProcessTerm>();
        networkMap.forEach((processName, term) -> {
            var repairer = new SpawnRepair(processName, networkMap);
            var procedures = new HashMap<String, Behaviour>();
            var parameters = new HashMap<>(term.parameters);//Copy parameters
            //Fix the procedures
            term.procedures.forEach((procedure, behaviour) -> {
                procedures.put(procedure, repairer.Visit(behaviour));
            });
            //Fix the main behaviour
            Behaviour mainBehaviour = repairer.Visit(term.rawMain());
            //repairer accumulates procedures that the spawned processes use. Add those to the parent's procedures
            procedures.putAll(repairer.spawnedProcedures);
            parameters.putAll(repairer.spawnedProcedureParameters);
            fixedMap.put(processName, new ProcessTerm(procedures, parameters, mainBehaviour));
        });

        return new Network(fixedMap);
    }
}
