package extraction.network;

import extraction.Label;

import java.util.HashMap;
import java.util.Map;

public abstract class Behaviour extends NetworkASTNode {

    public Behaviour(Action action){
        super(action);
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

    public static abstract class Interaction extends Behaviour{
        public final Behaviour continuation;
        Interaction(Action action, Behaviour continuation){
            super(action);
            this.continuation = continuation;
        }
    }

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

    public static abstract class Receiver extends Interaction{
        public final String sender;
        Receiver(Action action, Behaviour continuation, String sender){
            super(action, continuation);
            this.sender = sender;
        }
    }
}
