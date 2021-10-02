package extraction;

import extraction.GraphBuilder.BuildGraphResult;
import extraction.Node.ConcreteNode;
import extraction.network.Network;
import extraction.network.ProcessTerm;
import extraction.network.Network.Advancement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

/**
 * Searches for ways to advance the network of a ConcreteNode, then
 * makes GraphBuilder attempt building its SEG on that advancement.
 * If GraphBuilder finds that the new network creates an invalid loops,
 * Prospector will try to find another advancement.
 * The advancement is found by looking through the processes of the network
 * to find a main Behaviour that can reduce in the network. The order it
 * tries the processes is determined by the extraction strategy provided
 * its constructor
 */
public class Prospector {
    private final Strategy strategy;
    private final GraphBuilder builder;
    public boolean disableMulticom = false;
    Prospector(Strategy extractionStrategy, GraphBuilder builder){
        strategy = extractionStrategy;
        this.builder = builder;
    }

    /**
     * Searches for ways to advance the Network of currentNode, then makes BuildGraph try
     * build its SEG using the advanced network.
     * The Network of currentNode will remain unchanged by this function.
     * @param currentNode The node containing the Network to try and advance.
     * @return OK if this branch of the SEG is complete, or FAIL if the network is not extractable.
     */
    BuildGraphResult prospect(ConcreteNode currentNode){
        //Get copy safe to modify
        Network network = currentNode.network.copy();
        //Unfold procedure invocations, and get a copy of the original of the unfolded processes
        HashMap<String, ProcessTerm> unfoldedProcesses = network.unfold();
        HashMap<String, ProcessTerm> originalProcesses = network.copyProcesses();

        //Really doesn't need to be a copy
        //TODO Shouldn't this operate on the unfolded processes?
        var processOrder = copyAndSortProcesses(currentNode);
        //As far as I understand, the ordering of the Set's iterator is the same as that of the LinkedHashMap
        for (var processNames : processOrder.keySet()){

            //Try to advance the Network by reducing the chosen process.
            Advancement advancement = network.tryAdvance(processNames);
            //If the chosen process could not reduce the network, try the next one
            if (advancement == null)
                continue;

            //Fold back procedure invocations of unused processes.
            var involvedProcesses = advancement.actors();
            foldBackNetworks(advancement, unfoldedProcesses, involvedProcesses);

            //Build out the graph using the progress of the network
            BuildGraphResult result = builder.buildGraph(advancement, currentNode);

            //In case of bad loops, the graph remains unchanged.
            // Reset changes to the Network, and try the next process
            //TODO Find a better way to restore Network. Perhaps copy the whole thing
            if (result == BuildGraphResult.BAD_LOOP) {
                network.restoreProcesses(originalProcesses, involvedProcesses);
                continue;
            }

            //Return the result of building the graph.
            //Will either be OK on success, or FAIL if the network is not extractable
            return result;
        }

        //If all processes has terminated, this branch of the SEG is complete
        if (currentNode.network.allTerminated())
            return BuildGraphResult.OK;
        //If multicom not enabled, there is nothing more to try and extraction will fail
        if (disableMulticom)
            return BuildGraphResult.FAIL;

        for (var processNames : processOrder.keySet()){

            //Try to advance the Network by reducing the chosen process.
            Advancement advancement = network.multicomAdvance(processNames);
            //If the chosen process could not reduce the network, try the next one
            if (advancement == null)
                continue;

            //Fold back procedure invocations of unused processes.
            var involvedProcesses = advancement.actors();
            foldBackNetworks(advancement, unfoldedProcesses, involvedProcesses);

            //Build out the graph using the progress of the network
            BuildGraphResult result = builder.buildGraph(advancement, currentNode);

            //In case of bad loops, the graph remains unchanged.
            // Reset changes to the Network, and try the next process
            if (result == BuildGraphResult.BAD_LOOP)
                continue;

            //Return the result of building the graph.
            //Will either be OK on success, or FAIL if the network is not extractable
            return result;
        }

        //No process in the network can reduce. Extraction failed.
        return BuildGraphResult.FAIL;
    }

    /**
     * Folds back the main Behaviour of all Networks in an Advancement, if they did not reduce.
     * @param advancement Container for the Network(s) to fold back.
     * @param unfoldedProcesses Copy of the ProcessTerm that was unfolded.
     * @param involvedProcesses Names of processes not to fold back.
     */
    private void foldBackNetworks(Advancement advancement,
                                  HashMap<String, ProcessTerm> unfoldedProcesses,
                                  HashSet<String> involvedProcesses){
        advancement.network().foldExcept(unfoldedProcesses, involvedProcesses);
        if (advancement.elseNetwork() != null)
            advancement.elseNetwork().foldExcept(unfoldedProcesses, involvedProcesses);
    }

    /**
     * Returns an ordered HashMap of the processes of the Network in the node, sorted after the strategy this
     * GraphBuilder uses, such that higher priority ProcessTerms are earlier in the HashMap.
     * The ProcessTerms in the HashMap are copies of the original, so changes to the elements in the original
     * HashMap do not affect the copy and vice versa.
     * @param node The node containing the Network with a list of ProcessTerms to be sorted
     * @return An ordered HashMap sorted after this instances extraction strategy.
     */
    private LinkedHashMap<String, ProcessTerm> copyAndSortProcesses(Node.ConcreteNode node){
        return strategy.copyAndSort(node);
    }

}
