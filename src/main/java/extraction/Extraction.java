package extraction;

import extraction.choreography.Choreography;
import extraction.choreography.Program;
import extraction.network.Network;
import extraction.network.utils.NetworkPurger;
import extraction.network.utils.Splitter;
import extraction.network.utils.WellFormedness;
import parsing.Parser;

import java.util.*;

/**
 * This class is made for interfacing with the choreography extraction algorithm.
 */
public class Extraction {

    Strategy extractionStrategy;

    public Extraction(Strategy strategy){
        extractionStrategy = strategy;
    }

    public static Program extractChoreography(String networkDescription){
        return extractChoreography(networkDescription, Strategy.Default);
    }
    public static Program extractChoreography(String networkDescription, Strategy extractionStrategy){
        return extractChoreography(networkDescription, extractionStrategy, Set.of());
    }
    public static Program extractChoreography(String networkDescription, Strategy extractionStrategy, Set<String> services){
        return new Extraction(extractionStrategy).extractChoreography(networkDescription, services);
    }

    public Program extractChoreography(String networkDescription, Set<String> services){
        Network network = Parser.stringToNetwork(networkDescription);
        NetworkPurger.purgeNetwork(network);

        if (!WellFormedness.compute(network)){
            System.out.println("Network is not well-formed, and can therefore not be extracted");
            return new Program(List.of(), List.of());
        }
        System.out.println("The extraction.network is well-formed and extraction can proceed");

        var parallelNetworks = Splitter.splitNetwork(network);
        if (parallelNetworks == null){
            System.out.println("The network could not be split into parallel networks, and extraction has been aborted");
            return new Program(List.of(), List.of());
        }
        System.out.println("The input network has successfully been split into parallel independent networks");

        List<ChorStatsPair> results = Collections.synchronizedList(new ArrayList<>());
        parallelNetworks.parallelStream().forEach(net -> results.add(extract(net, services)));

        results.sort(new ResultSorter());

        var choreographies = new ArrayList<Choreography>();
        var statistics = new ArrayList<Program.GraphStatistics>();

        results.forEach(pair -> {
            choreographies.add(pair.chor);
            statistics.add(pair.stats);
        });

        return new Program(choreographies, statistics);
    }

    public Program extractChoreographySequentially(String networkDescription, Set<String> services){
        Network network = Parser.stringToNetwork(networkDescription);
        NetworkPurger.purgeNetwork(network);

        if (!WellFormedness.compute(network)){
            System.out.println("Network is not well-formed, and can therefore not be extracted");
            return new Program(List.of(), List.of());
        }
        System.out.println("The extraction.network is well-formed and extraction can proceed");

        ChorStatsPair result = extract(network, services);
        return new Program(List.of(result.chor), List.of(result.stats));
    }

    private ChorStatsPair extract(Network network, Set<String> services){
        var executionGraphResult = new GraphBuilder(extractionStrategy).buildExecutionGraph(network, services);

        var statistics = new Program.GraphStatistics(executionGraphResult.graph.vertexSet().size(), executionGraphResult.badLoopCounter);
        if (executionGraphResult.buildGraphResult != GraphBuilder.BuildGraphResult.OK){
            return new ChorStatsPair(null, statistics);
        }
        var choreography = new ChoreographyBuilder().buildChoreography(executionGraphResult.rootNode, executionGraphResult.graph);

        return new ChorStatsPair(choreography, statistics);
    }

    private static class ResultSorter implements Comparator<ChorStatsPair>{

        @Override
        public int compare(ChorStatsPair pair1, ChorStatsPair pair2) {
            return pair1.chor.toString().compareTo(pair2.chor.toString());
        }
    }

    private static class ChorStatsPair{
        Choreography chor;
        Program.GraphStatistics stats;
        public ChorStatsPair(Choreography chor, Program.GraphStatistics stats){
            this.chor = chor;
            this.stats = stats;
        }
    }

}
