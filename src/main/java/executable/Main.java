package executable;

import endpointprojection.EndPointProjection;
import extraction.Extraction;
import extraction.Strategy;
import extraction.choreography.Program;
import extraction.network.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import parsing.Parser;

import java.util.*;

import static java.util.List.of;

@SuppressWarnings("unused")
public class Main {
    static String testNetwork =
            "c { def X {a!<pwd>; a&{ok: s?; stop, ko: X}} main {X}} | " +
                    "a { def X {c?; s?; if s then c+ok; s+ok; stop else c+ko; s+ko; X} main {X}} | " +
                    "s { def X {a!<s>; a&{ok: c!<t>; stop, ko:X}} main {X}}";
    static String testNetwork2 =
            "a { def X {b!<pwd>; b&{ok: stop, ko: X}} main {X}} | " +
                    "b { def X {a?; if a then a+ok; stop else a+ko; X} main {X}}";
    static String simpleNetwork =
            "a { def X {b!<pwd>; stop} main {X}} | " +
                    "b {def X {a?; stop} main {X}}";
    static String offeringNetwork =
            "a { def X {b+ok; b+ko; stop} main {X}}" +
                    "| b {def X {a&{ok: X, ko: stop}} main {X}}";
    static String conditionalNetwork =
            "a { def X {b!<pwd>; b?; if b then stop else stop} main {X}}" +
                    "| b { def X {a?; if a then a!<no>; stop else a!<yes>; stop} main {X}}";
    static String triProcesses =
            "a { def X {b!<y>; c!<z>; b?; c?; stop} main {X}}" +
                    "| b { def X {a?; c?; a!<ok>; stop} main {X}}" +
                    "| c { def X {a?; b!<ok>; a!<ok>; stop} main {X}}";
    static String offering2 =
            "a { def X {b!<msg>; b&{ok: stop, ko: X}} main {X}}" +
                    "| b { def X {a?; if a then a+ok; stop else a+ko; X} main {X}}";
    static String offeringAsync =
            "a { def X {b!<msg>; b!<msg2>; b&{ok: stop, ko: X}} main {X}}" +
                    "| b { def X {a?; if a then a+ok; a?; stop else a+ko; a?; X} main {X}}";
    static String loop =
            "a { def X {b!<msg>; X} main {X}}" +
                    "| b { def X {a?; X} main {X}}";
    static String async =
            "a { main {b!<msg1>; b?; stop} } | " +
                    "b { main {a!<msg2>; a?; stop} }";
            //"a { def X {b!<msg1>; b?; stop} main {X} } | " +
              //      "b { def X {a!<msg2>; a?; stop} main {X} }";
    static String async2 =
            "a { main {b?; b!<msg1>; b?; stop} } |" +
                    "b { main {a!<pre>; a!<msg2>; c!<msg3>; a?; stop} } |" +
                    "c { main {b?; stop} }";
    static String alt2bit =
            "a { def X {b?; b!<0>; b?; b!<1>; X} main {b!<0>; b!<1>; X} } |" +
                    "b { def Y {a?; a!<ack0>; a?; a!<ack1>; Y} main {Y} }";
    static String multicomUnfolding =
            "a{def X {b?; stop} main {b!<msg>; X}} |"+
                    "b { main {a!<msg2>; a?; stop}}";
    static String acquaint =
            "a { main { b<->c; stop}} |" +
                    "b { main { a?c; stop}} |" +
                    "c { main {a?b; stop}}";
    static String introductionTest =
            "main {a.e->b; a.b<->c; stop}";
    static String chorLoop =
            "def X { p.e->q; X} main { X }";
    static String introductionMulticom =
            "a { main { b<->c; b?; stop}} |" +
                    "b { main { a!<msg>; a?c; stop}} |" +
                    "c {main { a?b; stop}}";
    static String parameterTest =
            "a { def X(a,b,c) { stop } def Y(a) { stop } def Z() { stop } def W { stop } main { stop } }";
    static String parametizdTest =
            "a { def X(q) { q?; stop } main { b!<msg>; X(b)} } |" +
                    "b { main { a?; a!<resp>; stop } }";

    public static void main(String []args){
        System.out.println("Hello World");

        var dict = new LinkedHashMap<>();

        /*
        String chorString = chorLoop;
        Program chor = Parser.stringToProgram(chorString);
        System.out.println(chor.toString());
        var projection = EndPointProjection.project(chorString);
        System.out.println(EndPointProjection.project(chorString));
        //*/
        //*
        String networksString = parametizdTest;
        System.out.println(networksString);
        Network network = Parser.stringToNetwork(networksString);
        System.out.println(network.toString());
        var extractor = new Extraction(Strategy.Default);
        var choreography = extractor.extractChoreographySequentially(networksString, Set.of());
        String chor = choreography.toString();
        System.out.println(chor);
        //*/

        /*
        int initial = 200;
        int end = 400;
        var processes = new ArrayList<String>(initial);
        for (int i = 0; i < initial; i++){
            processes.add(Integer.toString(i));
        }
        var adjMatrix = new AdjacencyMatrix(processes);
        //var adjMatrix = new graphContainer(processes);
        int counter = 0;
        var rand = new Random();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 20000; i++){
            if (adjMatrix.isIntroduced(String.valueOf(rand.nextInt(initial)), String.valueOf(rand.nextInt(initial))))
                counter++;
        }
        for (int i = initial; i < end; i++){
            String spawned = Integer.toString(i);
            adjMatrix.spawn(String.valueOf(rand.nextInt(i)), spawned);
            for (int j = 0; j < 10; j++){
                adjMatrix.introduce(spawned, String.valueOf(rand.nextInt(i)));
            }
            for (int j = 0; j < 1000; j++){
                if (adjMatrix.isIntroduced(String.valueOf(rand.nextInt(i)), String.valueOf(rand.nextInt(i))))
                    counter++;
            }

        }
        long stop = System.currentTimeMillis();
        System.out.println(stop-start);//*/
    }

    public static class graphContainer{
        private Graph<String, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        public graphContainer(List<String> processNames){
            for (String processName : processNames) {
                graph.addVertex(processName);
            }
            for (String processa : processNames){
                for (String processb : processNames){
                    graph.addEdge(processa, processb);
                }
            }
        }

        public void spawn(String parent, String child){
            graph.addVertex(child);
            introduce(parent, child);

        }

        public void introduce(String p, String q){
            graph.addEdge(p,q);
        }

        public boolean get(String p, String q){
            return graph.containsEdge(p,q);
        }
    }
}
