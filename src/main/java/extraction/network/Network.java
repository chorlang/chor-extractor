package extraction.network;

import extraction.AdjacencyMatrix;
import extraction.Label;
import extraction.Label.*;
import extraction.network.Behaviour.*;
import utility.Pair;

import java.util.*;

public class Network extends NetworkASTNode {
    public HashMap<String, ProcessTerm> processes;      //Map from process names to procedures
    private final AdjacencyMatrix introduced;           //Which processes are known to each other
    private int nextID = 0;                             //Next available process ID

    /**
     * A Network object stores a mapping from process names to process terms (procedures).
     * @param processes HashMap&lt;String, ProcessTerm&gt; where the String is the process name
     * @param introduced Which processes are aware of each other, and able to communicate
     */
    public Network(HashMap<String, ProcessTerm> processes, AdjacencyMatrix introduced){
        super(Action.NETWORK);
        this.processes = processes;
        this.introduced = introduced;
    }
    /**
     * A Network object stores a mapping from process names to process terms (procedures).
     * Assumes all processes are aware of each other
     * @param processes HashMap&lt;String, ProcessTerm&gt; where the String is the process name
     */
    public Network(HashMap<String, ProcessTerm> processes){
        this(processes, new AdjacencyMatrix(processes.keySet().stream().toList()));
    }

    /**
     * Replaces every main behaviour in processes which is a ProcedureInvocation with its procedure definition.
     * @return A map of the processes that was unfolded, before the unfolding.
     */
    public HashMap<String, ProcessTerm> unfold(){
        HashMap<String, ProcessTerm> unfoldedProcesses = new HashMap<>();
        processes.forEach((name, term) -> {
            if (term.rawMain() instanceof ProcedureInvocation) {
                unfoldedProcesses.put(name, term.copy());
                term.unfoldRecursively();
            }
        });
        return unfoldedProcesses;
    }

    /**
     * Restores ProcessTerm mains, if the process name is not in exceptions.
     * Specifically, the main behaviour of each ProcessTerm in the network gets replaced with that from
     * originalProcesses, if an entry exists for that process, unless its name is in exceptions.
     * @param originalProcesses Processes to restore, and what to restore them to.
     * @param exceptions Names of processes that should not be restored, even if they are in originalProcesses.
     */
    public void restoreExcept(HashMap<String, ProcessTerm> originalProcesses, HashSet<String> exceptions){
        originalProcesses.forEach((name, term) -> {
            if (!exceptions.contains(name)){
                processes.get(name).restore(term);
            }
        });
    }

    /**
     * Restores the main Behaviour of selected processes to that of a previous copy.
     * @param originalProcesses The processes to restore from.
     * @param toRestore Names of the processes to restore
     */
    public void restoreProcesses(HashMap<String, ProcessTerm> originalProcesses, Collection<String> toRestore){
        toRestore.forEach(processName -> {
            ProcessTerm oldTerm = originalProcesses.get(processName);
            if (oldTerm == null)
                processes.remove(processName);  //Remove if process just spawned
            else
                processes.get(processName).restore(oldTerm);
        });
    }

    /**
     * Container to store information about the advancement (reduced processes) of a Network.
     * If elseLabel and elseNetwork is not null, then a conditional was reduced. The then branch are
     * the other fields.
     * The field actors is a set of all processes that reduced during the advancement.
     */
    public static record Advancement(Label label, Network network,
                                     Network elseNetwork, Label.ConditionLabel.ElseLabel elseLabel,
                                     HashSet<String> actors){
        public Advancement(Label label, Network network, HashSet<String> actors){
            this(label, network, null, null, actors);
        }
    }

    /**
     * Attempts to advance the Network by spawning a new process
     * @param process The name of the process to reduce if its main Behaviour is spawn
     * @return An advancement record if the Network advanced, or null on failure.
     */
    public Advancement spawnAdvance(String process){
        ProcessTerm spawnerTerm = processes.get(process);
        if (!(spawnerTerm.rawMain() instanceof Spawn spawner))
            return null;

        String variableName = spawner.variable;
        String realName = String.format("%s/%s%d", process, spawner.variable, nextID++);

        //Assign variable, and create the term for the spawned process
        spawnerTerm.substitute(variableName, realName);
        ProcessTerm spawnedProcess = spawnerTerm.spawnNew(spawner.processBehaviour);

        processes.put(realName, spawnedProcess);                //Add the new process to the network
        introduced.spawn(process, realName);                    //Parent and child are introduced at spawn

        SpawnLabel label = new SpawnLabel(process, realName);   //Create the label for this spawn
        spawnerTerm.reduce();                                   //Reduce the parent process
        return new Advancement(label, this, new HashSet<>(){{add(process); add(realName);}});
    }

