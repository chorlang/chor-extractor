package utility.ChorGen;

/*
 * Procedure call (no continuation).
 */
public class CallNode implements ChoreographyNode {

    private String procedure;

    public CallNode(String procedure) {
        this.procedure = procedure;
    }

    /*
     * Getters (and no setters).
     */
    public String getProcedure() {
        return procedure;
    }

    /*
     * For implementing the visitor pattern.
     */
    public void accept(CNVisitor v) {
        v.visit(this);
    }

    public String toString() {
        return procedure;
    }

}
