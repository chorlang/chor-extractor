package extraction;

import extraction.choreography.*;
import extraction.Node.*;
import org.jgrapht.graph.DirectedPseudograph;

import java.util.*;

public class ChoreographyBuilder {
    private final DirectedPseudograph<Node, Label> graph;
    //private List<ProcedureHead> procedureHeads;
    private ChoreographyBuilder(DirectedPseudograph<Node, Label> graph){
        this.graph = graph;
    }

    static Choreography buildChoreography(ConcreteNode rootNode, DirectedPseudograph<Node, Label> graph){
        //Loops are assumed to have >1 incoming edges. Add extra incoming edge to root to avoid special case.
        var dummyNode = new Node(){};
        var dummyLabel = new Label() {};
        graph.addVertex(dummyNode);
        graph.addEdge(dummyNode, rootNode, dummyLabel);
        var builder = new ChoreographyBuilder(graph);
        List<ProcedureHead> procedureHeads = builder.unrollGraph();

        //If the rootNode is at the beginning of a loop / start of a procedure, we would like the
        //main Choreography to be a ProcedureInvocation of that procedure.
        ChoreographyBody main = null;
        for (var procedureHead : procedureHeads){
            if (procedureHead.top == rootNode){
                main = new ProcedureInvocation(procedureHead.procedureName, procedureHead.parameters);
                break;
            }
        }
        //If the root is not the start of a procedure, just build it normally.
        if (main == null)
            main = builder.buildChoreographyBody(rootNode);

        //Build all ProcedureDefinitions
        var procedures = new ArrayList<ProcedureDefinition>();
        procedureHeads.forEach(ph -> procedures.add(new ProcedureDefinition(
                ph.procedureName,
                ph.parameters,
                builder.buildChoreographyBody(ph.top))
        ));

        return new Choreography(main, procedures);
    }

    private ChoreographyBody buildChoreographyBody(Node node){
        var edges = graph.outgoingEdgesOf(node);

        switch (edges.size()) {
            case 0: {
                if (node instanceof ConcreteNode concreteNode)
                    return terminationOrException(concreteNode);
                if (node instanceof InvocationNode invocation)
                    return new ProcedureInvocation(invocation.procedureName, invocation.parameters);
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

    private ArrayList<ProcedureHead> unrollGraph(){
        //ID of the next procedure to add
        int procedureID = 1;
        //List of the nodes at the start/head of procedures. The top of loops
        var procedureHeads = new ArrayList<ProcedureHead>();

        //Find set of nodes that are at the top/beginning of a loop in the graph.
        for (var node : graph.vertexSet()){
            if (node instanceof ConcreteNode concreteNode && graph.incomingEdgesOf(node).size() > 1) {
                //Find the parameters for the procedure
                List<String> parameters = getParameters(concreteNode);
                procedureHeads.add(new ProcedureHead("X" + procedureID++, parameters, concreteNode));
            }
        }

        //Do the unrolling
        for (ProcedureHead procedureHead : procedureHeads) {
            ConcreteNode node = procedureHead.top;
            List<String> parameters = procedureHead.parameters;
            String procedureName = procedureHead.procedureName;

            //It needs a new set to avoid ConcurrentModificationException
            Set<Label> incomingEdges = new HashSet<>(graph.incomingEdgesOf(node));
            for (Label label : incomingEdges) {
                //Remove the loop from the graph. Remember where it started
                Node sourceNode = graph.getEdgeSource(label);
                graph.removeEdge(label);

                //Get the mapping from parameter values to parameters. Make it if needed
                //The "" is Map.of() is necessary to ensure the map is of the correct type
                //Reverse to get mapping from parameters to parameter values
                var mapping = reverseMap(label.becomes != null ? label.becomes : Map.of("", ""));

                //Create a list of all parameter values. If a parameter value is not provided, then
                //a process has the same name as the parameter, and can be used in its stead
                List<String> parameterValues = parameters.stream().map(parameter ->
                        mapping.getOrDefault(parameter, parameter)
                ).toList();

                //Create a new InvocationNode with the parameters for this particular invocation
                var invocationNode = new InvocationNode(procedureName, parameterValues, node);
                //And add it to the graph. Point the previously looping edge to this node.
                graph.addVertex(invocationNode);
                graph.addEdge(sourceNode, invocationNode, label);
            }
        }

        return procedureHeads;
    }
    private Map<String, String> reverseMap(Map<String, String> original){
        HashMap<String, String> map = new HashMap<>(original.size());
        original.forEach((key, value) -> {
            if (map.put(value, key) != null)
                throw new IllegalStateException("A ProcedureInvocation could not be generated, because multiple " +
                        "processes would need to be passed to the same parameter. Parameter map is: " + original);
        });
        return map;
    }
    private List<String> getParameters(ConcreteNode node){
        var parameters = new LinkedHashSet<String>();
        graph.incomingEdgesOf(node).forEach(label -> parameters.addAll(label.becomes.values()));
        return parameters.stream().sorted().toList();
    }
    private record ProcedureHead(String procedureName, List<String> parameters, ConcreteNode top){}
}
