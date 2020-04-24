package extraction.choreography;

import java.util.HashSet;

public class ProcedureDefinition extends ChoreographyASTNode implements Comparable<ProcedureDefinition>{
    public final String name;
    public final ChoreographyBody body;
    public final HashSet<String> usedProcesses;

    private final Type chorType = Type.PROCEDURE_DEFINITION;
    public Type getType(){
        return chorType;
    }

    public ProcedureDefinition(String name, ChoreographyBody body, HashSet<String> usedProcesses){
        this.name = name;
        this.body = body;
        this.usedProcesses = usedProcesses;
    }

    public String toString(){
        return String.format("def %s { %s }", name, body.toString());
    }

    @Override
    public int compareTo(ProcedureDefinition o) {
        if (o == null)
            throw new NullPointerException("Cannot compare ProcedureDefinition to null");
        return name.compareTo(o.name);
    }
}
