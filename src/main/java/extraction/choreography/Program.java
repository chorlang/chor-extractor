package extraction.choreography;

import java.util.List;

public class Program extends ChoreographyASTNode{
    public List<Choreography> choreographies;
    public List<GraphStatistics> statistics;

    public final Type chorType = Type.PROGRAM;

    public Program(List<Choreography> choreographies, List<GraphStatistics> statistics){
        this.choreographies = choreographies;
        this.statistics = statistics;
    }

    public String toString(){
        var builder = new StringBuilder();
        //Here it is important to use String.valueOf(choreography) (called implicit) rather than choreography.toString() to handle null values
        choreographies.forEach(choreography -> builder.append(choreography).append(" || "));
        builder.delete(builder.length() - 4, builder.length()); //Remove last four characters to remove trailing " || ".
        return builder.toString();
    }

    public static class GraphStatistics{
        public GraphStatistics(int nodeCount, int badLoopCount){
            this.nodeCount = nodeCount;
            this.badLoopCount = badLoopCount;
        }
        public int nodeCount;
        public int badLoopCount;
    }
}
