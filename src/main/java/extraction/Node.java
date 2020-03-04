package extraction;

import network.Network;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Slightly hacky way of implementing Nodes for the graph. Sorry.
 *
 * Defines a method getNodeTypes which returns an enum of concrete or invocation.
 * This interface contains two nested classes implementing it. ConcreteNode and InvocationNode.
 */
public interface Node {
    enum NodeType{
        concrete, invocation
    }

    NodeType getNodeType();

    class ConcreteNode implements Node{
        public Network network;
        public String choicePath;
        public Integer ID;
        public HashSet<Integer> badNodes;
        public HashMap<String, Boolean> marking;

        public ConcreteNode(Network network, String choicePath, Integer ID, HashSet<Integer> badNodes, HashMap<String, Boolean> marking){
            this.network = network;
            this.choicePath = choicePath;
            this.ID = ID;
            this.badNodes = badNodes;
            this.marking = marking;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.concrete;
        }
    }

    class InvocationNode implements Node{
        public String procedureName;
        public Node node;

        public InvocationNode(String procedureName, Node node){
            this.procedureName = procedureName;
            this.node = node;
        }

        @Override
        public NodeType getNodeType(){
            return NodeType.invocation;
        }
    }
}
