package extraction.network;

import extraction.Label.*;
import extraction.network.Behaviour.*;
import utility.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessTerm extends NetworkASTNode {
    public final HashMap<String, Behaviour> procedures;     //Map from procedure names to their behaviours
    final HashMap<String, List<String>> parameters;         //Map from procedure names to their parameter variable names
                                                            //Is assumed to be readonly when extracting.
    private final int proceduresHash;
    /**
     * The current main Behaviour of this process, with variable names.
     * Consider using main() to read it, as it substitutes variable names with their corresponding values.
     * Do NOT assign this field to what is returned from main(). Assigning it to the fields of
     * what is returned from main() is ok.
     */
    Behaviour main;                                         //The main behaviour for the procedure
    //Used for getting real process names from variables.
    static class ValueMap extends HashMap<String, String>{
        public ValueMap(){}
        public ValueMap(Map<String, String> c){
            super(c);
        }
        @Override
        public String get(Object key){
            return getOrDefault(key, (String)key);  //If no mapping exists, assume constant value
        }
        @Override
        public String put(String key, String value){
            return super.put(key, get(value));      //If value->pID exists, then create key->pID instead
        }
    }
    ValueMap substitutions = new ValueMap();

    /**
     * Constructor for ProcessTerm
     * @param procedures A HashMap&lt;String, Behavior&gt; from the procedure name as string, to the Behaviour of that procedure
     * @param main The main Behaviour for this procedure
     */
    public ProcessTerm(HashMap<String, Behaviour> procedures, Behaviour main){
        this(procedures, new HashMap<>(), main);
    }
    public ProcessTerm(HashMap<String, Behaviour> procedures, HashMap<String, List<String>> parameters, Behaviour main){
        super(Action.PROCESS_TERM);
        this.procedures = procedures;
        this.parameters = parameters;
        this.main = main;
        proceduresHash = proceduresHashValue();
    }
    private ProcessTerm(HashMap<String, Behaviour> procedures, HashMap<String, List<String>> parameters, ValueMap substitutions, Behaviour main){
        this(procedures, parameters, main);
        this.substitutions = new ValueMap(substitutions);
    }

    /**
     * Restores this process to a previous state.
     * @param oldMain The main behaviour to restore to.
     * @param oldVariables The variable assignments (Internally called substitutions) to restore to. This parameter is copied.
     */
    void restore(Behaviour oldMain, ValueMap oldVariables){
        main = oldMain;
        substitutions = new ValueMap(oldVariables);
    }

    /**
     * Create a new ProcessTerm that has been spawned from this one.
     * Initializes a new ProcessTerm with the same procedures as this one, but with the provided main Behaviour.
     * The new term gets a copy of all variable mappings from this one. You should assign the new process' name
     * to a variable in the parent process before calling this function.
     * @param mainBehaviour The main behaviour of the new process
     * @return The created ProcessTerm
     */
    ProcessTerm spawnNew(Behaviour mainBehaviour){
        return new ProcessTerm(procedures, parameters, new ValueMap(substitutions), mainBehaviour);
    }

    /**
     * Returns (possibly a copy of) the main Behaviour of this process, where
     * variable names will be replaced by real values in the Behaviours fields.
     * Behaviours stored in the returned Behaviour does not undergo variable substitution.
     */
    public Behaviour runtimeMain() {
        return main.realValues(substitutions);
    }

    /**
     * Returns the main behaviour of this process as it is defined, not considering the current state of the network.
     */
    public Behaviour rawMain() { return main; }

    /**
     * Returns a InteractionLabel for the network operation needed to advance this process.
     * In other words, the label represents the interaction that would make this process' main behaviour
     * be replaced by its continuation.
     * Assumes the main Behaviour is something that sends (Instance of Behaviour.Sending).
     * @param processName The name of the process that maps to this ProcessTerm.
     * @return A InteractionLabel, that may not be applicable to the network yet, or null if no such label exists.
     */
    InteractionLabel prospectInteraction(String processName){
        return main instanceof Sender send ? send.labelFrom(processName, substitutions) : null;
    }

    Pair<ConditionLabel.ThenLabel, ConditionLabel.ElseLabel> prospectCondition(String processName){
        return main instanceof Condition cond ? cond.labelsFrom(processName) : null;
    }

    SpawnLabel prospectSpawning(String processName){
        return main instanceof Spawn spawner ? spawner.labelFrom(processName, substitutions) : null;
    }



    /**
     * Learn that in this process, variable varName should be substituted by processName.
     * @param varName Name of a process variable within this process.
     * @param processName Name of a real process.
     */
    public void substitute(String varName, String processName){
        //The sub.get() handles if varName is already bound to a value.
        //substitutions.put(substitutions.get(varName), processName);
        substitutions.put(varName, processName);
    }

    /**
     * Checks if the main Behaviour of this process is Termination, or an ProcedureInvocation that expands to Termination.
     * @return true if the Behaviour is Termination, or expands into Termination.
     */
    public boolean isTerminated(){
        return becomesTermination(main);
    }
    private boolean becomesTermination(Behaviour behaviour){
        if (behaviour instanceof Termination)
            return true;
        else if (behaviour instanceof ProcedureInvocation invocation)
            return becomesTermination(procedures.get(invocation.procedure));
        return false;
    }

    /**
     * If the main Behaviour is ProcedureInvocation, repeatedly replace it by its procedure definition
     * until the main Behaviour is no longer ProcedureInvocation.
     */
    void unfoldRecursively() {
        if (runtimeMain() instanceof ProcedureInvocation invocation){
            String procedure = invocation.procedure;
            var paramVar = parameters.get(procedure);
            var paramVal = invocation.parameters;
            //Check that each parameter can be bound to a variable
            if (paramVar.size() < paramVal.size())
                throw new IllegalStateException("Procedure invocation has too many parameters." +
                        "Expected: " + procedure+parametersToString(paramVar) + " Got: " + invocation);
            //Bind variables to parameter values.
            for (int i = 0; i < paramVal.size(); i++){
                substitute(paramVar.get(i), substitutions.get(paramVal.get(i)));
            }
            main = procedures.get(invocation.procedure);
            unfoldRecursively();
        }
    }


    /**
     * Converts the procedures into a human readable format
     * @return string representing this mapping
     */
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        procedures.forEach((key, value) ->
                builder.append(String.format("def %s%s{%s} ", key, parametersToString(parameters.get(key)), value)));
        builder.append(String.format("main {%s}}", main));
        return builder.toString();
    }
    private String parametersToString(List<String> parameters){
        if (parameters == null || parameters.size() == 0)
            return "";
        return parameters.toString().replace('[', '(').replace(']', ')');
    }


    /**
     * Makes a copy o this ProcessTerm.
     * Note that field procedures is not copied, and is assumed to be read only during extraction.
     * Behaviours stored in this instance is not copied, but should be unmodifiable anyway.
     * @return copy of this object instance
     */
    public ProcessTerm copy(){
        return new ProcessTerm(procedures, parameters, substitutions, main);
    }

    /**
     * Compares this object with another object for equality.
     * @param other the object to compare with
     * @return true if both objects are functionally identical
     */
    public boolean equals(ProcessTerm other){
        if (this == other)                          //Trivially true if it is the same object
            return true;
        if (    hashCode() != other.hashCode() ||   //The terms cannot be identical with different hash-codes
                !main.equals(other.main) ||         //The main Behaviours must be identical
                procedures.size() != other.procedures.size())   //Must have the same number of procedures
            return false;
        //Compare all processes, and fail if there is a difference.
        for (String procedureName : procedures.keySet()){
            var otherBehaviour = other.procedures.get(procedureName);
            if (otherBehaviour == null || !otherBehaviour.equals(procedures.get(procedureName)))
                return false;
        }
        return true;
    }
    public boolean equals(Object other){
        if (!(other instanceof ProcessTerm otherTerm))
            return false;
        return equals(otherTerm);
    }

    /**
     * Calculates the hashcode for this ProcessTerm.
     * The hash is calculated from the mapping, as well as the main behaviour
     * @return the hash value considering all behaviours
     */
    public int hashCode(){
        return 31 * proceduresHash + main.hashCode();
    }
    private int proceduresHashValue(){
        return procedures.hashCode();
    }

    /**
     * @return a read only map form variable names, to their bound values. If a variable is not bound,
     * the variable name is considered the variables value.
     */
    public Map<String, String> getVariables(){
        return Collections.unmodifiableMap(substitutions);
    }

    /**
     * Used to allow modification of main outside the network package.
     * The field main is not supposed to be modified directly outside the control of the network, but the classes
     * in fuzzing, and networkRefactor makes custom changes to the networks for testing and or autogeneration of
     * new networks.
     */
    public static class HackProcessTerm{
        public final ProcessTerm term;
        public final HashMap<String, Behaviour> procedures;
        /**
         * Create a wrapper that allows explicit changes to a ProcessTerm, including changing the main Behaviour.
         * Intended for making raw changes to a network. Only use if you know what you are doing.
         * @param term The ProcessTerm to wrap.
         */
        public HackProcessTerm(ProcessTerm term){
            this.term = term;
            procedures = term.procedures;
        }
        public void changeMain(Behaviour newMain){
            term.main = newMain;
        }
        public Behaviour main(){
            return term.main;
        }
    }
}
