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

    //TODO: Use the builder pattern to clean up this atrocity
    public static Program extractChoreography(String networkDescription){
        return extractChoreography(networkDescription, Strategy.Default);
    }
    public static Program extractChoreography(String networkDescription, Strategy extractionStrategy){
        return extractChoreography(networkDescription, extractionStrategy, Set.of());
    }
    public static Program extractChoreography(String networkDescription, Strategy extractionStrategy, Set<String> services){
        return new Extraction(extractionStrategy).extractChoreography(networkDescription, services);
    }
    public static Program extractChoreographySequentially(String networkDescription, Strategy extractionStrategy, Set<String> services){
        return new Extraction(extractionStrategy).extractChoreographySequentially(networkDescription, services);
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
            System.out.println("The network could not be split into parallel networks. " +
                    "Extraction proceeds assuming a fully connected network");
            parallelNetworks = new HashSet<>(){{add(network);}};
        } else
            System.out.println("The input network has successfully been split into parallel independent networks");

        List<ChorStatsPair> results = Collections.synchronizedList(new ArrayList<>());
        parallelNetworks.parallelStream().forEach(net -> {
            //net.acquaintances.matrix.get(1).set(0, Boolean.FALSE);    //Test only known processes may interact
            results.add(extract(net, services));
        });

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
        System.out.println("The Network is well-formed and extraction can proceed");

        ChorStatsPair result = extract(network, services);
        //List.of() requires non-null parameters, that's why the singletons.
        var chorSingleton = new ArrayList<Choreography>(){{add(result.chor);}};
        var statsSingleton = new ArrayList<Program.GraphStatistics>(){{add(result.stats);}};
        return new Program(chorSingleton, statsSingleton);
    }

    private ChorStatsPair extract(Network network, Set<String> services){
        var graphContainer = GraphBuilder.buildSEG(network, services, extractionStrategy);

        var statistics = new Program.GraphStatistics(graphContainer.graph().vertexSet().size(), graphContainer.badLoopCounter());
        if (graphContainer.buildGraphResult() != GraphBuilder.BuildGraphResult.OK){
            return new ChorStatsPair(null, statistics);
        }
        var choreography = ChoreographyBuilder.buildChoreography(graphContainer.rootNode(), graphContainer.graph());

        return new ChorStatsPair(choreography, statistics);
    }

    private static class ResultSorter implements Comparator<ChorStatsPair>{

        @Override
        public int compare(ChorStatsPair pair1, ChorStatsPair pair2) {
            String chor1 = String.valueOf(pair1.chor);
            String chor2 = String.valueOf(pair2.chor);
            return chor1.compareTo(chor2);
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
