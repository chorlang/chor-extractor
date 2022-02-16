package utility.ChorGen;

/*
 * Terminated body (no continuation).
 */
public class TerminationNode implements ChoreographyNode {

    /*
     * For implementing the visitor pattern.
     */
    public void accept(CNVisitor v) {
        v.visit(this);
    }

    public String toString() {
        return "0";
    }

}
