package utility.networkStatistics;

import extraction.network.Network;

import java.util.ArrayList;
import java.util.Collections;

public class NetworkStatistics {
    public final int minLengthOfProcesses;
    public final int maxLengthOfProcesses;
    public final double avgLengthOfProcesses;
    public final int minNumberOfProceduresInProcesses;
    public final int maxNumberOfProceduresInProcesses;
    public final double avgNumberOfProceduresInProcesses;
    public final int minNumberOfConditionalsInProcesses;
    public final int maxNumberOfConditionalsInProcesses;
    public final double avgNumberOfConditionalsInProcesses;
    public final int numberOfProcessesWithConditionals;
    public final int minProcedureLengthInProcesses;
    public final int maxProcedureLengthInProcesses;
    public final double avgProcedureLengthInProcesses;
    
    public NetworkStatistics(
    int minLengthOfProcesses,
    int maxLengthOfProcesses,
    double avgLengthOfProcesses,
    int minNumberOfProceduresInProcesses,
    int maxNumberOfProceduresInProcesses,
    double avgNumberOfProceduresInProcesses,
    int minNumberOfConditionalsInProcesses,
    int maxNumberOfConditionalsInProcesses,
    double avgNumberOfConditionalsInProcesses,
    int numberOfProcessesWithConditionals,
    int minProcedureLengthInProcesses,
    int maxProcedureLengthInProcesses,
    double avgProcedureLengthInProcesses){
        this.minLengthOfProcesses = minLengthOfProcesses;
        this.maxLengthOfProcesses = maxLengthOfProcesses;
        this.avgLengthOfProcesses = avgLengthOfProcesses;
        this.minNumberOfProceduresInProcesses = minNumberOfProceduresInProcesses;
        this.maxNumberOfProceduresInProcesses = maxNumberOfProceduresInProcesses;
        this.avgNumberOfProceduresInProcesses = avgNumberOfProceduresInProcesses;
        this.minNumberOfConditionalsInProcesses = minNumberOfConditionalsInProcesses;
        this.maxNumberOfConditionalsInProcesses = maxNumberOfConditionalsInProcesses;
        this.avgNumberOfConditionalsInProcesses = avgNumberOfConditionalsInProcesses;
        this.numberOfProcessesWithConditionals = numberOfProcessesWithConditionals;
        this.minProcedureLengthInProcesses = minProcedureLengthInProcesses;
        this.maxProcedureLengthInProcesses = maxProcedureLengthInProcesses;
        this.avgProcedureLengthInProcesses = avgProcedureLengthInProcesses;
    }

    public static NetworkStatistics compute(Network network){
        var lengthOfProcesses = new ArrayList<Integer>();
        var lengthOfProcedures = new ArrayList<Integer>();
        var numberOfConditionals = new ArrayList<Integer>();
        var numberOfProcedures = new ArrayList<Integer>();

        network.processes.forEach((__, processTerm) ->{
            lengthOfProcesses.add(new NetworkProcessActions().Visit(processTerm));
            numberOfProcedures.add(processTerm.procedures.size());
            numberOfConditionals.add(new NetworkProcessConditionals().Visit(processTerm));
            lengthOfProcedures.addAll(new NetworkProcessActionsPerProcedure().getLength(processTerm));
        });

        if (lengthOfProcesses.size() == 0)
            lengthOfProcesses.add(0);
        if (numberOfProcedures.size() == 0)
            numberOfProcedures.add(0);
        if (numberOfConditionals.size() == 0)
            numberOfConditionals.add(0);
        if (lengthOfProcedures.size() == 0){
            lengthOfProcedures.add(0);
        }

        int minLengthOfProcesses = Collections.min(lengthOfProcesses);
        int maxLengthOfProcesses = Collections.max(lengthOfProcesses);
        double avgLengthOfProcesses = average(lengthOfProcesses);

        int minNumberOfProceduresInProcesses = Collections.min(numberOfProcedures);
        int maxNumberOfProceduresInProcesses = Collections.max(numberOfProcedures);
        double avgNumberOfProceduresInProcesses = average(numberOfProcedures);

        int minNumberOfConditionalsInProcesses = Collections.min(numberOfConditionals);
        int maxNumberOfConditionalsInProcesses = Collections.max(numberOfConditionals);
        double avgNumberOfConditionalsInProcesses = average(numberOfConditionals);
        int numberOfProcessesWithConditionals = 0;
        for (var processConditionalCount : numberOfConditionals){
            if (processConditionalCount > 0)
                numberOfProcessesWithConditionals++;
        }

        int minProcedureLengthInProcesses = Collections.min(lengthOfProcedures);
        int maxProcedureLengthInProcesses = Collections.max(lengthOfProcedures);
        double avgProcedureLengthInProcesses = average(lengthOfProcedures);

        return new NetworkStatistics(
            minLengthOfProcesses,
            maxLengthOfProcesses,
            avgLengthOfProcesses,
            minNumberOfProceduresInProcesses,
            maxNumberOfProceduresInProcesses,
            avgNumberOfProceduresInProcesses,
            minNumberOfConditionalsInProcesses,
            maxNumberOfConditionalsInProcesses,
            avgNumberOfConditionalsInProcesses,
            numberOfProcessesWithConditionals,
            minProcedureLengthInProcesses,
            maxProcedureLengthInProcesses,
            avgProcedureLengthInProcesses
        );
        
    }

    private static double average(ArrayList<Integer> collection){
        double avgValue = 0;
        for (var length : collection)
            avgValue += length;
        return avgValue /= collection.size();
    }
}
