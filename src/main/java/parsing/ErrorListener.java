package parsing;

import antlrgen.NetworkParser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.BitSet;

/**
 * Class to listen for syntax and parser errors in the generated antlr parser.
 * Adding this listener will cause the parser to throw a ParseCancellationException upon encountering an error.
 * The purpose is to abort when there are problems with the input. This is not the default behaviour.
 */
class ErrorListener extends BaseErrorListener {
    private final String[] inputLines;
    public ErrorListener(String input){
        inputLines = input.split("\r\n|[\r\n]");//Split on line-break
    }

    //Listens for syntax errors in the parser
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e) throws ParseCancellationException {
        String errMessage = ("Syntax error at line %d, position %d: %s%n" +
                "\t%s%n" +
                "\t%s^").formatted(line, charPositionInLine, msg, inputLines[line-1], " ".repeat(charPositionInLine));
        throw new ParseCancellationException(errMessage);
    }

    //Listens for ambiguity errors in the parser.
    //Throws an error on ambiguous continuations for nested conditionals. Warns on other ambiguities
    @Override
    public void reportAmbiguity(org.antlr.v4.runtime.Parser recognizer, DFA dfa,
                                int startIndex, int stopIndex, boolean exact, BitSet ambigAlts,
                                ATNConfigSet configs) throws ParseCancellationException{
        int stopLine = 0;
        while (inputLines[stopLine].length() < stopIndex){
            stopIndex -= inputLines[stopLine].length();
            stopLine++;
        }
        int startline = 0;
        while (inputLines[startline].length() < startIndex){
            startIndex -= inputLines[startline].length();
            startline++;
        }
        String errorPosition;
        if (stopLine == startline)
            errorPosition= "Ambiguity at line %d, starting at position %d, ending at %d: %n\t%s%n\t%s^%s".formatted(stopLine+1, startIndex, stopIndex, inputLines[stopLine], " ".repeat(startIndex-1), " ".repeat(stopIndex-startIndex-2));
        else {
            errorPosition = "Ambiguity from line %d, position %d to line %d position %d:%n".formatted(startline + 1, startIndex, stopLine+1, stopIndex);
            errorPosition += "\t%s%n\t%s^%n".formatted(inputLines[startline], " ".repeat(startIndex-1));
            for (int i = startline+1; i < stopLine; i++){
                errorPosition += "\t%s%n".formatted(inputLines[i]);
            }
            errorPosition += "\t%s%n\t%s^%n".formatted(inputLines[stopLine], " ".repeat(stopIndex-1));
        }

        //I'm assuming here that any ambiguities related to conditionals are due to nested conditionals
        //that has too few continue/endif tokens.
        if (recognizer.getContext() instanceof NetworkParser.ConditionContext){
            String errorMessage = ("Parsing of input aborted due to an ambiguity, on which among nested conditionals a continuation belongs to.%n" +
                    "%s%n" +
                    "Insert \"endif\" following the conditionals without continuations to resolve the ambiguity.").formatted(errorPosition);
            throw new ParseCancellationException(errorMessage);
        }

        String errorMessage = "WARNING: Parser encountered an ambiguity. The parser will attempt to auto-resolve it, but does not guarantee a correct resolution.%n%s".formatted(errorPosition);
        System.err.println(errorMessage);
        super.reportAmbiguity(recognizer, dfa, startIndex, stopIndex, exact, ambigAlts, configs);
    }

    //There are more listener functions https://www.antlr.org/api/Java/org/antlr/v4/runtime/ANTLRErrorListener.html
    //But I believe this is sufficient to catch any errors
}
