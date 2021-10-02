package extraction.network;

import extraction.Label;
import utility.Pair;

/**
 * Stores conditional behavior, along with the "then" and "else" branches
 */
public class Condition extends Behaviour {
    public final String expression;
    public final Behaviour thenBehaviour, elseBehaviour;

    /**
     * Behavior to store conditional statements
     * @param expression The boolean expression to branch on
     * @param thenBehaviour The behavior for when the expression returns true.
     * @param elseBehaviour The behavior for when the expression returns false.
     */
    public Condition(String expression, Behaviour thenBehaviour, Behaviour elseBehaviour){
        super(Action.CONDITION);
        this.expression = expression;
        this.thenBehaviour = thenBehaviour;
        this.elseBehaviour = elseBehaviour;
    }

    public Pair<Label.ConditionLabel.ThenLabel, Label.ConditionLabel.ElseLabel> labelsFrom(String process) {
        var thenLabel = new Label.ConditionLabel.ThenLabel(process, expression);
        var elseLabel = new Label.ConditionLabel.ElseLabel(process, expression);
        return new Pair<>(thenLabel, elseLabel);
    }

    public String toString(){
        return String.format("if %s then %s else %s", expression, thenBehaviour, elseBehaviour);
    }

    public boolean equals(Behaviour other) {
        if (this == other)
            return true;
        if (other.action != Action.CONDITION)
            return false;
        Condition otherC = (Condition)other;
        return expression.equals(otherC.expression) &&
                thenBehaviour.equals(otherC.thenBehaviour) &&
                elseBehaviour.equals(otherC.elseBehaviour);
    }

    public int hashCode(){
        int hash = expression.hashCode() * 31;
        hash += thenBehaviour.hashCode();
        return hash * 31 + elseBehaviour.hashCode();
    }
}
