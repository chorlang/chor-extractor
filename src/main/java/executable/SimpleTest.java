package executable;

import extraction.Extraction;
import extraction.Strategy;
import extraction.network.Behaviour;
import extraction.network.Network;
import parsing.Parser;

import java.util.Set;

public class SimpleTest {
    static String testNetwork =
            "c { def X {a!<pwd>; a&{ok: s?; stop, ko: X}} main {X}} | " +
                    "a { def X {c?; s?; if s then c+ok; s+ok; stop else c+ko; s+ko; X} main {X}} | " +
                    "s { def X {a!<s>; a&{ok: c!<t>; stop, ko:X}} main {X}}";
    public static void main(String[] args){
        System.out.println("Begins simple testing.\nTest extraction.network to extract from:");
        System.out.println(testNetwork + "\n");

        Network network = Parser.stringToNetwork(testNetwork);
        System.out.println("The Network AST generated is:");
        System.out.println(network.toString() + "\n");

        String result = checkEquals(network) ? "Success" : "Failure. The subsequent tests might fail, or not work correctly";
        System.out.println("Result of Network copying test: " + result + "\n");

        System.out.println("Testing choreography extraction with InteractionFirst strategy...");
        Strategy st = Strategy.InteractionsFirst;
        var extractor = new Extraction(st);
        var program = extractor.extractChoreography(testNetwork, Set.of());
        System.out.println("Choreography AST generated. Result:");
        System.out.println(program.toString());
    }

    static boolean checkEquals(Network n){
        var copy = n.copy();
        return copy.equals(n);
    }
}
