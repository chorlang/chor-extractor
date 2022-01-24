package extraction.network;

import extraction.Label;

import java.util.HashMap;

/**
 * Behavior for evaluating an expression and sending the result ot another process.
 *
 * Note that the variable "process" from the Kotlin implementation is "receiver"
 */
public class Send extends Behaviour.Sender {
    private final int hash;

    /**
     * Constructs a send behavior, which represents evaluates an
     * expression and sending it to another process.
     * @param receiver The name of the process that is sent the result of the expression
     * @param expression The expression to send the result of
     * @param continuation The behavior to continue as after sending.
     */
    public Send(String receiver, String expression, Behaviour continuation){
        super(Action.SEND, continuation, receiver, expression);
        hash = hashValue();
    }

    @Override
    Behaviour realValues(HashMap<String, String> substitutions) {
        return new Send(substitutions.get(receiver), expression, continuation);
    }

    public Label.InteractionLabel labelFrom(String process, ProcessTerm.ValueMap sub){
        return new Label.CommunicationLabel(sub.get(process), sub.get(receiver), expression);
    }

    @Override
    public String toString(){
        return String.format("%s!<%s>; %s", receiver, expression, continuation);
    }

    @Override
    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (!(other instanceof Send otherS))
            return false;
        return receiver.equals(otherS.receiver) &&
                expression.equals(otherS.expression) &&
                continuation.equals(otherS.continuation);
    }

    @Override
    boolean compareData(Behaviour other){
        return other instanceof Send send && receiver.equals(send.receiver) && expression.equals(send.expression);
    }

    @Override
    public int hashCode(){
        return hash;
    }
    private int hashValue(){
        int hash = receiver.hashCode() * 31;
        hash += expression.hashCode();
        return hash ^ Integer.rotateRight(continuation.hashCode(), 1);
    }
}
