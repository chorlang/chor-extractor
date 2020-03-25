package network;

public interface Behaviour {

    enum Action {
        CONDITION,
        OFFERING,
        PROCEDURE_INVOCATION,
        RECEIVE,
        SELECTION,
        SEND,
        TERMINATION,
        NETWORK,
        PROCESS_TERM
    }

    /**
     * Java do not store type information at runtime, so this function
     * helps identify the type when working with generic Behavior.
     * @return enum Action of the type of class
     */
    Action getAction();

    /**
     * Performs a deep copy of this behavior
     * @return A deep copy of same type
     */
    Behaviour copy();

    /**
     * Behaviors are expected to overwrite their hashcode
     * to take relevant data into account.
     * @return Hash of relevant stored data structures.
     */
    int hashCode();

    /**
     * Performs proper comparison of objects, rather than reference comparison.
     * @param other Behavior to compare to
     * @return true of the objects are equivalent, false otherwise
     */
    boolean equals(Behaviour other);

    /**
     * Behaviors are expected to overwrite the toString() methods
     * to better print the expressions they contain.
     * @return String of contained expressions, operations, and procedures.
     */
    String toString();


}
