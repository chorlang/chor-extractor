package utility.ChorGen;

/*
 * Class for concatenating two choreographies, using the visitor pattern.
 */
public class FatSemi implements CNVisitor {

    private ChoreographyNode result;

    public FatSemi(ChoreographyNode continuation) {
        this.result = continuation;
    }

    /*
     * The attribute contains the body we want to return, so in the base cases nothing happens.
     * For the remaining eta types, we either recur or combine the results of the recursive calls.
     */
    public void visit(TerminationNode n) {} //Termination needs to be replaced, so nothing happens here

    public void visit(NothingNode n) {      //Nothing should be left alone, so it can return to a continuation
        result = new NothingNode();
    }

    public void visit(CommunicationNode n) {
        n.getNextAction().accept(this);
        result = new CommunicationNode(n.getSender(), n.getReceiver(), n.getMessage(), result);
    }

    public void visit(SelectionNode n) {
        n.getNextAction().accept(this);
        result = new SelectionNode(n.getSender(), n.getReceiver(), n.getLabel(), result);
    }

    public void visit(IntroductionNode n) {
        n.getNextAction().accept(this);
        result = new IntroductionNode(n.getIntroducer(), n.getLeftProcess(), n.getRightProcess(), result);
    }

    public void visit(SpawnNode n) {
        n.getNextAction().accept(this);
        result = new SpawnNode(n.getParent(), n.getChild(), result);
    }

    public void visit(ConditionalNode n) {
        ChoreographyNode toAppend = result;    //Save result at this point in time.
        n.getThenAction().accept(this);     //Append result to the then branch.
        ChoreographyNode realThen = result;
        result = toAppend;                     //Restore the result
        n.getElseAction().accept(this);     //Append to the else branch
        ChoreographyNode realElse = result;
        result = toAppend;                     //Restore the result
        n.getContinuation().accept(this);   //Append to the continuation
        ChoreographyNode realContinuation = result;
        result = new ConditionalNode(n.getDecider(), n.getCondition(), n.getPreAction(), realThen, realElse, realContinuation);
    }

    public void visit(CallNode n) {}

    /**
     * Returns a new ChoreographyNode where every TerminationNode of the first
     * parameter is substituted by the second.
     */
    public static ChoreographyNode run(ChoreographyNode first, ChoreographyNode second) {
        FatSemi runObject = new FatSemi(second);
        first.accept(runObject);
        return runObject.result;
    }

}
