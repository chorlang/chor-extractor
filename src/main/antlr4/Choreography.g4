grammar Choreography;
import CommonLexerRules;

@header {package antlrgen;}

program : choreography ('||' choreography)* ;

choreography: procedureDefinition* main;

procedureDefinition : 'def' procedure '{' behaviour '}';

main : 'main {' behaviour '}';

behaviour : interaction
    |   condition
    |   procedureInvocation
    |   TERMINATE
    ;

condition : 'if' process '.' expression 'then' behaviour 'else' behaviour;

procedureInvocation: procedure;

interaction : communication | selection;

communication: process '.' expression '->' process ';' behaviour;
selection: process '->' process '[' expression '];' behaviour;

expression : Identifier
    |   BooleanLiteral
    |   Wildcard
    |   INT
    ;

TERMINATE : 'stop' | '0';