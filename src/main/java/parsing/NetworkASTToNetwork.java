package parsing;

import antlrgen.NetworkBaseVisitor;
import extraction.network.*;
import antlrgen.NetworkParser.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Class used to convert the antlr parse tree into this program's internal network representation.
 */
class NetworkASTToNetwork extends NetworkBaseVisitor<NetworkASTNode> {
    private static final NetworkASTToNetwork instance = new NetworkASTToNetwork();

    /**
     * Converts a parse tree to the Network representation used internally for extraction.
     * @param parseTree The parse tree for the input network
     * @return A Network object, used internally for extraction.
     */
    static Network toNetwork(NetworkContext parseTree){
        return (Network)(instance.visit(parseTree));
    }

    @Override
    public NetworkASTNode visit(ParseTree tree) {
        if (tree == null)
            return Behaviour.NoneBehaviour.instance;
        return super.visit(tree);
    }

    @Override public NetworkASTNode visitNetwork(NetworkContext ctx){
        int numOfProcesses = ctx.processBehaviour().size();
        HashMap<String, ProcessTerm> networkMapping = new HashMap<>(numOfProcesses);
        for (int i = 0; i < numOfProcesses; i++){
            networkMapping.put(ctx.process(i).getText(), (ProcessTerm) visit(ctx.processBehaviour(i)));
        }
        return new Network(networkMapping);
    }

    @Override public NetworkASTNode visitProcessBehaviour(ProcessBehaviourContext ctx){
        int numOfProcedures = ctx.procedureDefinition().size();
        HashMap<String, Behaviour> procedures = new HashMap<>(numOfProcedures);
        HashMap<String, List<String>> parameters = new HashMap<>(numOfProcedures);
        for (ProcedureDefinitionContext pctx : ctx.procedureDefinition()){
            String procedureName = pctx.procedure().getText();
            procedures.put(procedureName, (Behaviour) visit(pctx.behaviour()));
            var parametersctx = pctx.parameters();
            if (parametersctx != null && parametersctx.parameterList() != null)
                parameters.put(procedureName, Arrays.stream(parametersctx.parameterList().getText().split(",")).toList());
            else
                parameters.put(procedureName, List.of());
        }

        return new ProcessTerm(procedures, parameters, (Behaviour) visit(ctx.behaviour()));
    }

    @Override public NetworkASTNode visitNothing(NothingContext ctx){
        //Branching behaviours may omit their continuation
        //Commented out because it should be handled by visit() as defined above, but keeping it just in case
        /*if (ctx.parent instanceof ConditionContext ||
                ctx.parent instanceof ProcedureInvocationContext ||
                ctx.parent instanceof OfferingContext)
            return Behaviour.NoneBehaviour.instance;*/
        //Other behaviours must break out to a branching behaviours continuation (unless they terminate or loop)
        return Behaviour.BreakBehaviour.instance;
    }

    @Override public NetworkASTNode visitSending(SendingContext ctx){
        return new Send(ctx.process().getText(), ctx.expression().getText(), (Behaviour) visit(ctx.behaviour()));
    }

    @Override public NetworkASTNode visitReceiving(ReceivingContext ctx){
        return new Receive(ctx.process().getText(), (Behaviour) visit(ctx.behaviour()));
    }

    @Override public NetworkASTNode visitSelection(SelectionContext ctx){
        return new Selection(ctx.process().getText(), ctx.expression().getText(), (Behaviour) visit(ctx.behaviour()));
    }

    @Override public NetworkASTNode visitOffering(OfferingContext ctx){
        HashMap<String, Behaviour> labeledBehaviours = new HashMap<>();
        for (LabeledBehaviourContext label : ctx.labeledBehaviour()){
            labeledBehaviours.put(label.expression().getText(), (Behaviour) visit(label.behaviour()));
        }
        return new Offering(ctx.process().getText(), labeledBehaviours, (Behaviour) visit(ctx.continuation));
    }

    @Override public NetworkASTNode visitIntroduce(IntroduceContext ctx){
        return new Introduce(ctx.introductee1.getText(), ctx.introductee2.getText(), (Behaviour) visit(ctx.behaviour()));
    }

    @Override public NetworkASTNode visitIntroductee(IntroducteeContext ctx){
        return new Introductee(ctx.introducer.getText(), ctx.introducedProcess.getText(), (Behaviour) visit(ctx.behaviour()));
    }

    @Override public NetworkASTNode visitCondition(ConditionContext ctx){
        return new Condition(ctx.expression().getText(),
                (Behaviour) visit(ctx.thenBehaviour),
                (Behaviour) visit(ctx.elseBehaviour),
                (Behaviour) visit(ctx.continuation));
    }

    @Override public NetworkASTNode visitProcedureDefinition(ProcedureDefinitionContext ctx){
        return visit(ctx.behaviour());
    }

    @Override public NetworkASTNode visitProcedureInvocation(ProcedureInvocationContext ctx){
        String procedureName = ctx.procedure().getText();
        var parametersctx = ctx.parameters();
        List<String> parameters;
        if (parametersctx != null && parametersctx.parameterList() != null)
            parameters = Arrays.stream(parametersctx.parameterList().getText().split(",")).toList();
        else
            parameters = List.of();
        return new ProcedureInvocation(procedureName, parameters, (Behaviour) visit(ctx.continuation));
    }

    @Override public NetworkASTNode visitSpawn(SpawnContext ctx){
        String variable = ctx.process().getText();
        return new Spawn(variable, (Behaviour) visit(ctx.childbehaviour), (Behaviour) visit(ctx.continuation));
    }

    @Override public NetworkASTNode visitTerminal(TerminalNode n){
        return Termination.instance;
    }
}
