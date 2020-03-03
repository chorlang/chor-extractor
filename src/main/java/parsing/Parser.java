package parsing;

import antlrgen.NetworkLexer;
import antlrgen.NetworkParser;
import network.*;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class Parser {
    Network stringToNetwork(String networkDescription){
        NetworkParser.NetworkContext networkAST = parseNetwork(networkDescription);
        return NetworkASTToNetwork.toNetwork(networkAST);
    }

    @SuppressWarnings("deprecated")
    NetworkParser.NetworkContext parseNetwork(String network) {
        //The non-deprecated ANTLRInputStream do not work for some reason.
        ANTLRInputStream inputStream = null;
        try {
            inputStream = new ANTLRInputStream(new ByteArrayInputStream(network.getBytes()));
        } catch (IOException e) {
            System.out.println("The network input string created an exception when converting to Stream, somehow.");
            e.printStackTrace();
            System.exit(1);
        }
        NetworkLexer lexer = new NetworkLexer(inputStream);
        NetworkParser parser = new NetworkParser(new CommonTokenStream(lexer));
        return parser.network();
    }
}
