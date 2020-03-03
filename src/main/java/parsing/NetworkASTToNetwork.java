package parsing;

import antlrgen.NetworkBaseVisitor;
import network.*;
import antlrgen.NetworkParser.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;

public class NetworkASTToNetwork extends NetworkBaseVisitor<Behaviour> {
    static Network toNetwork(NetworkContext parseTree){
        return (Network)(new NetworkASTToNetwork().getNetwork(parseTree));
    }

    Behaviour getNetwork(NetworkContext parseTree){
        return this.visit(parseTree);
    }



    @Override public Behaviour visitNetwork(NetworkContext ctx){
        int numOfProcesses = ctx.processBehaviour().size();
        HashMap<String, ProcessTerm> networkMapping = new HashMap<>(numOfProcesses);
        for (int i = 0; i < numOfProcesses; i++){
            networkMapping.put(ctx.process(i).getText(), (ProcessTerm) visit(ctx.processBehaviour(i)));
        }
        return new Network(networkMapping);
    }

    @Override public Behaviour visitProcessBehaviour(ProcessBehaviourContext ctx){
        int numOfProcedures = ctx.procedureDefinition().size();
        HashMap<String, Behaviour> procedures = new HashMap<>(numOfProcedures);
        for (int i = 0; i < numOfProcedures; i++){
            procedures.put(ctx.procedure(i).getText(), visit(ctx.procedureDefinition(i)));
        }
        return new ProcessTerm(procedures, visit(ctx.behaviour()));
    }

    @Override public Behaviour visitSending(SendingContext ctx){
        return new Send(ctx.process().getText(), ctx.expression().getText(), visit(ctx.behaviour()));
    }

    @Override public Behaviour visitReceiving(ReceivingContext ctx){
        return new Receive(ctx.process().getText(), visit(ctx.behaviour()));
    }

    @Override public Behaviour visitSelection(SelectionContext ctx){
        return new Selection(ctx.process().getText(), ctx.process().getText(), visit(ctx.behaviour()));
    }

    @Override public Behaviour visitOffering(OfferingContext ctx){
        HashMap<String, Behaviour> labeledBehaviours = new HashMap<>();
        for (LabeledBehaviourContext label : ctx.labeledBehaviour()){
            labeledBehaviours.put(label.expression().getText(), visit(label.behaviour()));
        }
        return new Offering(ctx.process().getText(), labeledBehaviours);
    }

    @Override public Behaviour visitCondition(ConditionContext ctx){
        return new Condition(ctx.expression().getText(),
                visit(ctx.behaviour(0)),
                visit(ctx.behaviour(1)));
    }

    @Override public Behaviour visitProcedureDefinition(ProcedureDefinitionContext ctx){
        return visit(ctx.behaviour());
    }

    @Override public Behaviour visitProcedureInvocation(ProcedureInvocationContext ctx){
        return new ProcedureInvocation(ctx.procedure().getText());
    }

    @Override public Behaviour visitTerminal(TerminalNode n){
        return Termination.getTermination();
    }
}
