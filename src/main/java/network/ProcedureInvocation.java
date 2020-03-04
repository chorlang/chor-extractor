package network;

/**
 * Simple Behavior that calls a procedure
 */
public class ProcedureInvocation implements Behaviour {
    String procedure;

    /**
     * Constructor for the ProcedureInvocation Behavior.
     * Simply stores the procedure (string) that it invokes.
     * @param procedure Name of procedure to invoke.
     */
    public ProcedureInvocation(String procedure){
        this.procedure = procedure;
    }

    public String toString(){
        return procedure;
    }

    public ProcedureInvocation copy(){
        return new ProcedureInvocation(procedure);
    }

    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (other.getAction() != Action.procedureInvocation)
            return false;
        ProcedureInvocation otherPI = (ProcedureInvocation)other;
        return procedure.equals(otherPI.procedure);
    }

    public int hashCode(){
        return procedure.hashCode();
    }

    public Action getAction(){
        return Action.procedureInvocation;
    }
}
