package extraction.network;

import extraction.network.utils.TreeHost;
import extraction.network.utils.TreeVisitor;

public abstract class Behaviour implements TreeHost<Behaviour> {

    public <T> T accept(TreeVisitor<T, Behaviour> visitor) {
        return visitor.Visit(this);
    }

    public enum Action {
        CONDITION,
        OFFERING,
        PROCEDURE_INVOCATION,
        RECEIVE,
        SELECTION,
        SEND,
        TERMINATION,
        NETWORK,
        PROCESS_TERM,
        ACQUAINT,
        FAMILIARIZE
    }

    /**
     * Java do not store type information at runtime, so this function
     * helps identify the type when working with generic Behavior.
     * @return enum Action of the type of class
     */
    public abstract Action getAction();

    /**
     * Performs a deep copy of this behavior
     * @return A deep copy of same type
     */
    public abstract Behaviour copy();

    /**
     * Behaviors are expected to overwrite their hashcode
     * to take relevant data into account.
     * @return Hash of relevant stored data structures.
     */
    public abstract int hashCode();

    /**
     * Performs proper comparison of objects, rather than reference comparison.
     * @param other Behavior to compare to
     * @return true of the objects are equivalent, false otherwise
     */
    public abstract boolean equals(Behaviour other);

    /**
     * Behaviors are expected to overwrite the toString() methods
     * to better print the expressions they contain.
     * @return String of contained expressions, operations, and procedures.
     */
    public abstract String toString();


}