    /**
     * Attempts to advance the network by reducing on the given process, and the processes
     * it communicates with, if any. Returns the Label of the reduction, and modifies this Network.
     * If process' main Behaviour is Condition, returns a ThenLabel, the Network of the else branch,
     * and the ElseLabel. This Network becomes the Network of the then branch.
     * Returns null on failure, in which case the network is unmodified.
     * @param process The name of the process to attempt to reduce on.
     * @return An Advancement record. If the Network reduces on an interaction, then this Network gets changed
     * to the resulting network, and the records label field is the Label of the interaction. Other fields are null.
     * This the Network reduces on an Conditional, then this Network gets changed to the Network of the then branch,
     * while the records label field is the corresponding ThenLabel. The elseNetwork and elseLabel are corresponding
     * to the else branch of the Conditional.
     * On failure, returns null, in which case this Network is unchanged.
     */
    public Advancement CommunicationConditionalAdvance(String process){
        var term = processes.get(process);
        var interaction = term.prospectInteraction(process);
        if (interaction != null){
            return reduceInteraction(interaction);
        }
        var condition = term.prospectCondition(process);
        if (condition != null){
            return reduceCondition(condition);
        }
        return null;
    }

    /**
     * Reduces this Network to the then branch of a conditional (if applicable), and generates a copy
     * of this Network reducing to the else branch.
     * @return An Advancement record containing the then and else labels from this function's parameter,
     * as well as a Network identical to this one reduced to the else branch.
     */
    private Advancement reduceCondition(Pair<ConditionLabel.ThenLabel, ConditionLabel.ElseLabel> labels){
        var thenLabel = labels.first;
        var elseLabel = labels.second;
        String process = thenLabel.process; //Name of the process with the conditional
        if (!(processes.get(process).rawMain() instanceof Condition conditional))
            return null;

        Network elseNetwork = this.copy();
        processes.get(process).reduce(true);
        elseNetwork.processes.get(process).reduce(false);

        return new Advancement(thenLabel, this, elseNetwork, elseLabel, getInvolvedProcesses(thenLabel));
    }

    /**
     * Reduces the Network using a InteractionLabel as basis, if possible.
     * @return An Advancement record where the label field is set to this function label parameter.
     * On failure returns null, and the Network remains unchanged.
     */
    private Advancement reduceInteraction(InteractionLabel label){
        ProcessTerm sendProcess = processes.get(label.sender);
        ProcessTerm receiveProcess = processes.get(label.receiver);

        //Check the interaction is at all possible
        if ( !( sendProcess.runtimeMain() instanceof Behaviour.Sender sender &&
                receiveProcess.runtimeMain() instanceof Behaviour.Receiver receiver &&
                sender.receiver.equals(label.receiver) &&
                receiver.sender.equals(label.sender) &&
                sender.expression.equals(label.expression) &&
                introduced.isIntroduced(label.sender, label.receiver)))
            return null;

        boolean reduced = false;

        //Check interaction is of the right type
        if (    label instanceof CommunicationLabel &&
                sender instanceof Send send &&
                receiver instanceof Receive receive){
            sendProcess.reduce();
            receiveProcess.reduce();
            reduced = true;
        }
        else if ( label instanceof SelectionLabel &&
                sender instanceof Selection select &&
                receiver instanceof Offering offer){
            sendProcess.reduce();
            receiveProcess.reduce(select.label);
            reduced = true;
        }
        else if ( label instanceof IntroductionLabel intro &&
                sender instanceof Introduce introducer &&
                receiver instanceof Introductee introducteeR &&
                //Check the process hidden as the expression term
                processes.get(intro.leftProcess).runtimeMain() instanceof Introductee introducteeL &&
                introducteeL.sender.equals(intro.introducer) &&
                introduced.isIntroduced(intro.introducer, intro.leftProcess)){
            ProcessTerm receiveProcessL = processes.get(intro.leftProcess);
            //We need the unmodified mains to get the variable names for the introduced process
            introducteeL = (Introductee) receiveProcessL.rawMain();
            introducteeR = (Introductee) receiveProcess.rawMain();
            //Bind varname in left process to the name of the right, and vice versa.
            receiveProcessL.substitute(introducteeL.processID, intro.rightProcess);
            receiveProcess.substitute(introducteeR.processID, intro.leftProcess);
            //Remember that the introduced processes are now allowed to communicate
            introduced.introduce(intro.leftProcess, intro.rightProcess);

            //Advance the network.
            sendProcess.reduce();
            receiveProcess.reduce();
            receiveProcessL.reduce();
            reduced = true;
        }
        if (!reduced)
            return null;
        return new Advancement(label, this, getInvolvedProcesses(label));
    }

