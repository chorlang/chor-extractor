package extraction.choreography;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Choreography {
    public ChoreographyBody main;
    public List<ProcedureDefinition> procedures;
    public Set<String> processes;

    public Choreography(ChoreographyBody main, List<ProcedureDefinition> procedures, Set<String> processes){
        this.main = main;
        this.procedures = procedures;
        this.processes = processes;
    }

    public Choreography(ChoreographyBody main, List<ProcedureDefinition> procedures){
        this(main, procedures, Set.of());
    }

    public String toString(){
        procedures.sort(null);
        StringBuilder procedureText = new StringBuilder();
        procedures.forEach(procDef -> procedureText.append(procDef.toString()).append(' '));
        return String.format("%s"+"main {%s}", procedureText.toString(), main);
    }
}
