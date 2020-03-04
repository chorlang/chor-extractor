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
        public HashMap<String, ProcessTerm> copyAndSort(HashMap<String, ProcessTerm> network) {
            return InteractionFirst.copyAndSort(network);
        }
    },
    /**
     * Places all processes who's main procedures involve intra-process communication
     * first, followed by anything else, in no particular order.
     */
    InteractionFirst{
        @Override
        public HashMap<String, ProcessTerm> copyAndSort(HashMap<String, ProcessTerm> network){
            HashMap<String, ProcessTerm> sortedNetwork = new LinkedHashMap<>(network.size());

            network.forEach((String processName, ProcessTerm process) -> {
                if (process.main.getAction() == send
                || process.main.getAction() == receive
                || process.main.getAction() == selection
                || process.main.getAction() == offering){
                    sortedNetwork.put(processName, process);
                }
            });

            network.forEach((String processName, ProcessTerm process) -> {
                if (!sortedNetwork.containsKey(processName)){
                    sortedNetwork.put(processName, process);
                }
            });

            return sortedNetwork;
        }
    };

    public abstract HashMap<String, ProcessTerm> copyAndSort(HashMap<String, ProcessTerm> network);
}