package extraction.network;

import java.util.List;

/**
 * Simple Behavior that calls a procedure
 */
public class ProcedureInvocation extends Behaviour {
    public final String procedure;
    private final List<String> parameters;

    /**
     * Constructor for the ProcedureInvocation Behavior.
     * Simply stores the procedure (string) that it invokes.
     * @param procedure Name of procedure to invoke.
     */
    public ProcedureInvocation(String procedure, List<String> parameters){
        super(Action.PROCEDURE_INVOCATION);
        this.parameters = parameters;
        this.procedure = procedure;
    }
    public ProcedureInvocation(String procedure){
        this(procedure, List.of());
    }

    public String toString(){
        return procedure;
    }

    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (!(other instanceof ProcedureInvocation otherPI))
            return false;
        return procedure.equals(otherPI.procedure) && parameters.equals(otherPI.parameters);
    }

    public int hashCode(){
        return procedure.hashCode();
    }
}
