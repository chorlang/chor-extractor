package extraction.network;

import extraction.Label;

import java.util.HashMap;

public abstract class Behaviour extends NetworkASTNode {
    Behaviour continuation;
    public Behaviour(Action action, Behaviour continuation){
        super(action);
        this.continuation = continuation;
    }

    public Behaviour getContinuation(){
        return continuation;
    }

    /**
     * Returns a copy of this Behaviour, but where process variable names are substituted with the values
     * they map to in substitutions. Note that substitutions must have an entry for all processes and variables
     * in the Network. Can be achieved by a HashMap where .get(p) = .getOrDefault(p,p)
     * Continuations and other such fields are unaffected.
     */
    abstract Behaviour realValues(HashMap<String, String> substitutions);

    /**
     * Behaviors are expected to overwrite their hashcode
     * to take relevant data into account.
     * @return Hash of relevant stored data structures.
     */
    public abstract int hashCode();

    /**
     * Compares fields and nested Behaviours recursively
     * @param other Behavior to compare to
     * @return true of the objects are equivalent, false otherwise
     */
    public abstract boolean equals(Behaviour other);

    /**
     * Subclasses that extends this class, are Behaviours that perform some interaction between processes.
     */
    public static abstract class Interaction extends Behaviour{
        Interaction(Action action, Behaviour continuation){
            super(action, continuation);
        }
    }

    /**
     * Subclasses that extends this class, are Behaviours that sends a message to a different process.
     * These classes are guaranteed to have the field "receiver" for the name/variable of the process
     * that will receive the message, and "expression" which contains the actual message.
     */
    public static abstract class Sender extends Interaction{
        public final String receiver, expression;
        Sender(Action action, Behaviour continuation, String receiver, String expression){
            super(action, continuation);
            this.receiver = receiver;
            this.expression = expression;
        }


        /**
         * Creates an InteractionLabel from this interacting behaviour.
         * Variable names are substituted for real names.
         * @param process The name of the process this Behaviour is from.
         * @param substitutions Mapping from both variable names and real names, to real process names.
         *                      A map containing only variable mappings, and where get(key) = getOrDefault(key,key) will do
         * @return A new InteractionLabel for the interaction needed to reduce this Behaviour.
         */
        abstract Label.InteractionLabel labelFrom(String process, ProcessTerm.ValueMap substitutions);
    }

    /**
     * Subclasses that extends this class, are Behaviours that expect to receive a message from a different process.
     * These classes are guaranteed to have the field "sender" which is the name/variable of the process
     * that they expect to receive some message from.
     */
    public static abstract class Receiver extends Interaction{
        public final String sender;
        Receiver(Action action, Behaviour continuation, String sender){
            super(action, continuation);
            this.sender = sender;
        }
    }

    /**
     * Behaviour with minimal impact on a process, designed to be replaced at runtime with a different behaviour.
     * Whenever this behaviour becomes the end of a process' main behaviour, it should immediately be replaced
     * by the continuation of an ancestor conditional or procedure invocation.
     */
    public static class BreakBehaviour extends Behaviour{
        public static final BreakBehaviour instance = new BreakBehaviour();
        private BreakBehaviour(){
            super(null, null);
        }

        @Override
        Behaviour realValues(HashMap<String, String> substitutions) {
            throw new IllegalCallerException("This is a stand-in Behaviour object, and is supposed to be replaced before usage.");
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Behaviour other) {
            return this == other;   //Constructor is private, so there is only ever one instance
        }

        @Override
        public String toString() {
            return "»";
        }
    }

    /**
     * Behaviour that means there is no continuation, because all preceding branches terminate or loop.
     */
    public static class NoneBehaviour extends BreakBehaviour{
        public static final NoneBehaviour instance = new NoneBehaviour();
        private NoneBehaviour(){super();}
        @Override
        public String toString(){
            return "";
        }
    }
}
