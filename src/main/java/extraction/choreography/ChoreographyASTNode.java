package extraction.choreography;

import extraction.network.utils.TreeHost;
import extraction.network.utils.TreeVisitor;

public abstract class ChoreographyASTNode implements TreeHost<ChoreographyASTNode> {
    public <T> T accept(TreeVisitor<T, ChoreographyASTNode> visitor) {
        return visitor.Visit(this);
    }

    public enum Type {
        CHOREOGRAPHY,
        COMMUNICATION,
        CONDITION,
        PROCEDURE_DEFINITION,
        PROCEDURE_INVOCATION,
        PROGRAM,
        SELECTION,
        TERMINATION,
        MULTICOM
    }

    /**
     * @return The enum value of ChoreographyASTNode.Type corresponding to the concrete class of this object
     */
    public abstract Type getType();
}
