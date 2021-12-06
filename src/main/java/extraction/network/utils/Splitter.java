package extraction.network.utils;

import extraction.network.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Splitter {
    private static ProcessInteractionChecker interactionChecker = new ProcessInteractionChecker();
    /**
     * This function splits a Network into a set of Networks whose processes can function independently on each other.
     * If for two processes in the original Network, a and b, a never interacts with b, that be directly or
     * indirectly though another process, then a and b will be placed in different Networks.
     * Please note the resulting Networks reference objects of the original Network, and therefore the Network
     * parsed as parameter to this function should <u>NOT</u> be modified after this method call.
     * @param network The network to split. Do not modify this network after calling this function.
     * @return A set of Networks that whose processes function independently of each other.
     */
    public static HashSet<Network> splitNetwork(Network network){
        var processSetList = getProcessSets(network);
        if (processSetList == null)
            return null;

        var networks = new HashSet<Network>();
        for (var processSet : processSetList){
            var processes = new HashMap<String, ProcessTerm>();
            processSet.forEach(processName -> processes.put(processName, network.processes.get(processName)));
            networks.add(new Network(processes));
        }
        return networks;
    }

    /**
     * Builds a list of sets where each set contains the names of processes that interact within the same set.
     * In other words, if for two processes a and b, a can execute without b, then a and b are in different sets.
     * @param network The network to extract interacting process sets from.
     * @return List of sets of the names of processes that depend on each other.
     */
        private static ArrayList<HashSet<String>> getProcessSets(Network network){
        //Build a map, mapping each process to a set of processes it directly interacts with.
        var map = new HashMap<String, HashSet<String>>();
        network.processes.forEach((processName, processTerm) ->
                map.put(processName, new HashSet<>(
                        interactionChecker.Visit(processTerm).stream().filter(  //Create the set
                                name -> network.processes.containsKey(name)     //Filter out variables
                        ).toList()
                ))
        );

        var setList = new ArrayList<HashSet<String>>();
        var unprocessed = new HashSet<>(network.processes.keySet());

        //While there are processes which have not yet been grouped in a set.
        while (!unprocessed.isEmpty()) {
            //interactingProcesses is the set to be added to the list being returned at the end.
            var interactingProcesses = new HashSet<String>();

            //Build a set, reachable, of the names of all processes that can be reached from
            //a yet unprocessed process, (including itself in the set).
            var unprocessedProcessName = unprocessed.iterator().next();
            var reachable = map.get(unprocessedProcessName);
            reachable.add(unprocessedProcessName);

            //While the set of processes that can be reached contains elements not in the set of interacting processes.
            while (!reachable.equals(interactingProcesses)) {
                //subtraction is all the sets still not added to interactingProcesses.
                var subtraction = new HashSet<>(reachable);
                subtraction.removeAll(interactingProcesses);
                for (String otherProcessName : subtraction) {
                    if (map.get(otherProcessName) == null)
                        return null;
                    interactingProcesses.add(otherProcessName);
                    //When we added a process to a set, we do no longer need to consider it in its own set for the future.
                    unprocessed.remove(otherProcessName);
                    //Expand the set of reachable processes to include those that can otherProcess interacts directly with.
                    reachable.addAll(map.get(otherProcessName));
                }
            }
            setList.add(interactingProcesses);
        }
        return setList;
    }

}
