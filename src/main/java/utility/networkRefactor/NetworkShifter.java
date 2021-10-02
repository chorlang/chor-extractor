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
        switch (b.action){
            case TERMINATION:
                return b;
            case PROCEDURE_INVOCATION:
                var p = (ProcedureInvocation)b;
                return newCalls.get(p.procedure);
            case SEND:
                var sen = (Send)b;
                return new Send(sen.receiver, sen.expression, replace(newCalls, sen.continuation));
            case RECEIVE:
                var r = (Receive)b;
                return new Receive(r.sender, replace(newCalls, r.continuation));
            case SELECTION:
                var sel = (Selection)b;
                return new Selection(sel.receiver, sel.label, replace(newCalls, sel.continuation));
            case OFFERING:
                var o = (Offering)b;
                var branches = new HashMap<String, Behaviour>();
                o.branches.forEach((label, branch) -> branches.put(label, replace(newCalls, branch)));
                return new Offering(o.sender, branches);
            case CONDITION:
                var c = (Condition)b;
                return new Condition(c.expression, replace(newCalls, c.thenBehaviour), replace(newCalls, c.elseBehaviour));
            default:
                throw new IllegalStateException();
        }
    }

    private static BPair computeShift(String name, Behaviour b, double p){
        switch (b.action){
            case SEND:
                if (random.nextDouble() < p){
                    var s = (Send)b;
                    var shift = computeShift(name, s.continuation, p);
                    return new BPair(new Send(s.receiver, s.expression, shift.prefix), shift.newBody);
                } else{
                    return new BPair(new ProcedureInvocation(name), b);
                }
            case RECEIVE:
                if (random.nextDouble() < p){
                    var r = (Receive)b;
                    var shift = computeShift(name, r.continuation, p);
                    return new BPair(new Receive(r.sender, shift.prefix), shift.newBody);
                }else {
                    return new BPair(new ProcedureInvocation(name), b);
                }
            case SELECTION:
                if (random.nextDouble() < p){
                    var s = (Selection)b;
                    var shift = computeShift(name, s.continuation, p);
                    return new BPair(new Selection(s.receiver, s.label, shift.prefix), shift.newBody);
                } else {
                    return new BPair(new ProcedureInvocation(name), b);
                }
            default:
                return new BPair(new ProcedureInvocation(name), b);
        }
    }
}
