package utility.networkRefactor;

import extraction.network.*;
import extraction.network.utils.NetworkPurger;
import org.jetbrains.annotations.NotNull;
import parsing.Parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/*
This file was cursed.
It took several minutes for IntelliJ to update errors and warnings, and it often did it wrong.
When trying to commit, I got a warning about being unable to check for null expressions in loops. (or something like that)

The cursed code turned out to be
            for (var name : result.procedures.keySet()){
                result.procedures.put(name, compute(result.procedures.get(name), processTerm.procedures, p));
            }
specifically the loop body seemed to be the power behind the curse. Replacing it by replaceAll()
seems to have fixed it. I pray it has been fixed for good
 */
public class NetworkUnfolder {
    private static final Random random = new Random();

    private static ProcessTerm compute(ProcessTerm processTerm, double p, int iterations){
        var result = new ProcessTerm.HackProcessTerm( processTerm.copy() );
        for (int i = 0; i < iterations; i++){
            result.changeMain( compute(result.main(), result.procedures, p) );
            result.procedures.replaceAll((n, v) -> compute(result.procedures.get(n), processTerm.procedures, p));
        }
        return result.term;
    }

    public static Network compute(String inputNetwork, double p, int iterations){
        var n = Parser.stringToNetwork(inputNetwork);
        NetworkPurger.purgeNetwork(n);

        n.processes.replaceAll((k, v) -> compute(n.processes.get(k), p, iterations));
        return n;
    }

    private static Behaviour compute(Behaviour b, Map<String, Behaviour> procedures, double p){
        switch (b.action){
            case TERMINATION:
                return Termination.instance;
            case SEND:
                var s = (Send)b;
                return new Send(s.receiver, s.expression, compute(s.continuation, procedures, p));
            case RECEIVE:
                var r = (Receive)b;
                return new Receive(r.sender, compute(r.continuation, procedures, p));
            case SELECTION:
                var sel = (Selection)b;
                return new Selection(sel.receiver, sel.label, compute(sel.continuation, procedures, p));
            case OFFERING:
                Offering o = (Offering)b;
                var branches = new HashMap<String, Behaviour>();
                o.branches.forEach((key, value) -> branches.put(key, compute(value, procedures, p)));
                return new Offering(o.sender, branches);
            case CONDITION:
                var c = (Condition)b;
                return new Condition(c.expression, compute(c.thenBehaviour, procedures, p), compute(c.elseBehaviour, procedures, p));
            case PROCEDURE_INVOCATION:
                var proc = (ProcedureInvocation)b;
                if (random.nextDouble() < p){
                    return procedures.get(proc.procedure);
                } else{
                    return new ProcedureInvocation(proc.procedure);
                }
            default:
                throw new IllegalStateException();
        }
    }
}
