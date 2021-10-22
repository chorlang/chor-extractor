package extraction.network;

import extraction.Label;

import java.util.HashMap;
import java.util.Map;

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

    @Override
    public Label.InteractionLabel labelFrom(String process, ProcessTerm.ValueMap sub){
        return new Label.CommunicationLabel(sub.get(process), sub.get(receiver), sub.get(expression));
    }

    public String toString(){
        return String.format("%s!<%s>; %s", receiver, expression, continuation);
    }

    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (other.action != Action.SEND)
            return false;
        Send otherS = (Send)other;
        return receiver.equals(otherS.receiver) &&
                expression.equals(otherS.expression) &&
                continuation.equals(otherS.continuation);
    }

    public int hashCode(){
        return hash;
    }
    private int hashValue(){
        int hash = continuation.hashCode() * 31;
        hash += receiver.hashCode();
        hash *= 31;
        hash += expression.hashCode();
        return hash;
    }
}
