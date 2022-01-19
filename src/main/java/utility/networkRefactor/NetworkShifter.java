package utility.networkRefactor;

import extraction.network.*;
import extraction.network.utils.NetworkPurger;
import parsing.Parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class NetworkShifter {
    private static Random random = new Random();

    private static class BPair{
        Behaviour prefix, newBody;
        BPair(Behaviour first, Behaviour second){
            this.prefix = first;
            this.newBody = second;
        }
    }

    public static Network compute(String inputNetwork, double p){
        var n = Parser.stringToNetwork(inputNetwork);
        NetworkPurger.purgeNetwork(n);

        for (var term : n.processes.values()){
            var processTerm = new ProcessTerm.HackProcessTerm(term);
            var newCalls = new HashMap<String, Behaviour>();
            var newDefs = new HashMap<String, Behaviour>();
            processTerm.procedures.forEach((procedureName, procedureBody) -> {
                var shift = computeShift(procedureName, procedureBody, p);
                newCalls.put(procedureName, shift.prefix);
                newDefs.put(procedureName, shift.newBody);
            });
            processTerm.changeMain( replace(newCalls, processTerm.main()) );
            newDefs.forEach( (name, newDef) -> processTerm.procedures.put(name, replace(newCalls, newDef)));
        }
        return n;
    }

    private static Behaviour replace(Map<String, Behaviour> newCalls, Behaviour b){
        switch (b){
            case Termination t:
                return b;
            case ProcedureInvocation p:
                return newCalls.get(p.procedure);
            case Send sen:
                return new Send(sen.receiver, sen.expression, replace(newCalls, sen.getContinuation()));
            case Receive r:
                return new Receive(r.sender, replace(newCalls, r.getContinuation()));
            case Selection sel:
                return new Selection(sel.receiver, sel.label, replace(newCalls, sel.getContinuation()));
            case Offering o:
                var branches = new HashMap<String, Behaviour>();
                o.branches.forEach((label, branch) -> branches.put(label, replace(newCalls, branch)));
                return new Offering(o.sender, branches);
            case Condition c:
                return new Condition(c.expression, replace(newCalls, c.thenBehaviour), replace(newCalls, c.elseBehaviour));
            default:
                throw new IllegalStateException();
        }
    }

    private static BPair computeShift(String name, Behaviour b, double p){
        switch (b){
            case Send s:
                if (random.nextDouble() < p){
                    var shift = computeShift(name, s.getContinuation(), p);
                    return new BPair(new Send(s.receiver, s.expression, shift.prefix), shift.newBody);
                } else{
                    return new BPair(new ProcedureInvocation(name), b);
                }
            case Receive r:
                if (random.nextDouble() < p){
                    var shift = computeShift(name, r.getContinuation(), p);
                    return new BPair(new Receive(r.sender, shift.prefix), shift.newBody);
                }else {
                    return new BPair(new ProcedureInvocation(name), b);
                }
            case Selection s:
                if (random.nextDouble() < p){
                    var shift = computeShift(name, s.getContinuation(), p);
                    return new BPair(new Selection(s.receiver, s.label, shift.prefix), shift.newBody);
                } else {
                    return new BPair(new ProcedureInvocation(name), b);
                }
            default:
                return new BPair(new ProcedureInvocation(name), b);
        }
    }
}
