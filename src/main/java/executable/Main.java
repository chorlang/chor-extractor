package executable;

import extraction.GraphBuilder;
import extraction.Strategy;
import network.*;
import parsing.Parser;

import java.util.Set;

public class Main {
    static String testNetwork =
            "c { def X {a!<pwd>; a&{ok: s?; stop, ko: X}} main {X}} | " +
                    "a { def X {c?; s?; if s then c+ok; s+ok; stop else c+ko; s+ko; X} main {X}} | " +
                    "s { def X {a!<s>; a&{ok: c!<t>; stop, ko:X}} main {X}}";
    static String testNetwork2 =
            "c { def X {a!<pwd>; a&{ok: stop, ko: X}} main {X}} | " +
                    "a { def X {c?; if c then c+ok; stop else c+ko; X} main {X}}";
    static String simpleNetwork =
            "a { def X {b!<pwd>; stop} main {X}} | " +
                    "b {def X {a?; stop} main {X}}";
    static String offeringNetwork =
            "a { def X {b+ok; b+ko; stop} main {X}}" +
                    "| b {def X {a&{ok: X, ko: stop} main {X}}";
    static String conditionalNetwork =
            "a { def X {b!<pwd>; b?; if b then stop else X} main {X}}" +
                    "| b { def X {a?; if a then a!<no>; stop else a!<yes>; X} main {X}}";
    static String triProcesses =
            "a { def X {b!<y>; c!<z>; b?; c?; stop} main {X}}" +
                    "| b { def X {a?; c?; a!<ok>; stop} main {X}}" +
                    "| c { def X {a?; b!<ok>; a!<ok>; stop} main {X}}";
    static String offering2 =
            "a { def X {b!<msg>; b&{ok: stop, ko: X}} main {X}}" +
                    "| b { def X {a?; if a then a+ok; stop else a+ko; X} main {X}}";
    static String loop =
            "a { def X {b!<msg>; X} main {X}}" +
                    "| b { def X {a?; X} main {X}}";

    public static void main(String []args){
        System.out.println("Hello World");
        Network network = Parser.stringToNetwork(testNetwork2);
        System.out.println(network.toString());
        Behaviour pi = new ProcedureInvocation("X");
        checkEquals(pi);
        var snd = new Send("b", "msg", pi);
        checkEquals(snd);
        var rcv = new Receive("a", pi);
        checkEquals(rcv);
        checkEquals(network);
        //*
        Strategy st = Strategy.InteractionFirst;
        var builder = new GraphBuilder(st);
        var graph = builder.makeGraph(network, Set.of());//*/
        System.out.println("Done");

    }

    static void checkEquals(Behaviour b){
        var copy = b.copy();
        System.out.println(copy.equals(b));
    }
}
