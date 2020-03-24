package extraction.choreography;

public class Termination extends ChoreographyBody {
    private static Termination instance = new Termination();

    public Type chorType = Type.TERMINATION;

    public static Termination getInstance(){
        return instance;
    }

    private String termination = "stop";

    @Override
    public String toString() {
        return termination;
    }
}
