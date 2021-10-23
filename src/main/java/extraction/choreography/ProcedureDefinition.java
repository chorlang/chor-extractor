package extraction.choreography;

import java.util.HashSet;
import java.util.List;

public class ProcedureDefinition extends ChoreographyASTNode implements Comparable<ProcedureDefinition>{
    public final String name;
    public final ChoreographyBody body;
    public final List<String> parameters;

    private final Type chorType = Type.PROCEDURE_DEFINITION;
    public Type getType(){
        return chorType;
    }

    public ProcedureDefinition(String name, List<String> parameters, ChoreographyBody body){
        this.name = name;
        this.body = body;
        this.parameters = parameters;
    }

     public ProcedureDefinition(String name, ChoreographyBody body){
        this.name = name;
        this.body = body;
        parameters = List.of();
    }

    public String toString(){
        if (parameters.isEmpty())
            return String.format("def %s { %s }", name, body.toString());
        String parString = parameters.toString().replace('[', '(').replace(']', ')');
        return String.format("def %s%s { %s }", name, parString, body.toString());
    }

    @Override
    public int compareTo(ProcedureDefinition o) {
        return name.compareTo(o.name);
    }
}
