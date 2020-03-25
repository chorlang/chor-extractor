package extraction;

import network.ProcessTerm;

import java.util.HashMap;
import java.util.LinkedHashMap;

import static network.Behaviour.Action.*;

/**
 * This enum class contains different implementations of copyAndSort() which takes a network
 * in the form of a HashMap, and returns a HashMap that is sorted according to a specific
 * strategy. The returned HashMap is a LinkedHashMap, as that retains the order that values
 * are put into it.
 */
public enum Strategy {
    Default {
        @Override
        public LinkedHashMap<String, ProcessTerm> copyAndSort(HashMap<String, ProcessTerm> network) {
            return InteractionFirst.copyAndSort(network);
        }
    },
    /**
     * Places all processes who's main procedures involve intra-process communication
     * first, followed by anything else, in no particular order.
     */
    InteractionFirst{
        @Override
        public LinkedHashMap<String, ProcessTerm> copyAndSort(HashMap<String, ProcessTerm> network){
            LinkedHashMap<String, ProcessTerm> sortedNetwork = new LinkedHashMap<>(network.size());

            network.forEach((String processName, ProcessTerm process) -> {
                if (process.main.getAction() == SEND
                || process.main.getAction() == RECEIVE
                || process.main.getAction() == SELECTION
                || process.main.getAction() == OFFERING){
                    sortedNetwork.put(processName, process.copy());
                }
            });

            network.forEach((String processName, ProcessTerm process) -> {
                if (!sortedNetwork.containsKey(processName)){
                    sortedNetwork.put(processName, process.copy());
                }
            });

            return sortedNetwork;
        }
    };

    public abstract LinkedHashMap<String, ProcessTerm> copyAndSort(HashMap<String, ProcessTerm> network);
}