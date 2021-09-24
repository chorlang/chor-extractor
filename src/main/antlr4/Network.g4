grammar Network;
import CommonLexerRules;

@header {
    package antlrgen;
}

network: process processBehaviour ('|' process processBehaviour)*;

processBehaviour : '{' ('def' procedure procedureDefinition)* 'main' '{' behaviour '}' '}'
    ;

procedureDefinition : '{' behaviour '}';

behaviour : interaction
    |   offering
    |   condition
    |   procedureInvocation
    |   introduce
    |   introductee
    |   TERMINATE
    ;

interaction : sending
    |   receiving
    |   selection
    ;

sending: process '!<' expression '>;' behaviour;
receiving: process '?;' behaviour;
selection: process '+' expression ';' behaviour;

offering: process '&{' (labeledBehaviour) (',' labeledBehaviour)* '}';
labeledBehaviour: expression ':' behaviour;

introduce: process '<->' process ';' behaviour;
introductee: process '?' process ';' behaviour;


condition: 'if' expression 'then' behaviour 'else' behaviour;

procedureInvocation: procedure;

expression : Identifier
    |   BooleanLiteral
    |   Wildcard
    |   INT
    ;

TERMINATE : 'stop';
