package endpointprojection;

import extraction.choreography.Choreography;
import extraction.choreography.Program;
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
        return new Network(networkMap);
    }
}
