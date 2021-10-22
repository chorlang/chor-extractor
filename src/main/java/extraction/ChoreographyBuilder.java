package extraction;

import extraction.choreography.*;
import extraction.Node.*;
import org.jgrapht.graph.DirectedPseudograph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ChoreographyBuilder {
    private final DirectedPseudograph<Node, Label> graph;
    private ChoreographyBuilder(DirectedPseudograph<Node, Label> graph){
        this.graph = graph;
    }

    static Choreography buildChoreography(ConcreteNode rootNode, DirectedPseudograph<Node, Label> graph){
        //Loops are assumed to have >1 incoming edges. Add extra incoming edge to root to avoid special case.
        var dummyNode = new Node(){};
        var dummyLabel = new Label() {
            @Override
            public Label copy() {
                return null;
            }
        };
        graph.addVertex(dummyNode);
        graph.addEdge(dummyNode, rootNode, dummyLabel);
        var builder = new ChoreographyBuilder(graph);
        ArrayList<InvocationNode> invocationNodes = builder.unrollGraph();

        //If the rootNode is at the beginning of a loop / start of a procedure, we would like the
        //main Choreography to be a ProcedureInvocation of that procedure.
        ChoreographyBody main = null;
        for (var invocationNode : invocationNodes){
            if (invocationNode.node == rootNode){
                main = new ProcedureInvocation(invocationNode.procedureName);
                break;
            }
        }
        //If the root is not the start of a procedure, just build it normally.
        if (main == null)
            main = builder.buildChoreographyBody(rootNode);

        //Build all ProcedureDefinitions
        var procedures = new ArrayList<ProcedureDefinition>();
        invocationNodes.forEach(invocationNode -> procedures.add(new ProcedureDefinition(
                invocationNode.procedureName,
                builder.buildChoreographyBody(invocationNode.node)
        )));

        return new Choreography(main, procedures);
    }

    private ChoreographyBody buildChoreographyBody(Node node){
        var edges = graph.outgoingEdgesOf(node);

        switch (edges.size()) {
            case 0: {
                if (node instanceof ConcreteNode concreteNode)
                    return terminationOrException(concreteNode);
                if (node instanceof InvocationNode invocation)
                    return new ProcedureInvocation(invocation.procedureName);
                throw new IllegalStateException("Unexpected Node type: " + node.getClass().getName());
            }
            case 1: {
                Label edge = edges.iterator().next();
                Node edgeTarget = graph.getEdgeTarget(edge);
                if (edge instanceof Label.CommunicationLabel comm) {
                    return new Communication(comm.sender, comm.receiver, comm.expression,
                            buildChoreographyBody(edgeTarget));
                }
                if (edge instanceof Label.SelectionLabel select) {
                    return new Selection(select.sender, select.receiver, select.expression,
                            buildChoreographyBody(edgeTarget));
                }
                if (edge instanceof Label.MulticomLabel multicom) {
                    return new Multicom(multicom.communications,
                            buildChoreographyBody(edgeTarget));
                }
                if (edge instanceof Label.IntroductionLabel introduction) {
                    return new Introduction(introduction.introducer, introduction.leftProcess, introduction.rightProcess,
                            buildChoreographyBody(edgeTarget));
                }
                if (edge instanceof Label.SpawnLabel spawnLabel){
                    return new Spawn(spawnLabel.parent, spawnLabel.child, buildChoreographyBody(edgeTarget));
                }
                throw new IllegalStateException("Unexpected edge type: " + edge.getClass().getName());
            }
            case 2: {
                Label[] labels = edges.toArray(new Label[2]);
                if (labels[0] instanceof Label.ConditionLabel.ElseLabel)
                    labels = new Label[]{labels[1], labels[0]}; //Ensure that thenLabel is first
                if ( !( labels[0] instanceof Label.ConditionLabel.ThenLabel thenLabel &&
                        labels[1] instanceof Label.ConditionLabel.ElseLabel elseLabel))
                    throw new IllegalStateException("Node has two outgoing edges, but their labels are not then, and else labels");
                return new Condition(thenLabel.process, thenLabel.expression,
                        buildChoreographyBody(graph.getEdgeTarget(thenLabel)),
                        buildChoreographyBody(graph.getEdgeTarget(elseLabel)));
            }
            default:
                throw new IllegalStateException("Bad graph. A node has more than 2 outgoing edges.");
        }

    }
    private ChoreographyBody terminationOrException(Node.ConcreteNode node){
        for (var processTerm : node.network.processes.values()){
            if (!processTerm.isTerminated())
                throw new IllegalStateException("Bad graph: No more edges found, but not all processes where terminated");
        }
        return Termination.getInstance();
    }

    //TODO ProcedureInvocation on the Choreography side of things
    private ArrayList<InvocationNode> unrollGraph(){
        //InvocationNodes added by the unrolling
        var invocationNodes = new ArrayList<InvocationNode>();
        //ID of the next procedure to add
        int procedureID = 1;
        //The node at the start/head of procedures. The top of loops
        var procedureHeads = new HashMap<String, ConcreteNode>();

        //Find set of nodes that are at the top/beginning of a loop in the graph.
        for (var node : graph.vertexSet()){
            if (node instanceof ConcreteNode concreteNode && graph.incomingEdgesOf(node).size() > 1)
                procedureHeads.put("X" + procedureID++, concreteNode);
        }

        //Do the unrolling
        procedureHeads.forEach((key, node) -> {
            //Create new InvocationNode
            var invocationNode = new InvocationNode(key, node);
            graph.addVertex(invocationNode);
            invocationNodes.add(invocationNode);

            //Unravel loops, making them point ot the new InvocationNode instead of the top of the loop
            var incomingEdges = new HashSet<>(graph.incomingEdgesOf(node));
            incomingEdges.forEach(label -> {
                Node sourceNode = graph.getEdgeSource(label);
                graph.removeEdge(label);
                graph.addEdge(sourceNode, invocationNode, label);
            });
        });

        return invocationNodes;
    }
}
