package network;

import java.util.HashMap;

/**
 * This Behavior expects to receive a label and branch/switch behavior depending upon
 * the received label.
 *
 * The "process" variable from the Kotlin implementation is equal to "sender"
 */
public class Offering implements Behaviour {
    String sender;
    HashMap<String, Behaviour> branches;

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
        HashMap<String, Behaviour> branchesCopy = new HashMap<>(branches.size());
        branches.forEach((key, value) ->
                branchesCopy.put(key, value.copy()));
        return new Offering(sender, branchesCopy);
    }

    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (other.getAction() != Action.Offering)
            return false;
        Offering otherO = (Offering)other;
        if (!sender.equals(otherO.sender))
            return false;
        return branches.equals(otherO.branches);
    }

    public int hashCode(){
        int hash = sender.hashCode() * 31;
        hash += branches.hashCode();
        return hash;
    }

    public Action getAction(){
        return Action.Offering;
    }
}
