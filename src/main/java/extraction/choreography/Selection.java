package extraction.choreography;

public class Selection extends ChoreographyBody.Interaction {
    public String sender, receiver, label;

    public ChoreographyBody continuation;

    public ChoreographyBody getContinuation(){
        return continuation;
    }
    public void setContinuation(ChoreographyBody continuation) {this.continuation = continuation;}


    private final Type chorType = Type.SELECTION;
    public Type getType(){
        return chorType;
    }
    public String getSender(){
        return sender;
    }
    public String getReceiver(){
        return receiver;
    }

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
        return String.format("%s->%s[%s]; %s", sender, receiver, label, continuation.toString());
    }
}
