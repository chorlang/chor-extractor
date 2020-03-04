package network;

/**
 * Behavior representing termination of a process
 *
 * Termination always means the same. While it is not possible to
 * make the constructor return a reference to the same object,
 * I can make a method that returns the first instance
 */
public class Termination implements Behaviour {
    static Termination instance = new Termination();
    //The String representation in networks:
    static String terminationTerm = "stop";

    /**
     * Constructs a new Termination. Since all Termination instances
     * are identical, it is better to reuse one instance by calling
     * Termination.getTermination() instead, as it reduces resource usage.
     */
    public Termination(){}

    /**
     * Gets an already existing instance of Termination.
     * Preferably use this rather than the constructor.
     * @return An existing Termination instance
     */
    public static Termination getTermination(){
        return instance;
    }

    public String toString(){
        return terminationTerm;
    }

    public Termination copy(){
        return getTermination();
    }

    public boolean equals(Behaviour other){
        return other.getAction() == Action.termination;
    }

    public int hashCode(){
        return terminationTerm.hashCode();  //Just so it is something
    }

    public Action getAction(){
        return Action.termination;
    }
}
