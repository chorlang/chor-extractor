package extraction.network;

import extraction.Label;

import java.util.HashMap;

/**
 * Behaviour for being informed by another process of the existence of a third process.
 */
public class Introductee extends Behaviour.Receiver {
    public final String processID;
    private final int hash;

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
        hash = hashValue();
    }

    @Override
    Behaviour realValues(HashMap<String, String> substitutions) {
        return new Introductee(substitutions.get(sender), processID, continuation);
    }

    @Override
    public int hashCode() {
        return hash;
    }
    private int hashValue(){
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
