package utility.ChorGen;

import java.util.*;

/*
 * Class for generating random choreographies, based on some parameters.
 */
public class ChoreographyGenerator {

    private final int LENGTH, NUM_PROCESSES, NUM_IFS, NUM_PROCEDURES, NUM_SPAWNED;
    private final long SEED;
    private int messageCounter, labelCounter, ifCounter;
    private String[] processNames, procedureNames;
    private Random generator;

    /*
     * A constructor with some default values, for testing purposes.
     */
    private ChoreographyGenerator() {
        this.generator = new Random();
        SEED = generator.nextLong();
        generator.setSeed(SEED);
        this.LENGTH = 30;
        this.NUM_PROCESSES = 5;
        this.processNames = new String[]{"p","q","r","s","t"};
        this.NUM_IFS = 0;
        this.NUM_PROCEDURES = 0;
        this.procedureNames = new String[]{};
        NUM_SPAWNED=0;
        reset();
    }

    /*
     * The real constructor.
     * - seed: initial seed for the random number generator
     * - length: maximum length of each name/main body
     * - numProcesses: maximum number of processesInChoreography used
     * - numIfs: maximum number of conditional instructions (globally)
     * - numProcedures: maximum number of name definitions (excluding main)
     * Most parameters are bounds because we do not check for duplicates.
     * This constructor also initializes a set of process and name names that are
     * shared among all generated choreographies.
     */
    public ChoreographyGenerator(long seed, int length, int numProcesses, int numIfs, int numProcedures, int numSpawned)
        throws GeneratorException {
        if (numProcedures >= length || numSpawned >= numProcesses)
            throw new GeneratorException("Total size of the body must be at least the number of procedures.");
        this.SEED = seed;
        this.generator = new Random(seed);
        this.LENGTH = length;
        this.NUM_PROCESSES = numProcesses;
        this.NUM_IFS = numIfs;
        this.NUM_PROCEDURES = numProcedures;
        this.NUM_SPAWNED = numSpawned;
        init();
    }

    /*
     * The same, but without the seed.
     */
    public ChoreographyGenerator(int length, int numProcesses, int numIfs, int numProcedures, int numSpawned)
        throws GeneratorException {
        if (numProcedures >= length || numSpawned >= numProcesses)
            throw new GeneratorException("Total size of the body must be at least the number of procedures.");
        this.generator = new Random();
        SEED = generator.nextLong();
        generator.setSeed(SEED);
        this.LENGTH = length;
        this.NUM_PROCESSES = numProcesses;
        this.NUM_IFS = numIfs;
        this.NUM_PROCEDURES = numProcedures;
        this.NUM_SPAWNED = numSpawned;
        init();
    }

    /*
     * Initializes the auxiliary attributes.
     */
    private void init() {
        // we generate process names randomly as strings of lowercase letters
        HashSet<String> auxNames = new HashSet<String>();
        int bound = Math.toIntExact(Math.round(Math.ceil(Math.log(NUM_PROCESSES)/Math.log(26))));
        List<String> keywords = Arrays.asList("if","def","then","else","stop", "continue", "spawn", "with", "endif", "main");

        while (auxNames.size() < NUM_PROCESSES) {
            String name = "";
            for (int j=0; j<bound; j++)
                name += (char)(generator.nextInt(26)+97);
            if (!keywords.contains(name))
                auxNames.add(name);
        }
        this.processNames = auxNames.toArray(new String[0]);

        // we generate name names as strings of uppercase letters
        auxNames = new HashSet<String>();
        if (NUM_PROCEDURES > 0) {
            bound = (NUM_PROCEDURES == 1 ? 1 : Math.toIntExact(Math.round(Math.ceil(Math.log(NUM_PROCEDURES)/Math.log(26)))));
            while (auxNames.size() < NUM_PROCEDURES) {
                String name = "";
                for (int j=0; j<bound; j++)
                    name += (char)(generator.nextInt(26)+65);
                auxNames.add(name);
            }
        }
        this.procedureNames = auxNames.toArray(new String[0]);
    }

    /*
     * Returns the (original) seed in this object.
     */
    public long getSeed() {
        return SEED;
    }

    /*
     * Resets the internal counters, so that a new body can be generated.
     */
    private void reset() {
        this.messageCounter = 0;
        this.labelCounter = 0;
        this.ifCounter = 0;
    }

    /*
     * Generate a random array of ints with a given sum.
     */
    private int[] randomArray(int size, int sum) {
        int[] result = new int[size];
        for (int i=0; i<size; i++)
            result[i] = 0;
        for (int i=0; i<sum; i++)
            result[generator.nextInt(size)]++;
        return result;
    }

    /**
     * Sets at least one, possible more entries in a boolean array to true.
     * @param arr The array to modify.
     */
    private void setOneOrMore(boolean[] arr){
        for (int i = 0; i < arr.length; i++){
            arr[generator.nextInt(0,arr.length)] = true;
        }
    }

    /*
     * The method we're interested in.
     */
    public Choreography generate() {
        reset();
        Choreography c = new Choreography();

        // control how the if statements are distributed among procedures
        int[] ifArray = randomArray(NUM_PROCEDURES+1,NUM_IFS);

        // same for name sizes -- but they must be at least 1
        int[] procedureSizes = randomArray(NUM_PROCEDURES+1,LENGTH-NUM_PROCEDURES);
        for (int i=1; i<=NUM_PROCEDURES; i++)
            procedureSizes[i]++;

        // distribute number of spawns
        int[] spawnSizes = randomArray(NUM_PROCEDURES+1, NUM_SPAWNED);

        c.addProcedure("main",generateBody(procedureSizes[0],ifArray[0],spawnSizes[0],false));

        int index = 1;
        for (String name:procedureNames) {
            c.addProcedure(name,generateBody(procedureSizes[index],ifArray[index],spawnSizes[index],false));
            index++;
        }
        return c;

    }

