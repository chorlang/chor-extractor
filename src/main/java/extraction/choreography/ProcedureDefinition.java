package extraction.choreography;

import java.util.HashSet;

public class ProcedureDefinition implements Comparable{
    public String name;
    public ChoreographyBody body;
    public HashSet<String> usedProcesses;

    public ProcedureDefinition(String name, ChoreographyBody body, HashSet<String> usedProcesses){
        this.name = name;
        this.body = body;
        this.usedProcesses = usedProcesses;
    }

    public String toString(){
        return String.format("def %s { %s }", name, body.toString());
    }

    @Override
    public int compareTo(Object o) {
        if (o == null)
            throw new NullPointerException("Cannot compare ProcedureDefinition to null");
        if (o.getClass() != this.getClass())
            throw new ClassCastException("Object tested for equality with ProcedureDefinition is not a ProcedureDefinition");
        var other = (ProcedureDefinition)o;

        return name.compareTo(other.name);
    }
}
