package utility.ChorGen;

import extraction.choreography.Spawn;

/*
 * Class for checking that there are no self-communications in a body, using the visitor pattern.
 */
public class NoSelfCommunications implements CNVisitor {

    private boolean hasSelfies;

    public NoSelfCommunications() {
        hasSelfies = false;
    }

    /*
     * Only communications may change the value of the attribute.
     * For the remaining eta types, we either terminate or combine the results of the recursive calls.
     */
    public void visit(TerminationNode n) {}
    public void visit(NothingNode n) {}

    public void visit(CommunicationNode n) {
        if (n.getSender().equals(n.getReceiver())) {
            hasSelfies = true;
            return;
        }
        n.getNextAction().accept(this);
    }

    public void visit(SelectionNode n) {
        if (n.getSender().equals(n.getReceiver())) {
            hasSelfies = true;
            return;
        }
        n.getNextAction().accept(this);
    }

    public void visit(IntroductionNode n){
        if (n.getIntroducer().equals(n.getLeftProcess()) || n.getIntroducer().equals(n.getRightProcess())){
            hasSelfies = true;
            return;
        }
        n.getNextAction().accept(this);
    }

    public void visit(SpawnNode n){
        if (n.getParent().equals(n.getChild())){
            hasSelfies = true;
            return;
        }
        n.getNextAction().accept(this);
    }

    public void visit(ConditionalNode n) {
        n.getThenAction().accept(this);
        n.getElseAction().accept(this);
    }

    public void visit(CallNode n) {}

    /*
     * Method for running the algorithm.
     */
    public static boolean run(ChoreographyNode n) {
        NoSelfCommunications runObject = new NoSelfCommunications();
        n.accept(runObject);
        return runObject.hasSelfies;
    }

}
