package extraction;

import extraction.choreography.*;
import org.jgrapht.graph.DirectedPseudograph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ChoreographyBuilder {
    private DirectedPseudograph<Node, Label> graph;

    Choreography buildChoreography(Node.ConcreteNode rootNode, DirectedPseudograph<Node, Label> graph){
        this.graph = graph;
        ArrayList<Node.InvocationNode> invocationNodes = unrollGraph(rootNode);

        //Why the list? Only the first element is used. In any case it stays because of Lambda shenanigans
        var mainInvokers = new ArrayList<Node.InvocationNode>();
        invocationNodes.forEach(invNode -> {
            if (invNode.node == rootNode)
                mainInvokers.add(invNode);
        });

        //Don't really understand the main invoker thing.
        ChoreographyBody main;
        if (mainInvokers.isEmpty())
            main = buildChoreographyBody(rootNode);
        else
            main = new ProcedureInvocation(mainInvokers.get(0).procedureName);

        var procedures = new ArrayList<ProcedureDefinition>();
        for (var invNode : invocationNodes)
            procedures.add(new ProcedureDefinition(invNode.procedureName, buildChoreographyBody(invNode.node), new HashSet<>()));

        return new Choreography(main, procedures);
    }

    private ChoreographyBody buildChoreographyBody(Node node){
        var edges = graph.outgoingEdgesOf(node);

        switch (edges.size()) {
            case 0 -> {
                if (node.getNodeType() == Node.NodeType.CONCRETE)
                    return terminationOrException((Node.ConcreteNode) node);
                if (node.getNodeType() == Node.NodeType.INVOCATION)
                    return new ProcedureInvocation(((Node.InvocationNode) node).procedureName);
                throw new IllegalStateException("Unknown Node type: " + node.getClass().getName());
            }
            case 1 -> {
                Label edge = edges.iterator().next();
                if (edge.labelType == Label.LabelType.COMMUNICATION) {
                    var comm = (Label.InteractionLabel.CommunicationLabel) edge;
                    return new Communication(comm.sender, comm.receiver, comm.expression, buildChoreographyBody(graph.getEdgeTarget(edge)));
                }
                if (edge.labelType == Label.LabelType.SELECTION) {
                    var select = (Label.InteractionLabel.SelectionLabel) edge;
                    return new Selection(select.receiver, select.sender, select.expression, buildChoreographyBody(graph.getEdgeTarget(edge)));
                }
                if (edge.labelType == Label.LabelType.MULTICOM) {
                    return new Multicom(((Label.MulticomLabel)edge).communications, buildChoreographyBody(graph.getEdgeTarget(edge)));
                }
                throw new IllegalStateException("Unary edge in graph, but not of type Communication or Selection. Is of type: " + edge.getClass().getName());
            }
            case 2 -> {
                Label[] labels = edges.toArray(Label[]::new);
                if (labels[0].labelType == Label.LabelType.ELSE)
                    labels = new Label[]{labels[1], labels[0]};
                var thenLabel = (Label.ConditionLabel.ThenLabel) labels[0];
                var elseLabel = (Label.ConditionLabel.ElseLabel) labels[1];
                return new Condition(thenLabel.process, thenLabel.expression, buildChoreographyBody(graph.getEdgeTarget(thenLabel)), buildChoreographyBody(graph.getEdgeTarget(elseLabel)));
            }
            default -> throw new IllegalStateException("Bad graph. A node has more than 2 outgoing edges.");
        }

    }
    private ChoreographyBody terminationOrException(Node.ConcreteNode node){
        for (var processTerm : node.network.processes.values()){
            if (!GraphExpander.isTerminated(processTerm.main, processTerm.procedures))
                throw new IllegalStateException("Bad graph: No more edges found, but not all processes where terminated");
        }
        return Termination.getInstance();
    }

    private ArrayList<Node.InvocationNode> unrollGraph(Node.ConcreteNode rootNode){
        var invocations = new ArrayList<Node.InvocationNode>();
        int count = 1;
        var recursiveNodes = new HashMap<String, Node.ConcreteNode>();

        if (graph.incomingEdgesOf(rootNode).size() == 1)
            recursiveNodes.put("X" + count++, rootNode);

        for (var node : graph.vertexSet()){
            if (node.getNodeType() == Node.NodeType.CONCRETE && graph.incomingEdgesOf(node).size() > 1)
                recursiveNodes.put("X" + count++, (Node.ConcreteNode)node);
        }

        recursiveNodes.forEach((key, node) -> {
            var invocationNode = new Node.InvocationNode(key, node);
            graph.addVertex(invocationNode);
            invocations.add(invocationNode);

            var incomingEdges = new HashSet<>(graph.incomingEdgesOf(node));
            incomingEdges.forEach(label -> {
                var sourceNode = graph.getEdgeSource(label);
                graph.removeEdge(label);
                graph.addEdge(sourceNode, invocationNode, label);
            });
        });

        return invocations;
    }
}
