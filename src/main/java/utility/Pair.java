package utility;

import org.antlr.v4.runtime.misc.EqualityComparator;
import org.jetbrains.annotations.NotNull;

public class Pair<FirstT, SecondT>{
    public FirstT first;
    public SecondT second;
    public Pair(FirstT first, SecondT second){
        this.first = first;
        this.second = second;
    }

    public int hashCode() {
        return first.hashCode() + second.hashCode() * 31;
    }

    public boolean equals(Object o){
        if (o == null){
            return false;
        }
        if (getClass() != o.getClass()){
            return false;
        }
        var pair = (Pair<?, ?>)o;
        return first.equals(pair.first) && second.equals(pair.second);
    }
}
