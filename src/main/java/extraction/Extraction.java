package extraction;

import extraction.choreography.Choreography;
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
        GraphBuilder builder = new GraphBuilder(Strategy.Default);
        var executionGraphResult = builder.executionGraphBuilder(network, Set.of());
        if (executionGraphResult.buildGraphResult != GraphBuilder.BuildGraphResult.OK){
            System.out.println("Could not build execution graph");
            return null;
        }
        var graph = executionGraphResult.graph;
        var rootNode = executionGraphResult.rootNode;
        var chorExtractor = new ChoreographyBuilder();
        var choreography = chorExtractor.buildChoreography(rootNode, graph);
        return choreography;
    }

}
