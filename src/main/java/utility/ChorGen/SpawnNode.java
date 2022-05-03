package utility.ChorGen;

public class SpawnNode implements ChoreographyNode{
    private String parent, child;
    private ChoreographyNode nextAction;

    public SpawnNode(String parent, String child, ChoreographyNode nextAction) {
        this.parent = parent;
        this.child = child;
        this.nextAction = nextAction;
    }

    /*
     * Getters (and no setters).
     */
    public String getParent() {
        return parent;
    }

    public String getChild() {
        return child;
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
        return parent + " spawns " + child + "; " + nextAction.toString();
    }
}
