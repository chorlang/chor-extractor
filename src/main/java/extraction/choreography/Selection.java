package extraction.choreography;

public class Selection extends ChoreographyBody.Interaction {
    public String sender, receiver, label;

    public ChoreographyBody continuation;

    public Type chorType = Type.SELECTION;

    public Selection(String sender, String receiver, String label, ChoreographyBody continuation){
        this.sender = sender;
        this.receiver = receiver;
        this.label = label;
        this.continuation = continuation;
    }
    public Selection(String sender, String receiver, String label){
        this(sender, receiver, label, null);
    }

    @Override
    public String toString() {
        return String.format("%s->%s[%s]", sender, receiver, label);
    }
}
