package utility.ChorGen;

/*
 * Selection actions, with a continuation.
 */
public class SelectionNode implements ChoreographyNode {

    private String sender, receiver, label;
    private ChoreographyNode nextAction;

    public SelectionNode(String sender, String receiver, String label, ChoreographyNode nextAction) {
        this.sender = sender;
        this.receiver = receiver;
        this.label = label;
        this.nextAction = nextAction;
    }

    /*
     * Getters (and no setters).
     */
    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }
    public String getLabel() {
        return label;
    }

    public ChoreographyNode getNextAction() {
        return nextAction;
    }

    /*
     * For implementing the visitor pattern.
     */
    public void accept(CNVisitor v) {
        v.visit(this);
    }

    public String toString() {
        return sender + " -> " + receiver + "[" + label + "]; " + nextAction.toString();
    }

}
