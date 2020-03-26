package extraction.network.utils;

public interface TreeHost<HostType> {
    <T> T accept(TreeVisitor<T,HostType> visitor);
}
