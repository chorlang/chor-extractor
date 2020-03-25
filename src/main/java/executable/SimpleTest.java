package executable;

import extraction.Extraction;
import extraction.Strategy;
import network.Behaviour;
import network.Network;
import parsing.Parser;

public class SimpleTest {
    static String testNetwork =
            "c { def X {a!<pwd>; a&{ok: s?; stop, ko: X}} main {X}} | " +
                    "a { def X {c?; s?; if s then c+ok; s+ok; stop else c+ko; s+ko; X} main {X}} | " +
                    "s { def X {a!<s>; a&{ok: c!<t>; stop, ko:X}} main {X}}";
    public static void main(String[] args){
        System.out.println("Begins simple testing.\nTest network to extract from:");
        System.out.println(testNetwork + "\n");

        Network network = Parser.stringToNetwork(testNetwork);
        System.out.println("The Network AST generated is:");
        System.out.println(network.toString() + "\n");

        String result = checkEquals(network) ? "Success" : "Failure. The subsequent tests might fail, or not work correctly";
        System.out.println("Result of Network copying test: " + result + "\n");

        System.out.println("Testing choreography extraction with InteractionFirst strategy...");
        Strategy st = Strategy.InteractionFirst;
        var extractor = new Extraction(st);
        var choreography = extractor.extractChoreography(testNetwork);
        System.out.println("Choreography AST generated. Result:");
        System.out.println(choreography.toString());
    }

    static boolean checkEquals(Behaviour b){
        var copy = b.copy();
        return copy.equals(b);
    }
}
