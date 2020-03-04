package network;

/**
 * Stores conditional behavior, along with the "then" and "else" branches
 */
public class Condition implements Behaviour {
    String expression;
    Behaviour thenBehaviour, elseBehaviour;

    /**
     * Behavior to store conditional statements
     * @param expression The boolean expression to branch on
     * @param thenBehaviour The behavior for when the expression returns true.
     * @param elseBehaviour The behavior for when the expression returns false.
     */
    public Condition(String expression, Behaviour thenBehaviour, Behaviour elseBehaviour){
        this.expression = expression;
        this.thenBehaviour = thenBehaviour;
        this.elseBehaviour = elseBehaviour;
    }

    public String toString(){
        return String.format("if %s then %s else %s", expression, thenBehaviour, elseBehaviour);
    }

    public Action getAction() {
        return Action.condition;
    }

    public Behaviour copy() {
        return new Condition(expression, thenBehaviour.copy(), elseBehaviour.copy());
    }

    public boolean equals(Behaviour other) {
        if (this == other)
            return true;
        if (other.getAction() != Action.condition)
            return false;
        Condition otherC = (Condition)other;
        return expression.equals(otherC.expression) &&
                thenBehaviour.equals(otherC.thenBehaviour) &&
                elseBehaviour.equals(otherC.elseBehaviour);
    }
}
