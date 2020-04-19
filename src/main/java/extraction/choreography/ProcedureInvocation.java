package extraction.choreography;

public class ProcedureInvocation extends ChoreographyBody {
    public final String procedure;

    private final Type chorType = Type.PROCEDURE_INVOCATION;
    public Type getType(){
        return chorType;
    }

    public ProcedureInvocation(String procedure){
        this.procedure = procedure;
    }

    @Override
    public String toString() {
        return procedure;
    }
}
