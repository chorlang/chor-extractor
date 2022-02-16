package utility.ChorGen;

/*
 * Conditional actions, with two possible continuations.
 */
public class ConditionalNode implements ChoreographyNode {

    private String decider, condition;
    // preAction: common prefix to both branches (see toString method)
    private ChoreographyNode preAction, thenAction, elseAction, continuation;

    public ConditionalNode(String decider, String condition, ChoreographyNode thenAction, ChoreographyNode elseAction){
        this(decider,condition,thenAction,elseAction, new NothingNode());
    }
    public ConditionalNode(String decider, String condition, ChoreographyNode thenAction, ChoreographyNode elseAction, ChoreographyNode continuation) {
        this(decider, condition, new TerminationNode(), thenAction, elseAction, continuation);
    }

    public ConditionalNode(String decider, String condition, ChoreographyNode preAction, ChoreographyNode thenAction, ChoreographyNode elseAction, ChoreographyNode continuation) {
        this.decider = decider;
        this.condition = condition;
        this.preAction = preAction;
        this.thenAction = thenAction;
        this.elseAction = elseAction;
        this.continuation = continuation;
    }

    /*
     * Getters (and no setters).
     */
    public String getDecider() {
        return decider;
    }

    public String getCondition() {
        return condition;
    }

    public ChoreographyNode getPreAction() {
        return preAction;
    }

    public ChoreographyNode getThenAction() {
        return thenAction;
    }

    public ChoreographyNode getElseAction() {
        return elseAction;
    }

    public ChoreographyNode getContinuation() { return continuation; }

    /*
     * For implementing the visitor pattern.
     */
    public void accept(CNVisitor v) {
        v.visit(this);
    }

    public String toString() {
        ChoreographyNode realThen = FatSemi.run(preAction,thenAction),
            realElse = FatSemi.run(preAction,elseAction);
        return "if " + decider + "." + condition + " then " + realThen + " else " + realElse +
                (continuation instanceof NothingNode ? " endif" : " continue " + continuation);
    }

}
