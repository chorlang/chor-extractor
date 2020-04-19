package utility.choreographyStatistics;

import extraction.choreography.Choreography;
import extraction.choreography.Program;

import javax.naming.OperationNotSupportedException;

public class ChoreographyStatistics {
    public final int numberOfActions, numberOfProcesses, numberOfProcedures, numberOfConditionals;
    public ChoreographyStatistics(int numberOfActions, int numberOfProcesses, int numberOfProcedures, int numberOfConditionals){
        this.numberOfActions = numberOfActions;
        this.numberOfProcesses = numberOfProcesses;
        this.numberOfProcedures = numberOfProcedures;
        this.numberOfConditionals = numberOfConditionals;
    }

    public static ChoreographyStatistics compute(Program program){
        var choreographyList = program.choreographies;
        if (choreographyList.size() == 1)
            return getChoreographyStatistics(choreographyList.get(0));
        throw new UnsupportedOperationException("It is only possible to gather choreography statistics for a Program with a single choreography.");
    }

    private static ChoreographyStatistics getChoreographyStatistics(Choreography choreography){
        return new ChoreographyStatistics(NumberOfActions.compute(choreography),
                choreography.processes.size(),
                choreography.procedures.size(),
                NumberOfConditionals.compute(choreography));
    }
}
