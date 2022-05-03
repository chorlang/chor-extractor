package utility.ChorGen;

public class IntroductionNode implements ChoreographyNode{
    private String introducer, leftProcess, rightProcess;
    private ChoreographyNode nextAction;

    public IntroductionNode(String introducer, String leftProcess, String rightProcess, ChoreographyNode nextAction) {
        this.introducer = introducer;
        this.leftProcess = leftProcess;
        this.rightProcess = rightProcess;
        this.nextAction = nextAction;
    }

    /*
     * Getters (and no setters).
     */
    public String getIntroducer() {
        return introducer;
    }

    public String getLeftProcess() {
        return leftProcess;
    }
    public String getRightProcess() {
        return rightProcess;
    }

    public ChoreographyNode getNextAction() {
        return nextAction;
    }

    /*
     * For implementing the visitor pattern.
     */
    public void accept(CNVisitor v) {
        v.visit(this);
    }

    public String toString() {
        return introducer + "." + leftProcess + " <-> " + rightProcess + "; " + nextAction.toString();
    }
}
