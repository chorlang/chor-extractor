package extraction.network;

public interface TreeVisitor<ReturnType, TreeType> {
    ReturnType Visit(TreeType hostNode);
}