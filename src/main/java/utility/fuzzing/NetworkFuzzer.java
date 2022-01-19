package utility.fuzzing;

import extraction.network.*;
import extraction.network.utils.NetworkPurger;
import parsing.Parser;

import java.util.*;

public class NetworkFuzzer {
    private static class FuzzerParams{
        int deletions, swaps;
        FuzzerParams(int deletions, int swaps){
            this.deletions = deletions;
            this.swaps = swaps;
        }
        
    }

    static Random random = new Random();

    private static <SetType> SetType randomElement(Set<SetType> set){
        int item = random.nextInt(set.size());
        int index = 0;
        for (SetType entry : set){
            if (index == item)
                return entry;
            index++;
        }
        throw new ArithmeticException("If " + index+" and " + item+" are off by one, please fix the function this error originates from");
    }

    private static List<Integer> processSizes(Map<String, Behaviour> branches){
        var sizes = new ArrayList<Integer>(branches.size());
        for (var branch : branches.values()){
            sizes.add(ProcessSize.compute(branch));
        }
        return sizes;
    }
    
    public static Network fuzz(String inputNetwork, int deletions, int swaps) {
        var n = Parser.stringToNetwork(inputNetwork);
        NetworkPurger.purgeNetwork(n);

        var processName = randomElement(n.processes.keySet());
        var processTerm = new ProcessTerm.HackProcessTerm( n.processes.get(processName) );

        var keys = new ArrayList<>(processTerm.procedures.keySet());
        var sizeList = new ArrayList<Integer>(processTerm.procedures.size());
        for (var procedure : processTerm.procedures.values()){
            sizeList.add(ProcessSize.compute(procedure));
        }
        keys.add("main");
        sizeList.add( ProcessSize.compute( processTerm.main() ) );

        var paramsMap = splitParams(new FuzzerParams(deletions, swaps), mkSizes(keys, sizeList));

        paramsMap.forEach ((procName, params ) -> {
            if(procName.equals("main")) {
                processTerm.changeMain( fuzzProcess(processTerm.main(), ProcessSize.compute(processTerm.main()), params) );
            } else {
                processTerm.procedures.put(procName, fuzzProcess( processTerm.procedures.get(procName), ProcessSize.compute(processTerm.procedures.get(procName)), params ));
            }
        });
        return n;
    }

    private static Map<String, Integer>  mkSizes(List<String> keys, List<Integer> values) {
        assert(keys.size() == values.size());
        var map = new HashMap<String, Integer>();
        for(int i = 0; i < keys.size(); i++) {
            map.put(keys.get(i), values.get(i));
        }
        return map;
    }

    private static FuzzerParams decDels(FuzzerParams params) {
        return new FuzzerParams(params.deletions - 1, params.swaps);
    }
    private static FuzzerParams decSwaps(FuzzerParams params) {
        return new FuzzerParams(params.deletions, params.swaps - 1);
    }

