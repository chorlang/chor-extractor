grammar Network;
import CommonLexerRules;

@header {
    package antlrgen;
}

network: process processBehaviour ('|' process processBehaviour)*;

processBehaviour : '{' procedureDefinition* 'main' '{' behaviour '}' '}';

procedureDefinition : 'def' procedure parameters? '{' behaviour '}';

behaviour : interaction
    |   offering
    |   condition
    |   procedureInvocation
    |   introduce
    |   introductee
    |   spawn
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

procedureInvocation: procedure parameters?;

parameters : '(' parameterList? ')';
parameterList : parameterList ',' parameterList | parameter;
parameter : process;

spawn : 'spawn' process 'with' behaviour 'continue' behaviour;

expression : Identifier
    |   BooleanLiteral
    |   Wildcard
    |   INT
    ;

TERMINATE : 'stop';
