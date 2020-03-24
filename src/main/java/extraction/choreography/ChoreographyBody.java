package extraction.choreography;

public abstract class ChoreographyBody {
    public enum Type {
        COMMUNICATION,
        CONDITION,
        PROCEDUREINVOCATION,
        SELECTION,
        TERMINATION
    }

    public Type chorType;

    public int hashCode(){
        return toString().hashCode();
    }

    abstract public String toString();

    abstract static class Interaction extends ChoreographyBody{
        public ChoreographyBody continuation;
    }

}
