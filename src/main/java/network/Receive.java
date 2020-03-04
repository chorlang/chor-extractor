package network;

/**
 * Behavior for receiving information from another process.
 *
 * Note that the "process" variable from the kotlin implementation is "sender"
 */
public class Receive implements Behaviour {
    String sender;
    Behaviour continuation;

    /**
     * Constructs a Behavior for receiving messages
     * @param sender Name of the process to receive from
     * @param continuation The behavior to perform after receiving message
     */
    public Receive(String sender, Behaviour continuation){
        this.sender = sender;
        this.continuation = continuation;
    }

    public String toString(){
        return String.format("%s?; %s", sender, continuation);
    }

    public Receive copy(){
        return new Receive(sender, continuation.copy());
    }

    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (other.getAction() != Action.receive)
            return false;
        Receive otherR = (Receive)other;
        return sender.equals(otherR.sender) && continuation.equals(otherR.continuation);
    }

    public int hashCode(){
        int hash = continuation.hashCode() * 31;
        hash += sender.hashCode();
        return hash;
    }

    public Action getAction(){
        return Action.receive;
    }
}
