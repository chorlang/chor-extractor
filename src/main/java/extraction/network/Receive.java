package extraction.network;

import java.util.HashMap;

/**
 * Behavior for receiving information from another process.
 *
 * Note that the "process" variable from the kotlin implementation is "sender"
 */
public class Receive extends Behaviour.Receiver {
    private int hash;
    /**
     * Constructs a Behavior for receiving messages
     * @param sender Name of the process to receive from
     * @param continuation The behavior to perform after receiving message
     */
    public Receive(String sender, Behaviour continuation){
        super(Action.RECEIVE, continuation, sender);
        hash = hashValue();
    }

    @Override
    Behaviour realValues(HashMap<String, String> substitutions) {
        return new Receive(substitutions.get(sender), continuation);
    }

    @Override
    public String toString(){
        return String.format("%s?; %s", sender, continuation);
    }

    @Override
    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (!(other instanceof Receive otherR))
            return false;
        return sender.equals(otherR.sender) && continuation.equals(otherR.continuation);
    }

    @Override
    boolean compareData(Behaviour other){
        return other instanceof Receive rec && sender.equals(((Receive) other).sender);
    }

    @Override
    public int hashCode(){
        return hash;
    }

    private int hashValue(){
        return sender.hashCode() ^ Integer.rotateRight(continuation.hashCode(), 1);
    }
}
