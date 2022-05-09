grammar CommonLexerRules;

Identifier : [a-zA-Z0-9_]+;
process : Identifier;
procedure : Identifier;
BooleanLiteral : 'true' | 'false';
StringLiteral : '"' ~('\r' | '\n' | '"')* '"' ;

Wildcard : 'this';
WS : [ \t\r\n]+ -> skip ;

INT: [0-9]+ ;


