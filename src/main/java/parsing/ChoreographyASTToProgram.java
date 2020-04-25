package parsing;

import antlrgen.ChoreographyBaseVisitor;
import antlrgen.ChoreographyParser.*;
import extraction.choreography.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class ChoreographyASTToProgram extends ChoreographyBaseVisitor<ChoreographyASTNode> {
    private int iteration = 0;
    private ArrayList<HashSet<String>> processesInChoreography = new ArrayList<>();

    static Program toProgram(ParseTree tree){
        return (Program)(new ChoreographyASTToProgram().getProgram(tree));
    }

    public ChoreographyASTNode getProgram(ParseTree tree){
        return this.visit(tree);
    }

    @Override public ChoreographyBody  visitCommunication(CommunicationContext ctx) {
        var sender = ctx.process(0).getText();
        var receiver = ctx.process(1).getText();
        var expression = ctx.expression().getText();

        processesInChoreography.get(iteration).add(sender);
        processesInChoreography.get(iteration).add(receiver);

        var continuation = visit(ctx.behaviour());

        return new Communication(sender, receiver, expression, (ChoreographyBody)continuation);
    }

    @Override public ChoreographyBody  visitSelection(SelectionContext ctx) {
        var sender = ctx.process(0).getText();
        var receiver = ctx.process(1).getText();
        var expression = ctx.expression().getText();

        processesInChoreography.get(iteration).add(sender);
        processesInChoreography.get(iteration).add(receiver);

        var continuation = visit(ctx.behaviour());

        return new Selection(sender, receiver, expression, (ChoreographyBody)continuation);
    }

    @Override public ChoreographyBody  visitCondition(ConditionContext ctx) {
        var process = ctx.process().getText();
        var expression = ctx.expression().getText();

        processesInChoreography.get(iteration).add(process);

        var thenChoreography = visit(ctx.behaviour(0));
        var elseChoreography = visit(ctx.behaviour(1));

        return new Condition(process, expression, (ChoreographyBody)thenChoreography, (ChoreographyBody)elseChoreography);
    }

    @Override public ChoreographyASTNode  visitChoreography(ChoreographyContext ctx) {
        var procedures = new ArrayList<ProcedureDefinition>();
        ctx.procedureDefinition().stream().forEach(i -> procedures.add((ProcedureDefinition)visit(i)));
        return new Choreography((ChoreographyBody)visit(ctx.main()), procedures, processesInChoreography.get(iteration));
    }

    @Override public ChoreographyASTNode  visitProgram(ProgramContext ctx) {
        var choreographyList = new ArrayList<Choreography>();
        for (var choreography : ctx.choreography()){
            processesInChoreography.add(new HashSet<>());
            choreographyList.add((Choreography)visit(choreography));
            iteration++;
        }
        return new Program(choreographyList, new ArrayList<>());
    }

    @Override public ChoreographyASTNode  visitProcedureDefinition(ProcedureDefinitionContext ctx) {
        return new ProcedureDefinition(ctx.procedure().getText(), (ChoreographyBody)visit(ctx.behaviour()), processesInChoreography.get(iteration));
    }

    @Override public ChoreographyASTNode  visitMain(MainContext ctx) {
        return visit(ctx.behaviour());
    }

    @Override public ChoreographyASTNode  visitProcedureInvocation(ProcedureInvocationContext ctx) {
        var procedureName = ctx.procedure().getText();
        return new ProcedureInvocation(procedureName);
    }

    @Override public ChoreographyASTNode  visitTerminal(TerminalNode node) {
        return Termination.getInstance();
    }
    
}
