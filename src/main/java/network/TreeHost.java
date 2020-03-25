package network;

public interface TreeHost<HostType> {
    <T> T accept(TreeVisitor<T,HostType> visitor);
}
