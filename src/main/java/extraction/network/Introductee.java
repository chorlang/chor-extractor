package extraction.network;

import extraction.Label;

/**
 * Behaviour for being informed by another process of the existence of a third process.
 */
public class Introductee extends Behaviour.Receiver {
    public final String processID;

    /**
     * Constructs a Introductee behaviour, which represents this process being told by
     * another process about the existence of a third process, so that this process and
     * the third process may later communicate.
     * @param sender An already acquainted process, informing of the existence of another process.
     * @param processID The name of the new process
     * @param continuation The behaviour to continue as.
     */
    public Introductee(String sender, String processID, Behaviour continuation){
        super(Action.INTRODUCTEE, continuation, sender);
        this.processID = processID;
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
        if (!(other instanceof Introductee introductee))
            return false;
        return sender.equals(introductee.sender) &&
                processID.equals(introductee.processID) &&
                continuation.equals(introductee.continuation);

    }

    @Override
    public String toString() {
        return String.format("%s?%s; %s", sender, processID, continuation);
    }
}
