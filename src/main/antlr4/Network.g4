grammar Network;
import CommonLexerRules;

@header {
    package antlrgen;
}

//Network definition
network: process processBehaviour ('|' process processBehaviour)* EOF; //Must match up til end of file
//Definition of a single process
processBehaviour : '{' procedureDefinition* 'main' '{' behaviour '}' '}';
//Definition of a process' procedures
procedureDefinition : 'def' procedure parameters? '{' behaviour '}';
//Procedure parameters
parameters : '(' parameterList? ')';
parameterList : parameterList ',' parameterList | parameter;
parameter : process;


behaviour : sending
    |   receiving
    |   selection
    |   offering
    |   condition
    |   procedureInvocation
    |   introduce
    |   introductee
    |   spawn
    |   TERMINATE
    |   nothing
    ;

nothing:;

//Send a message
sending: process '!<' expression '>;' behaviour;
receiving: process '?;' behaviour;

//Selection of an offered behaviour
selection: process '+' expression ';' behaviour;
offering: process '&{' (labeledBehaviour) (',' labeledBehaviour)* '}' (';' continuation=behaviour)?;
labeledBehaviour: expression ':' behaviour;

//Introduction of processes to make them aware of each other
introduce: introductee1=process '<->' introductee2=process ';' behaviour;
introductee: introducer=process '?' introducedProcess=process ';' behaviour;

//Conditional branching
condition: 'if' expression 'then' thenBehaviour=behaviour 'else' elseBehaviour=behaviour ('continue' continuation=behaviour)?;

//Invocation of procedures
procedureInvocation: procedure parameters? (';' continuation=behaviour)?;

//Spawn a new process into the network
spawn : 'spawn' process 'with' childbehaviour=behaviour 'continue' continuation=behaviour;

//Information echanged or used in processes, but not relevant for choreography extraction
expression : Identifier
    |   BooleanLiteral
    |   Wildcard
    |   INT
    ;

//This token indicates the process terminates
TERMINATE : 'stop';
