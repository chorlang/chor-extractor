package utility.ChorGen;

/*
 * Communication actions, with a continuation.
 */
public class CommunicationNode implements ChoreographyNode {

    private String sender, receiver, message;
    private ChoreographyNode nextAction;

    public CommunicationNode(String sender, String receiver, String message, ChoreographyNode nextAction) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
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
    public String getMessage() {
        return message;
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
        return sender + "." + message + " -> " + receiver + "; " + nextAction.toString();
    }

}