    private static Behaviour fuzzProcess(Behaviour b, int size, FuzzerParams params) {
        if( params.deletions + params.swaps == 0 )
            return b;

        if( size == 0 )
            throw new IllegalStateException("Reached size 0");

        if( random.nextDouble() <= (params.deletions + params.swaps + 0.0)/size ) {
            var p = random.nextInt(params.deletions + params.swaps);
            if(p < params.deletions) {
                // We do a deletion;
                switch( b ) {
                    case Send s: {
                        return fuzzProcess(s.getContinuation(), size - 1, decDels(params));
                    }
                    case Receive s: {
                        return fuzzProcess(s.getContinuation(), size - 1, decDels(params));
                    }
                    case Selection s: {
                        return fuzzProcess(s.getContinuation(), size - 1, decDels(params));
                    }
                    case Offering o: {
                        var cont = o.branches.get(randomElement(o.branches.keySet()));
                        return fuzzProcess( cont, ProcessSize.compute(cont), decDels(params) );
                    }
                    
                    case Condition c: {
                        Behaviour cont; if ( random.nextBoolean() )
                            cont = c.thenBehaviour;
                        else
                            cont = c.elseBehaviour;
                        return fuzzProcess( cont, ProcessSize.compute(cont), decDels(params) );
                    }
                    case Termination t: throw new IllegalStateException("Reached termination");
                    case ProcedureInvocation pi: return fuzzProcess(Termination.instance, 0, decDels(params));
                    default:
                        throw new UnsupportedOperationException("Fuzzing does not support newer behaviours");
                }
            } else {
                // We do a swap;
                switch ( b ) {
                    case Send newB: {
                        var c = newB.getContinuation();
                        switch (c) {
                            case Send cont: {
                                return new Send(cont.receiver, cont.expression, fuzzProcess(new Send(newB.receiver, newB.expression, cont.getContinuation()), size - 1, decSwaps(params)));
                            }
                            case Receive cont: {
                                return new Receive(cont.sender, fuzzProcess(new Send(newB.receiver, newB.expression, cont.getContinuation()), size - 1, decSwaps(params)));
                            }
                            case Selection cont: {
                                return new Selection(cont.receiver, cont.label, fuzzProcess(new Send(newB.receiver, newB.expression, cont.getContinuation()), size - 1, decSwaps(params)));
                            }
                            case Offering cont: {
                                var processSizes = new ArrayList<Integer>(cont.branches.size());
                                for (var branch : cont.branches.values()) {
                                    processSizes.add(ProcessSize.compute(branch));
                                }
                                var splitParams = splitParams(decSwaps(params), mkSizes(new ArrayList<>(cont.branches.keySet()), processSizes));
                                var branches = new HashMap<String, Behaviour>();
                                var chosen = randomElement(cont.branches.keySet());
                                splitParams.forEach((label, lblParams) -> {
                                    if (label.equals(chosen)) {
                                        branches.put(label, fuzzProcess(
                                                new Send(newB.receiver, newB.expression, cont.branches.get(label)),
                                                ProcessSize.compute(cont.branches.get(label)) + 1,
                                                lblParams));
                                    } else
                                        branches.put(label, fuzzProcess(cont.branches.get(label), ProcessSize.compute(cont.branches.get(label)), lblParams));
                                });
                                return new Offering(cont.sender, branches);
                            }
                            case Condition cont: {
                                var splitParams = splitParams(decSwaps(params), mkSizes(List.of("then", "else"), List.of(ProcessSize.compute(cont.thenBehaviour), ProcessSize.compute(cont.elseBehaviour))));
                                return new Condition(
                                        cont.expression,
                                        fuzzProcess(new Send(newB.receiver, newB.expression, cont.thenBehaviour), ProcessSize.compute(cont.thenBehaviour), splitParams.get("then")),
                                        fuzzProcess(cont.elseBehaviour, ProcessSize.compute(cont.elseBehaviour), splitParams.get("else"))
                                );
                            }
                            case Termination t:
                                return fuzzProcess(c, 0, decSwaps(params));
                            case ProcedureInvocation pi:
                                return fuzzProcess(c, 1, decSwaps(params));
                            default:
                                break;
                        }
                        break;
                    }
                    case Receive newB: {
                        var c = newB.getContinuation();
                        switch ( c) {
                            case Send cont:{
                                return new Send(cont.receiver, cont.expression, fuzzProcess(new Receive(newB.sender, cont.getContinuation()) , size - 1, decSwaps(params)));
                            }
                            case Receive cont: {
                                return new Receive(cont.sender, fuzzProcess(new Receive(newB.sender, cont.getContinuation()) , size - 1, decSwaps(params)));
                            }
                            case Selection cont:{
                                return new Selection(cont.receiver, cont.label, fuzzProcess(new Receive(newB.sender, cont.getContinuation()) , size - 1, decSwaps(params)));
                            }
                            case Offering cont: {
                                var splitParams = splitParams(decSwaps(params), mkSizes( new ArrayList<>(cont.branches.keySet()), processSizes(cont.branches) ) );
                                var branches = new HashMap<String, Behaviour>();
                                var chosen = randomElement(cont.branches.keySet());
                                splitParams.forEach ((label, lblParams ) -> {
                                    if (label.equals(chosen)) {
                                        branches.put(label, fuzzProcess(
                                                new Receive(newB.sender, cont.branches.get(label)),
                                        ProcessSize.compute(cont.branches.get(label))+1,
                                                lblParams
                                                ));
                                    } else
                                        branches.put(label, fuzzProcess(cont.branches.get(label), ProcessSize.compute(cont.branches.get(label)), lblParams));
                                });
                                return new Offering(cont.sender, branches);
                            }
                            case Condition cont: {
                                var splitParams = splitParams(decSwaps(params), mkSizes( List.of("then", "else"), List.of( ProcessSize.compute(cont.thenBehaviour), ProcessSize.compute(cont.elseBehaviour) ) ));
                                return new Condition(
                                        cont.expression,
                                        fuzzProcess( new Receive(newB.sender, cont.thenBehaviour) , ProcessSize.compute(cont.thenBehaviour), splitParams.get("then")),
                                fuzzProcess( cont.elseBehaviour, ProcessSize.compute(cont.elseBehaviour), splitParams.get("else") )
                                );
                            }
                            case Termination t: return fuzzProcess(c, 0, decSwaps(params));
                            case ProcedureInvocation pi: return fuzzProcess(c, 1, decSwaps(params));
                            default:
                                break;
                        }
                        break;
                    }
                    case Selection newB: {
                        Behaviour c = newB.getContinuation();
                        switch ( c) {
                            case Send cont:{
                                return new Send(cont.receiver, cont.expression, fuzzProcess(new Selection(newB.receiver, newB.label, cont.getContinuation()) , size - 1, decSwaps(params)));
                            }
                            case Receive cont:{
                                return new Receive(cont.sender, fuzzProcess(new Selection(newB.receiver, newB.label, cont.getContinuation()) , size - 1, decSwaps(params)));
                            }
                            case Selection cont:{
                                return new Selection(cont.receiver, cont.label, fuzzProcess(new Selection(newB.receiver, newB.label, cont.getContinuation()) , size - 1, decSwaps(params)));
                            }
                            case Offering cont: {
                                var splitParams = splitParams(decSwaps(params), mkSizes( new ArrayList<>(cont.branches.keySet()), processSizes(cont.branches)));
                                var branches = new HashMap<String, Behaviour>();
                                var chosen = randomElement(cont.branches.keySet());
                                splitParams.forEach ((label, lblParams ) -> {
                                    if (label.equals(chosen))
                                        branches.put(label, fuzzProcess(
                                                new Selection(newB.receiver, newB.label, cont.branches.get(label)),
                                    ProcessSize.compute(cont.branches.get(label)) + 1,
                                            lblParams
                                                ));
                                            else
                                    branches.put(label, fuzzProcess(cont.branches.get(label), ProcessSize.compute(cont.branches.get(label)), lblParams));
                                });
                                return new Offering(cont.sender, branches);
                            }
                            case Condition cont: {
                                var splitParams = splitParams(decSwaps(params), mkSizes( List.of("then", "else"), List.of( ProcessSize.compute(cont.thenBehaviour), ProcessSize.compute(cont.elseBehaviour) ) ));
                                return new Condition(
                                        cont.expression,
                                        fuzzProcess( new Selection(newB.receiver, newB.label, cont.thenBehaviour) , ProcessSize.compute(cont.thenBehaviour), splitParams.get("then") ),
                                fuzzProcess( cont.elseBehaviour, ProcessSize.compute(cont.elseBehaviour), splitParams.get("else") )
                                );
                            }
                            case Termination t: return fuzzProcess(c, 0, decSwaps(params));
                            case ProcedureInvocation pi: return fuzzProcess(c, 1, decSwaps(params));
                            default:
                                break;
                        }
                        break;
                    }
                    case Offering newB: {
                        var chosen = randomElement(newB.branches.keySet());
                        var cont = newB.branches.get(chosen);
                        var branches = new HashMap<String, Behaviour>();
                        newB.branches.keySet().forEach ((label ) -> {
                            if (label.equals(chosen))
                                switch ( cont ) {
                                    case Send s: branches.put(label, ((Send)cont).getContinuation()); break;
                                    case Receive r: branches.put(label, ((Receive)cont).getContinuation()); break;
                                    case Selection s: branches.put(label, ((Selection)cont).getContinuation()); break;
                                    case Offering o:{
                                        var keylist = new ArrayList<>(o.branches.keySet());
                                        keylist.sort(null);
                                        branches.put(label, o.branches.get(keylist.get(0)));
                                        break;
                                    }
                                    case Condition condition: branches.put(label, ((Condition)cont).thenBehaviour); break;
                                    case Termination t: throw new IllegalStateException("Reached termination");
                                    case ProcedureInvocation pi: branches.put(label, Termination.instance); break;
                                    default: throw new IllegalStateException("Unreachable case");
                            }
                            else
                                branches.put(label, newB.branches.get(label));
                        });
                        
                        switch (cont ) {
                            case Send contT: {
                                return new Send( contT.receiver, contT.expression, fuzzProcess(new Offering(newB.sender, branches), ProcessSize.compute(new Offering(newB.sender, branches)), decSwaps(params)));
                            }
                            case Receive contT: {
                                return new Receive( contT.sender, fuzzProcess(new Offering(newB.sender, branches), ProcessSize.compute(new Offering(newB.sender, branches)), decSwaps(params)) );
                            }
                            case Selection ContT: {
                                var contT = (Selection)cont;
                                return new Selection(contT.receiver, contT.label, fuzzProcess(new Offering(newB.sender, branches), ProcessSize.compute(new Offering(newB.sender, branches)), decSwaps(params)));
                            }
                            case Offering contT: {
                                var splitParams = splitParams(decSwaps(params), mkSizes( new ArrayList<>(contT.branches.keySet()), processSizes(contT.branches)));
                                var newBranches = new HashMap<String, Behaviour>();
                                var keylist = new ArrayList<>(contT.branches.keySet());
                                keylist.sort(null);
                                splitParams.forEach ((label, lblParams ) -> {
                                    if (label.equals(keylist.get(0)))
                                        newBranches.put(label, fuzzProcess(
                                                new Offering( newB.sender, branches ),
                                                ProcessSize.compute(new Offering( newB.sender, branches )),
                                                lblParams
                                        ));
                                    else
                                        newBranches.put(label, fuzzProcess(contT.branches.get(label), ProcessSize.compute(contT.branches.get(label)), lblParams));
                                });
                                return new Offering(contT.sender, newBranches);
                            }
                            case Condition contT: {
                                var splitParams = splitParams(decSwaps(params), mkSizes( List.of("then", "else"), List.of( ProcessSize.compute(contT.thenBehaviour), ProcessSize.compute(contT.elseBehaviour) ) ));
                                new Condition(
                                        contT.expression,
                                        fuzzProcess(new Offering(newB.sender, branches), ProcessSize.compute(new Offering(newB.sender, branches)), splitParams.get("then")),
                                fuzzProcess(contT.elseBehaviour, ProcessSize.compute(contT.elseBehaviour), splitParams.get("else"))
                                );
                                return fuzzProcess(cont, 0, decSwaps(params));
                            }
                            case Termination t: return fuzzProcess(cont, 0, decSwaps(params));
                            case ProcedureInvocation pi: return fuzzProcess(cont, 1, decSwaps(params));
                            default: throw new IllegalStateException("Unreachable case");
                        }
                    }
                    case Termination t: throw new IllegalStateException("Reached termination");
                    case ProcedureInvocation pi: return fuzzProcess(Termination.instance, 0, decSwaps(params));
                    case Condition newB:{
                        var t = newB.thenBehaviour;
                        switch ( t) {
                            case Send cont: {
                                return new Send(cont.receiver, cont.expression, fuzzProcess(
                                        new Condition(newB.expression, newB.thenBehaviour, newB.elseBehaviour),
                                        ProcessSize.compute(new Condition(newB.expression, newB.thenBehaviour, newB.elseBehaviour)),
                                        decSwaps(params)));
                            }
                            case Receive cont: {
                                return new Receive(cont.sender, fuzzProcess(
                                        new Condition(newB.expression, newB.thenBehaviour, newB.elseBehaviour),
                                        ProcessSize.compute(new Condition(newB.expression, newB.thenBehaviour, newB.elseBehaviour)),
                                        decSwaps(params)));
                            }
                            case Selection cont: {
                                return new Selection( cont.receiver, cont.label,
                                        fuzzProcess(
                                                new Condition(newB.expression, newB.thenBehaviour, newB.elseBehaviour),
                                                ProcessSize.compute(new Condition(newB.expression, newB.thenBehaviour, newB.elseBehaviour)),
                                                decSwaps(params)));
                            }
                            case Offering cont: {
                                var splitParams = splitParams(decSwaps(params), mkSizes( new ArrayList<>(cont.branches.keySet()), processSizes(cont.branches)));
                                var newBranches = new HashMap<String, Behaviour>();
                                splitParams.forEach ((label, lblParams ) -> {
                                    var keylist = new ArrayList<>(cont.branches.keySet());
                                    keylist.sort(null);
                                    if (label.equals(keylist.get(0)))
                                        newBranches.put(label, fuzzProcess(
                                                new Condition(newB.expression, newB.thenBehaviour, newB.elseBehaviour),
                                                ProcessSize.compute(new Condition(newB.expression, newB.thenBehaviour, newB.elseBehaviour)),
                                                lblParams)
                                        );
                                    else
                                        newBranches.put(label, fuzzProcess(cont.branches.get(label), ProcessSize.compute(cont.branches.get(label)), lblParams));
                                });
                                return new Offering(cont.sender, newBranches);
                            }
                            case Condition cont: {
                                var splitParams = splitParams(decSwaps(params), mkSizes( List.of("then", "else"), List.of( ProcessSize.compute(cont.thenBehaviour), ProcessSize.compute(cont.elseBehaviour) ) ));
                                new Condition(
                                        cont.expression,
                                        fuzzProcess(new Condition(newB.expression, newB.thenBehaviour, newB.elseBehaviour), 
                                                ProcessSize.compute(new Condition(newB.expression, newB.thenBehaviour, newB.elseBehaviour)), 
                                                splitParams.get("then")),
                                fuzzProcess(cont.elseBehaviour, ProcessSize.compute(cont.elseBehaviour), splitParams.get("else"))
                                );
                                return fuzzProcess(t, 0, decSwaps(params));
                            }
                            case Termination term: return fuzzProcess(t, 0, decSwaps(params));
                            case ProcedureInvocation pi: return fuzzProcess(t, 1, decSwaps(params));
                            default: throw new IllegalStateException("Unreachable case");
                        }
                    }
                    default: throw new IllegalStateException("Unreachable code");
                }
            }
            throw new IllegalStateException("Unreachable code");
        } else {
            // We fuzz the continuation;
            switch (b) {
                case Send newB: {
                    return new Send(newB.receiver, newB.expression, fuzzProcess(newB.getContinuation(), size - 1, params));
                }
                case Receive newB: {
                    return new Receive(newB.sender, fuzzProcess(newB.getContinuation(), size - 1, params));
                }
                case Selection newB: {
                    return new Selection(newB.receiver, newB.label, fuzzProcess(newB.getContinuation(), size - 1, params));
                }
                case Offering newB: {
                    var splitParams = splitParams(params, mkSizes(new ArrayList<>(newB.branches.keySet()), processSizes(newB.branches)));
                    var branches = new HashMap<String, Behaviour>();
                    splitParams.forEach ((label, lblParams ) ->
                            branches.put(label, fuzzProcess(newB.branches.get(label), ProcessSize.compute(newB.branches.get(label)), lblParams)));
                    return new Offering(newB.sender, branches);
                }
                case Condition newB: {
                    var splitParams = splitParams(params, mkSizes(List.of("then", "else"), List.of(ProcessSize.compute(newB.thenBehaviour), ProcessSize.compute(newB.elseBehaviour))));
                    return new Condition(
                            newB.expression,
                            fuzzProcess(newB.thenBehaviour, ProcessSize.compute(newB.thenBehaviour), splitParams.get("then")),
                    fuzzProcess(newB.elseBehaviour, ProcessSize.compute(newB.elseBehaviour), splitParams.get("then"))
                    );
                }
                case Termination t: throw new IllegalStateException("Reached termination");
                case ProcedureInvocation pi: throw new IllegalStateException("Reached procedure call");
                default: throw new IllegalStateException("Unreachable code");
            }
        }
    }

    private static Map<String, FuzzerParams> splitParams(FuzzerParams params, Map<String, Integer> sizes) {
        var paramsMap = new HashMap<String, FuzzerParams>();
        sizes.keySet().forEach(it -> paramsMap.put(it, new FuzzerParams(0, 0)));

        var dels = params.deletions;
        var swaps = params.swaps;
        while(dels + swaps > 0) {
            var chooseDel = random.nextInt(dels + swaps) < dels;

            var flag = false;
            while( !flag ) {
                var random = randomElement(paramsMap.keySet());
                if ( paramsMap.get(random).deletions + paramsMap.get(random).swaps < sizes.get(random)) {
                    if ( chooseDel ) {
                        paramsMap.get(random).deletions++;
                        dels--;
                    } else {
                        paramsMap.get(random).swaps++;
                        swaps--;
                    }
                    flag = true;
                }
            }
        }

        return paramsMap;
    }
}
