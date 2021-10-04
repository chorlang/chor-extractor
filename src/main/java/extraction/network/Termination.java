package extraction.network;

import java.util.HashMap;

/**
 * Behavior representing termination of a process
 *
 * Termination always means the same. While it is not possible to
 * make the constructor return a reference to the same object,
 * I can make a method that returns the first instance
 */
public class Termination extends Behaviour {
    public static final Termination instance = new Termination();
    //The String representation in networks:
    static final String terminationTerm = "stop";

    @Override
    Behaviour realValues(HashMap<String, String> substitutions){
        return this;
    }

    /**
     * Constructs a new Termination. Since all Termination instances
     * are identical, it is better to reuse one instance by calling
     * Termination.getTermination() instead, as it reduces resource usage.
     */
    private Termination(){
        super(Action.TERMINATION);
    }

    public String toString(){
        return terminationTerm;
    }

    public boolean equals(Behaviour other){
        return other.action == Action.TERMINATION;
    }

    public int hashCode(){
        return 1;
    }
}
