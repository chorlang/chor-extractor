package executable;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.util.mxCellRenderer;
import endpointprojection.EndPointProjection;
import executable.tests.AccumulateData;
import executable.tests.Benchmarking;
import executable.tests.Benchmarks;
import extraction.*;
import extraction.Label;
import extraction.choreography.Program;
import extraction.network.NetAnalyser;
import extraction.network.Network;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DirectedPseudograph;
import parsing.Parser;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
            "a { def X {b!<1>; b?; b!<0>; b?; X} main {b!<0>; X} } |" +
                    "b { def X {a?; a!<ack0>; a?; a!<ack1>; X} main {X} }";
    static String syncAlt2bit =
            "a { def X{b!<0>; b?; b!<1>; b?; X} main{X} } |" +
                    "b { def X{a?; a!<ack0>; a?; a!<ack1>; X} main{X} }";
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
    static String generalContinuationLoop =
            "a { def Y{ X; b&{retry: Y, fin: } } def X{ if e then b+ok; X else b+ko; b?; } main{ b!<init>; Y; stop} } |" +
            "b { def X{ a&{ok: X, ko: a!<akn>; if retry then a+retry; X else a+fin; } } main{ a?; X; stop} }";
    static String spawnConditional = "a{ def X{ if e then spawn child with X continue stop else stop } main{ X } }";

    static String spawnRecursive = "a{ \n" +
            "  def X(parent){ \n" +
            "    if end then parent!<result>; stop else spawn child with X(child) continue child?; parent!<result>; stop\n" +
            "  } main { X(b) } \n" +
            "} |\n" +
            "b{ main{ a?; stop } } ";

    static String osci = "a{ def osci(p){ p?; p!<resp>; osci(p) } main { spawn b with osci(a) continue b!<start>; osci(b) } }";
    public static String np = """
p{def A{if c1 then n + L; j + L; h + L; x + L; v + L; d + L; s + L; c + L; q + L; » else n + R; j + R; h + R; x + R; v + R; d + R; s + R; c + R; q + R; stop continue V} def V{d&{R: », L: x!<m5>; W}; h&{R: V, L: »}; d!<m4>; stop} def W{h&{l5: A}} def M{h&{R: q&{R: stop, L: »}, L: »}; h&{R: M, L: V}} main {M}} | q{def A{p&{R: stop, L: »}; V} def V{d&{R: », L: W}; h&{R: V, L: »}; stop} def W{A} def M{h&{R: if c11 then n + L; j + L; x + L; h + L; v + L; d + L; s + L; c + L; p + L; » else n + R; j + R; x + R; h + R; v + R; d + R; s + R; c + R; p + R; stop endif, L: »}; h&{R: M, L: V}} main {M}} | s{def A{p&{R: stop, L: »}; V} def V{d&{R: », L: W}; h&{R: V, L: »}; stop} def W{A} def M{h&{R: q&{R: stop, L: »}, L: »}; h&{R: M, L: V}} main {M}} | c{def A{p&{R: stop, L: »}; V} def V{d&{R: », L: if c6 then » else » continue W}; h&{R: V, L: »}; stop} def W{n + l4; A} def M{h&{R: q&{R: stop, L: »}; d&{l8: »}, L: »}; h&{R: M, L: V}} main {M}} | d{def A{p&{R: stop, L: »}; j + l2; V} def V{if c5 then n + L; j + L; h + L; x + L; v + L; s + L; c + L; q + L; p + L; W else n + R; j + R; h + R; x + R; v + R; s + R; c + R; q + R; p + R; » continue h&{R: V, L: »}; p?; stop} def W{A} def M{h&{R: q&{R: stop, L: »}; c + l8; », L: »}; h&{R: M, L: V}} main {M}} | v{def A{p&{R: stop, L: »}; V} def V{d&{R: », L: W}; h&{R: V, L: »}; stop} def W{A} def M{h&{R: q&{R: stop, L: »}, L: »}; h&{R: M, L: V}} main {M}} | h{def A{p&{R: stop, L: »}; V} def V{d&{R: », L: W}; if c4 then n + L; j + L; x + L; v + L; d + L; s + L; c + L; q + L; p + L; » else n + R; j + R; x + R; v + R; d + R; s + R; c + R; q + R; p + R; V continue stop} def W{p + l5; A} def M{if c10 then n + L; j + L; x + L; v + L; d + L; s + L; c + L; q + L; p + L; » else n + R; j + R; x + R; v + R; d + R; s + R; c + R; q + R; p + R; q&{R: stop, L: »} continue if c9 then n + L; j + L; x + L; v + L; d + L; s + L; c + L; q + L; p + L; V else n + R; j + R; x + R; v + R; d + R; s + R; c + R; q + R; p + R; M endif} main {M}} | x{def A{p&{R: stop, L: »}; V} def V{d&{R: », L: p?; W}; h&{R: V, L: »}; stop} def W{A} def M{h&{R: q&{R: stop, L: »}, L: »}; h&{R: M, L: V}} main {M}} | j{def A{n + l1; p&{R: stop, L: »}; d&{l2: V}} def V{d&{R: », L: W}; h&{R: V, L: »}; stop} def W{A} def M{h&{R: q&{R: stop, L: »}, L: »}; h&{R: M, L: V}} main {M}} | n{def A{j&{l1: p&{R: stop, L: »}; V}} def V{d&{R: », L: W}; h&{R: V, L: »}; stop} def W{c&{l4: A}} def M{h&{R: q&{R: stop, L: »}, L: »}; h&{R: M, L: V}} main {M}}""";
    public static String problem = """
    p{def A{if c1 then n + L; j + L; h + L; x + L; v + L; d + L; s + L; c + L; q + L; » else n + R; j + R; h + R; x + R; v + R; d + R; s + R; c + R; q + R; M continue V} def R{T} def B{s&{l3: Y}} def S{d&{R: C, L: R}} def C{S} def T{if c3 then j + L; h + L; v + L; d + L; s + L; c + L; » else j + R; h + R; v + R; d + R; s + R; c + R; S continue C} def V{d&{R: », L: x!<m5>; W}; h&{R: V, L: »}; d!<m4>; B} def W{h&{l5: A}} def G{h?; c&{R: G, L: T}} def Y{G} def M{h&{R: q&{R: stop, L: »}, L: »}; h&{R: M, L: V}} main {M}} | q{def A{p&{R: M, L: »}; V} def V{d&{R: », L: W}; h&{R: V, L: »}; stop} def W{A} def M{h&{R: if c11 then n + L; j + L; x + L; h + L; v + L; d + L; s + L; c + L; p + L; » else n + R; j + R; x + R; h + R; v + R; d + R; s + R; c + R; p + R; stop endif, L: »}; h&{R: M, L: V}} main {M}} | s{def A{p&{R: M, L: »}; V} def R{v?; T} def B{p + l3; Y} def S{d&{R: C, L: R}} def C{S} def T{p&{R: S, L: »}; C} def V{d&{R: », L: W}; h&{R: V, L: »}; B} def W{A} def G{c&{R: h + l6; G, L: T}} def Y{c + l7; G} def M{h&{R: q&{R: stop, L: »}, L: »}; h&{R: M, L: V}} main {M}} | c{def A{p&{R: M, L: »}; V} def R{T} def B{Y} def S{d&{R: C, L: R}} def C{S} def T{p&{R: S, L: »}; j?; C} def V{d&{R: », L: if c6 then » else » continue W}; h&{R: V, L: »}; B} def W{n + l4; A} def G{if c7 then j + L; h + L; v + L; d + L; s + L; p + L; T else j + R; h + R; v + R; d + R; s + R; p + R; G endif} def Y{j?; s&{l7: G}} def M{h&{R: q&{R: stop, L: »}; d&{l8: »}, L: »}; h&{R: M, L: V}} main {M}} | d{def A{p&{R: M, L: »}; j + l2; V} def R{T} def B{Y} def S{if c2 then j + L; h + L; v + L; c + L; s + L; p + L; R else j + R; h + R; v + R; c + R; s + R; p + R; C endif} def C{S} def T{p&{R: S, L: »}; C} def V{if c5 then n + L; j + L; h + L; x + L; v + L; s + L; c + L; q + L; p + L; W else n + R; j + R; h + R; x + R; v + R; s + R; c + R; q + R; p + R; » continue h&{R: V, L: »}; p?; B} def W{A} def G{c&{R: G, L: T}} def Y{G} def M{h&{R: q&{R: stop, L: »}; c + l8; », L: »}; h&{R: M, L: V}} main {M}} | v{def A{p&{R: M, L: »}; V} def R{s!<m1>; T} def B{Y} def S{d&{R: C, L: R}} def C{S} def T{p&{R: S, L: »}; C} def V{d&{R: », L: W}; h&{R: V, L: »}; B} def W{A} def G{c&{R: G, L: T}} def Y{G} def M{h&{R: q&{R: stop, L: »}, L: »}; h&{R: M, L: V}} main {M}} | h{def A{p&{R: M, L: »}; V} def R{T} def B{Y} def S{d&{R: C, L: R}} def C{j!<m2>; S} def T{p&{R: S, L: »}; C} def V{d&{R: », L: W}; if c4 then n + L; j + L; x + L; v + L; d + L; s + L; c + L; q + L; p + L; » else n + R; j + R; x + R; v + R; d + R; s + R; c + R; q + R; p + R; V continue B} def W{p + l5; A} def G{p!<m6>; c&{R: s&{l6: G}, L: T}} def Y{G} def M{if c10 then n + L; j + L; x + L; v + L; d + L; s + L; c + L; q + L; p + L; » else n + R; j + R; x + R; v + R; d + R; s + R; c + R; q + R; p + R; q&{R: stop, L: »} continue if c9 then n + L; j + L; x + L; v + L; d + L; s + L; c + L; q + L; p + L; V else n + R; j + R; x + R; v + R; d + R; s + R; c + R; q + R; p + R; M endif} main {M}} | x{def A{p&{R: M, L: »}; V} def V{d&{R: », L: p?; W}; h&{R: V, L: »}; stop} def W{A} def M{h&{R: q&{R: stop, L: »}, L: »}; h&{R: M, L: V}} main {M}} | j{def A{n + l1; p&{R: M, L: »}; d&{l2: V}} def R{T} def B{Y} def S{d&{R: C, L: R}} def C{h?; S} def T{p&{R: S, L: »}; c!<m3>; C} def V{d&{R: », L: W}; h&{R: V, L: »}; B} def W{A} def G{c&{R: G, L: T}} def Y{c!<m7>; G} def M{h&{R: q&{R: stop, L: »}, L: »}; h&{R: M, L: V}} main {M}} | n{def A{j&{l1: p&{R: M, L: »}; V}} def B{Y} def V{d&{R: », L: W}; h&{R: V, L: »}; B} def W{c&{l4: A}} def Y{if c8 then » else » continue stop} def M{h&{R: q&{R: stop, L: »}, L: »}; h&{R: M, L: V}} main {M}}
""";

    static String p2 = """
            d{ def V{ if c5 then p+L; j+L; A else p+R; j+R; continue p?; stop } def A{ p&{R: stop, L: }; j+l2; V } main{V} } |
            p{ def V{ d&{R: , L: A}; d!<m>; stop } def A{ if c1 then d+L; j+L; else d+R; j+R; stop continue V } main {V}} |
            j{ def V{ d&{R: , L: A}; stop } def A{ p&{R: stop, L:}; d&{l2: V} } main{V}}""";
    static String p3 = """
            p{ def X{if e then else continue Y} def Y{q?; q!<m>; X} main{X}} |
            q{ def X{p!<m>; p?; X} main{X}}
            """;
    static String quicksort1 = """
            s{ def QS(parent, self){ parent?; if baseCase then parent!<list>; stop else spawn a with QS(self, a) continue a!<smaller>; spawn b with QS(self, b) continue b!<bigger>; a?; b?; parent!<merge>; stop } main{ spawn p with QS(s, p) continue p!<list>; p?; stop } }
            """;
    static String quicksort = """
            s{ def QS{if basecase then stop else spawn a with QS continue spawn b with QS continue stop} main{QS}}
            """;
    static String renamingTest = """
            s{def A{s!<m>; B} def B{s?; C} def C{s+cont; A} def X(a,b,c){c&{cont: a?;}; b!<m>; X(c,a,b)}
            main{spawn a with A continue spawn b with B continue spawn c with C continue X(a,b,c)} }
            """;
    static String recursiveSpawn = """
            p {def X(parent){if e then spawn q with parent?; X(q); parent!<res>; stop continue q!<mes>; q?; else} main{spawn q with X(q); stop continue stop}}
            """;
    static String recChor = """
            def X(t){ if t.cont then t spawns q; X(q); q.m->t; else } main{X(p); stop}
            """;
    static String recChor2 = """
            def X(t){ if t.cont then t spawns q; X(q); q.m->t; else } main{X(p); stop}
            """;
    static String recNet = """
            p{def X(t){ if cont then spawn q with X(q); t!<m>; stop continue else } main{X(p); stop} }
            """;
    static String recNet2 = """
            s{def X(p,t){ if cont then spawn q with X(t,q) continue q?; p!<m>; stop else p!<m>; stop} main{X(p,s)} } |
            p{s?; stop}
            """;

    static String shopping = """
            customer{ def browse{ store!<item>; if checkout then store+buy; purchase else store+more; browse } def purchase{ store!<payment>; store&{accept: stop, reject: purchase} } main{ browse } } |
            store{ def offer{ customer?; customer&{buy: payment, more: offer} } def payment{ customer?; if accepted then customer+accept; stop else customer+reject; payment } main{ offer } }
            """;

    static String serverless = """
            internet{
                def task{ internet?entrypoint; entrypoint?server; server!<request>; server&{bad: stop, OK: server?; stop} }
                def X{ spawn client with task continue client<->entrypoint; X } main{ X } } |
            entrypoint{
                def service{ entrypoint?client; client?; if validReq then client+OK; client!<response>; stop else client+bad; stop }
                def handle{ internet?client; spawn worker with service continue worker<->client; handle }
                main{ handle } }
            """;


    public static void main(String []args) {
        System.out.println("Hello World");

        /*for (Strategy strategy : Strategy.values()){
            if (strategy != Strategy.Default)
                AccumulateData.accumulate(strategy);
        }*/

        //Benchmarking.benchmarkStrategy(Strategy.InteractionsFirst);
        //Benchmarks.INSTANCE.extraction(Strategy.InteractionsFirst);
        //AccumulateData.accumulate(Strategy.InteractionsFirst);

        /*var nets = Benchmarking.readNetworkFile("test/projection-50-6-30-0");
        var net = nets.get("C1026");
        long start = System.currentTimeMillis();
        var res = Extraction.newExtractor().extract(net);
        long time = System.currentTimeMillis() - start;
        System.out.println("Extraction time: "+time);
        start = System.currentTimeMillis();
        var clone = new DirectedPseudograph<>(Label.class);
        Graphs.addGraph(clone, res.extractionInfo.get(0).symbolicExecutionGraph());
        time = System.currentTimeMillis() - start;
        System.out.println("Clone time: "+time);*/

        //*
        String networksString = serverless;
        //System.out.println(networksString);
        Network network = Parser.stringToNetwork(networksString);
        System.out.println(network.toString());
        var result = Extraction.newExtractor().extract(networksString, Set.of("retailer"));
        //var purgedChor = Purger.purgeIsolated(choreography.choreographies.get(0));
        String chor = result.program.toString();
        var seg = result.extractionInfo.get(0).symbolicExecutionGraph();
        System.out.println(chor);
        //generateImage(result.extractionInfo.get(0).symbolicExecutionGraph(), "p2.png");
        //*/


        /*
        String chorstring;
        try{
            chorstring = Files.readString(Paths.get("chortest.txt"));
            Program p = Parser.stringToProgram(chorstring);
            Network n = EndPointProjection.project(p);
            System.out.println(n);
            System.out.println("=====");
            var result = Extraction.newExtractor().extract(n, Set.of());
            System.out.println(result.program);
            //generateImage(result.extractionInfo.get(0).symbolicExecutionGraph(), "simple1.png");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }//*/



        //Benchmarking.EndpointProjection();
        //Benchmarking.extractionBenchmarks();

        /*
        String chorString = chorLoop;
        Program chor = Parser.stringToProgram(chorString);
        System.out.println(chor.toString());
        var projection = EndPointProjection.project(chorString);
        System.out.println(EndPointProjection.project(chorString));
        //*/
    //*

        //generateImage(result.extractionInfo.get(0).symbolicExecutionGraph(), "graph2.png");
        //GraphBuilder.SEGContainer container = GraphBuilder.buildSEG(network, Set.of("retailer"), Strategy.Default);




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
    public static void generateImage(Graph<Node, Label> graph, String imgPath){
        JGraphXAdapter<Node, Label> graphXAdapter = new JGraphXAdapter<>(graph);

        mxGraphLayout layout = new mxHierarchicalLayout(graphXAdapter);
        layout.execute(graphXAdapter.getDefaultParent());

        BufferedImage image = mxCellRenderer.createBufferedImage(graphXAdapter, null, 1, Color.WHITE, false, null);
        File imgFile = new File(imgPath);
        try {
            ImageIO.write(image, "PNG", imgFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
