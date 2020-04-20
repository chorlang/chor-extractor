package extraction.choreography;

public abstract class ChoreographyBody extends ChoreographyASTNode {

    public int hashCode(){
        return toString().hashCode();
    }

    abstract public String toString();

    /*
    The Kotlin implementation has a class called CommunicationSelection that contains either an Communication or Selection,
    as well as another ChoreographyBody. Instead, I made those two classes extend this one, so they contain the
    continuation themselves. If nod needed, it can just be null.
     */
    public abstract static class Interaction extends ChoreographyBody{
        public abstract ChoreographyBody getContinuation();
        public abstract void setContinuation(ChoreographyBody continuation);
        public abstract String getSender();
        public abstract String getReceiver();
    }

}
