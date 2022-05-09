package endpointprojection;

import extraction.network.*;
import extraction.network.ProcessTerm;
import extraction.network.utils.TreeVisitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class SpawnRepair implements TreeVisitor<Behaviour, Behaviour> {
    private final String initialProcess, currentProcess;
    private final HashMap<String, ProcessTerm> terms;
    final HashMap<String, Behaviour> spawnedProcedures = new HashMap<>();
    final HashMap<String, List<String>> spawnedProcedureParameters = new HashMap<>();
    private final HashSet<String> children;

    /**
     * Object to fix projection mistakes related to process spawning.
     * @param process The name of the parent process.
     * @param terms The projected mappings of each process.
     */
    public SpawnRepair(String process, HashMap<String, ProcessTerm> terms){
        this.initialProcess = process;
        this.currentProcess = process;
        this.terms = terms;
        children = new HashSet<>();
        children.add(process);
    }
    private SpawnRepair(String initialProcess, String currentProcess, HashMap<String, ProcessTerm> terms, HashSet<String> children){
        this.initialProcess = initialProcess;
        this.currentProcess = currentProcess;
        this.terms = terms;
        this.children = children;
        children.add(currentProcess);
    }

    public Behaviour Visit(Behaviour host){
        switch (host){
            case ProcedureInvocation invocation -> {
                if (currentProcess.equals(initialProcess)) {
                    return invocation;
                }
                else {
                    return new ProcedureInvocation(invocation.procedure+"_"+currentProcess, invocation.getParameters(), Visit(invocation.continuation));
                }
            }
            case Spawn sp -> {
                //No need to add procedures, if a spawned process spawns a replacement for its parent.
                boolean madeSpawnedProcedures = children.contains(sp.variable);
                //The boolean must be calculated before the below init, because of side effects.
                var childRepair = new SpawnRepair(initialProcess, sp.variable, terms, children);
                if (!madeSpawnedProcedures) {
                    //Add the procedures the child needs, and rename them to distinguish them from their parent's
                    ProcessTerm term = terms.get(sp.variable);
                    term.procedures.forEach((name, original) -> {
                        Behaviour fixed = childRepair.Visit(original);
                        spawnedProcedures.putIfAbsent(name + "_" + sp.variable, fixed);
                        spawnedProcedureParameters.putIfAbsent(name+"_"+sp.variable, term.parameters.get(name));
                    });
                }
                Behaviour childBehaviour = childRepair.Visit(sp.processBehaviour);
                //Add procedures for children's children.
                childRepair.spawnedProcedures.keySet().forEach(procedureName -> {
                    if (!spawnedProcedures.containsKey(procedureName)){
                        spawnedProcedures.put(procedureName, childRepair.spawnedProcedures.get(procedureName));
                        spawnedProcedureParameters.put(procedureName, childRepair.spawnedProcedureParameters.get(procedureName));
                    }
                });

                return new Spawn(sp.variable, childBehaviour, Visit(sp.continuation));
            }
            case Termination t -> { return t; }
            case Behaviour.BreakBehaviour bb -> { return bb; }//NoneBehaviour is also handled by this case
            case Condition cond ->{
                return new Condition(cond.expression, Visit(cond.thenBehaviour), Visit(cond.elseBehaviour), Visit(cond.continuation));
            }
            case Offering offer -> {
                var branches = new HashMap<String, Behaviour>();
                offer.branches.forEach((label, behaviour) -> {
                    branches.put(label, Visit(behaviour));
                });
                return new Offering(offer.sender, branches, Visit(offer.continuation));
            }
            case Receive receive -> {
                return new Receive(receive.sender, Visit(receive.continuation));
            }
            case Introductee introductee -> {
                return new Introductee(introductee.sender, introductee.processID, Visit(introductee.continuation));
            }
            case Send send -> {
                return new Send(send.receiver, send.expression, Visit(send.continuation));
            }
            case Selection select -> {
                return new Selection(select.receiver, select.label, Visit(select.continuation));
            }
            case Introduce introducer -> {
                return new Introduce(introducer.leftReceiver, introducer.rightReceiver, Visit(introducer.continuation));
            }
            default -> throw new RuntimeException("When fixing spawns during projection, encountered a Behaviour of unknown type: "+host);
        }
    }

}
