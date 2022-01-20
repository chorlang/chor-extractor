package extraction.network;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;

import java.util.HashMap;
import java.util.Map;

/**
 * This Behavior expects to receive a label and branch/switch behavior depending upon
 * the received label.
 *
 * The "process" variable from the Kotlin implementation is equal to "sender"
 */
public class Offering extends Behaviour.Receiver {
    /**
     * Branches are unmodifiable.
     */
    public final Map<String, Behaviour> branches;
    private final int hash;

    /**
     * Creates an Offering object, aka receive a label, and switch on that label.
     * @param sender The process that the label is send from.
     * @param branches Map from labels (Strings) to branches (Behaviors)
     */
    public Offering(String sender, Map<String, Behaviour> branches, @NotNull Behaviour continuation){
        super(Action.OFFERING, continuation, sender);
        this.branches = Collections.unmodifiableMap(branches);
        hash = hashValue();
    }
    public Offering(String sender, Map<String, Behaviour> branches){
        this(sender, branches, NoneBehaviour.instance);
    }

    @Override
    Behaviour realValues(HashMap<String, String> substitutions) {
        return new Offering(substitutions.get(sender), branches, continuation);
    }

    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s&{", sender));
        branches.forEach((label, branch) ->
                builder.append(
                        String.format("%s: %s, ", label, branch)
                ));
        builder.delete(builder.length()-2, builder.length());
        builder.append("}");
        if (!(continuation instanceof BreakBehaviour))
            builder.append("; %s".formatted(continuation));
        return builder.toString();
    }

    @Override
    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (!(other instanceof Offering otherOffer))
            return false;
        if (!sender.equals(otherOffer.sender))
            return false;
        for (String label : branches.keySet()){
            var thisOption = branches.get(label);
            var otherOption = otherOffer.branches.get(label);
            if (label == null || !thisOption.equals(otherOption))
                return false;
        }
        return continuation.equals(other.continuation);
    }

    @Override
    public int hashCode(){
        return hash;
    }

    private int hashValue(){
        int hash = sender.hashCode() * 31;
        hash += branches.hashCode();
        return hash ^ Integer.rotateRight(continuation.hashCode(), 1);
    }
}
