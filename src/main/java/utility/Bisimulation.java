package utility;

import extraction.Label;
import extraction.Label.LabelType;
import extraction.choreography.*;
import extraction.network.Behaviour;
import parsing.Parser;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Bisimulation {
    private static final int MAX_SIMULATION_COUNTER = 200;

    public enum Throolean {
        OK,
        MAYBE,
        FAIL
    }

    public static Throolean bisimilar( String c1, String c2) {
    
        //var program1 = ((Program)new ChoreographyASTToProgram().getProgram(Parser.parseChoreography(c1)));
        //var program2 = ((Program)new ChoreographyASTToProgram().getProgram(Parser.parseChoreography(c2)));
        var program1 = Parser.stringToProgram(c1);
        var program2 = Parser.stringToProgram(c2);

        return bisimilar( program1.choreographies, program2.choreographies );
    }

    private static void println(Object o){
        System.out.println(o);
    }

//fun similar( String c1, String c2 ):Throolean;
//{
//    var program1 = new ChoreographyASTToProgram().getProgram(parseChoreography(c1)) as Program;
//    var program2 = new ChoreographyASTToProgram().getProgram(parseChoreography(c2)) as Program;
//
//    return similar( program1.choreographies, program2.choreographies );
//}

    static Throolean bisimilar(List<Choreography> list1, List<Choreography> list2) {
        if ( list1.size() != 1 || list2.size() != 1 ) {
            println("Bisimilarity not implemented for parallel choreographies");
            return Throolean.MAYBE;
        }
        switch ( similar(list1, list2) )  {
            case FAIL:
                return Throolean.FAIL;
            case OK:
                return similar(list2, list1);
            case MAYBE: {
                if (similar(list2, list1) == Throolean.FAIL) {
                    return Throolean.FAIL;
                } else {
                    return Throolean.MAYBE;
                }
            }
        }
        throw new IllegalArgumentException("ERROR: Bisimilarity check returned unknown type");
    }

    static Throolean similar( List<Choreography> list1, List<Choreography> list2 ) {
        var result = Throolean.OK;
        for( var c1 : list1 ) {
            if ( c1 == null )
                return Throolean.FAIL;

            var ok = false;
            for( var c2 : list2 ) {
                if ( c2 == null )
                    continue;

                if ( c1.processes.containsAll( c2.processes ) ) {
                    switch ( similar( c1, c2 ) ) {
                        case FAIL:
                            return Throolean.FAIL;
                        case MAYBE:
                            ok = true;
                            result = Throolean.MAYBE;
                            break;
                        case OK: 
                            ok = true;
                    }
                }
            }
            if (!ok)
                return Throolean.FAIL;
        }
        return result;
    }

    static Throolean similar(Choreography c1, Choreography c2) {
        ArrayList<Pair<ChoreographyBody, ChoreographyBody>> done = new ArrayList<>();
        ArrayList<Pair<ChoreographyBody, ChoreographyBody>> todo = new ArrayList<>();

        todo.add(new Pair<>(c1.main, c2.main));

        var counter = 0;

        while( !todo.isEmpty() && counter < MAX_SIMULATION_COUNTER ) {
            counter++;
            var firstEntry = todo.remove(0);
            var one = firstEntry.first;
            var two = firstEntry.second;
//        println("Getting actions");
            var actionsWithContinuations = getActionsWithContinuations(one, c1.procedures);
            for( var actionPair : actionsWithContinuations ) {
                var action1 = actionPair.first;
                var continuation1 = actionPair.second;
//            println("getting continuation ${two.toString().length}");
                var continuation2 = getContinuation( two, action1, c2.procedures );
                if ( continuation2 == null ) {
                    println( "Could not match $action1 with continuation $two" );
                    return Throolean.FAIL;
                } else {
                    if( !done.contains( new Pair<>(continuation1,continuation2) ) && !todo.contains( new Pair<>(continuation1, continuation2) ) ) {
//                    System.out.println( "TODO $continuation1, $continuation2 (size of todo: ${todo.size()}, size of done: ${done.size()})" );
//                    System.out.println( "TODO (size of todo: ${todo.size()}, size of done: ${done.size()})" );
                        todo.add( new Pair<>( continuation1, continuation2 ) );
                    }
                }
            }
//        System.out.println( "DONE $one, $two (size of todo: ${todo.size()}, size of done: ${done.size()})" );
//        System.out.println( "DONE (size of todo: ${todo.size()}, size of done: ${done.size()})" );
            done.add( new Pair<>( one, two ) );
        }

        if ( counter == MAX_SIMULATION_COUNTER ) {
            return Throolean.MAYBE;
        }

        return Throolean.OK;
    }

    private static Set<String> pn(Label label) {
        switch( label.labelType )  {
            case THEN:
            case ELSE:
                return Set.of(((Label.ConditionLabel)label).process);
            case COMMUNICATION:
            case SELECTION:
                var intLabel = (Label.InteractionLabel)label;
                return Set.of(intLabel.sender, intLabel.receiver);
        }
        throw new IllegalArgumentException("ERROR: Unrecognized label type " + label.getClass().getName());
    }

    private static ChoreographyBody.Interaction copyInteraction(ChoreographyBody.Interaction oldInteraction, ChoreographyBody newContinuation){
        switch (oldInteraction.getType()){
            case SELECTION:
                var s = (Selection)oldInteraction;
                return new Selection(s.sender,s.receiver,s.label, newContinuation);
            case COMMUNICATION:
                var c = (Communication)oldInteraction;
                return new Communication(c.sender, c.receiver, c.expression, newContinuation);
            default:
                throw new InvalidParameterException("ERROR: Unknown ChoreographyBody.Interaction subtype: " + oldInteraction.getClass().getName() + ". Not supported by copyInteraction in Bisimulation");
        }
    }

    static ChoreographyBody getContinuation( ChoreographyBody c, Label label, List<ProcedureDefinition> procedures) {
        switch ( c.getType() ) {
            case PROCEDURE_INVOCATION: {
                String proceduresName = ((ProcedureInvocation)c).procedure;
                var proceduresCopy = new ArrayList<ProcedureDefinition>();
                ChoreographyBody procedureBody = null;
                for( var procedure : procedures ) {
                    if( !procedure.name.equals(proceduresName) ) {
                        proceduresCopy.add( procedure );
                    } else {
                        procedureBody = procedure.body;
                    }
                }
                if ( procedureBody == null )
                    return null;
                else
                    return getContinuation( procedureBody, label, proceduresCopy );
            }
            case COMMUNICATION:
            case SELECTION:{
                var interaction = (ChoreographyBody.Interaction)c;
                if (equalLabels(labelFromInteraction(interaction), label)) {
                    return interaction.getContinuation();
                } else {
                    var lNames = pn(label);
                    if ( lNames.contains(interaction.getSender()) || lNames.contains(interaction.getReceiver()) )
                        return null;

                    var cont = getContinuation(interaction.getContinuation(), label, procedures);
                    if ( cont == null )
                        return null;
                    else{
                        return copyInteraction(interaction, cont);
                    }
                }
            }
            case CONDITION: {
                var cond = (Condition)c;
                if (label.labelType == LabelType.THEN) {
                    var condLabel = (Label.ConditionLabel) label;
                    if ( cond.process.equals(condLabel.process) && cond.expression.equals(condLabel.expression) ) {
                        return cond.thenChoreography;
                    }
                } else if (label.labelType == LabelType.ELSE){
                    var condLabel = (Label.ConditionLabel) label;
                    if ( cond.process.equals(condLabel.process) && cond.expression.equals(condLabel.expression) ) {
                        return cond.elseChoreography;
                    }
                }
                if ( pn(label).contains(cond.process) )
                    return null;

                var thenCont = getContinuation(cond.thenChoreography, label, procedures);
                var elseCont = getContinuation(cond.elseChoreography, label, procedures);
                if ( thenCont == null || elseCont == null )
                    return null;
                else
                    return new Condition( cond.process, cond.expression, thenCont, elseCont );
            }
            case TERMINATION:
            default:
                return null;
        }
    }

    static Boolean equalLabels( Label l1, Label l2 ) {
        switch( l1.labelType )  {
            case COMMUNICATION: 
                if( l2.labelType == LabelType.COMMUNICATION ) { 
                    var lx1 = (Label.InteractionLabel.CommunicationLabel)l1;
                    var lx2 = (Label.InteractionLabel.CommunicationLabel)l2;
                    return lx1.sender.equals(lx2.sender) && lx1.receiver.equals(lx2.receiver) && lx1.expression.equals(lx2.expression);
                }else 
                    return false;
            case SELECTION:
                if (l2.labelType == LabelType.SELECTION) {
                    var lx1 = (Label.InteractionLabel.SelectionLabel)l1;
                    var lx2 = (Label.InteractionLabel.SelectionLabel)l2;
                    return lx1.sender.equals(lx2.sender) && lx1.receiver.equals(lx2.receiver) && lx1.expression.equals(lx2.expression);
                }else
                    return false;
            case THEN:
                if (l2.labelType == LabelType.THEN){
                    var lx1 = (Label.ConditionLabel.ThenLabel)l1;
                    var lx2 = (Label.ConditionLabel.ThenLabel)l2;
                    return lx1.process.equals(lx2.process) && lx1.expression.equals(lx2.expression);
                }else
                    return false;
            case ELSE:
                if (l2.labelType == LabelType.ELSE){
                    var lx1 = (Label.ConditionLabel.ElseLabel)l1;
                    var lx2 = (Label.ConditionLabel.ElseLabel)l2;
                    return lx1.process.equals(lx2.process) && lx1.expression.equals(lx2.expression);
                }else
                    return false;
        }
        throw new IllegalArgumentException("Unknown label");
    }

    static List<Pair<Label, ChoreographyBody>> getActionsWithContinuations(ChoreographyBody c, List<ProcedureDefinition> procedures ){
        switch( c.getType() )  {
            case COMMUNICATION:
            case SELECTION: {
                var interaction = (ChoreographyBody.Interaction)c;
                return List.of(new Pair<>(labelFromInteraction(interaction), interaction.getContinuation()));
            }
            case CONDITION:{
            var cond = (Condition)c;
            return List.of(
                new Pair<>(new Label.ConditionLabel.ThenLabel(cond.process, cond.expression), cond.thenChoreography),
                new Pair<>(new Label.ConditionLabel.ElseLabel(cond.process, cond.expression), cond.elseChoreography));
        }
            case TERMINATION:
                return List.of();
            case PROCEDURE_INVOCATION:
                return getActionsWithContinuations(getProcedure(((ProcedureInvocation)c).procedure, procedures), procedures);
            default:
                throw new IllegalArgumentException();
        }
    }

    static ChoreographyBody getProcedure( String name, List<ProcedureDefinition> procedures ) {
        for( var procedure : procedures ) {
            if(procedure.name.equals(name)) {
                return procedure.body;
            }
        }
        throw new IllegalArgumentException( "Called a name that does not exist: $name" );
    }

    static Label labelFromInteraction(ChoreographyBody.Interaction interaction) {
        switch( interaction.getType() )  {
            case COMMUNICATION:
                return new Label.InteractionLabel.CommunicationLabel(interaction.getSender(), interaction.getReceiver(), ((Communication)interaction).expression);
            case SELECTION:
                return new Label.InteractionLabel.SelectionLabel(interaction.getSender(), interaction.getReceiver(), ((Selection)interaction).label);
            default:
                throw new IllegalArgumentException();
    }
    }
}
