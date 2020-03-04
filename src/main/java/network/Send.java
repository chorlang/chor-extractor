package network;

/**
 * Behavior for evaluating an expression and seninding the result ot another process.
 *
 * Note that the variable "process" from the Kotlin implementation is "receiver"
 */
public class Send implements Behaviour {
    String receiver, expression;
    Behaviour continuation;

    /**
     * Constructs a send behavior, which represents evaluates an
     * expression and sending it to another process.
     * @param receiver The name of the process that is sent the result of the expression
     * @param expression The expression to send the result of
     * @param continuation The behavior to continue as after sending.
     */
    public Send(String receiver, String expression, Behaviour continuation){
        this.receiver = receiver;
        this.expression = expression;
        this.continuation = continuation;
    }

    public String toString(){
        return String.format("%s!<%s>; %s", receiver, expression, continuation);
    }

    public Send copy(){
        return new Send(receiver, expression, continuation.copy());
    }

    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (other.getAction() != Action.send)
            return false;
        Send otherS = (Send)other;
        return receiver.equals(otherS.receiver) &&
                expression.equals(otherS.expression) &&
                continuation.equals(otherS.continuation);
    }

    public int hashCode(){
        int hash = continuation.hashCode() * 31;
        hash += receiver.hashCode();
        hash *= 31;
        hash += expression.hashCode();
        return hash;
    }

    public Action getAction(){
        return Action.send;
    }
}
