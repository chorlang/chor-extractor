package extraction.network;

import java.util.HashMap;

public class ProcessTerm extends Behaviour {
    public final HashMap<String, Behaviour> procedures;   //Map from procedure names to their behaviours
    public Behaviour main;                                //The main behaviour for the procedure
    //main cannot be static as it is modified

    /**
     * Constructor for ProcessTerm
     * @param procedures A HashMap&lt;String, Behavior&gt; from the procedure name as string, to the Behaviour of that procedure
     * @param main The main Behaviour for this procedure
     */
    public ProcessTerm(HashMap<String, Behaviour> procedures, Behaviour main){
        this.procedures = procedures;
        this.main = main;
    }

    /**
     * Converts the procedures into a human readable format
     * @return string representing this mapping
     */
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        procedures.forEach((key, value) ->
                builder.append(String.format("def %s{%s} ", key, value)));
        builder.append(String.format("main {%s}}", main));
        return builder.toString();
    }

    /**
     * Makes a deep copy of this object
     * @return copy of this object instance
     */
    public ProcessTerm copy(){
        var proceduresCopy = new HashMap<String, Behaviour>(procedures.size());
        procedures.forEach((key, value) ->
                proceduresCopy.put(key, value.copy()));
        return new ProcessTerm(proceduresCopy, main.copy());
        //return this;
    }

    /**
     * Compares this object with another object for equality.
     * @param other the object to compare with
     * @return true if both objects are functionally identical
     */
    public boolean equals(Behaviour other){
        if (this == other)          //If it is the same object
            return true;
        if (other.getAction() != Action.PROCESS_TERM)
            return false;
        ProcessTerm otherProcessTerm = (ProcessTerm) other;
        if (!main.equals(otherProcessTerm.main))     //The main Behaviours must be identical
            return false;
        for (String procedureName : procedures.keySet()){
            var otherBehaviour = otherProcessTerm.procedures.get(procedureName);
            if (otherBehaviour == null || !otherBehaviour.equals(procedures.get(procedureName)))
                return false;
        }
        return true;
        //return procedures.equals(otherProcessTerm.procedures); //They are equal if the mapping is equal
    }

    /**
     * Calculates the hashcode for this ProcessTerm.
     * The hash is calculated from the mapping, as well as the main behaviour
     * @return the hashvalue considering all behaviours
     */
    public int hashCode(){
        int hash = procedures.hashCode();
        return 31 * hash + main.hashCode();
    }

    public Action getAction(){
        return Action.PROCESS_TERM;
    }
}
