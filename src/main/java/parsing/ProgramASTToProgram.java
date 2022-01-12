package parsing;

import antlrgen.ChoreographyBaseVisitor;
import antlrgen.ChoreographyParser.*;
import extraction.choreography.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Class used to convert the antlr parse tree into this program's internal program/choreography representation
 */
class ProgramASTToProgram extends ChoreographyBaseVisitor<ChoreographyASTNode> {
    private int iteration = 0;
    private final ArrayList<HashSet<String>> processesInChoreography = new ArrayList<>();

    /**
     * Converts a parse tree to the program/choreography representation used internally.
     * @param parseTree The parse tree for the input program/choreography
     * @return A Program object, used internally to represent choreographies.
     */
    static Program toProgram(ParseTree parseTree){
        return (Program)(new ProgramASTToProgram().visit(parseTree));
    }


    @Override public ChoreographyBody visitCommunication(CommunicationContext ctx) {
        var sender = ctx.process(0).getText();
        var receiver = ctx.process(1).getText();
        var expression = ctx.expression().getText();

        processesInChoreography.get(iteration).add(sender);
        processesInChoreography.get(iteration).add(receiver);

        var continuation = visit(ctx.behaviour());

        return new Communication(sender, receiver, expression, (ChoreographyBody)continuation);
    }

    @Override public ChoreographyBody visitSelection(SelectionContext ctx) {
        var sender = ctx.process(0).getText();
        var receiver = ctx.process(1).getText();
        var expression = ctx.expression().getText();

        processesInChoreography.get(iteration).add(sender);
        processesInChoreography.get(iteration).add(receiver);

        var continuation = visit(ctx.behaviour());

        return new Selection(sender, receiver, expression, (ChoreographyBody)continuation);
    }

    @Override public ChoreographyBody visitIntroduction(IntroductionContext ctx){
        List<ProcessContext> processes = ctx.process();
        String introducer = processes.get(0).getText();
        String process1 = processes.get(1).getText();
        String process2 = processes.get(2).getText();
        ChoreographyASTNode continuation = visit(ctx.behaviour());

        return new Introduction(introducer, process1, process2, (ChoreographyBody) continuation);
    }

    @Override public ChoreographyBody visitCondition(ConditionContext ctx) {
        var process = ctx.process().getText();
        var expression = ctx.expression().getText();

        processesInChoreography.get(iteration).add(process);

        var thenChoreography = visit(ctx.behaviour(0));
        var elseChoreography = visit(ctx.behaviour(1));

        return new Condition(process, expression, (ChoreographyBody)thenChoreography, (ChoreographyBody)elseChoreography);
    }

    @Override public ChoreographyASTNode visitChoreography(ChoreographyContext ctx) {
        var procedures = new ArrayList<ProcedureDefinition>();
        ctx.procedureDefinition().forEach(i -> procedures.add((ProcedureDefinition)visit(i)));
        return new Choreography((ChoreographyBody)visit(ctx.main()), procedures, processesInChoreography.get(iteration));
    }

    @Override public ChoreographyASTNode visitProgram(ProgramContext ctx) {
        var choreographyList = new ArrayList<Choreography>();
        for (var choreography : ctx.choreography()){
            processesInChoreography.add(new HashSet<>());
            choreographyList.add((Choreography)visit(choreography));
            iteration++;
        }
        return new Program(choreographyList, new ArrayList<>());
    }

    @Override public ChoreographyASTNode visitProcedureDefinition(ProcedureDefinitionContext ctx) {
        return new ProcedureDefinition(ctx.procedure().getText(), (ChoreographyBody)visit(ctx.behaviour()));
        //, processesInChoreography.get(iteration));
    }

    @Override public ChoreographyASTNode visitMain(MainContext ctx) {
        return visit(ctx.behaviour());
    }

    @Override public ChoreographyASTNode visitProcedureInvocation(ProcedureInvocationContext ctx) {
        var procedureName = ctx.procedure().getText();
        return new ProcedureInvocation(procedureName);
    }

    @Override public ChoreographyASTNode visitTerminal(TerminalNode node) {
        return Termination.getInstance();
    }
    
}
