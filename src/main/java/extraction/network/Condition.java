package extraction.network;

import extraction.Label;
import org.jetbrains.annotations.NotNull;
import utility.Pair;

import java.util.HashMap;

/**
 * Stores conditional behavior, along with the "then" and "else" branches
 */
public class Condition extends Behaviour {
    public final String expression;
    public final Behaviour thenBehaviour, elseBehaviour;
    private final int hash;

    @Override
    Behaviour realValues(HashMap<String, String> substitutions){
        return this;
    }

    /**
     * Behavior to store conditional statements
     * @param expression The boolean expression to branch on
     * @param thenBehaviour The behavior for when the expression returns true.
     * @param elseBehaviour The behavior for when the expression returns false.
     * @param continuation The continuation for any branch that does not terminate or loop.
     */
    public Condition(String expression, Behaviour thenBehaviour, Behaviour elseBehaviour, @NotNull Behaviour continuation){
        super(Action.CONDITION, continuation);
        this.expression = expression;
        this.thenBehaviour = thenBehaviour;
        this.elseBehaviour = elseBehaviour;
        hash = hashValue();
    }

    public Condition(String expression, Behaviour thenBehaviour, Behaviour elseBehaviour){
        this(expression, thenBehaviour, elseBehaviour, NoneBehaviour.instance);
    }

    public Pair<Label.ConditionLabel.ThenLabel, Label.ConditionLabel.ElseLabel> labelsFrom(String process) {
        var thenLabel = new Label.ConditionLabel.ThenLabel(process, expression);
        var elseLabel = new Label.ConditionLabel.ElseLabel(process, expression);
        return new Pair<>(thenLabel, elseLabel);
    }

    @Override
    public String toString(){
        String s = String.format("if %s then %s else %s", expression, thenBehaviour, elseBehaviour);
        if (!(continuation instanceof BreakBehaviour))
            s += " continue " + continuation;
        return s;
    }

    @Override
    public boolean equals(Behaviour other) {
        if (this == other)
            return true;
        if (!(other instanceof Condition otherC))
            return false;
        return expression.equals(otherC.expression) &&
                thenBehaviour.equals(otherC.thenBehaviour) &&
                elseBehaviour.equals(otherC.elseBehaviour) &&
                continuation.equals(other.continuation);
    }

    @Override
    boolean compareData(Behaviour other){
        return other instanceof Condition con && expression.equals(con.expression);
    }

    @Override
    public int hashCode(){
        return hash;
    }
    private int hashValue(){
        int hash = expression.hashCode() * 31;
        hash += thenBehaviour.hashCode() * 31;
        hash += elseBehaviour.hashCode();
        return hash ^ Integer.rotateRight(continuation.hashCode(), 1);
    }
}
