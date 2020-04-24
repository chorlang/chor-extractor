package extraction.network;

import java.util.HashMap;

/**
 * This Behavior expects to receive a label and branch/switch behavior depending upon
 * the received label.
 *
 * The "process" variable from the Kotlin implementation is equal to "sender"
 */
public class Offering extends Behaviour {
    public final String sender;
    public final HashMap<String, Behaviour> branches;

    /**
     * Creates an Offering object, aka receive a label, and switch on that label.
     * @param sender The process that the label is send from.
     * @param branches Map from labels (Strings) to branches (Behaviors)
     */
    public Offering(String sender, HashMap<String, Behaviour> branches){
        this.sender = sender;
        this.branches = branches;
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

    public Offering copy(){
        /*var branchesCopy = new HashMap<String, Behaviour>(branches.size());
        branches.forEach((key, value) ->
                branchesCopy.put(key, value.copy()));
        return new Offering(sender, branchesCopy);*/
        return this;
    }

    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (other.getAction() != Action.OFFERING)
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
        int hash = sender.hashCode() * 31;
        hash += branches.hashCode();
        return hash;
    }

    public Action getAction(){
        return Action.OFFERING;
    }
}
