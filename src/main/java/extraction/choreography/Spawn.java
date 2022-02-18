package extraction.choreography;

public class Spawn extends ChoreographyBody{
    public final String spawner, spawned;
    final ChoreographyBody continuation;
    public Spawn(String spawner, String spawned, ChoreographyBody continuation){
        this.spawner = spawner;
        this.spawned = spawned;
        this.continuation = continuation;
    }

    @Override
    public Type getType() {
        return Type.SPAWN;
    }

    public ChoreographyBody getContinuation() { return continuation; }

    @Override
    public String toString() {
        return String.format("%s spawns %s; %s", spawner, spawned, continuation);
    }
}
