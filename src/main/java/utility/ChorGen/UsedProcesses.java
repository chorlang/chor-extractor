package utility.ChorGen;

import java.util.HashSet;

/*
 * Class for computing processesInChoreography used in a eta, using the visitor pattern.
 * Deliberately ignores recursive calls.
 */
public class UsedProcesses implements CNVisitor {

    private HashSet<String> processes;

    public UsedProcesses() {
        processes = new HashSet<String>();
    }

    /*
     * Nearly everything can change the value of the attribute...
     */
    public void visit(TerminationNode n) {}
    public void visit(NothingNode n) {}

    public void visit(CommunicationNode n) {
        processes.add(n.getSender());
        processes.add(n.getReceiver());
        n.getNextAction().accept(this);
    }

    public void visit(SelectionNode n) {
        processes.add(n.getSender());
        processes.add(n.getReceiver());
        n.getNextAction().accept(this);
    }

    public void visit(ConditionalNode n) {
        processes.add(n.getDecider());
	n.getPreAction().accept(this);
        n.getThenAction().accept(this);
        n.getElseAction().accept(this);
    }

    public void visit(CallNode n) {}

    /*
     * Method for running the algorithm.
     */
    public static HashSet<String> run(ChoreographyNode n) {
        UsedProcesses runObject = new UsedProcesses();
        n.accept(runObject);
        return runObject.processes;
    }
}
