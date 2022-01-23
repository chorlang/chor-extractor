package extraction.choreography;

import java.util.List;

public class Program extends ChoreographyASTNode{
    public final List<Choreography> choreographies;
    public final List<GraphData> statistics;

    private final Type chorType = Type.PROGRAM;
    public Type getType(){
        return chorType;
    }

    public Program(List<Choreography> choreographies, List<GraphData> statistics){
        this.choreographies = choreographies;
        this.statistics = statistics;
    }

    public String toString(){
        var builder = new StringBuilder();
        //Here it is important to use String.valueOf(choreography) (called implicit) rather than choreography.toString() to handle null values
        choreographies.forEach(choreography -> builder.append(choreography).append(" || "));
        if (builder.isEmpty())
            builder.append("(Empty Program)1234");//The 1234 is removed at the next line
        builder.delete(builder.length() - 4, builder.length()); //Remove last four characters to remove trailing " || ".
        return builder.toString();
    }

    public static class GraphData {
        public GraphData(int nodeCount, int badLoopCount){
            this.nodeCount = nodeCount;
            this.badLoopCount = badLoopCount;
        }
        public int nodeCount;
        public int badLoopCount;
    }
}
