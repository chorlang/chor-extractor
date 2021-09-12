package extraction.network;

/**
 * Behaviour for being informed by another process of the existence of a third process.
 */
public class Familiarize extends Behaviour{
    public final String sender, processID;
    public final Behaviour continuation;

    /**
     * Constructs a Familiarize behaviour, which represents this process being told by
     * another process about the existence of a third process, so that this process and
     * the third process may later communicate.
     * @param sender An already acquainted process, informing of the existence of another process.
     * @param processID The name of the new process
     * @param continuation The behaviour to continue as.
     */
    public Familiarize(String sender, String processID, Behaviour continuation){
        this.sender = sender;
        this.processID = processID;
        this.continuation = continuation;
    }
    @Override
    public Action getAction() {
        return Action.FAMILIARIZE;
    }

    @Override
    public Behaviour copy() {
        return this;
    }

    @Override
    public int hashCode() {
        int hash = continuation.hashCode() * 31;
        hash += sender.hashCode();
        hash *= 31;
        hash += processID.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Behaviour other) {
        if (this == other)
            return true;
        if (!(other instanceof Familiarize familiarize))
            return false;
        return sender.equals(familiarize.sender) &&
                processID.equals(familiarize.processID) &&
                continuation.equals(familiarize.continuation);

    }

    @Override
    public String toString() {
        return String.format("%s?%s; %s", sender, processID, continuation);
    }
}
