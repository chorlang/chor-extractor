package extraction.choreography;

import java.util.List;

public class ProcedureInvocation extends ChoreographyBody {
    public final String procedure;
    public final List<String> parameters;

    private final Type chorType = Type.PROCEDURE_INVOCATION;
    public Type getType(){
        return chorType;
    }

    public ProcedureInvocation(String procedure, List<String> parameters){
        this.procedure = procedure;
        this.parameters = parameters;
    }
    public ProcedureInvocation(String procedure){
        this.procedure = procedure;
        parameters = List.of();
    }

    @Override
    public String toString() {
        String parString = parameters.isEmpty() ? ""
                : parameters.toString().replace('[', '(').replace(']', ')');
        return procedure + parString;
    }
}
