package extraction.choreography;

public class Termination extends ChoreographyBody {
    private static Termination instance = new Termination();

    private final Type chorType = Type.TERMINATION;
    public Type getType(){
        return chorType;
    }

    public static Termination getInstance(){
        return instance;
    }

    private static final String termination = "stop";

    @Override
    public String toString() {
        return termination;
    }
}
