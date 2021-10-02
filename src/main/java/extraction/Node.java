package extraction;

import extraction.network.Network;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Slightly hacky way of implementing Nodes for the graph. Sorry.
 *
 * Defines a method getNodeTypes which returns an enum of concrete or invocation.
 * This interface contains two nested classes implementing it. ConcreteNode and InvocationNode.
 */
public interface Node {

    /*record ConcreteNode(Network network, String choicePath, int ID, int flipCounter, HashMap<String, Boolean> marking) implements Node {}

    record InvocationNode(String procedureName, Node node) implements Node {}
*/
    /*enum NodeType{
        CONCRETE, INVOCATION
    }

    //NodeType getNodeType();
*/
    class ConcreteNode implements Node{
        public Network network;
        public String choicePath;
        public int ID;
        public int flipCounter;
        public HashMap<String, Boolean> marking;

        public ConcreteNode(Network network, String choicePath, int ID, int flipCounter, HashMap<String, Boolean> marking){
            this.network = network;
            this.choicePath = choicePath;
            this.ID = ID;
            this.flipCounter = flipCounter;
            this.marking = marking;
        }

        /*@Override
        public NodeType getNodeType() {
            return NodeType.CONCRETE;
        }*/
    }

    class InvocationNode implements Node{
        public String procedureName;
        public Node node;

        public InvocationNode(String procedureName, Node node){
            this.procedureName = procedureName;
            this.node = node;
        }

/*        @Override
        public NodeType getNodeType(){
            return NodeType.INVOCATION;
        }*/
    }
}
