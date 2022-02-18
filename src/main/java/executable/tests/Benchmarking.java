package executable.tests;

import edu.emory.mathcs.backport.java.util.Collections;
import endpointprojection.EndPointProjection;
import extraction.Extraction;
import extraction.Strategy;
import extraction.choreography.Choreography;
import extraction.choreography.Program;
import extraction.network.Network;
import parsing.Parser;
import utility.Pair;
import utility.choreographyStatistics.ChoreographyStatistics;
import utility.choreographyStatistics.LengthOfProcedures;
import utility.choreographyStatistics.NumberOfActions;
import utility.networkStatistics.NetworkStatistics;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Benchmarking {
    private static final String TEST_DIR = "test/";
    private static final String OUTPUT_DIR = TEST_DIR;
    private static final String CHOREOGRAPHY_PREFIX = "choreography-";
    private static final String PROJECTION_PREFIX = "projection-";
    private static final String EXTRACTION_PREFIX = "extraction-";
    private static final String PROJECTION_STATISTICS_PREFIX = "stats-projection-";
    private static final String EXTRACTION_STATISTICS_PREFIX = "stats-extraction-";
    private static final String COMBINED_STATISTICS_PREFIX = "stats-";
    private static final String SEPARATOR = "\t";
    private static final String FORMAT = ".tsv";

    //Create header string form a list of column names concatenated with SEPARATOR in between.
    private static final String PROJECTION_STATISTICS_HEADER =
            Stream.of("testId","numberOfActions","numberOfProcesses","numberOfProcedures","numberOfConditionals", "minLengthOfProcesses","maxLengthOfProcesses","avgLengthOfProcesses", "minNumberOfProceduresInProcesses","maxNumberOfProceduresInProcesses","avgNumberOfProceduresInProcesses", "minNumberOfConditionalsInProcesses","maxNumberOfConditionalsInProcesses","avgNumberOfConditionalsInProcesses","numberOfProcessesWithConditionals", "minProcedureLengthInProcesses","maxProcedureLengthInProcesses","avgProcedureLengthInProcesses").
                    reduce((lstring, rstring)->lstring+SEPARATOR+rstring).get();
    private static final String EXtrACTION_STATISTICS_HEADER =
            Stream.of("testId","strategy","time(msec)","nodes","badLoops","mainLength","numOfProcedures","minProcedureLength","maxProcedureLength","avgProcedureLength")
                    .reduce((lstring,rstring)->lstring+SEPARATOR+rstring).get();

    /**
     * Find all choreography files in the test directory,
     * and projects then to networks.
     */
    public static void EndpointProjection(){
        checkOutputDir();

        var choreographyFiles = readChoreographyFiles(TEST_DIR);
        choreographyFiles.forEach((fileID, chorMap)->{
            //Check that the file has not already been projected.
            if (Files.notExists(Paths.get(OUTPUT_DIR+fileID))){
                //Map from chor names to their choreography and network projection
                var projectionMap = new HashMap<String, Pair<Program, Network>>();
                chorMap.forEach((chorID, choreography)->{
                    System.out.printf("Projecting %s from %s%s%n",chorID,CHOREOGRAPHY_PREFIX,fileID);
                    Program program = Parser.stringToProgram(choreography);
                    if (program == null)
                        throw new RuntimeException("Could not parse choreography: \n"+choreography);
                    projectionMap.put(chorID, new Pair<>(program, EndPointProjection.project(program)));
                });
                //Write the projection and statistics to file.
                writeNetworksProjectionsToFile(projectionMap, PROJECTION_PREFIX+fileID);
                writeNetworkProjectionStatisticsToFile(projectionMap, PROJECTION_STATISTICS_PREFIX+fileID);
            }
        });
    }

    private static void writeNetworksProjectionsToFile(Map<String, Pair<Program, Network>> projectionMap, String filename){
        try (PrintWriter writer = new PrintWriter(OUTPUT_DIR+filename)){
            writer.println("testID"+SEPARATOR+"network");
            projectionMap.forEach((chorID, pair)->{
                writer.printf("%s%s%s%n", chorID, SEPARATOR, pair.second);
            });
        }catch (FileNotFoundException e){
            System.err.println("Unable to open or create file "+OUTPUT_DIR+filename);
            throw new RuntimeException(e);
        }
    }
    private static void writeNetworkProjectionStatisticsToFile(Map<String, Pair<Program, Network>> projectionMap, String filename){
        //Adding the file ending makes it easier to see it is a spreadsheet
        try (PrintWriter writer = new PrintWriter(OUTPUT_DIR+filename+FORMAT)){
            writer.println(PROJECTION_STATISTICS_HEADER);
            projectionMap.forEach((chorID, pair)->{
                var networkStatistics = NetworkStatistics.compute(pair.second);
                var choreographyStatistics = ChoreographyStatistics.compute(pair.first);
                writer.printf("%s%s", chorID, SEPARATOR);
                writer.printf("%s%s%s%s%s%s%s%s",
                        choreographyStatistics.numberOfActions,SEPARATOR,
                        choreographyStatistics.numberOfProcesses,SEPARATOR,
                        choreographyStatistics.numberOfProcedures,SEPARATOR,
                        choreographyStatistics.numberOfConditionals,SEPARATOR);
                writer.printf("%s%s%s%s%s%s%s%s%s%s%s%s",
                        networkStatistics.minLengthOfProcesses,SEPARATOR,
                        networkStatistics.maxLengthOfProcesses,SEPARATOR,
                        networkStatistics.avgLengthOfProcesses,SEPARATOR,
                        networkStatistics.minNumberOfProceduresInProcesses,SEPARATOR,
                        networkStatistics.maxNumberOfProceduresInProcesses,SEPARATOR,
                        networkStatistics.avgNumberOfProceduresInProcesses,SEPARATOR);
                writer.printf("%s%s%s%s%s%s%s%s%s%s%s%s%s%n",
                        networkStatistics.minNumberOfConditionalsInProcesses,SEPARATOR,
                        networkStatistics.maxNumberOfConditionalsInProcesses,SEPARATOR,
                        networkStatistics.avgNumberOfConditionalsInProcesses,SEPARATOR,
                        networkStatistics.numberOfProcessesWithConditionals,SEPARATOR,
                        networkStatistics.minProcedureLengthInProcesses,SEPARATOR,
                        networkStatistics.maxProcedureLengthInProcesses,SEPARATOR,
                        networkStatistics.avgProcedureLengthInProcesses);
            });
        }catch (FileNotFoundException e){
            System.err.println("Unable to open or create file "+OUTPUT_DIR+filename);
            throw new RuntimeException(e);
        }
    }

    /**
     * Read all choreography files in the provided directory.
     * @param dirPath the path of the directory with the choreography files to read.
     * @return A mapping from the name of each file to another mapping from
     * choreography names to choreography string representations.
     */
    private static HashMap<String, HashMap<String, String>> readChoreographyFiles(String dirPath){
        var dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory())
            throw new RuntimeException("The provided directory path "+dirPath+" does not exists, or is not a directory");

        var fileToChoreographyMap = new HashMap<String, HashMap<String, String>>();
        for (String filename : dir.list()){
            if (filename.startsWith(CHOREOGRAPHY_PREFIX))
                fileToChoreographyMap.put(filename.substring(CHOREOGRAPHY_PREFIX.length()),
                        readChoreographyFile(dirPath+filename));
        }
        return fileToChoreographyMap;
    }

    /**
     * Reads the choreographies stored in a single file.
     * @param filePath The path to the file to read.
     * @return A mapping from the names of each choreography in
     * the file, to their string representation.
     */
    private static HashMap<String, String> readChoreographyFile(String filePath){
        var choreographies = new HashMap<String,String>();

        try{
            var chorLines = new ArrayList<String>();//Accumulator for the choreography currently being read
            var chorName = "";//Name of the choreography currently being read
            for (String line : Files.lines(Paths.get(filePath)).toList()){
                if (line.startsWith("***"))
                    chorName = line.replace("***","").trim();//Remove stars from chor name
                else if (line.startsWith("def") || line.startsWith("main"))
                    chorLines.add(line);//the line is simply part of the choreography definition
                else if (line.isEmpty()){
                    //End of the current choreography. Save it and reset.
                    choreographies.put(chorName, chorLines.stream().reduce("", String::concat));
                    chorLines.clear();
                    chorName = "";
                }
                else
                    throw new RuntimeException("Could not parse choreography. Unexpected input from \"%S\" in file \"%s\"".formatted(line, filePath));
            }
            //Edge case if the input file does not contain an empty line at the end.
            if (!chorLines.isEmpty())
                choreographies.put(chorName, chorLines.stream().reduce("", String::concat));

        }catch (FileNotFoundException e){
            System.err.println("Could not find projection file " + filePath);
            throw new RuntimeException(e);
        }catch (IOException e){
            System.err.println("An error occured while attempting to read projection file " + filePath);
            throw new RuntimeException(e);
        }

        return choreographies;
    }


    public static void extractionBenchmarks(){
        for (Strategy s : Strategy.values()){
            if (s != Strategy.Default)
                benchmarkStrategy(s);
        }
    }

    public static void benchmarkStrategy(Strategy strategy){
        checkOutputDir();

        //Get a map from filenames to a map of network names to network string definitions
        var networkFiles = readNetworkFiles(TEST_DIR);

        networkFiles.forEach((fileID, networkMap)->{
            if (Files.notExists(Paths.get(OUTPUT_DIR+EXTRACTION_PREFIX+strategy.name()+"-"+fileID))){
                var extractionMap = new HashMap<String, Pair<Program, Long>>();
                networkMap.forEach((networkID, network) ->{
                    System.out.printf("Extracting %s from %s%s with strategy %s%n",networkID,PROJECTION_PREFIX,fileID,strategy.name());
                    Long start = System.currentTimeMillis();
                    var result = Extraction.newExtractor()
                            .setStrategy(strategy).extract(network);
                    Long executionTime = System.currentTimeMillis() - start;

                    if (result == null)
                        throw new RuntimeException("Error extracting the network "+network);
                    else if (result.program.choreographies.contains(null))
                        System.err.printf("There is an unextractable network in %s from %s%s with strategy %s%n",networkID,PROJECTION_PREFIX,fileID,strategy.name());
                    extractionMap.put(networkID, new Pair<>(result.program, executionTime));
                });
                writeExtractionsToFile(extractionMap, "%s%s-%s".formatted(EXTRACTION_PREFIX,strategy.name(),fileID),strategy.name());
                writeExtractionStatisticsToFile(extractionMap, "%s%s-%s".formatted(EXTRACTION_STATISTICS_PREFIX,strategy.name(),fileID), strategy.name());
            }
        });
    }

    private static void writeExtractionsToFile(Map<String, Pair<Program, Long>> extractionMap, String filename, String strategyname){
        try (PrintWriter writer = new PrintWriter(OUTPUT_DIR+filename)){
            writer.println("testID"+SEPARATOR+"strategy"+SEPARATOR+"choreography");
            extractionMap.forEach((id, pair)->
                    writer.printf("%s%s%s%s%s%n",id,SEPARATOR,strategyname,SEPARATOR,pair.first));
        }catch (FileNotFoundException e){
            System.err.println("Unable to open or create file "+OUTPUT_DIR+filename);
            throw new RuntimeException(e);
        }
    }

    private static void writeExtractionStatisticsToFile(Map<String, Pair<Program, Long>> extractionMap, String filename, String strategyname){
        try (PrintWriter writer = new PrintWriter(OUTPUT_DIR+filename)){
            writer.println(EXtrACTION_STATISTICS_HEADER);
            extractionMap.forEach((id, pair)-> {
                Program program = pair.first;
                //Sum nodecounts and baddloopcounts across all choreographies
                var statistics = program.statistics.stream()
                        .reduce(new Program.GraphData(0,0),
                                (left,right) -> new Program.GraphData(
                                        left.nodeCount+ right.nodeCount,
                                        left.badLoopCount+right.badLoopCount));
                //The total number of procedures across all choreographies
                int numOfProcedures = program.choreographies.stream()
                        .map(chor -> chor.procedures.size())
                        .reduce(0, Integer::sum);
                //A list of procedure lengths for procedures in all choreographies.
                List<Integer> lengthOfProcedures = program.choreographies.stream()
                        .map(LengthOfProcedures::getLength)
                        .reduce(new ArrayList<>(), (left,right)->{left.addAll(right);return left;});
                //The total number of actions across all choreographies
                int numberOfActions = program.choreographies.stream()
                        .map(NumberOfActions::compute).reduce(0,Integer::sum);
                writer.printf("%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%n",
                        id,SEPARATOR,
                        strategyname,SEPARATOR,
                        pair.second,SEPARATOR,
                        statistics.nodeCount,SEPARATOR,
                        statistics.badLoopCount,SEPARATOR,
                        numberOfActions,SEPARATOR,
                        numOfProcedures,SEPARATOR,
                        Collections.min(lengthOfProcedures),SEPARATOR,
                        Collections.max(lengthOfProcedures),SEPARATOR,
                        lengthOfProcedures.isEmpty() ? 0 :
                                lengthOfProcedures.stream().reduce(0,Integer::sum) / lengthOfProcedures.size());

            });
        }catch (FileNotFoundException e){
            System.err.println("Unable to open or create file "+OUTPUT_DIR+filename);
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds and reads in all networks that are projections of choreographies
     * @param dirPath The directory with the projection files.
     * @return A mapping from file-name to another mapping from network-ID to
     * the string representation of the network.
     */
    private static HashMap<String, HashMap<String, String>> readNetworkFiles(String dirPath){
        var dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory())
            throw new RuntimeException("Provided directory path " + dirPath + " does not exist, or is not a directory");

        var fileToNetworksMap = new HashMap<String, HashMap<String, String>>();
        for (String filename : dir.list()){
            if (filename.startsWith(PROJECTION_PREFIX)){
                fileToNetworksMap.put(filename.substring(PROJECTION_PREFIX.length()),
                        readNetworkFile(dirPath+filename));
            }
        }
        return fileToNetworksMap;
    }

    /**
     * Reads a single file with network projections.
     * @param filePath The full or relative path of the file to read.
     * @return A mapping from the name of each network in the file, to
     * the networks string representation.
     */
    private static HashMap<String, String> readNetworkFile(String filePath){
        var networks = new HashMap<String,String>();

        try{
            Files.lines(Paths.get(filePath)).forEach(line->{
                int sepIndex = line.indexOf(SEPARATOR);
                if (sepIndex != -1)
                    networks.put(line.substring(0,sepIndex), line.substring(sepIndex+1));
            });
        }catch (FileNotFoundException e){
            System.err.println("Could not find projection file " + filePath);
            throw new RuntimeException(e);
        }catch (IOException e){
            System.err.println("An error occured while attempting to read projection file " + filePath);
            throw new RuntimeException(e);
        }
        //Remove the tsv header
        networks.remove("testID");
        return networks;
    }

    /**
     * Creates the output directory if it does not already exist,
     * or throws an error if that is not possible.
     */
    private static void checkOutputDir(){
        var dir = new File(OUTPUT_DIR);
        if (!dir.exists())
            if (!dir.mkdir())
                throw new InternalError("Could not create output dir " + OUTPUT_DIR);
        else if (!dir.isDirectory())
            throw new InternalError(OUTPUT_DIR + " is a file, not a directory");
    }
}
