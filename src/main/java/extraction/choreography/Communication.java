package extraction.choreography;

public class Communication extends ChoreographyBody.Interaction{
    public String sender, receiver, expression;

    public ChoreographyBody continuation;

    private final Type chorType = Type.COMMUNICATION;
    public Type getType(){
        return chorType;
    }
    public String getSender(){
        return sender;
    }
    public String getReceiver(){
        return receiver;
    }

    public Communication(String sender, String receiver, String expression, ChoreographyBody continuation){
        this.sender = sender;
        this.receiver = receiver;
        this.expression = expression;
        this.continuation = continuation;
    }
    public Communication(String sender, String receiver, String expression){
        this(sender, receiver, expression, null);
    }

    @Override
    public String toString() {
        return String.format("%s.%s->%s; %s", sender, expression, receiver, continuation.toString());
    }
}
