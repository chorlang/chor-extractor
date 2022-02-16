package utility.ChorGen;

/*
 * Class for deciding whether two nodes represent the same body, using the visitor pattern.
 */
public class Equals implements CNVisitor {

    private boolean equals;
    private ChoreographyNode other;

    public Equals(ChoreographyNode other) {
        this.other = other;
        equals = true;
    }

    public void visit(TerminationNode n) {
        if (!(other instanceof TerminationNode))
            equals = false;
    }

    public void visit(NothingNode n){
        if (!(other instanceof NothingNode))
            equals = false;
    }

    public void visit(CommunicationNode n) {
        if (!(other instanceof CommunicationNode)) {
            equals = false;
            return;
        }
        CommunicationNode testing = (CommunicationNode) other;
        if (!n.getSender().equals(testing.getSender()))
            equals = false;
        if (!n.getReceiver().equals(testing.getReceiver()))
            equals = false;
        if (!n.getMessage().equals(testing.getMessage()))
            equals = false;
	if (equals) {
	    other = testing.getNextAction();
	    n.getNextAction().accept(this);
	}
    }

    public void visit(SelectionNode n) {
        if (!(other instanceof SelectionNode)) {
            equals = false;
            return;
        }
        SelectionNode testing = (SelectionNode) other;
        if (!n.getSender().equals(testing.getSender()))
            equals = false;
        if (!n.getReceiver().equals(testing.getReceiver()))
            equals = false;
        if (!n.getLabel().equals(testing.getLabel()))
            equals = false;
	if (equals) {
	    other = testing.getNextAction();
	    n.getNextAction().accept(this);
	}
    }

    public void visit(ConditionalNode n) {
        if (!(other instanceof ConditionalNode)) {
            equals = false;
            return;
        }
        ConditionalNode testing = (ConditionalNode) other;
        if (!n.getDecider().equals(testing.getDecider()))
            equals = false;
        if (!n.getCondition().equals(testing.getCondition()))
            equals = false;
        other = testing.getPreAction();
        n.getPreAction().accept(this);
        if (!equals) return; // no point in continuing
        other = testing.getThenAction();
        n.getThenAction().accept(this);
        if (!equals) return; // no point in continuing
        other = testing.getElseAction();
        n.getElseAction().accept(this);
    }

    public void visit(CallNode n) {
        if (!(other instanceof CallNode)) {
            equals = false;
            return;
        }
        if (!n.getProcedure().equals(((CallNode)other).getProcedure()))
            equals = false;
    }

    /*
     * Method for running the algorithm.
     */
    public static boolean run(ChoreographyNode n, ChoreographyNode other) {
        Equals runObject = new Equals(other);
        n.accept(runObject);
        return runObject.equals;
    }

}
