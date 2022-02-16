package extraction.choreography;

import static extraction.choreography.ChoreographyASTNode.Type.RESUME;

public abstract class ChoreographyBody extends ChoreographyASTNode {

    @Override public int hashCode(){
        return toString().hashCode();
    }

    abstract public String toString();

    @Override public boolean equals(Object o){
        if (this == o)
            return true;
        if (this.getClass() != o.getClass())
            return false;
        var other = (ChoreographyBody)o;
        return this.toString().equals(other.toString());
    }

    /*
    The Kotlin implementation has a class called CommunicationSelection that contains either an Communication or Selection,
    as well as another ChoreographyBody. Instead, I made those two classes extend this one, so they contain the
    continuation themselves. If nod needed, it can just be null.
     */
    public abstract static class Interaction extends ChoreographyBody{
        public abstract ChoreographyBody getContinuation();
        public abstract String getSender();
        public abstract String getReceiver();
    }

    /**
     * Class representing the end of a choreography body, but not termination,
     * instead continuing as some other behaviour.
     * This can be used in procedure bodies, or conditional branches, to indicate that
     * the next body is the continuation of their parent invocation/conditional.
     * Is also used for continuations of invocations or conditionals, to indicate that
     * their continuation is empty.
     */
    public static class NoneBody extends ChoreographyBody{
        public static final NoneBody instance = new NoneBody();
        private NoneBody(){}

        @Override
        public int hashCode(){return 0;}
        @Override
        public String toString(){return "";}
        @Override
        public boolean equals(Object o){
            return o instanceof NoneBody;
        }
        @Override
        public Type getType(){
            return RESUME;
        }

    }

}
