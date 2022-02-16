package utility.ChorGen;

/*
 * Class for concatenating two choreographies, using the visitor pattern.
 */
public class FatSemi implements CNVisitor {

    private ChoreographyNode continuation, result;

    public FatSemi(ChoreographyNode continuation) {
        this.result = continuation;
    }

    /*
     * The attribute contains the body we want to return, so in the base cases nothing happens.
     * For the remaining eta types, we either recur or combine the results of the recursive calls.
     */
    public void visit(TerminationNode n) {}

    public void visit(NothingNode n) {}

    public void visit(CommunicationNode n) {
        n.getNextAction().accept(this);
        result = new CommunicationNode(n.getSender(), n.getReceiver(), n.getMessage(), result);
    }

    public void visit(SelectionNode n) {
        n.getNextAction().accept(this);
        result = new SelectionNode(n.getSender(), n.getReceiver(), n.getLabel(), result);
    }

    public void visit(ConditionalNode n) {
        //*
        ChoreographyNode continuation = result;
        n.getThenAction().accept(this);
        ChoreographyNode realThen = result;
        result = continuation;
        n.getElseAction().accept(this);
        ChoreographyNode realElse = result;
        result = new ConditionalNode(n.getDecider(), n.getCondition(), n.getPreAction(), realThen, realElse);//*/
        //n.getContinuation().accept(this);
        //result = new ConditionalNode(n.getDecider(), n.getCondition(), n.getPreAction(), n.getThenAction(), n.getElseAction(), result);
    }

    public void visit(CallNode n) {}

    /*
     * Method for running the algorithm.
     */
    public static ChoreographyNode run(ChoreographyNode first, ChoreographyNode second) {
        FatSemi runObject = new FatSemi(second);
        first.accept(runObject);
        return runObject.result;
    }

}
