package extraction.network;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
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
     * @param continuation The behaviour to execute when exiting the procedure call.
     */
    public ProcedureInvocation(String procedure, List<String> parameters, @NotNull Behaviour continuation){
        super(Action.PROCEDURE_INVOCATION, continuation);
        this.parameters = parameters;
        this.procedure = procedure;
        hash = hashValue();
    }


    /**
     * Used by some of the older test generation classes
     */
    public ProcedureInvocation(String procedure){
        this(procedure, List.of(), NoneBehaviour.instance);
    }

    @Override
    Behaviour realValues(HashMap<String, String> substitutions){
        return new ProcedureInvocation(procedure, parameters.stream().map(substitutions::get).toList(), continuation);
    }

    /**
     * Returns a read-only view of the parameter values used in this invocation
     */
    public List<String> getParameters(){
        return Collections.unmodifiableList(parameters);
    }

    @Override
    public String toString(){
        String s = procedure + parametersToString();
        if (!(continuation instanceof BreakBehaviour))
            s += "; " + continuation;
        return s;
    }
    private String parametersToString(){
        if (parameters == null || parameters.size() == 0)
            return "";
        return parameters.toString().replace('[', '(').replace(']', ')');
    }

    @Override
    public boolean equals(Behaviour other){
        if (this == other)
            return true;
        if (!(other instanceof ProcedureInvocation otherPI))
            return false;
        return procedure.equals(otherPI.procedure) && continuation.equals(other.continuation);
        //Be careful if you decide to take parameters into account.
        //Identical calls may have different parameter names parsed to them
    }

    @Override
    public int hashCode(){
        return hash;
    }

    //I omitted the parameters for the hash, as they should take variables into account to function properly
    //which is cumbersome, and shouldn't matter too much anyway.
    private int hashValue(){
        return procedure.hashCode() ^ Integer.rotateRight(continuation.hashCode(), 1);
    }
}
