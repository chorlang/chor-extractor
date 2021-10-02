package extraction.network;

/**
 * Simple Behavior that calls a procedure
 */
public class ProcedureInvocation extends Behaviour {
    public final String procedure;

    /**
     * Constructor for the ProcedureInvocation Behavior.
     * Simply stores the procedure (string) that it invokes.
     * @param procedure Name of procedure to invoke.
     */
    public ProcedureInvocation(String procedure){
        super(Action.PROCEDURE_INVOCATION);
        this.procedure = procedure;
    }

    public String toString(){
        return procedure;
    }

    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (other.action != Action.PROCEDURE_INVOCATION)
            return false;
        ProcedureInvocation otherPI = (ProcedureInvocation)other;
        return procedure.equals(otherPI.procedure);
    }

    public int hashCode(){
        return procedure.hashCode();
    }
}
