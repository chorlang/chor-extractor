package parsing;

import antlrgen.ChoreographyLexer;
import antlrgen.ChoreographyParser;
import antlrgen.NetworkLexer;
import antlrgen.NetworkParser;
import extraction.choreography.Program;
import extraction.network.*;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class Parser {
    public static Network stringToNetwork(String networkDescription){
        NetworkParser.NetworkContext networkAST = parseNetwork(networkDescription);
        return NetworkASTToNetwork.toNetwork(networkAST);
    }

    @SuppressWarnings("deprecation")
    private static NetworkParser.NetworkContext parseNetwork(String network) {
        //The non-deprecated ANTLRInputStream do not work for some reason.
        ANTLRInputStream inputStream = null;
        try {
            inputStream = new ANTLRInputStream(new ByteArrayInputStream(network.getBytes()));
        } catch (IOException e) {
            System.out.println("The extraction.network input string created an exception when converting to Stream, somehow.");
            e.printStackTrace();
            System.exit(1);
        }
        NetworkLexer lexer = new NetworkLexer(inputStream);
        NetworkParser parser = new NetworkParser(new CommonTokenStream(lexer));
        return parser.network();
    }

    public static Program stringToProgram(String choreographyDescription){
        return ChoreographyASTToProgram.toProgram(parseChoreography(choreographyDescription));
    }

    public static ChoreographyParser.ProgramContext parseChoreography(String choreographyDescription){
        var stream = new ANTLRInputStream(choreographyDescription);
        var lexer = new ChoreographyLexer(stream);
        var parser = new ChoreographyParser(new CommonTokenStream(lexer));
        return parser.program();
    }
}
