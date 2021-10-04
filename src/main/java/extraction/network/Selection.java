package extraction.network;

import extraction.Label;

import java.util.HashMap;
import java.util.Map;

/**
 * This behavior represents sending a label to a process
 *
 * Note that the variable "process" in the original implementation is "receiver"
 */
public class Selection extends Behaviour.Sender {
    public final String label; //Same as expression in superclass

    /**
     * Constructor for the Selection behavior, aka sending a label to another process.
     * @param receiver The name of the process to send the label to.
     * @param label The label t send.
     * @param continuation The behavior to continue as after sending.
     */
    public Selection(String receiver, String label, Behaviour continuation){
        super(Action.SELECTION, continuation, receiver, label);
        this.label = label;
    }

    @Override
    Behaviour realValues(HashMap<String, String> substitutions) {
        return new Selection(substitutions.get(receiver), label, continuation);
    }

    @Override
    public Label.InteractionLabel labelFrom(String process, Map<String, String> sub){
        return new Label.SelectionLabel(process, sub.get(receiver), sub.get(label));
    }

    public String toString(){
        return String.format("%s + %s; %s", receiver, label, continuation);
    }

    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (other.action != Action.SELECTION)
            return false;
        Selection otherSelect = (Selection)other;
        return receiver.equals(otherSelect.receiver) &&
                label.equals(otherSelect.label) &&
                continuation.equals(otherSelect.continuation);
    }

    public int hashCode(){
        int hash = continuation.hashCode() * 31;
        hash += receiver.hashCode();
        hash *= 31;
        hash += label.hashCode();
        return hash;
    }
}