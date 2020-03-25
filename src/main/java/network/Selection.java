package network;

/**
 * This behavior represents sending a label to a process
 *
 * Note that the variable "process" in the original implementation is "receiver"
 */
public class Selection implements Behaviour {
    public String receiver, label;
    public Behaviour continuation;

    /**
     * Constructor for the Selection behavior, aka sending a label to another process.
     * @param receiver The name of the process to send the label to.
     * @param label The label t send.
     * @param continuation The behavior to continue as after sending.
     */
    public Selection(String receiver, String label, Behaviour continuation){
        this.continuation = continuation;
        this.receiver = receiver;
        this.label = label;
    }

    public String toString(){
        return String.format("%s + %s; %s", receiver, label, continuation);
    }

    public Selection copy(){
        return new Selection(receiver, label, continuation.copy());
    }

    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (other.getAction() != Action.SELECTION)
            return false;
        Selection otherSelect = (Selection)other;
        return receiver.equals(otherSelect.receiver) &&
                label.equals(otherSelect.label) &&
                continuation.equals(otherSelect.continuation);
    }

    public int hashCode(){
        int hash = continuation.hashCode() * 31;
        hash += receiver.hashCode();
        hash *= 31;
        hash += label.hashCode();
        return hash;
    }

    public Action getAction(){
        return Action.SELECTION;
    }
}