    public Advancement multicomAdvance(String process){
        class FauxIntroductionLabel extends InteractionLabel {
            /**
             * Create a faux InteractionLabel that is functionally like an IntroductionLabel.
             * Introduction in findMulticom is done by one of this label, and one IntroductionLabel
             */
            FauxIntroductionLabel(String introducer, String introductee, String introducedProcess){
                //The ordering needs to be right for multicom to work.
                super(introducer, introducedProcess, introductee, LabelType.INTRODUCTION);
            }
            @Override
            public String toString() {
                return String.format("%s.%s<->%s Debug only", sender, expression, receiver);
            }
        }

        var processes = copyProcesses(); //Shadow processes with a copy to modify
        var known = introduced.copy();
        var processTerm = processes.get(process);

        var actions = new ArrayList<InteractionLabel>();
        var actors = new HashSet<String>();
        var waiting = new LinkedList<InteractionLabel>();

        if (!(processTerm.rawMain() instanceof Sender sender))
            return null; //Only sending behaviours can start a multicom
        //Add initial Label to the waiting list
        var label = processTerm.prospectInteraction(process);
        if (label instanceof IntroductionLabel intro)
            //Introductions are handles with two labels which are both considered two-way communications
            waiting.add(new FauxIntroductionLabel(label.sender, label.receiver, label.expression));
        waiting.add(label);
        processTerm.reduce(); //Reduce the added interaction

        while (waiting.size() > 0){
            var next = waiting.remove();
            //Check the processes are introduced before com. Should also cover introductions by using FauxIntro
            if (!known.isIntroduced(next.sender, next.receiver))
                return null;
            if (!(next instanceof FauxIntroductionLabel))
                actions.add(next);
            updateActors(actors, next);

            //Go through the process' Behaviour, until a receiving Behaviour is reached.
            processTerm = processes.get(next.receiver);
            Behaviour blocking = processTerm.runtimeMain();
            while (!(blocking instanceof Receiver receiver)){
                if (blocking instanceof ProcedureInvocation){
                    processTerm.unfoldRecursively();
                    blocking = processTerm.runtimeMain();
                    continue;
                } else if (!(blocking instanceof Sender)) {
                    return null; //Process not of the required form. Multicom not possible
                }
                //Guaranteed to be sender. The above pattern match won't typecast for some reason
                sender = (Sender)blocking;
                label = processTerm.prospectInteraction(next.receiver);
                if (sender instanceof Introduce)
                    waiting.add(new FauxIntroductionLabel(label.sender, label.receiver, label.expression));
                waiting.add(label);
                processTerm.reduce();
                blocking = processTerm.runtimeMain();  //Blocking must have actual variable values.
            }

            //All send/select/introduce actions are added to waiting.
            //Blocking is now receive/offer/introductee
            //Check that it receives from the right process
            if (!receiver.sender.equals(next.sender))
                return null;
            //Reduce the network, and check the Label and receiver type matches
            if (receiver instanceof Offering offering && next instanceof SelectionLabel)
                processTerm.reduce(next.expression);
            else if (receiver instanceof Receive && next instanceof CommunicationLabel ||
                     receiver instanceof Introductee &&
                             (next instanceof IntroductionLabel || next instanceof FauxIntroductionLabel))
                processTerm.reduce();
            else
                return null; //Label and receiver type does not match.
            //Introduce processes. Real IntroductionLabel always comes after the faux one, so at this point,
            //both receivers have received, and the introduction is fully complete.
            if (next instanceof IntroductionLabel intro)
                known.introduce(intro.leftProcess, intro.rightProcess);
        }

        if (actions.size() == 1)
            return null;    //Single interactions not multicom, and should already have been attempted
        return new Advancement(new MulticomLabel(actions), new Network(processes, known), actors);
    }





