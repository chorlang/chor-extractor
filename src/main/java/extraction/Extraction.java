package extraction;

import extraction.choreography.Choreography;
import extraction.choreography.WellFormedness;
import network.Network;
import parsing.Parser;

import java.util.Set;

/**
 * This class is made for interfacing with the choreography extraction algorithm.
 */
public class Extraction {

    Strategy extractionStrategy;

    public Extraction(Strategy strategy){
        extractionStrategy = strategy;
    }

    public Choreography extractChoreography(String networkDescription){
        Network network = Parser.stringToNetwork(networkDescription);

        if (!WellFormedness.compute(network)){
            System.out.println("Network is not well-formed, and can therefore not be extracted");
            return null;
        }
        System.out.println("The network is well-formed and extraction can proceed");

        GraphBuilder builder = new GraphBuilder(Strategy.Default);
        var executionGraphResult = builder.executionGraphBuilder(network, Set.of());
        if (executionGraphResult.buildGraphResult != GraphBuilder.BuildGraphResult.OK){
            System.out.println("Could not build execution graph");
            return null;
        }
        var graph = executionGraphResult.graph;
        var rootNode = executionGraphResult.rootNode;
        var chorExtractor = new ChoreographyBuilder();
        return chorExtractor.buildChoreography(rootNode, graph);
    }

}
