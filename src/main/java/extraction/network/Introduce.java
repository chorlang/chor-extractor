package extraction.network;

import extraction.Label;

import java.util.HashMap;
import java.util.Map;

/**
 * Behaviour for making to (distinct other) processes acquainted (aware of each other)
 *
 * Although logic wise it doesn't matter which of the processes to acquaint are in fields
 * process1 and process2, hashCode() and equals(Behaviour other) produces different results
 * if the ordering is swapped.
 */
public class Introduce extends Behaviour.Sender{
    public final String process1, process2; //Same as expression and receiver from superclass
    private final int hash;

    /**
     * Creates a new Introduce behaviour, which represents introducing two processes (not itself)
     * to each other, so that they may communicate. Requires the acquainting process to already
     * be acquainted with the two acquaintees.
     * @param process1 Name of process to be introduced to process2.
     * @param process2 Name of process to be introduced to process1
     * @param continuation The behaviour to continue as after the interaction.
     */
    public Introduce(String process1, String process2, Behaviour continuation){
        super(Action.INTRODUCE, continuation, process1, process2);
        this.process1 = process1;
        this.process2 = process2;
        hash = hashValue();
    }

    @Override
    Behaviour realValues(HashMap<String, String> substitutions) {
        return new Introduce(substitutions.get(process1), substitutions.get(process2), continuation);
    }

    @Override
    public Label.InteractionLabel labelFrom(String process, Map<String, String> sub){
        return new Label.IntroductionLabel(process, sub.get(process1), sub.get(process2));
    }

    @Override
    public int hashCode() {
        return hash;
    }
    private int hashValue(){
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
        if (!(other instanceof Introduce introduce))
            return false;
        return process1.equals(introduce.process1) &&
                process2.equals(introduce.process2) &&
                continuation.equals(introduce.continuation);
    }

    @Override
    public String toString() {
        return String.format("%s<->%s; %s",process1, process2, continuation);
    }
}
