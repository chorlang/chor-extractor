package executable;
import com.beust.jcommander.Parameter;
import extraction.Strategy;

import java.util.ArrayList;
import java.util.List;

//Docs at http://jcommander.org/

public class Parameters {
    @Parameter(names = {"-i", "--input", "--input-file"}, description = "Specify a file containing the input network. If not set, input will be read from stdin", order = 0)
    public String inputFile = null;

    @Parameter(names = {"-o", "--output", "--output-file"}, description = "Specify a file name to write the resulting choreography to. If not set, output will be written to stdout", order = 1)
    public String outputFile = null;

    @Parameter(names = {"--services"}, variableArity = true, description = "Specify the names of processes that acts as services. Example: \"--services database seller\"", order = 2)
    public List<String> services = new ArrayList<>();

    @Parameter(names = {"--strategy"}, description = "Which extraction strategy to use", order = 3)
    public Strategy strategy = Strategy.Default;

    @Parameter(names = {"-h", "--help"}, help = true, description = "Print this help message")
    public boolean help = false;

    @Parameter(names = {"-d", "--debug"}, description = "Print debug info to stdout")//TODO Currently does nothing
    public boolean debug = false;

    @Parameter(names = {"-p", "--disable-purge"}, description = "Disable purging of processes that immediately terminate")
    public boolean purgingDisabled = false;

    @Parameter(names = {"-m", "--disable-multicom"}, description = "Disable the usage of multicoms (asynchronous interactions) during extraction")
    public boolean multicomDisabled = false;

    @Parameter(names = {"-s", "--extract-sequentially"}, description = "By default, if the input consists of multiple, independent networks, " +
            "they are split up and extracted in parallel in separate threads. This setting disables that behaviour")
    public boolean extractSequentially = false;

    @Parameter(names = {"--draw", "--draw-SEG"}, description = "Draws the generated SEG to an PNG image with the provided name. (Experimental. Might be significantly changed or removed in the future)")
    public String SEGImage = null;

    @Parameter(names = "--draw-unrolled", description = "Draws an unrolled version of the SEG. (Experimental. Might be significantly changed or removed in the future)")
    public String unrolledImage = null;
}
