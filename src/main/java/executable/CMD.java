package executable;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.util.mxCellRenderer;
import extraction.Extraction;
import extraction.Label;
import extraction.Node;
import extraction.network.Network;
import org.codehaus.plexus.util.IOUtil;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import parsing.Parser;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Set;

/**
 * Command line tool for extraction
 */
public class CMD {
    public static void main(String[] args){
        //Check CMD usage
        var parameters = new Parameters();
        var builder = JCommander.newBuilder().addObject(parameters).build();
        builder.setProgramName("chorextr");
        try {
            builder.parse(args);
        }catch (ParameterException e){
            System.err.println("Incorrect parameter usage");
            builder.usage();
            System.exit(1);
        }
        if (parameters.help){
            builder.usage();
            System.exit(0);
        }

        //Read input string
        String input = null;
        if (parameters.inputFile == null) {
            StringBuilder inputBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))){
                if (!reader.ready()){
                    System.err.println("Please pipe the input network to stdin, or specify an input file with -i. Use --help for help");
                    System.exit(1);
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    inputBuilder.append(line);
                }
            } catch (IOException e) {
                System.err.println("Error reading from stdin");
                e.printStackTrace();
                System.exit(1);
            }
            input = inputBuilder.toString();
        }
        else {
            try (FileInputStream fin = new FileInputStream(parameters.inputFile)){
                input = IOUtil.toString(fin, "UTF-8");
            }catch (FileNotFoundException e) {
                System.err.printf("The provided file \"%s\" could not be found.%n", parameters.inputFile);
                System.exit(1);
            } catch (IOException e) {
                System.err.println("Error while attempting to read input file.");
                e.printStackTrace();
                System.exit(1);
            }
        }

        //Parse input string
        Network network = Parser.stringToNetwork(input);
        if (network == null)
            System.exit(1); //Error printed by parser

        //Set up extractor with the provided settings
        Extraction extractor = Extraction.newExtractor();
        extractor.setStrategy(parameters.strategy);
        if (parameters.multicomDisabled)
            extractor.disableMulticom();
        if (parameters.purgingDisabled)
            extractor.disablePurge();
        if (parameters.extractSequentially)
            extractor.sequentialExtraction();

        //Perform the extraction
        Extraction.ExtractionResult result = extractor.extract(network, Set.copyOf(parameters.services));

        //Write output to stdout or file
        if (parameters.outputFile == null){
            System.out.println(result.program);
        }
        else{
            try (FileWriter fout = new FileWriter(parameters.outputFile)){
                fout.write(result.program.toString());
            } catch (IOException e) {
                System.err.printf("Unable to write result to file \"%s\"%n", parameters.outputFile);
                e.printStackTrace();
            }
        }

        //Generate images if requested
        if (parameters.SEGImage != null)
            generateImage(result.extractionInfo.get(0).symbolicExecutionGraph(), parameters.SEGImage);
        if (parameters.unrolledImage != null){
            generateImage(result.extractionInfo.get(0).unrolledGraph(), parameters.unrolledImage);
        }
    }

    static void generateImage(Graph<Node, Label> graph, String imgPath){
        JGraphXAdapter<Node, Label> graphXAdapter = new JGraphXAdapter<>(graph);

        mxGraphLayout layout = new mxHierarchicalLayout(graphXAdapter);
        layout.execute(graphXAdapter.getDefaultParent());

        BufferedImage image = mxCellRenderer.createBufferedImage(graphXAdapter, null, 2, Color.WHITE, true, null);
        File imgFile = new File(imgPath);
        try {
            ImageIO.write(image, "PNG", imgFile);
        } catch (IOException e) {
            System.err.printf("Unable to write to or create image file \"%s\"%n", imgPath);
            e.printStackTrace();
        }
    }
}
