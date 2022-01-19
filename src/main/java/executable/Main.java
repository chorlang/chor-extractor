package executable;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.layout.mxOrganicLayout;
import com.mxgraph.util.mxCellRenderer;
import endpointprojection.EndPointProjection;
import extraction.*;
import extraction.Label;
import extraction.choreography.Program;
import extraction.choreography.Purger;
import extraction.network.*;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DirectedPseudograph;
import parsing.Parser;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static java.util.List.of;

@SuppressWarnings("unused")
public class Main {
    static String logistics = "supplier {" +//retailer is a service process
            "def X {shipper?; consignee?; Y} " +
            "def Y {if needToShip " +
            "then shipper+item; consignee+item; X " +
            "else shipper+done; consignee+done; " +
            "retailer!<UpdatePOandDeliverySchedule>; retailer?; retailer?; retailer!<FinalizedPOandDeliverySchedule>; stop}" +
            "main { retailer!<PlannedOrderVariations>; retailer?; retailer?; Y}" +
            "} | " +
            "retailer {main {" +
            "supplier?; supplier!<OrderDeliveryVariations>; supplier!<DeliverCheckPointRequest>; " +
            "supplier?; supplier!<POandDeliveryScheduleMods>; shipper!<ConfirmationofDeliverySchedule>; " +
            "supplier!<AcceptPOandDeliverySchedule>; supplier?; stop}} |" +
            "consignee {" +
            "def X{supplier!<DeliveryItem>; Z} " +
            "def Z {supplier&{item: X, done: stop}}" +
            "main{Z}} | " +
            "shipper {" +
            "def X{supplier!<DeliveryItem>; Z} " +
            "def Z {supplier&{item: X, done: retailer?; stop}}" +
            "main{Z}}";
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
    static String nestedParameterTest =
                    "a { def X(p) { p!<msg>; Y(p) } def Y(q) { q?; q!<hi>; Z(q) } def Z(r) { r!<ok>; X(r) } main { b?; X(b) } } |" +
                    "b { def X(p) { p?; Y(p) } def Y(q) { q!<greet>; q?; Z(q) } def Z(r) { r?; X(r) } main { a!<start>; X(a) } }";
    static String spawn =
            "a { main {spawn b with a?; a!<resp>; stop continue b!<msg>; b?; stop} }";
    static String oscilator =
                    "a { def Rec(sender){ sender?; Send(sender) }" +
                    "def Send(receiver){ receiver!<hi>; Rec(receiver) }" +
                    "main { Send(b) } } | " +
                    "b { def Rec(sender){ sender?; Send(sender) }" +
                    "def Send(receiver){ receiver!<hi>; Rec(receiver) }" +
                    "main { Rec(a) } }";
    static String paramLoop =
                    "a { def X(r){ spawn q with a?; stop continue q!<hi>; X(q) } main { c!<start>; b!<hi>; X(b) } } |" +
                    "b { main { a?; stop } } |" +
                    "c { main { a?; stop } }";
    static String spawnLoop =
            "a { def X{ spawn q with X continue X } main { X } }";
    static String spawnOsci =
            "p { " +
                    "def X{ spawn a with p?q; Y(q) continue spawn b with p?r; Z(r) continue a<->b; X }" +
                    "def Y(second){ second!<hi>; second?; Y(second) }" +
                    "def Z(first){ first?; first!<hello>; Z(first) }" +
                    "main { X } }";
    static String paramIntro =
                    "a { def X(q) { q<->b; stop } main { spawn z with a?d; d?; stop continue X(z) } } | " +
                    "b { main { a?c; c!<hi>; stop } }";
    static String identicalBound =
                "p { def L1(q){ q!<hi>; q?; L1(q) }" +
                    "def L2(a,b){ a?; b?; b!<ok>; a!<ok>; L2(a,b) }" +
                    "def X{ spawn a with p?q; L1(q) continue spawn b with p?q; L1(q) continue spawn c with p?a; p?b; L2(a,b) continue a<->c; b<->c; X } main { X } }";
    static String download =
            "c { def download{ if getNextFile then " +
                    "s+more; spawn w with c?sw; sw?; stop continue s?sw; w<->sw; download else " +
                    "s+end; stop } " +
                    "main { s!<req>; s?; download } " +
               "} | " +
            "s { def serve{ c&{more: spawn w with s?c; c?cw; cw!<file>; stop continue c<->w; serve, end: stop} } main { c?; c!<filelist>; serve } }";
    static String mergeSort =
            "sorter { " +
                    "def msgSort(p, t){ p?; if listLengthIsOne then " +
                    "   p!<list>; stop " +
                    "else " +
                    "   spawn a with msgSort(t, a) continue spawn b with msgSort(t, b) continue a!<leftHalf>; b!<rightHalf>; a?; b?; p!<mergedLists>; stop }" +
                " main { spawn s with msgSort(sorter, s) continue s!<list>; s?; stop } }";
    static String terminationLoop =
            "a { def loop{ spawn p with a?; a!<hello>; stop continue p!<hi>; p?; loop } main { spawn p with a?; a!<hello>; stop continue b?; p!<hi>; p?; loop } } | b { main { a!<start>; stop } }";
    static String serverless =
            "handler { def listen{ client?; spawn worker with handler?client; handler?; client!<resp>; stop continue client<->worker; worker!<req>; listen } main { listen } } |" +
                    "client { def request{ handler!<req>; handler?instance; instance?; request } main { request } }";
    static String hierarchy =
            "CEO { def w(man){ man?; man!<solution>; stop } " +
                    "def m(dir, this){ dir?; spawn worker1 with w(this) continue spawn worker2 with w(this) continue worker1!<problem>; worker2!<problem>; worker1?; worker2?; dir!<solutions>; stop } " +
                    "def d(p, this){ p?; spawn manager1 with m(this, manager1) continue spawn manager2 with m(this, manager2) continue manager1!<task1>; manager2!<task2>; manager1?; manager2?; p!<progress>; stop } " +
                    "main { spawn director1 with d(CEO, director1) continue spawn director2 with d(CEO, director2) continue director1!<direction>; director2!<direction>; director1?; director2?; stop } }";

