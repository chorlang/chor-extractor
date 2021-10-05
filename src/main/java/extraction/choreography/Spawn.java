package extraction.choreography;

public class Spawn extends ChoreographyBody{
    final String spawner, spawned;
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

    @Override
    public String toString() {
        return String.format("%s spawns %s; %s", spawner, spawned, continuation);
    }
}
