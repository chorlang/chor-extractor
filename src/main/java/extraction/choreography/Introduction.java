package extraction.choreography;

/**
 * ChoreographyBody to store the interaction between three processes, where the process
 * "introducer" which is already familiar with "process1" and "process2", makes
 * "process1" and "process2" familiar with each other, so that they may
 * communicate with each other going forward.
 */
public class Introduction extends ChoreographyBody{
    public final String introducer, process1, process2;
    public final ChoreographyBody continuation;

    /**
     * Creates an Introduction ChoreographyBody where a process makes two other processes
     * aware of each other.
     * @param introducer Name of a process already introduced to process1 and process2
     * @param process1 Name of process to be introduced to process2
     * @param process2 Name of process to be introduces to process1
     * @param continuation ChoreographyBody to continue as after the introduction
     */
    public Introduction(String introducer, String process1, String process2, ChoreographyBody continuation){
        this.introducer = introducer;
        this.process1 = process1;
        this.process2 = process2;
        this.continuation = continuation;
    }
    @Override
    public Type getType() {
        return Type.INTRODUCTION;
    }

    @Override
    public String toString() {
        return String.format("%s.%s<->%s; %s", introducer, process1, process2, continuation);
    }
}