    static String parseError =
            "a { def X {b!<pwd>; stop} main {X}} | " +
                    "b {def X {a?; stop} main {X}}";
    static String generalContinuation =
            "a { def X{ b?; } main { b!<hi>; X; b!<hello>; stop} } | b { main { a?; a!<resp>; a?; stop } }";

    public static void main(String []args){
        System.out.println("Hello World");
        /*
        String chorString = chorLoop;
        Program chor = Parser.stringToProgram(chorString);
        System.out.println(chor.toString());
        var projection = EndPointProjection.project(chorString);
        System.out.println(EndPointProjection.project(chorString));
        //*/
        //*
        String networksString = generalContinuation;
        System.out.println(networksString);
        Network network = Parser.stringToNetwork(networksString);
        System.out.println(network.toString());
        WellFormedness.isWellFormed(network);
        var extractor = new Extraction(Strategy.Default);
        var choreography = extractor.extractChoreography(networksString, Set.of("retailer"));
        //var purgedChor = Purger.purgeIsolated(choreography.choreographies.get(0));
        String chor = choreography.toString();
        System.out.println(chor);

        /*GraphBuilder.SEGContainer container = GraphBuilder.buildSEG(network, Set.of("retailer"), Strategy.Default);
        DirectedPseudograph<Node, Label> graph = container.graph();
        JGraphXAdapter<Node, Label> graphXAdapter = new JGraphXAdapter<>(graph);
        mxGraphLayout layout = new mxHierarchicalLayout(graphXAdapter);
        layout.execute(graphXAdapter.getDefaultParent());

        BufferedImage image = mxCellRenderer.createBufferedImage(graphXAdapter, null, 2, Color.WHITE, true, null);
        File imgFile = new File("graph.png");
        try {
            ImageIO.write(image, "PNG", imgFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //System.out.println(purgedChor);
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
