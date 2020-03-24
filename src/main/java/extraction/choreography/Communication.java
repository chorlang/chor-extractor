package extraction.choreography;

public class Communication extends ChoreographyBody.Interaction{
    public String sender, receiver, expression;

    public ChoreographyBody continuation;

    public Type chorType = Type.COMMUNICATION;

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
        return String.format("%s.%s->%s", sender, expression, receiver);
    }
}
