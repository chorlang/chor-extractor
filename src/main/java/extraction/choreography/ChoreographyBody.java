package extraction.choreography;

public abstract class ChoreographyBody {
    public enum Type {
        COMMUNICATION,
        CONDITION,
        PROCEDURE_INVOCATION,
        SELECTION,
        TERMINATION
    }

    //This might not be needed. Also it should be an abstract getter to ensure it would be overwritten.
    public Type chorType;

    public int hashCode(){
        return toString().hashCode();
    }

    abstract public String toString();

    /*
    The Kotlin implementation has a class called CommunicationSelection that contains either an Communication or Selection,
    as well as another ChoreographyBody. Instead, I made those two classes extend this one, so they contain the
    continuation themselves. If nod needed, it can just be null.
     */
    abstract static class Interaction extends ChoreographyBody{
        public ChoreographyBody continuation;
    }

}
