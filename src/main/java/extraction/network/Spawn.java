package extraction.network;

import extraction.Label;

import java.util.HashMap;

public class Spawn extends Behaviour.Interaction {
    public final String variable;
    public final Behaviour processBehaviour;
    private final int hash;

    public Spawn(String variable, Behaviour processBehaviour, Behaviour continuation){
        super(Action.SPAWN, continuation);
        this.variable = variable;
        this.processBehaviour = processBehaviour;
        hash = hashValue();
    }

    Label.SpawnLabel labelFrom(String processName, HashMap<String, String> substitutions){
        return new Label.SpawnLabel(processName, substitutions.get(variable));
    }

    @Override
    Behaviour realValues(HashMap<String, String> substitutions) {
        return new Spawn(variable, processBehaviour, continuation);
    }

    @Override
    public int hashCode(){
        return hash;
    }
    private int hashValue(){
        int hash = variable.hashCode() * 31;
        hash += processBehaviour.hashCode();
        return hash ^ Integer.rotateRight(continuation.hashCode(), 1);
    }

    @Override
    public boolean equals(Behaviour other) {
        if (!(other instanceof Spawn otherSpawn))
            return false;
        return variable.equals(otherSpawn.variable) &&
                processBehaviour.equals(otherSpawn.processBehaviour) &&
                continuation.equals(otherSpawn.continuation);
    }

    @Override
    boolean compareData(Behaviour other){
        return other instanceof Spawn spawn && variable.equals(spawn.variable) &&
                processBehaviour.equals(spawn.processBehaviour);
        /*  While this function normally just compares non-behaviour data, I decided the children's behaviours
            should be compared the traditional way. It seems unlikely two spawn terms are equivalent, but
            uses continuations differently. This makes it much easier to verify the terms spawn the same
            process, even if it is based on an assumption that isn't true.
         */
    }

    @Override
    public String toString() {
        return String.format("spawn %s with %s continue %s", variable, processBehaviour, continuation);
    }
}
