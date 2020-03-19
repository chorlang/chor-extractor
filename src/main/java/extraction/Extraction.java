package extraction;

import network.Network;
import network.ProcessTerm;
import extraction.Node.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedPseudograph;
import parsing.Parser;

import java.util.HashMap;

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
        Graph<Node, Label> graph = builder.makeGraph(network);
    }


    /* ==================
        Helper functions
       ================== */



}
