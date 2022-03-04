package executable.tests;

import extraction.Strategy;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class AccumulateData {
    static final String EXTRACTION_STATISTICS_PREFIX = Benchmarking.EXTRACTION_STATISTICS_PREFIX;
    static final String COMBINED_STATISTICS_PREFIX = "stats-";
    static final String TEST_DIR = Benchmarking.TEST_DIR;
    static final String SEPARATOR = Benchmarking.SEPARATOR;
    static final String FORMAT = Benchmarking.FORMAT;
    static final String COMBINED_EXTRACTION_STATISTICS_HEADER =
            Benchmarking.EXTRACTION_STATISTICS_HEADER +
            "%slength%snumProcesses%snumIfs%snumProcedures%n".formatted(SEPARATOR,SEPARATOR,SEPARATOR,SEPARATOR);

    public static void accumulate(Strategy strategy){
        var dir = new File(TEST_DIR);
        if (!dir.exists() || !dir.isDirectory())
            throw new RuntimeException("The directory "+TEST_DIR+" does not exists");

        String outPath = TEST_DIR+COMBINED_STATISTICS_PREFIX+strategy+FORMAT;
        try (var writer = new BufferedWriter(new FileWriter(outPath))){
            writer.write(COMBINED_EXTRACTION_STATISTICS_HEADER);
            for (String fileName : dir.list()){
                if (!fileName.startsWith(EXTRACTION_STATISTICS_PREFIX+strategy))
                    continue;
                String data = readStatisticsFile(fileName);
                writer.write(data);
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static String readStatisticsFile(String fileName){
        String filePath = TEST_DIR+fileName;

        //Get the parameters from the file name, separated by the SEPARATOR
        String parameters = Arrays.stream(fileName.split("-")).filter(s->s.matches("\\d+")).reduce("",(left,right)->left+SEPARATOR+right);

        try{
            return Files.lines(Paths.get(filePath))
                    .filter(l->!(l.startsWith("testId") || l.startsWith("testID")))//Remove first line
                    .map(l->l+parameters+"\n")  //Append the parameters
                    .reduce(String::concat)     //Concatenate the lines
                    .get();

        } catch (IOException e) {
            throw new RuntimeException("There where an error while reading "+filePath,e);
        }
    }
}
