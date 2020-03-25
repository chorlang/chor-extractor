package extraction.network;

public interface NetworkASTVisitor<T> {
    T visit(Network n);
    T visit(Condition n);
    T visit(Offering n);
    T visit(ProcedureInvocation n);
    T visit(ProcessTerm n);
    T visit(Receive n);
    T visit(Selection n);
    T visit(Send n);
    T visit(Termination n);
}
