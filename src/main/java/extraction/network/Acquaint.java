package extraction.network;

/**
 * Behaviour for making to (distinct other) processes acquainted (aware of each other)
 *
 * Although logic wise it doesn't matter which of the processes to acquaint are in fields
 * process1 and process2, hashCode() and equals(Behaviour other) produces different results
 * if the ordering is swapped.
 */
public class Acquaint extends Behaviour{
    public final String process1, process2;
    public final Behaviour continuation;

    /**
     * Creates a new Acquaint behaviour, which represents introducing two processes (not itself)
     * to each other, so that they may communicate. Requires the acquainting process to already
     * be acquainted with the two acquaintees.
     * @param process1 Name of process to be introduced to process2.
     * @param process2 Name of process to be introduced to process1
     * @param continuation The behaviour to continue as after the interaction.
     */
    public Acquaint(String process1, String process2, Behaviour continuation){
        this.process1 = process1;
        this.process2 = process2;
        this.continuation = continuation;
    }

    @Override
    public Action getAction() {
        return Action.ACQUAINT;
    }

    @Override
    public Behaviour copy() {
        return this;
    }

    @Override
    public int hashCode() {
        int hash = continuation.hashCode() * 31;
        hash += process1.hashCode();
        hash *= 31;
        hash += process2.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Behaviour other) {
        if (this == other)
            return true;
        if (!(other instanceof Acquaint acquaint))
            return false;
        return process1.equals(acquaint.process1) &&
                process2.equals(acquaint.process2) &&
                continuation.equals(acquaint.continuation);
    }

    @Override
    public String toString() {
        return String.format("%s<->%s; %s",process1, process2, continuation);
    }
}
