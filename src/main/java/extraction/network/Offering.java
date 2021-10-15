package extraction.network;

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
    public Offering(String sender, Map<String, Behaviour> branches){
        super(Action.OFFERING, null, sender); //Not sure what to do about continuation here
        this.branches = Collections.unmodifiableMap(branches);
        hash = hashValue();
    }

    @Override
    Behaviour realValues(HashMap<String, String> substitutions) {
        return new Offering(substitutions.get(sender), branches);
    }

    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s&{", sender));
        branches.forEach((label, branch) ->
                builder.append(
                        String.format("%s: %s, ", label, branch)
                ));
        builder.delete(builder.length()-2, builder.length());
        builder.append("}");
        return builder.toString();
    }

    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (other.action != Action.OFFERING)
            return false;
        Offering otherOffer = (Offering)other;
        if (!sender.equals(otherOffer.sender))
            return false;
        for (String label : branches.keySet()){
            var thisOption = branches.get(label);
            var otherOption = otherOffer.branches.get(label);
            if (label == null || !thisOption.equals(otherOption))
                return false;
        }
        return true;
        //return branches.equals(otherOffer.branches);
    }

    public int hashCode(){
        return hash;
    }
    private int hashValue(){
        int hash = sender.hashCode() * 31;
        hash += branches.hashCode();
        return hash;
    }

    public Action getAction(){
        return Action.OFFERING;
    }
}
