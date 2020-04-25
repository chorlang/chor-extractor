package utility;

import extraction.Extraction;
import extraction.Strategy;

import java.util.Set;

public class TestUtils {
    public static Strategy parseStrategy(String strategy) {
        Strategy s = null;
        for (var strat : Strategy.values()){
            if (strat.name().equals(strategy)){
                s = strat;
                break;
            }
        }
        if (s == null)
            return Strategy.InteractionsFirst;
        else
            return s;
    }

    public static void printExtractionResult(String network, String choreography, String expected) {
        if (!expected.equals(choreography)) {
            System.out.println("ERROR");
        } else {
            printExtractionResult(network, choreography);
        }
    }

    public static void printExtractionResult(String network, String choreography) {
        System.out.println("Input network:\n\t$network\nOutput choreography:\n\t$choreography");
    }

    public static void runExtractionTest(String network, String ... services) {
        printExtractionResult(network, Extraction.extractChoreography(network, Strategy.Default, Set.of(services)).toString());
    }

    public static void runSequentialExtractionTest(String network, String ... services) {
        printExtractionResult(network, Extraction.extractChoreographySequentially(network, Strategy.Default, Set.of(services)).toString());
    }
}