    /*
     * The key ingredient: generating a name body.
     * Parameters:
     * - size is the [exact] number of actions in the body of this name
     * - numIfs is the [maximum] number of if statements in the body of this name
     * - doResume is true if this node must have a branch that resumes a parent, or false if all
     *   branches must terminate or invoke a procedure.
     * Because of the use of random numbers, it is possible that there are less if statements
     * than allowed. The likelihood decreases as size/numIfs increases.
     */
    private ChoreographyNode generateBody(int size, int numIfs, int numSpawns, boolean doResume) {
        if (size == 0) {
            if (doResume)//No more branching can be done, so thins branch must resume
                return new NothingNode();
            int endType = generator.nextInt(NUM_PROCEDURES+1);
            if (endType == NUM_PROCEDURES)
                return new TerminationNode();
            else
                return new CallNode(procedureNames[endType]);
        }
        else if (generator.nextDouble()*size < numSpawns){
            String parent = processNames[generator.nextInt(NUM_PROCESSES)];
            String child = processNames[generator.nextInt(NUM_PROCESSES)];
            while (parent.equals(child))
                child = processNames[generator.nextInt(NUM_PROCESSES)];
            return new SpawnNode(parent, child, generateBody(size-1, numIfs, numSpawns-1, doResume));
        }
        else if (generator.nextDouble()*size < numIfs) {
            //Array of which branch is allowed to resume
            boolean[] resumes = new boolean[2];//Initializes to false
            int[] ifs;
            int[] sizes;
            ChoreographyNode continuation;
            if (generator.nextBoolean()) {
                //Distribute the remaining number of terms and conditionals between the pre-action,
                //the then and else branches, and the continuation.
                ifs = randomArray(4, numIfs-1);
                sizes = randomArray(4, size-numIfs);
                continuation = generateBody(sizes[3], ifs[3], numSpawns, doResume);
                setOneOrMore(resumes);//Ensures at least one branch resumes to the continuation
            }
            else {
                if (doResume)//If an ancestor conditional has a continuation to return to.
                    setOneOrMore(resumes);
                //Distribute the remaining number of terms and conditionals between
                //the pre-action and the branches.
                ifs = randomArray(3, numIfs-1);
                sizes = randomArray(3, size-numIfs);
                continuation = new NothingNode();
            }
            String decider = processNames[generator.nextInt(NUM_PROCESSES)];
            String condition = "c" + (++ifCounter);

            return new ConditionalNode(decider, condition,
                                       generateBody(sizes[0]+ifs[0],ifs[0],numSpawns, false),
                                       generateBody(sizes[1]+ifs[1],ifs[1],numSpawns,resumes[0]),
                                       generateBody(sizes[2]+ifs[2],ifs[2],numSpawns,resumes[1]),
                                       continuation);
        }
        else {
            String sender = processNames[generator.nextInt(NUM_PROCESSES)];
            String receiver = processNames[generator.nextInt(NUM_PROCESSES)];
            while (receiver.equals(sender))
                receiver = processNames[generator.nextInt(NUM_PROCESSES)];
            if (generator.nextBoolean()) {
                String message = "m" + (++messageCounter);
                return new CommunicationNode(sender, receiver, message, generateBody(size-1,numIfs,numSpawns,doResume));
            }
            else {
                String label = "l" + (++labelCounter);
                return new SelectionNode(sender, receiver, label, generateBody(size-1,numIfs,numSpawns,doResume));
            }
        }

    }

    /*
     * For testing purposes only.
     */
    public static void main(String[] args) throws GeneratorException {
        /*
        Random generator = new Random();
        generator.setSeed(123);
        for (int i=0; i<10; i++)
            System.out.println(""+generator.nextDouble());
        */

        ChoreographyGenerator // g1 = new ChoreographyGenerator(),
            g2 = new ChoreographyGenerator(123,15,4,5,2);
        //g3 = new ChoreographyGenerator(15,4,5,2);

        // System.out.println("g1:");
        // System.out.println(Arrays.toString(g1.processNames));
        // System.out.println(Arrays.toString(g1.procedureNames));
        // System.out.println(g1.generateBody().toString());
        // System.out.println(g1.generateBody().toString());
        // System.out.println(g1.generateBody().toString());
        // System.out.println(g1.generate().toString());
        // System.out.println();

        System.out.println("g2:");
        // System.out.println(Arrays.toString(g2.processNames));
        // System.out.println(Arrays.toString(g2.procedureNames));
        // System.out.println(g2.generateBody().toString());
        // System.out.println(g2.generateBody().toString());
        // System.out.println(g2.generateBody().toString());
        System.out.println(g2.generate().toString());
        System.out.println();

        // System.out.println("g3:");
        // System.out.println(Arrays.toString(g3.processNames));
        // System.out.println(Arrays.toString(g3.procedureNames));
        // System.out.println(g3.generateBody().toString());
        // System.out.println(g3.generateBody().toString());
        // System.out.println(g3.generateBody().toString());
        // System.out.println(g3.generate().toString());
        // System.out.println();
    }
}
