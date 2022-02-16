grammar Choreography;
import CommonLexerRules;

@header {package antlrgen;}

program : choreography ('||' choreography)* ;

choreography: procedureDefinition* main;

procedureDefinition : 'def' procedure parameters? '{' behaviour '}';
parameters : '(' parameterList? ')';
parameterList : parameterList ',' parameterList | parameter;
parameter : process;

main : 'main {' behaviour '}';

behaviour : interaction
    |   condition
    |   procedureInvocation
    |   introduction
    |   nothing
    |   TERMINATE
    ;

nothing:;

condition : 'if' process '.' expression 'then' thenBehaviour=behaviour 'else' elseBehaviour=behaviour 'continue' continuation=behaviour
        |   'if' process '.' expression 'then' thenBehaviour=behaviour 'else' elseBehaviour=behaviour 'endif'
        |   'if' process '.' expression 'then' thenBehaviour=behaviour 'else' elseBehaviour=behaviour
        ;

procedureInvocation: procedure parameters? (';' continuation=behaviour)?;

interaction : communication | selection;

communication: sender=process '.' expression '->' receiver=process ';' continuation=behaviour;
selection: sender=process '->' receiver=process '[' expression '];' continuation=behaviour;
introduction: introducer=process '.' leftIntroductee=process '<->' rightIntroductee=process ';' continuation=behaviour;

expression : Identifier
    |   BooleanLiteral
    |   Wildcard
    |   INT
    ;

TERMINATE : 'stop' | '0';