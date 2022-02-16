package extraction.choreography;

public class Condition extends ChoreographyBody {
    public final String process, expression;
    public final ChoreographyBody thenChoreography, elseChoreography, continuation;

    private final Type chorType = Type.CONDITION;
    public Type getType(){
        return chorType;
    }

    public Condition(String process, String expression, ChoreographyBody thenChoreography, ChoreographyBody elseChoreography, ChoreographyBody continuation){
        this.process = process;
        this.expression = expression;
        this.thenChoreography = thenChoreography;
        this.elseChoreography = elseChoreography;
        this.continuation = continuation;
    }
    public Condition(String process, String expression, ChoreographyBody thenChoreography, ChoreographyBody elseChoreography){
        this(process, expression, thenChoreography, elseChoreography, NoneBody.instance);
    }

    @Override
    public String toString() {
        return String.format("if %s.%s then %s else %s%s%s", process, expression, thenChoreography.toString(),
                elseChoreography.toString(), continuation instanceof NoneBody ? "" : "continue ", continuation.toString());
    }
}
