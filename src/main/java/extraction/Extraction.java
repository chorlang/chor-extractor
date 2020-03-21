package extraction;

import extraction.GraphBuilder;
import network.Network;
import org.jgrapht.Graph;
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

    public void extractChoreography(String networkDescription){
        Network network = Parser.stringToNetwork(networkDescription);
        GraphBuilder builder = new GraphBuilder(Strategy.Default);
        Graph<Node, Label> graph = builder.makeGraph(network, Set.of());
    }


    /* ==================
        Helper functions
       ================== */



}
