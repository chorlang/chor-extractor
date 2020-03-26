package extraction.network.utils;

public interface TreeVisitor<ReturnType, TreeType> {
    ReturnType Visit(TreeType hostNode);
}