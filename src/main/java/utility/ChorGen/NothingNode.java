package utility.ChorGen;

/*
Represents nothing. You can think to this as a "nulL" node.
I made this because having null as a special case can get messy.

It is used for when the continuation of conditionals or selections/offering does not exist.
It is also used instead of Termination whenever the behaviour should resume a parent choreography's continuation.
 */
public class NothingNode implements ChoreographyNode {

    @Override
    public void accept(CNVisitor v) {
        v.visit(this);
    }

    public String toString(){
        return "";
    }
}
