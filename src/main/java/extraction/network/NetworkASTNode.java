package extraction.network;

import extraction.network.utils.TreeHost;
import extraction.network.utils.TreeVisitor;

public abstract class NetworkASTNode implements TreeHost<NetworkASTNode> {

    protected NetworkASTNode(Action action) {
        this.action = action;
    }

    public <T> T accept(TreeVisitor<T, NetworkASTNode> visitor) {
        return visitor.Visit(this);
    }

    public final Action action;

    /**
     * Network nodes are expected to overwrite the toString() methods
     * to better print the expressions they contain.
     * @return String of contained expressions, operations, and procedures.
     */
    public abstract String toString();
}
