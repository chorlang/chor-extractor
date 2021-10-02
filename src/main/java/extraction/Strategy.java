package extraction;

import extraction.network.*;

import java.util.*;

/**
 * This enum class contains different implementations of copyAndSort() which takes a network
 * in the form of a HashMap, and returns a HashMap that is sorted according to a specific
 * strategy. The returned HashMap is a LinkedHashMap, as that retains the order that values
 * are put into it.
 */
public enum Strategy {
    Default {
        @Override
        public LinkedHashMap<String, ProcessTerm> copyAndSort(Node.ConcreteNode node) {
            return InteractionsFirst.copyAndSort(node);
        }
    },
    /**
     * Places all processes whose main procedures involve intra-process communication
     * first, followed by anything else, in no particular order.
     */
    InteractionsFirst {
        @Override
        public LinkedHashMap<String, ProcessTerm> copyAndSort(Node.ConcreteNode node) {
            var network = node.network.processes;
            LinkedHashMap<String, ProcessTerm> sortedNetwork = new LinkedHashMap<>(network.size());

            network.forEach((String processName, ProcessTerm process) -> {
                if (process.main() instanceof Send
                || process.main() instanceof Receive
                || process.main() instanceof Selection
                || process.main() instanceof Offering){
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
    },

    ConditionsFirst{
        @Override
        public LinkedHashMap<String, ProcessTerm> copyAndSort(Node.ConcreteNode node) {
            var network = node.network.processes;
            var sortedNetwork = new LinkedHashMap<String, ProcessTerm>(network.size());

            network.forEach((processName, processTerm) ->{
                if (processTerm.main() instanceof Condition)
                    sortedNetwork.put(processName, processTerm.copy());
            });
            network.forEach((processName, processTerm) -> {
                if (processTerm.main() instanceof Selection || processTerm.main() instanceof Offering)
                    sortedNetwork.put(processName, processTerm.copy());
            });
            network.forEach((processName, processTerm) -> {
                if (!sortedNetwork.containsKey(processName))
                    sortedNetwork.put(processName, processTerm.copy());
            });

            return sortedNetwork;
        }
    },

    UnmarkedFirst{
        @Override
        public LinkedHashMap<String, ProcessTerm> copyAndSort(Node.ConcreteNode node) {
            var network = node.network.processes;
            var sortedNetwork = new LinkedHashMap<String, ProcessTerm>(network.size());

            network.forEach((processName, processTerm) -> {
                if (!node.marking.containsKey(processName))
                    sortedNetwork.put(processName, processTerm.copy());
            });
            network.forEach((processName, processTerm) -> {
                if (node.marking.containsKey(processName))
                    sortedNetwork.put(processName, processTerm.copy());
            });

            return sortedNetwork;
        }
    },

    UnmarkedThenInteractions{
        @Override
        public LinkedHashMap<String, ProcessTerm> copyAndSort(Node.ConcreteNode node) {
            var processes = node.network.processes;
            var sortedProcesses = new LinkedHashMap<String, ProcessTerm>(processes.size());

            var markedList = new ArrayList<String>();
            var unmarkedInteractionsList = new ArrayList<String>();
            var unmarkedOthersList = new ArrayList<String>();

            node.marking.forEach((processName, marking) -> {
                if (marking)
                    markedList.add(processName);
                else
                    if (isInteraction(node.network.processes.get(processName).main()))
                        unmarkedInteractionsList.add(processName);
                    else
                        unmarkedOthersList.add(processName);
            });

            unmarkedInteractionsList.forEach(processName -> sortedProcesses.put(processName, processes.get(processName).copy()));
            unmarkedOthersList.forEach(processName -> sortedProcesses.put(processName, processes.get(processName).copy()));
            markedList.forEach(processName -> sortedProcesses.put(processName, processes.get(processName).copy()));

            return sortedProcesses;
        }
        private boolean isInteraction(Behaviour b){
            return switch (b.action) {
                case SEND, RECEIVE, SELECTION, OFFERING -> true;
                default -> false;
            };
        }
    },

    Random{
        @Override
        public LinkedHashMap<String, ProcessTerm> copyAndSort(Node.ConcreteNode node) {
            var processes = node.network.processes;
            var sortedProcesses = new LinkedHashMap<String, ProcessTerm>();
            var processNames = new ArrayList<>(processes.keySet());
            Collections.shuffle(processNames);
            processNames.forEach(processName -> sortedProcesses.put(processName, processes.get(processName).copy()));
            return sortedProcesses;
        }
    },

    LongestFirst{
        @Override
        public LinkedHashMap<String, ProcessTerm> copyAndSort(Node.ConcreteNode node) {
            var sortedProcesses = new LinkedHashMap<String, ProcessTerm>();
            var processList = new ArrayList<>(node.network.processes.entrySet());
            processList.sort(new SortByLength());
            Collections.reverse(processList);

            processList.forEach((entry) -> sortedProcesses.put(entry.getKey(), entry.getValue()));
            return sortedProcesses;
        }
    },
    ShortestFirst{
        @Override
        public LinkedHashMap<String, ProcessTerm> copyAndSort(Node.ConcreteNode node) {
            var sortedProcesses = new LinkedHashMap<String, ProcessTerm>();
            var processList = new ArrayList<>(node.network.processes.entrySet());
            processList.sort(new SortByLength());

            processList.forEach((entry) -> sortedProcesses.put(entry.getKey(), entry.getValue()));
            return sortedProcesses;
        }
    },

    UnmarkedThenRandom{
        @Override
        public LinkedHashMap<String, ProcessTerm> copyAndSort(Node.ConcreteNode node) {
            var processes = node.network.processes;
            var sortedProcesses = new LinkedHashMap<String, ProcessTerm>();

            var markedList = new ArrayList<String>();
            var unmarkedList = new ArrayList<String>();

            node.marking.forEach((processName, marked) -> {
                if (marked)
                    markedList.add(processName);
                else
                    unmarkedList.add(processName);
            });

            Collections.shuffle(unmarkedList);
            Collections.shuffle(markedList);

            unmarkedList.forEach(processName -> sortedProcesses.put(processName, processes.get(processName).copy()));
            markedList.forEach(processName -> sortedProcesses.put(processName, processes.get(processName).copy()));

            return sortedProcesses;
        }
    },

    UnmarkedThenSelections {
        @Override
        public LinkedHashMap<String, ProcessTerm> copyAndSort(Node.ConcreteNode node) {
            var processes = node.network.processes;
            var sortedProcesses = new LinkedHashMap<String, ProcessTerm>();

            var markedSelections = new ArrayList<String>();
            var unmarkedSelections = new ArrayList<String>();
            var markedSending = new ArrayList<String>();
            var unmarkedSending = new ArrayList<String>();
            var markedElse = new ArrayList<String>();
            var unmarkedElse = new ArrayList<String>();

            node.marking.forEach((processName, marked) -> {
                switch (node.network.processes.get(processName).main().action){
                    case SELECTION:
                    case OFFERING:
                        if (marked)
                            markedSelections.add(processName);
                        else
                            unmarkedSelections.add(processName);
                    case SEND:
                    case RECEIVE:
                        if (marked)
                            markedSending.add(processName);
                        else
                            unmarkedSending.add(processName);
                    default:
                        if (marked)
                            markedElse.add(processName);
                        else
                            unmarkedElse.add(processName);
                }
            });

            copyProcesses(unmarkedSelections, processes, sortedProcesses);
            copyProcesses(unmarkedSending, processes, sortedProcesses);
            copyProcesses(unmarkedElse, processes, sortedProcesses);
            copyProcesses(markedSelections, processes, sortedProcesses);
            copyProcesses(markedSending, processes, sortedProcesses);
            copyProcesses(markedElse, processes, sortedProcesses);

            return sortedProcesses;
        }
    },

    UnmarkedThenConditions {
        @Override
        public LinkedHashMap<String, ProcessTerm> copyAndSort(Node.ConcreteNode node) {
            var processes = node.network.processes;
            var sortedProcesses = new LinkedHashMap<String, ProcessTerm>();

            var markedConditions = new ArrayList<String>();
            var unmarkedConditions = new ArrayList<String>();
            var markedSelections = new ArrayList<String>();
            var unmarkedSelections = new ArrayList<String>();
            var markedElse = new ArrayList<String>();
            var unmarkedElse = new ArrayList<String>();

            node.marking.forEach((processName, marked) -> {
                switch (node.network.processes.get(processName).main().action){
                    case CONDITION:
                        if (marked)
                            markedConditions.add(processName);
                        else
                            unmarkedConditions.add(processName);
                    case SELECTION:
                    case OFFERING:
                        if (marked)
                            markedSelections.add(processName);
                        else
                            unmarkedSelections.add(processName);
                    default:
                        if (marked)
                            markedElse.add(processName);
                        else
                            unmarkedElse.add(processName);
                }
            });

            copyProcesses(unmarkedConditions, processes, sortedProcesses);
            copyProcesses(unmarkedSelections, processes, sortedProcesses);
            copyProcesses(unmarkedElse, processes, sortedProcesses);
            copyProcesses(markedConditions, processes, sortedProcesses);
            copyProcesses(markedSelections, processes, sortedProcesses);
            copyProcesses(markedElse, processes, sortedProcesses);

            return sortedProcesses;
        }
    };

    private static void copyProcesses(List<String> processNames, HashMap<String, ProcessTerm> processMap, HashMap<String, ProcessTerm> copyMap){
        processNames.forEach(processName -> copyMap.put(processName, processMap.get(processName).copy()));
    }

    private static class SortByLength implements Comparator<Map.Entry<String, ProcessTerm>>{
        @Override
        public int compare(Map.Entry<String, ProcessTerm> entry1, Map.Entry<String, ProcessTerm> entry2) {
            String term1 = entry1.getValue().main().toString();
            String term2 = entry2.getValue().main().toString();
            return term1.length() - term2.length();
        }
    }

    public abstract LinkedHashMap<String, ProcessTerm> copyAndSort(Node.ConcreteNode node);
}