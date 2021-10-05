package extraction.network;

import extraction.Label;

import java.util.HashMap;

public class Spawn extends Behaviour{
    final String variable;
    final Behaviour processBehaviour, continuation;
    public Spawn(String variable, Behaviour processBehaviour, Behaviour continuation){
        super(Action.SPAWN);
        this.variable = variable;
        this.processBehaviour = processBehaviour;
        this.continuation = continuation;
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
        int hash = variable.hashCode() * 31;
        hash += processBehaviour.hashCode();
        return hash * 31 + continuation.hashCode();
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
    public String toString() {
        return String.format("spawn %s with %s; %s", variable, processBehaviour, continuation);
    }
}
