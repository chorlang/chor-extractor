package extraction.network.utils;

import extraction.network.*;

import java.util.HashMap;

public class NetworkPurger {
    /**
     * Removes processes from the network which do not have any useful actions. Specifically, those which simply
     * terminate before performing any useful work.
     * @param network The network to purge for useless processes.
     */
    public static void purgeNetwork(Network network){
        /*
        Creates a new HashMap, then for each entry in the Networks processes, add it to the new map if it does not
        immediately terminate, or is a procedure invocation that immediately terminates.
        In other words, copies all entries over to the new map, but excludes those that would terminate before doing
        anything else first.
         */
        var map = new HashMap<String, ProcessTerm>();
        network.processes.forEach((processName, processTerm) -> {
            boolean exclude;
            if (processTerm.main() instanceof Termination)
                exclude = true;
            else if (processTerm.main() instanceof ProcedureInvocation invocation)
                //Should this not unfold procedure invocations recursively?
                exclude = processTerm.procedures.get(invocation.procedure) instanceof Termination;
            else
                exclude = false;
            if (!exclude)
                map.put(processName, processTerm);
        });

        /*
        If all processes have been excluded, then the Network might just do nothing (per design perhaps).
        In that case, one process is added anyway so extraction can proceed. (A network that does nothing
        can still be extracted).
        */
        if (map.isEmpty()) {
            var processName = network.processes.keySet().iterator().next();
            map.put(processName, network.processes.get(processName));
        }

        network.processes = map;
    }
}
