package extraction.choreography;

import extraction.Label;

import java.util.List;

public class Multicom extends ChoreographyBody{

    public final List<Label.InteractionLabel> communications;
    public final ChoreographyBody continuation;

    public Multicom(List<Label.InteractionLabel> communications, ChoreographyBody continuation){
        this.communications = communications;
        this.continuation = continuation;

    }

    public final ChoreographyBody getContinuation(){
        return continuation;
    }

    @Override
    public Type getType() {
        return Type.MULTICOM;
    }

    @Override
    public String toString(){
        var out = new StringBuilder("(");
        for (Label.InteractionLabel com : communications){
            out.append(com.toString()).append(" | ");
        }
        out.delete(out.length()-3, out.length()); //Delete trailing " | "
        out.append(") ").append(continuation.toString());
        return out.toString();
    }
}