    /* ------------------------------
        Utility and Helper functions
       ------------------------------ */

    /**
     * Returns a set of process names from a Label.
     * @param label A ConditionLabel or InteractionLabel to get the involved processes from.
     * @return Set of every process name Label
     */
    private HashSet<String> getInvolvedProcesses(Label label){
        var involved = new HashSet<String>();
        if (label instanceof Label.InteractionLabel interaction){
            involved.add(interaction.sender);
            involved.add(interaction.receiver);
            if (label instanceof Label.IntroductionLabel)
                involved.add(interaction.expression);
        } else if (label instanceof Label.ConditionLabel conditional){
            involved.add(conditional.process);
        } else
            throw new IllegalArgumentException("The Advancement parameter contains an unsupported Label type");
        return involved;
    }

    /**
     * Adds all processes from a Label to a set
     * @param actors The set to add to
     * @param label The label to find processes from
     */
    private void updateActors(HashSet<String> actors, Label label){
        if (label instanceof InteractionLabel interaction){
            actors.add(interaction.sender);
            actors.add(interaction.receiver);
            if (label instanceof IntroductionLabel)
                actors.add(interaction.expression);
        } else if (label instanceof ConditionLabel conditional){
            actors.add(conditional.process);
        }
    }

    /**
     * Checks if all processes of this Network has terminated.
     * Specifically, it returns true if the main Behaviour for all ProcessTerm is Termination.
     * @return true if all processes has terminated. false otherwise.
     */
    public boolean allTerminated(){
        for (ProcessTerm process : processes.values()) {
            if (!process.isTerminated())
                return false;
        }
        return true;
    }

    /**
     * Creates a string of all the process names to process terms mappings
     * @return String of the stored HashMap
     */
    public String toString(){
        StringBuilder builder = new StringBuilder();
        processes.forEach((processName, procedure) ->
                builder.append(processName).append(procedure.toString()).append(" | "));
        if (builder.length() >= 3){ //Remove trailing " | "
            builder.delete(builder.length() - 3, builder.length());
        }
        return builder.toString();
    }

    /**
     * Returns a string representation of this network intended for human reading.
     * Only the main behaviour is included (no procedure definitions), and there is
     * a line break between processes.
     */
    public String toPrettyString(){
        StringBuilder builder = new StringBuilder();
        processes.forEach((processName, processTerm)->{
            builder.append("%s ▹ ".formatted(processName)).append(processTerm.rawMain().toString()).append(" |\n");
        });
        if (builder.length() >= 3)//Remove trailing " |\n"
            builder.delete(builder.length() - 3, builder.length());
        return builder.toString();
    }


    /**
     * Creates a semi-shallow copy of this Network.
     * Operating on the copy does not modify the original Network.
     * (Assuming the objects contained in the fields of the network are all unmodifiable)
     * @return An identical Network
     */
    public Network copy(){
        var copy = new Network(copyProcesses(), introduced.copy());
        copy.nextID = nextID;
        return copy;
    }

    /**
     * Creates a copy of processes, where each ProcessTerm is also a copy.
     */
    public HashMap<String, ProcessTerm> copyProcesses(){
        HashMap<String, ProcessTerm> processesCopy = new HashMap<>(processes.size());
        processes.forEach((key, value) -> processesCopy.put(key, value.copy()));
        return processesCopy;
    }

    /**
     * Compares this Networks mapping with another Networks mapping.
     * Ignores which processes have been introduced.
     * @param other Network to compare to
     * @return true, if all mappings are the same, and no map has an entry the other does not
     */
    public boolean equals(Network other){
        if (this == other)
            return true;
        if (processes.size() != other.processes.size())
            return false;

        for (var processName : processes.keySet()){
            var otherProcess = other.processes.get(processName);
            if (otherProcess == null || !otherProcess.equals(processes.get(processName)))
                return false;
        }
        return true;
    }

    /**
     * Calculates a hashcode from the process names and process terms.
     * @return Hash of this extraction.network mapping
     */
    public int hashCode(){
        return processes.entrySet().stream().mapToInt(
                //Map each process to its hash value in the stream
                entry -> {
                    if (entry.getValue().isTerminated())
                        return 0;
                    return entry.getKey().hashCode() * 31 + entry.getValue().hashCode() * 29;
                }
        //Add up all the hash values of the stream and return the total.
        ).reduce(0, Integer::sum);
    }
}

