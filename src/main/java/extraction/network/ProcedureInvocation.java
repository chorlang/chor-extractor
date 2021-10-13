package extraction.network;

import java.util.HashMap;
import java.util.List;

/**
 * Simple Behavior that calls a procedure
 */
public class ProcedureInvocation extends Behaviour {
    public final String procedure;
    final List<String> parameters;
    private final int hash;

    /**
     * Constructor for the ProcedureInvocation Behavior.
     * Simply stores the procedure (string) that it invokes.
     * @param procedure Name of procedure to invoke.
     */
    public ProcedureInvocation(String procedure, List<String> parameters){
        super(Action.PROCEDURE_INVOCATION);
        this.parameters = parameters;
        this.procedure = procedure;
        hash = hashValue();
    }

    @Override
    Behaviour realValues(HashMap<String, String> substitutions){
        return new ProcedureInvocation(procedure, parameters.stream().map(substitutions::get).toList());
    }

    public ProcedureInvocation(String procedure){
        this(procedure, List.of());
    }

    public String toString(){
        return procedure + parametersToString();
    }
    private String parametersToString(){
        if (parameters == null || parameters.size() == 0)
            return "";
        return parameters.toString().replace('[', '(').replace(']', ')');
    }

    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (!(other instanceof ProcedureInvocation otherPI))
            return false;
        return procedure.equals(otherPI.procedure);
        //Be careful if you decide to take parameters into account.
        //Identical calls may have different parameter names parsed to them
    }

    public int hashCode(){
        return hash;
    }
    private int hashValue(){
        return procedure.hashCode();
    }
}
