package extraction;

import extraction.GraphBuilder.BuildGraphResult;
import extraction.Node.ConcreteNode;
import extraction.network.Network;
import extraction.network.ProcessTerm;
import extraction.network.Network.Advancement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.function.Function;

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

        //Create advancer to manage datastructures, testing, and SEG building
        var advancer = new NetworkAdvancer(currentNode);
        //Reference to the internal Network of the advancer (which is a copy of that in currentNode).
        Network network = advancer.network;

        //Try advancing with interactions or conditionals
        BuildGraphResult result = advancer.advanceNetwork(network::CommunicationConditionalAdvance);
        if (result != null)
            return result;

        //Try advancing with multicom interactions (if enables)
        //Assumes no single communications are possible
        if (!disableMulticom) {
            result = advancer.advanceNetwork(network::multicomAdvance);
            if (result != null)
                return result;
        }

        //Try advancing by spawning a new process
        //Must be last, to prevent generating infinite SEGs under certain strategies.
        result = advancer.advanceNetwork(network::spawnAdvance);
        if (result != null)
            return result;

        if (network.allTerminated())
            return BuildGraphResult.OK;

        return BuildGraphResult.FAIL;

    }

    private class NetworkAdvancer{
        private final ConcreteNode currentNode;
        private final Network network;
        private final HashMap<String, ProcessTerm> unfoldedProcesses;
        private final LinkedHashMap<String, ProcessTerm> orderedProcesses;

        /**
         * Construct a helper object to assist in prospecting for viable advancements in a Network.
         * Takes care of internal bookkeeping and temporary data structures
         * @param currentNode The node to copy data from, including the internal Network instance.
         */
        NetworkAdvancer(ConcreteNode currentNode){
            this.currentNode = currentNode;                     //Keep for when expanding the SEG
            network = currentNode.network.copy();               //Work on copy
            unfoldedProcesses = network.unfold();               //Unfold procedures
            ConcreteNode unfoldedNode = currentNode.copy();     //Create temp copy with unfolded network
            unfoldedNode.network = network;
            orderedProcesses = copyAndSortProcesses(unfoldedNode);  //Sort based on strategy
        }

        /**
         * Attempts to advance the internally stored Network using the provided function.
         * @param tryAdvancement A function that attempts to advance the Network. It must take a process name as
         *                       parameter, which will be the bases of reducing/advancing the network.
         *                       The function must be from the internally stored Network instance.
         * @return OK if the network advanced, and the SEG successfully expanded, FAIL if the network is not
         * extractable, or null if no advancement was made, in which case the network is unmodified.
         */
        BuildGraphResult advanceNetwork(Function<String, Advancement> tryAdvancement){
            for (var processName : orderedProcesses.keySet()){
                //Try to advance the Network by reducing the chosen process.
                Advancement advancement = tryAdvancement.apply(processName);
                //If the chosen process could not reduce the network, try the next one
                if (advancement == null)
                    continue;

                //Fold back procedure invocations of unused processes.
                foldBackProcesses(advancement, unfoldedProcesses);

                //Build out the graph using the progress of the network
                BuildGraphResult result = builder.buildGraph(advancement, currentNode);

                //In case of bad loops, the graph remains unchanged.
                // Reset changes to the Network, and try the next process
                if (result == BuildGraphResult.BAD_LOOP) {
                    network.restoreProcesses(orderedProcesses, unfoldedProcesses.keySet()); //Unfold again
                    network.restoreProcesses(orderedProcesses, advancement.actors());       //Restore modified processes
                    continue;
                }

                //Return the result of building the graph.
                //Will either be OK on success, or FAIL if the network is not extractable
                return result;
            }
            //The tryAdvancement function could not advance the Network.
            return null;
        }
        /**
         * Folds back the main Behaviour of all Networks in an Advancement, if they did not reduce.
         * @param advancement Container for the Network(s) to fold back.
         * @param unfoldedProcesses Copy of the ProcessTerm that was unfolded.
         */
        private void foldBackProcesses(Advancement advancement,
                                       HashMap<String, ProcessTerm> unfoldedProcesses){
            HashSet<String> involvedProcesses = advancement.actors();
            advancement.network().restoreExcept(unfoldedProcesses, involvedProcesses);
            if (advancement.elseNetwork() != null)
                advancement.elseNetwork().restoreExcept(unfoldedProcesses, involvedProcesses);
        }
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
