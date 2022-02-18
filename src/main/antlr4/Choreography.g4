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

behaviour : communication
    |   selection
    |   condition
    |   procedureInvocation
    |   introduction
    |   nothing
    |   TERMINATE
    ;

nothing:;

//The top case must be first, or it causes the parser to report an ambiguity for some reason
condition : 'if' process '.' expression 'then' thenBehaviour=behaviour 'else' elseBehaviour=behaviour
        |   'if' process '.' expression 'then' thenBehaviour=behaviour 'else' elseBehaviour=behaviour 'continue' continuation=behaviour
        |   'if' process '.' expression 'then' thenBehaviour=behaviour 'else' elseBehaviour=behaviour 'endif'
        ;

procedureInvocation: procedure parameters? (';' continuation=behaviour)?;

communication: sender=process '.' expression '->' receiver=process ';' continuation=behaviour;
selection: sender=process '->' receiver=process '[' expression '];' continuation=behaviour;
introduction: introducer=process '.' leftIntroductee=process '<->' rightIntroductee=process ';' continuation=behaviour;

expression : Identifier
    |   BooleanLiteral
    |   Wildcard
    |   INT
    ;

TERMINATE : 'stop' | '0';