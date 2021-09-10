package executable;

import extraction.*;
import extraction.network.*;
import parsing.Parser;

import java.util.Set;

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

    public static void main(String []args){
        System.out.println("Hello World");

        //*
        String networksString = offeringAsync;
        Network network = Parser.stringToNetwork(networksString);
        System.out.println(network.toString());
        var extractor = new Extraction(Strategy.Default);
        var choreography = extractor.extractChoreography(networksString, Set.of());
        String chor = choreography.toString();
        System.out.println(chor);
        //*/

    }

    static void checkEquals(Behaviour b){
        var copy = b.copy();
        System.out.println(copy.equals(b));
    }
}
