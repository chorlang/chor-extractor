package extraction.choreography;

public class ProcedureInvocation extends ChoreographyBody {
    public final String procedure;

    public final Type chorType = Type.PROCEDURE_INVOCATION;

    public ProcedureInvocation(String procedure){
        this.procedure = procedure;
    }

    @Override
    public String toString() {
        return procedure;
    }
}
