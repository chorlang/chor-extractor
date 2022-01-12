package parsing;

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
    public static ErrorListener instance = new ErrorListener();

    //Listens for syntax errors in the parser
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e) throws ParseCancellationException {
        String errMessage = "Syntax error at line %d, position %d: %s".formatted(line, charPositionInLine, msg);
        throw new ParseCancellationException(errMessage);
    }

    //Listens for ambiguity errors in the parser.
    //Hopefully the grammar is unambiguous, but this will throw an error just in case
    @Override
    public void reportAmbiguity(org.antlr.v4.runtime.Parser recognizer, DFA dfa,
                                int startIndex, int stopIndex, boolean exact, BitSet ambigAlts,
                                ATNConfigSet configs) throws ParseCancellationException{
        super.reportAmbiguity(recognizer, dfa, startIndex, stopIndex, exact, ambigAlts, configs);
        String errMessage = "Parser encountered an ambiguity. Began at %d. Ambiguity at %d.".formatted(startIndex, stopIndex);
        throw new ParseCancellationException(errMessage);
    }

    //There are more listener functions https://www.antlr.org/api/Java/org/antlr/v4/runtime/ANTLRErrorListener.html
    //But I believe this is sufficient to catch any errors
}
