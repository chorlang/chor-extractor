package utility.ChorGen;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.ArrayList;

/*
 * AST-like representation of choreographies.
 */
public class Choreography {

    /*
     * A body is a map of names to name bodies.
     * We identify the body's body with a special name "main".
     */
    private HashMap<String,ChoreographyNode> procedures;

    public Choreography() {
        procedures = new HashMap<String,ChoreographyNode>();
    }

    /*
     * Extends this body with a new name.
     */
    public void addProcedure(String s,ChoreographyNode n) {
        procedures.put(s,n);
    }

    /*
     * Returns the body of a name in this body.
     */
    public ChoreographyNode getProcedure(String s) {
        return procedures.get(s);
    }

    /*
     * Checks whether a body is well-formed:
     * - no self-communications;
     * - main is defined;
     * - all called procedures are defined;
     * - no calls to main;
     * - all calls are guarded.
     */
    public boolean isValid() {
        // no self-communications
        for (ChoreographyNode p:procedures.values())
            if (NoSelfCommunications.run(p)) return false;

        // main is defined
        if (!procedures.containsKey("main")) return false;

        // all called procedures are defined and there are no calls to main
        for (String id:usedProcedures())
            if ((!procedures.containsKey(id)) || (id.equals("main"))) return false;

        // all calls are guarded -- except possibly main
        for (String id:procedures.keySet())
            if (!id.equals("main") && (procedures.get(id) instanceof CallNode)) return false;

        // done!
        return true;
    }

    /*
     * Not-so-clever amendment. Lots of stuff here.
     */

    /*
     * Returns the list of processesInChoreography used in each name.
     */
    private HashMap<String,HashSet<String>> usedProcesses() {
        Set<String> keys = procedures.keySet();

        // first: maps from body to process names and name calls in it
        HashMap<String,HashSet<String>> calls = new HashMap<String,HashSet<String>>();
        for (String name:keys)
            calls.put(name,UsedProcedures.run(procedures.get(name)));

        // second: iteratively compute a fixpoint...
        HashMap<String,HashSet<String>> usedProcesses = new HashMap<String,HashSet<String>>(),
            auxUsedProcesses = new HashMap<String,HashSet<String>>();

        for (String name:keys)
            auxUsedProcesses.put(name,UsedProcesses.run(procedures.get(name)));
        while (!usedProcesses.equals(auxUsedProcesses)) {
            for (String name:keys)
                usedProcesses.put(name,auxUsedProcesses.get(name));
            for (String name:keys) {
                HashSet<String> processSet = new HashSet<String>(usedProcesses.get(name));
                for (String called:calls.get(name))
                    processSet.addAll(usedProcesses.get(called));
                auxUsedProcesses.put(name,processSet);
            }
        }

        return usedProcesses;
    }

    /*
     * Now we can amend the body in a not-so-clever way.
     * For each name, we add selections to all processesInChoreography it uses after each conditional.
     * We return a new body (design option).
     */
    public Choreography amend() {
            HashMap<String,HashSet<String>> usedProcesses = usedProcesses();

            Choreography amended = new Choreography();

            for (String name:procedures.keySet())
                amended.addProcedure(name,AmendNode.run(procedures.get(name),usedProcesses.get(name)));

        return amended;
    }

    /*
     * String representation, including trailing newline because I'm lazy.
     */
    public String toString() {
        String result = "";
        for (String k:procedures.keySet()) {
	        if (!k.equals("main"))
		        result += "def " + k + " { " + procedures.get(k).toString() + " }\n";
        }
	    result += "main { " + procedures.get("main").toString() + " }\n";
        return result;
    }

    /*
     * Returns the list of procedures used in this body.
     */
    private HashSet<String> usedProcedures() {
        HashSet<String> result = new HashSet<String>();
        for (ChoreographyNode p:procedures.values())
            result.addAll(UsedProcedures.run(p));
        return result;
    }

    /*
     * Checks whether a body has deadcode (procedures that are never used).
     */
    public boolean hasDeadCode() {
        ArrayList<String> toInspect = new ArrayList<>();
        toInspect.add("main");
        HashSet<String> used = new HashSet<>();
        used.add("main");
        HashSet<String> done = new HashSet<>();
        while (!toInspect.isEmpty()) {
            //System.out.println("Inspecting "+toInspect.get(0));
            HashSet<String> newNames = UsedProcedures.run(procedures.get(toInspect.get(0)));
            done.add(toInspect.get(0));
            toInspect.addAll(newNames);
            toInspect.removeAll(done);
            used.addAll(newNames);
        }
        return (!used.equals(procedures.keySet()));
    }

    /*
     * For testing and debugging purposes only.
     */
    public static void main(String[] args) {
        Choreography c = new Choreography();

        TerminationNode tn = new TerminationNode();
        CallNode cx = new CallNode("X"),
            cy = new CallNode("Y"),
            cz = new CallNode("Z"),
            cm = new CallNode("main");

        // for X
        ConditionalNode i2 = new ConditionalNode("q","t2",cx,cz);
        c.addProcedure("X",i2);

        // for Y
        ConditionalNode i3 = new ConditionalNode("p","t3",tn,cx);
        CommunicationNode m3 = new CommunicationNode("p","q","e3",i3);
        c.addProcedure("Y",m3);

        // for Z
        CommunicationNode m4 = new CommunicationNode("r","q","e4",tn);
        c.addProcedure("Z",m4);

        // for main
        SelectionNode s1 = new SelectionNode("p","r","L",cx);
        SelectionNode s2 = new SelectionNode("q","r","R",cy);
        CommunicationNode m2 = new CommunicationNode("p","q","e2",s2);
        ConditionalNode i1 = new ConditionalNode("p","t1",s1,m2);
        CommunicationNode m1 = new CommunicationNode("p","q","e1",i1);
        c.addProcedure("main",m1);

        System.out.print(c);
        System.out.println(c.usedProcedures());
        System.out.println(c.isValid());

        HashMap<String,HashSet<String>> usedProcesses = c.usedProcesses();
        for (String key:c.procedures.keySet())
            System.out.println(key + " -> " + usedProcesses.get(key));

        Choreography newC = c.amend();
        System.out.println();
        System.out.print(newC);
        System.out.println(newC.usedProcedures());
        System.out.println(newC.isValid());
    }

}
