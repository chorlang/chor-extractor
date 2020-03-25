package network;

import java.util.HashMap;
import java.util.TreeMap;

public class Network extends Behaviour {
    public HashMap<String, ProcessTerm> processes;     //Map from process names to procedures

    /**
     * A Network object stores a mapping from process names to process terms (procedures).
     * @param processes HashMap&lt;String, ProcessTerm&gt; where the String is the process name
     */
    public Network(HashMap<String, ProcessTerm> processes){
        this.processes = processes;
    }

    /**
     * Creates a string of all the process names to process terms mappings
     * @return String of the stored HashMap
     */
    public String toString(){
        StringBuilder builder = new StringBuilder();
        processes.forEach((processName, procedure) ->
                builder.append(processName).append(procedure.toString()).append("|"));
        //I don't know why the last 3 chars are removed, but the Kotlin impl does it
        if (builder.length() >= 3){
            builder.delete(builder.length() - 1, builder.length());
        }
        return builder.toString();
    }

    /**
     * Creates a deep copy of this object
     * @return An identical Network
     */
    public Network copy(){
        HashMap<String, ProcessTerm> processesCopy = new HashMap<>(processes.size());
        processes.forEach((key, value) -> processesCopy.put(key, value.copy()));
        return new Network(processesCopy);
    }

    /**
     * Compares this Networks mapping with another Networks mapping
     * @param other Network to compare to
     * @return true, if all mappings are the same, and no map has an entry the other does not
     */
    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (other.getAction() != Action.NETWORK)
            return false;
        Network otherNetwork = (Network) other;
        if (processes.size() != otherNetwork.processes.size())
            return false;

        for (var processName : processes.keySet()){
            var otherProcess = otherNetwork.processes.get(processName);
            if (otherProcess == null || !otherProcess.equals(processes.get(processName)))
                return false;
        }
        return true;
        //return processes.equals(otherNetwork.processes);
    }

    /**
     * Calculates a hashcode from the process names and process terms.
     * @return Hash of this network mapping
     */
    public int hashCode(){
        /*Why do the ordering matter?*/

        //So, variables cannot be assigned in lambda expressions, but array items can!???
        int[] lambdaWorkaround = new int[]{0};

        //Annoying, but its the easiest way to sort HashMaps.
        TreeMap<String, ProcessTerm> sortedMap = new TreeMap<>(processes);
        //forEach is performed in order of entry set iteration, which I believe is sorted
        sortedMap.forEach((key, value) ->
                lambdaWorkaround[0] += (key.hashCode() * 31 + (value.hashCode() * 29)));
        return lambdaWorkaround[0];
    }

    public Action getAction() {
        return Action.NETWORK;
    }
}
