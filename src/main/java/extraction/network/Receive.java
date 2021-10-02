package extraction.network;

/**
 * Behavior for receiving information from another process.
 *
 * Note that the "process" variable from the kotlin implementation is "sender"
 */
public class Receive extends Behaviour.Receiver {

    /**
     * Constructs a Behavior for receiving messages
     * @param sender Name of the process to receive from
     * @param continuation The behavior to perform after receiving message
     */
    public Receive(String sender, Behaviour continuation){
        super(Action.RECEIVE, continuation, sender);
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
        int hash = continuation.hashCode() * 31;
        hash += sender.hashCode();
        return hash;
    }
}
