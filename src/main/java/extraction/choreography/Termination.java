package extraction.choreography;

public class Termination extends ChoreographyBody {
    private static Termination instance = new Termination();

    public final Type chorType = Type.TERMINATION;

    public static Termination getInstance(){
        return instance;
    }

    private static final String termination = "stop";

    @Override
    public String toString() {
        return termination;
    }
}
