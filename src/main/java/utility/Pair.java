package utility;

import org.antlr.v4.runtime.misc.EqualityComparator;
import org.jetbrains.annotations.NotNull;

public class Pair<FirstT, SecondT> implements EqualityComparator<Pair<FirstT, SecondT>>{
    public FirstT first;
    public SecondT second;
    public Pair(FirstT first, SecondT second){
        this.first = first;
        this.second = second;
    }

    @Override
    public int hashCode(Pair<FirstT, SecondT> p) {
        return p.first.hashCode() + p.second.hashCode() * 31;
    }

    @Override
    public boolean equals(Pair<FirstT, SecondT> p1, Pair<FirstT, SecondT> p2) {
        return p1.first.equals(p2.first) &&
                p1.second.equals(p2.second);
    }
}
