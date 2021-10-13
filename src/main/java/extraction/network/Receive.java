package extraction.network;

import java.util.HashMap;

/**
 * Behavior for receiving information from another process.
 *
 * Note that the "process" variable from the kotlin implementation is "sender"
 */
public class Receive extends Behaviour.Receiver {
    private final int hash;
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

    public String toString(){
        return String.format("%s?; %s", sender, continuation);
    }

    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (other.action != Action.RECEIVE)
            return false;
        Receive otherR = (Receive)other;
        return sender.equals(otherR.sender) && continuation.equals(otherR.continuation);
    }

    public int hashCode(){
        return hash;
    }
    private int hashValue(){
        int hash = continuation.hashCode() * 31;
        hash += sender.hashCode();
        return hash;
    }
}
