package parsing;

import antlrgen.ChoreographyBaseVisitor;
import antlrgen.ChoreographyParser.*;
import extraction.choreography.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Arrays;
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
        var sender = ctx.sender.getText();
        var receiver = ctx.receiver.getText();
        var expression = ctx.expression().getText();

        processesInChoreography.get(iteration).add(sender);
        processesInChoreography.get(iteration).add(receiver);

        var continuation = visit(ctx.continuation);

        return new Communication(sender, receiver, expression, (ChoreographyBody)continuation);
    }

    @Override public ChoreographyBody visitSelection(SelectionContext ctx) {
        var sender = ctx.sender.getText();
        var receiver = ctx.receiver.getText();
        var expression = ctx.expression().getText();

        processesInChoreography.get(iteration).add(sender);
        processesInChoreography.get(iteration).add(receiver);

        var continuation = visit(ctx.continuation);

        return new Selection(sender, receiver, expression, (ChoreographyBody)continuation);
    }

    @Override public ChoreographyBody visitIntroduction(IntroductionContext ctx){
        String introducer = ctx.introducer.getText();
        String process1 = ctx.leftIntroductee.getText();
        String process2 = ctx.rightIntroductee.getText();
        ChoreographyASTNode continuation = visit(ctx.continuation);

        return new Introduction(introducer, process1, process2, (ChoreographyBody) continuation);
    }

    @Override public ChoreographyBody visitCondition(ConditionContext ctx) {
        var process = ctx.process().getText();
        var expression = ctx.expression().getText();

        processesInChoreography.get(iteration).add(process);

        var thenChoreography = visit(ctx.thenBehaviour);
        var elseChoreography = visit(ctx.elseBehaviour);
        var continuation = visit(ctx.continuation);

        return new Condition(process, expression, (ChoreographyBody)thenChoreography, (ChoreographyBody)elseChoreography, (ChoreographyBody) continuation);
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
        var parametersctx = ctx.parameters();
        List<String> parameters;
        if (parametersctx != null && parametersctx.parameterList() != null)
            parameters = Arrays.stream(parametersctx.parameterList().getText().split(",")).toList();
        else
            parameters = List.of();
        return new ProcedureDefinition(ctx.procedure().getText(), parameters,
                (ChoreographyBody)visit(ctx.behaviour()));
    }

    @Override public ChoreographyASTNode visitMain(MainContext ctx) {
        return visit(ctx.behaviour());
    }

    @Override public ChoreographyASTNode visitProcedureInvocation(ProcedureInvocationContext ctx) {
        var procedureName = ctx.procedure().getText();
        var parametersctx = ctx.parameters();
        List<String> parameters;
        if (parametersctx != null && parametersctx.parameterList() != null)
            parameters = Arrays.stream(parametersctx.parameterList().getText().split(",")).toList();
        else
            parameters = List.of();
        return new ProcedureInvocation(procedureName, parameters,
                (ChoreographyBody) visit(ctx.continuation));
    }

    @Override public ChoreographyASTNode visitTerminal(TerminalNode node) {
        return Termination.getInstance();
    }

    @Override public ChoreographyASTNode visitNothing(NothingContext ctx){
        return ChoreographyBody.NoneBody.instance;
    }

    //Handle missing optional terms
    @Override public ChoreographyASTNode visit(ParseTree tree){
        if (tree == null)
            return ChoreographyBody.NoneBody.instance;
        return super.visit(tree);
    }
    
}
