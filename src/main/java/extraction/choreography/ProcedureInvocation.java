package extraction.choreography;

import java.util.List;

public class ProcedureInvocation extends ChoreographyBody {
    public final String procedure;
    public final List<String> parameters;
    public final ChoreographyBody continuation;

    private final Type chorType = Type.PROCEDURE_INVOCATION;
    public Type getType(){
        return chorType;
    }


    public ProcedureInvocation(String procedure, List<String> parameters, ChoreographyBody continuation){
        this.procedure = procedure;
        this.parameters = parameters;
        this.continuation = continuation;
    }
    public ProcedureInvocation(String procedure, List<String> parameters){
        this(procedure, parameters, NoneBody.instance);
    }
    public ProcedureInvocation(String procedure){
        this(procedure, List.of());
    }

    @Override
    public String toString() {
        String parString = parameters.isEmpty() ? ""
                : parameters.toString().replace('[', '(').replace(']', ')');
        String continueString = continuation instanceof NoneBody ? "" : "; " + continuation.toString();
        return procedure + parString + continueString;
    }
}
