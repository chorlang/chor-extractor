package extraction.network;

import extraction.Label;

import java.util.HashMap;

/**
 * Behaviour for making to (distinct other) processes acquainted (aware of each other)
 *
 * Although logic wise it doesn't matter which of the processes to acquaint are in fields
 * process1 and process2, hashCode() and equals(Behaviour other) produces different results
 * if the ordering is swapped.
 */
public class Introduce extends Behaviour.Sender{
    public final String leftReceiver, rightReceiver; //Same as expression and receiver from superclass
    private final int hash;

    /**
     * Creates a new Introduce behaviour, which represents introducing two processes (not itself)
     * to each other, so that they may communicate. Requires the acquainting process to already
     * be acquainted with the two acquaintees.
     * @param leftReceiver Name of process to be introduced to rightReceiver.
     * @param rightReceiver Name of process to be introduced to leftReceiver
     * @param continuation The behaviour to continue as after the interaction.
     */
    public Introduce(String leftReceiver, String rightReceiver, Behaviour continuation){
        super(Action.INTRODUCE, continuation, rightReceiver, leftReceiver);
        this.leftReceiver = leftReceiver;
        this.rightReceiver = rightReceiver;
        hash = hashValue();
    }

    @Override
    Behaviour realValues(HashMap<String, String> substitutions) {
        return new Introduce(substitutions.get(leftReceiver), substitutions.get(rightReceiver), continuation);
    }

    @Override
    public Label.InteractionLabel labelFrom(String process, ProcessTerm.ValueMap sub){
        return new Label.IntroductionLabel(process, sub.get(leftReceiver), sub.get(rightReceiver));
    }

    @Override
    public int hashCode() {
        return hash;
    }

    private int hashValue(){
        int hash = leftReceiver.hashCode() * 31;
        hash += rightReceiver.hashCode();
        return hash ^ Integer.rotateRight(continuation.hashCode(), 1);
    }

    @Override
    public boolean equals(Behaviour other) {
        if (this == other)
            return true;
        if (!(other instanceof Introduce introduce))
            return false;
        return leftReceiver.equals(introduce.leftReceiver) &&
                rightReceiver.equals(introduce.rightReceiver) &&
                continuation.equals(introduce.continuation);
    }

    @Override
    boolean compareData(Behaviour other){
        return other instanceof Introduce intr && leftReceiver.equals(intr.leftReceiver) &&
                rightReceiver.equals(intr.rightReceiver);
    }

    @Override
    public String toString() {
        return String.format("%s<->%s; %s", leftReceiver, rightReceiver, continuation);
    }
}
