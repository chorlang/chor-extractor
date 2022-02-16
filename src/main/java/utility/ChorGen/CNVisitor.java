package utility.ChorGen;

/*
 * Visitor for body nodes.
 */
public interface CNVisitor {

    void visit(TerminationNode n);

    void visit(CommunicationNode n);

    void visit(SelectionNode n);

    void visit(ConditionalNode n);

    void visit(CallNode n);

    void visit(NothingNode n);

}
