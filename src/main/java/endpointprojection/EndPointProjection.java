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
     * @param choreographyDescription
     * @return (parallel) networks projected from the initial choreography
     */
    public static Network Project(String choreographyDescription){
        var program = Parser.stringToProgram(choreographyDescription);
        return project(program);
    }

    /**
     * Projects choreographies to networks
     * @param program
     * @return
     */
    public static Network project(Program program){
        List<Choreography> choreographyList= program.choreographies;
        var networkMap = new HashMap<String, ProcessTerm>();
        for (var choreography : choreographyList){
            if (choreography == null)
                continue;
            for (var process : choreography.processes){
                try {
                    if (networkMap.containsKey(process))
                        throw new InputMismatchException();
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
