package parsing;

import antlrgen.NetworkBaseVisitor;
import extraction.network.*;
import antlrgen.NetworkParser.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;
import java.util.List;

public class NetworkASTToNetwork extends NetworkBaseVisitor<NetworkASTNode> {
    static Network toNetwork(NetworkContext parseTree){
        return (Network)(new NetworkASTToNetwork().getNetwork(parseTree));
    }

    NetworkASTNode getNetwork(NetworkContext parseTree){
        return this.visit(parseTree);
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
        for (int i = 0; i < numOfProcedures; i++){
            procedures.put(ctx.procedure(i).getText(), (Behaviour) visit(ctx.procedureDefinition(i)));
        }
        return new ProcessTerm(procedures, (Behaviour) visit(ctx.behaviour()));
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
        return new Offering(ctx.process().getText(), labeledBehaviours);
    }

    @Override public NetworkASTNode visitIntroduce(IntroduceContext ctx){
        List<ProcessContext> processContexts = ctx.process();
        ProcessContext pc1 = processContexts.get(0);
        ProcessContext pc2 = processContexts.get(1);
        return new Introduce(pc1.getText(), pc2.getText(), (Behaviour) visit(ctx.behaviour()));
    }

    @Override public NetworkASTNode visitIntroductee(IntroducteeContext ctx){
        List<ProcessContext> processContexts = ctx.process();
        ProcessContext snd = processContexts.get(0);
        ProcessContext pid = processContexts.get(1);
        return new Introductee(snd.getText(), pid.getText(), (Behaviour) visit(ctx.behaviour()));
    }

    @Override public NetworkASTNode visitCondition(ConditionContext ctx){
        return new Condition(ctx.expression().getText(),
                (Behaviour) visit(ctx.behaviour(0)),
                (Behaviour) visit(ctx.behaviour(1)));
    }

    @Override public NetworkASTNode visitProcedureDefinition(ProcedureDefinitionContext ctx){
        return visit(ctx.behaviour());
    }

    @Override public NetworkASTNode visitProcedureInvocation(ProcedureInvocationContext ctx){
        return new ProcedureInvocation(ctx.procedure().getText());
    }

    @Override public NetworkASTNode visitTerminal(TerminalNode n){
        return Termination.instance;
    }
}
