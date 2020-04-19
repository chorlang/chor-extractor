package endpointprojection;

import extraction.choreography.*;
import extraction.network.Behaviour;
import extraction.network.ProcessTerm;
import extraction.network.utils.TreeVisitor;
import utility.choreographyStatistics.UsedProcesses;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class uses classes from both the network and choreography package, which is problematic as the class names overlap.
 * Therefore, the classes from choreography is imported, while those form network, with the exception
 * of Behaviour and ProcessTerm, is not.
 */
public class BehaviourProjection implements TreeVisitor<Behaviour, ChoreographyASTNode> {
    private final String processName;
    private final Map<String, Set<String>> usedProcesses;
    private BehaviourProjection(String processName, Map<String, Set<String>> usedProcesses){
        this.processName = processName;
        this.usedProcesses = usedProcesses;
    }

    @Override
    public Behaviour Visit(ChoreographyASTNode hostNode){
        switch (hostNode.getType()){
            case COMMUNICATION:
                return visitCommunication((Communication) hostNode);
            case SELECTION:
                return visitSelection((Selection)hostNode);
            case CONDITION: {
                var host = (Condition)hostNode;
                if (processName.equals(host.process))
                    return new extraction.network.Condition(host.expression, host.thenChoreography.accept(this), host.elseChoreography.accept(this));
                else
                    return Merging.merge(host.thenChoreography.accept(this), host.elseChoreography.accept(this));
            }
            case TERMINATION:
                return extraction.network.Termination.getTermination();
            case PROCEDURE_INVOCATION: {
                var host = (ProcedureInvocation)hostNode;
                if (usedProcesses.get(host.procedure).contains(processName))
                    return new extraction.network.ProcedureInvocation(host.procedure);
                else
                    return extraction.network.Termination.getTermination();
            }
            case PROCEDURE_DEFINITION:
            case CHOREOGRAPHY:
            case PROGRAM:
            default:
                throw new UnsupportedOperationException("");
        }
    }

    private Behaviour visitCommunication(Communication hostNode){
        var continuation = hostNode.continuation.accept(this);
        if (processName.equals(hostNode.getSender()))
            return new extraction.network.Send(hostNode.receiver, hostNode.expression, continuation);
        if (processName.equals(hostNode.getReceiver()))
            return new extraction.network.Receive(hostNode.sender, continuation);
        return continuation;
    }
    private Behaviour visitSelection(Selection hostNode){
        var continuation = hostNode.continuation.accept(this);
        if (processName.equals(hostNode.sender))
            return new extraction.network.Selection(hostNode.receiver, hostNode.label, continuation);
        if (processName.equals(hostNode.receiver)){
            var labels = new HashMap<String, Behaviour>();
            labels.put(hostNode.label, continuation);
            return new extraction.network.Offering(hostNode.sender, labels);
        }
        return continuation;
    }

    static ProcessTerm project(Choreography choreography, String processName){
        var usedProcesses = UsedProcesses.usedProcesses(choreography);

        var procedureProjections = new HashMap<String, Behaviour>();
        for (var procedure : choreography.procedures){
            try {
                if (usedProcesses.get(procedure.name).contains(processName))
                    procedureProjections.put(procedure.name, project(procedure.body, processName, usedProcesses));
            }
            //The java version of Merge differs from the kotlin version, in that merge() may throw IllegalArgumentException
            //instead of MergingException. Just so you know in case of an unexpected bug not present in the kotlin version.
            catch (Merging.MergingException e){
                var newE = new Merging.MergingException("(procedure " + procedure.name+"): " + e.getMessage());
                newE.setStackTrace(e.getStackTrace());
                throw newE;
            }
        }
        return new ProcessTerm(procedureProjections, project(choreography.main, processName, usedProcesses));
    }

    private static Behaviour project(ChoreographyBody body, String processName, Map<String, Set<String>> usedProcesses){
        return body.accept(new BehaviourProjection(processName, usedProcesses));
    }
}
