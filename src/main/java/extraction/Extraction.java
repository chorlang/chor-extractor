package extraction;

import extraction.choreography.Choreography;
import extraction.choreography.Program;
import extraction.network.Network;
import extraction.network.utils.NetworkPurger;
import extraction.network.utils.Splitter;
import extraction.network.NetAnalyser;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DirectedPseudograph;
import parsing.Parser;

import java.util.*;

/**
 * This class is made for interfacing with the choreography extraction algorithm.
 */
@SuppressWarnings("UnusedReturnValue")
public class Extraction {
    //Options, and their default values
    Strategy extractionStrategy = Strategy.Default;
    boolean multicomEnable = true;
    boolean purge = true;
    boolean splitParallelNetworks = true;

    //Set options
    public Extraction setStrategy(Strategy extractionStrategy){this.extractionStrategy = extractionStrategy; return this;}
    public Extraction disableMulticom(){multicomEnable = false; return this;}
    public Extraction disablePurge(){purge = false; return this;}
    public Extraction sequentialExtraction(){splitParallelNetworks = false; return this;}

    public static Extraction newExtractor(){return new Extraction();}
    private Extraction(){}
    public Extraction(Strategy strategy){
        extractionStrategy = strategy;
    }

    //TODO: mark these as legacy
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
        return new Extraction(extractionStrategy).sequentialExtraction().extractChoreographySequentially(networkDescription, services);
    }

    /**
     * Extracts one or more choreographies from a network
     * @param network The string representation of the network to extract
     * @param services List of the names of service processes. These processes are not required to interact in loops
     *                 of the resulting choreography.
     * @return null if the input makes an invalid network.
     * Otherwise, an ExtractionData instance which contains a Program with one or more choreographs if extraction
     * succeeds. On failure, it will be null.
     * It will also contain the Symbolic Execution Graph generated during extraction (regardless if extraction
     * succeeds or not), the number of bad loops encountered during extraction, and the number of nodes in the graph.
     */
    public ExtractionResult extract(String network, Set<String> services){
        Network input = Parser.stringToNetwork(network);
        if (input == null)
            return null;
        return extract(input, services);
    }
    /**
     * Extracts one or more choreographies from a network
     * @param network The string representation of the network to extract
     * @return null if the input makes an invalid network.
     * Otherwise, an ExtractionData instance which contains a Program with one or more choreographs if extraction
     * succeeds. On failure, it will be null.
     * It will also contain the Symbolic Execution Graph generated during extraction (regardless if extraction
     * succeeds or not), the number of bad loops encountered during extraction, and the number of nodes in the graph.
     */
    public ExtractionResult extract(@NotNull String network){
        return extract(network, Set.of());
    }
    /**
     * Extracts one or more choreographies from a network
     * @param network The network to extract
     * @param services List of the names of service processes. These processes are not required to interact in loops
     *                 of the resulting choreography.
     * @return null if the input makes an invalid network.
     * Otherwise, an ExtractionData instance which contains a Program with one or more choreographs if extraction
     * succeeds. On failure, it will be null.
     * It will also contain the Symbolic Execution Graph generated during extraction (regardless if extraction
     * succeeds or not), the number of bad loops encountered during extraction, the number of nodes in the graph,
     * The result of the SEG generation, and the root node of the graphs
     */
    public ExtractionResult extract(@NotNull Network network, @NotNull Set<String> services){
        if (purge)
            NetworkPurger.purgeNetwork(network);
        if (!NetAnalyser.isSafe(network)){
            System.err.println("Network is not well formed. Aborting extraction.");
            return null;
        }
        HashSet<Network> independentNetworks;
        if (splitParallelNetworks){
            independentNetworks = Splitter.splitNetwork(network);
            if (independentNetworks == null){
                System.err.println("An internal problem occurred while trying to split the input into independent networks. " +
                        "This is either a bug, or an unexpected edge case. Please contact a developer, and provide the problematic network.\n" +
                        "As a workaround, you may attempt sequential extraction instead.");
                return null;
            }
        }else {
            independentNetworks = new HashSet<>(){{add(network);}};
        }

        List<Extracted> results = Collections.synchronizedList(new ArrayList<>(independentNetworks.size()));

        independentNetworks.parallelStream().forEach(independentNetwork ->
                results.add(performExtraction(independentNetwork, services)));

        return new ExtractionResult(results);
    }
    record Extracted(Choreography chor, DirectedPseudograph<Node,Label> SEG,
                     DirectedPseudograph<Node,Label> unrolled, int badLoops,
                     BuildGraphResult result, Node rootNode){}

    public record Data(DirectedPseudograph<Node,Label> symbolicExecutionGraph,
                              DirectedPseudograph<Node,Label> unrolledGraph,
                              int badLoopCount, int nodeCount,
                              BuildGraphResult result, Node rootNode){}

    /**
     * Container to hold the Program instance resulting from the extraction, as well as
     * data accumulated during extraction.
     * The Program contains a list of at least one Choreography instance, where each instance may be null.
     * Each Choreography instance is extracted from a set of processes that acts independently of the rest
     * of the input network. If all processes are interdependent, there will be only one Choreography.
     * If extraction fails, the Choreography will be null.
     * The list of data has one entry for each Choreography in the Program, and corresponds to the one of the same index.
     * The data is a record storing the generated Symbolic Execution Graph (SEG), the unrolled SEG if SEG generation
     * was fully completed (null otherwise), the result of building the SEG (OK, BAD_LOOP, FAIL),
     * the number of bad loops attempted during SEG generation, the number of nodes in the final SEG,
     * and the root node of both graphs (it is the same Node instance for both).
     */
    public static class ExtractionResult {
        public final Program program;
        public final List<Data> extractionInfo;
        ExtractionResult(List<Extracted> extracted){
            extracted.sort(new ResultSorter()); //Sort input

            //Generate the Program instance
            var choreographies = new ArrayList<Choreography>(extracted.size());
            var statistics = new ArrayList<Program.GraphData>(extracted.size());
            extracted.forEach(result -> {
                choreographies.add(result.chor);
                statistics.add(new Program.GraphData(result.SEG.vertexSet().size(), result.badLoops));
            });
            program = new Program(choreographies, statistics);

            //Arrange the related info
            extractionInfo = new ArrayList<>(extracted.size());
            extracted.forEach(result ->
                    extractionInfo.add(
                        new Data(result.SEG, result.unrolled, result.badLoops, result.SEG.vertexSet().size(), result.result, result.rootNode)
                    )
                );
        }
        private static class ResultSorter implements Comparator<Extracted>{
            @Override
            public int compare(Extracted res1, Extracted res2) {
                String chor1 = String.valueOf(res1.chor);
                String chor2 = String.valueOf(res2.chor);
                return chor1.compareTo(chor2);
            }
        }
    }

    /**
     * Actually does the extraction for a network that has been checked.
     */
    private Extracted performExtraction(Network network, Set<String> services){
        var graphContainer = GraphBuilder.buildSEG(network, services, extractionStrategy);

        Node rootNode = graphContainer.rootNode();
        BuildGraphResult result = graphContainer.buildGraphResult();
        int badLoopCount = graphContainer.badLoopCounter();

        DirectedPseudograph<Node,Label> SEG;
        DirectedPseudograph<Node, Label> unrolled;
        Choreography choreography;

        if (result != BuildGraphResult.OK){
            SEG = graphContainer.graph();
            unrolled = null;
            choreography = null;
        }
        else{
            SEG = new DirectedPseudograph<>(Label.class);
            Graphs.addGraph(SEG, graphContainer.graph());//Shallow copy the graph
            choreography = ChoreographyBuilder.buildChoreography(graphContainer.rootNode(), graphContainer.graph());
            unrolled = graphContainer.graph();//The call to buildChoreography unrolls the original graph
        }
        return new Extracted(choreography, SEG, unrolled, badLoopCount, result, rootNode);
    }

    private Program extractChoreography(String networkDescription, Set<String> services){
        Network network = Parser.stringToNetwork(networkDescription);
        NetworkPurger.purgeNetwork(network);

        if (!NetAnalyser.isSafe(network)){
            System.out.println("Network is not well-formed, and can therefore not be extracted");
            return new Program(List.of(), List.of());
        }
        System.out.println("The network is well-formed and extraction can proceed");

        var parallelNetworks = Splitter.splitNetwork(network);
        if (parallelNetworks == null){
            System.out.println("The network could not be split into parallel networks, and extraction has been aborted");
            return new Program(List.of(), List.of());
        } else if (parallelNetworks.size() == 1)
            System.out.println("All processes are interdependent");
        else
            System.out.println("The input network has been split into parallel independent networks");

        List<ChorStatsPair> results = Collections.synchronizedList(new ArrayList<>());
        parallelNetworks.parallelStream().forEach(net -> {
            //net.acquaintances.matrix.get(1).set(0, Boolean.FALSE);    //Test only known processes may interact
            results.add(extractLegacy(net, services));
        });

        results.sort(new ResultSorter());

        var choreographies = new ArrayList<Choreography>();
        var statistics = new ArrayList<Program.GraphData>();

        results.forEach(pair -> {
            choreographies.add(pair.chor);
            statistics.add(pair.stats);
        });

        return new Program(choreographies, statistics);
    }

    private Program extractChoreographySequentially(String networkDescription, Set<String> services){
        Network network = Parser.stringToNetwork(networkDescription);
        NetworkPurger.purgeNetwork(network);

        if (!NetAnalyser.isSafe(network)){
            System.out.println("Network is not well-formed, and can therefore not be extracted");
            return new Program(List.of(), List.of());
        }
        System.out.println("The Network is well-formed and extraction can proceed");
        //network.processes.put("stopped", new ProcessTerm(new HashMap<>(), Termination.instance));

        ChorStatsPair result = extractLegacy(network, services);
        //List.of() requires non-null parameters, that's why the singletons.
        var chorSingleton = new ArrayList<Choreography>(){{add(result.chor);}};
        var statsSingleton = new ArrayList<Program.GraphData>(){{add(result.stats);}};
        return new Program(chorSingleton, statsSingleton);
    }


    private ChorStatsPair extractLegacy(Network network, Set<String> services){
        var graphContainer = GraphBuilder.buildSEG(network, services, extractionStrategy);

        var statistics = new Program.GraphData(graphContainer.graph().vertexSet().size(), graphContainer.badLoopCounter());
        if (graphContainer.buildGraphResult() != BuildGraphResult.OK){
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
        Program.GraphData stats;
        public ChorStatsPair(Choreography chor, Program.GraphData stats){
            this.chor = chor;
            this.stats = stats;
        }
    }


}
