package parsing;

import antlrgen.ChoreographyLexer;
import antlrgen.ChoreographyParser;
import antlrgen.NetworkLexer;
import antlrgen.NetworkParser;
import extraction.choreography.Program;
import extraction.network.Network;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;

public class Parser {
    /**
     * Converts a String representation of a network to the internal representation used for extraction.
     * @param networkDescription The network as String representation.
     * @return A Network instance equivalent to the input String, or null if the String is invalid.
     */
    public static Network stringToNetwork(String networkDescription){
        //CharStream -> Lexer -> Parser
        CharStream inputStream = CharStreams.fromString(networkDescription);
        NetworkLexer lexer = new NetworkLexer(inputStream);
        NetworkParser parser = new NetworkParser(new CommonTokenStream(lexer));
        //Add custom error listener, that makes the parser throw an error if it fails for parse the input
        parser.removeErrorListeners();                      //remove default listener
        parser.addErrorListener(new ErrorListener(networkDescription));

        try{
            //Parse, get the root of the parse tree, then convert it to internal representation.
            return NetworkASTToNetwork.toNetwork(parser.network());
        } catch (ParseCancellationException e){
            //The String could not be parsed. Likely because of a syntax error.
            System.err.println("Error: Invalid network String provided as input:\n\t" + e.getMessage());
            return null;
        }
    }

    /**
     * Converts a program (one or more choreographies) in String format to this
     * program's internal representation.
     * @param programDescription The program/choreography in text form
     * @return A Program instance representing the choreography/choreographies of the String, or null if
     * the String is invalid.
     */
    public static Program stringToProgram(String programDescription){
        //CharStream -> Lexer -> Parser
        CharStream stream = CharStreams.fromString(programDescription);
        ChoreographyLexer lexer = new ChoreographyLexer(stream);
        ChoreographyParser parser = new ChoreographyParser(new CommonTokenStream(lexer));
        //Add custom error listener, that makes the parser throw an error if it fails for parse the input
        parser.removeErrorListeners();                      //remove default listener
        parser.addErrorListener(new ErrorListener(programDescription));

        try{
            //Parse, get the root of the parse tree, then convert it to internal representation.
            return ProgramASTToProgram.toProgram(parser.program());
        } catch (ParseCancellationException e){
            //The String could not be parsed. Likely because of a syntax error.
            System.err.println("Error: Invalid network String provided as input:\n\t" + e.getMessage());
            return null;
        }
    }
}
