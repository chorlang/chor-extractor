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
            case MULTICOM:
                throw new UnsupportedOperationException("Projection of Multicom not yet implemented");
            case CONDITION: {
                var host = (Condition)hostNode;
                Behaviour thenBehaviour = host.thenChoreography.accept(this);
                Behaviour elseBehaviour = host.elseChoreography.accept(this);
                Behaviour continuation = host.continuation instanceof ChoreographyBody.NoneBody ?
                        Behaviour.NoneBehaviour.instance : host.continuation.accept(this);//NoneBehaviour is different from BreakBehaviour in networks, but not in choreographies
                if (processName.equals(host.process)) {
                    return new extraction.network.Condition(host.expression, thenBehaviour, elseBehaviour, continuation);
                }
                else {
                    return Merging.merge(thenBehaviour, elseBehaviour, continuation);
                }
            }
            case TERMINATION:
                return extraction.network.Termination.instance;
            case PROCEDURE_INVOCATION: {
                var host = (ProcedureInvocation)hostNode;
                if (usedProcesses.get(host.procedure).contains(processName)) {
                    Behaviour continuation = host.continuation instanceof ChoreographyBody.NoneBody ?
                            Behaviour.NoneBehaviour.instance : host.continuation.accept(this);
                    return new extraction.network.ProcedureInvocation(host.procedure, host.parameters, continuation);
                }
                else
                    return extraction.network.Termination.instance;
            }
            case NONE:
                return Behaviour.BreakBehaviour.instance;
            case PROCEDURE_DEFINITION:
            case CHOREOGRAPHY:
            case PROGRAM:
            default:
                throw new UnsupportedOperationException("Could not project behaviour of unexpected type");
        }
    }

    private Behaviour visitCommunication(Communication hostNode){
        Behaviour continuation = hostNode.continuation.accept(this);
        if (processName.equals(hostNode.getSender()))
            return new extraction.network.Send(hostNode.receiver, hostNode.expression, continuation);
        if (processName.equals(hostNode.getReceiver()))
            return new extraction.network.Receive(hostNode.sender, continuation);
        return continuation;
    }
    private Behaviour visitSelection(Selection hostNode){
        Behaviour continuation = hostNode.continuation.accept(this);
        if (processName.equals(hostNode.sender))
            return new extraction.network.Selection(hostNode.receiver, hostNode.label, continuation);
        if (processName.equals(hostNode.receiver)){
            var labels = new HashMap<String, Behaviour>();
            labels.put(hostNode.label, continuation);
            return new extraction.network.Offering(hostNode.sender, labels);
        }
        return continuation;
    }

    /**
     * Projects the provided choreography for the provided process
     * @param choreography The choreography to project
     * @param processName The process of the projection
     * @return A process term representing the projection of the process.
     */
    static ProcessTerm project(Choreography choreography, String processName){
        //Maps from the names of every procedure, to a set of the processes used by that procedure
        var usedProcesses = UsedProcesses.usedProcesses(choreography);

        //Project all procedures
        var procedureProjections = new HashMap<String, Behaviour>();
        for (ProcedureDefinition procedure : choreography.procedures){
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
        //Project main, then put all the projections in a ProcessTerm
        return new ProcessTerm(procedureProjections, project(choreography.main, processName, usedProcesses));
    }

    /**
     * Projects a choreography body to a Behaviour for a specific process
     * @param body The choreography body to project
     * @param processName The name of the process to project to
     * @param usedProcesses A mapping from all procedure names of the choreography, to a set containing
     *                      the names of processes used in that procedure.
     * @return A Behaviour corresponding to the actions given by the choreography for the specified process.
     */
    private static Behaviour project(ChoreographyBody body, String processName, Map<String, Set<String>> usedProcesses){
        return body.accept(new BehaviourProjection(processName, usedProcesses));
    }
}
