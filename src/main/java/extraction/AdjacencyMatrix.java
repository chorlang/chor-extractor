package extraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Adjacency matrix for undirected graphs.
 * It is specialized to keep track of which processes are familiar with each other. If they are not familiar, then
 * they are not allowed to communicate.
 *
 * Each process gets an integer ID between 0 and n-1. The variable "matrix" stores a list of booleans. Each list in
 * matrix has the same number of entries as its index. This means that the lists stores relationships for all processes
 * with an ID lower that the index of the list. If for example you want to look up if processes with ID 5 and 7 know
 * each other, you would get the list at index 7 from matrix, then look and index 5 on that list.
 */
public class AdjacencyMatrix {
    public final ArrayList<ArrayList<Boolean>> matrix;
    private final HashMap<String, Integer> indexMap;

    /**
     * Creates a new undirected adjacency matrix where all processes know each other.
     * @param processNames List of initial processes, which all know of each other
     */
    public AdjacencyMatrix(List<String> processNames){
        int n = processNames.size();
        matrix = new ArrayList<>(n);
        for (int i = 1; i <= n; i++){
            var column = new ArrayList<Boolean>(i); //1 entry for process 0, n entries for process n-1
            for (int j = 0; j < i; j++){            //once per entry
                column.add(Boolean.TRUE);
            }
            matrix.add(column);
        }
        indexMap = new HashMap<>(n);                //Map process names to an ID
        for (int i = 0; i < n; i++){
            indexMap.put(processNames.get(i), i);
        }
    }

    /**
     * Constructor that copies the adjacency matrix, and index map from the parameters.
     * Used for making copies.
     */
    private AdjacencyMatrix(ArrayList<ArrayList<Boolean>> prevMatrix, HashMap<String, Integer> prevIndex){
        //Makes a deep copy of the matrix
        matrix = new ArrayList<>(prevMatrix.size());
        for (ArrayList<Boolean> row : prevMatrix){
            matrix.add(new ArrayList<>(row));
        }

        //Indexes do not need a deep copy, since the entries are permanent once added.
        indexMap = new HashMap<>(prevIndex);
    }

    /**
     * Adds a new process to the matrix, and sets up relationships
     * @param parent The name of the parent process which spawned the child
     * @param child The newly spawned process
     */
    public void spawn(String parent, String child){
        int n = indexMap.size();                    //Number of existing processes, not counting child
        indexMap.put(child, n);                     //Give process n+1 id #n, since they are 0-indexed
        var newcolumn = new ArrayList<Boolean>(n+1);  //There are n+1 processes in the network (including the new process
        for (int i = 0; i < n+1; i++){
            newcolumn.add(Boolean.FALSE);           //New process does not know anyone
        }
        newcolumn.set(n, Boolean.TRUE);             //Process knows itself
                                                    //Arguably, if it is checked if a process knows itself, an error should be thrown.
        matrix.add(newcolumn);
        introduce(parent, child);                   //Parents and child know each other

    }

    /**
     * Update the matrix to say two processes know of each other
     * @param p Process to be introduced to q
     * @param q Process to be introduced to p
     */
    public void introduce(String p, String q){
        int higher = indexMap.get(p);
        int lower = indexMap.get(q);
        if (lower > higher){        //Make higher the highest ID/index, and lower the lowest
            int tmp = higher;
            higher = lower;
            lower = tmp;
        }
        matrix.get(higher).set(lower, Boolean.TRUE);
    }

    /**
     * Returns true, if process p and q have already been introduced.
     */
    public boolean isIntroduced(String p, String q){
        int higher = indexMap.get(p);
        int lower = indexMap.get(q);
        if (lower > higher){        //Make higher the highest ID/index, and lower the lowest
            int tmp = higher;
            higher = lower;
            lower = tmp;
        }
        return matrix.get(higher).get(lower);
    }

    /**
     * Create a copy of this matrix
     */
    public AdjacencyMatrix copy(){
        return new AdjacencyMatrix(matrix, indexMap);
    }

    @Override
    public boolean equals(Object o){
        if (o == this)
            return true;
        if (!(o instanceof AdjacencyMatrix other) ||    //Ensure both adj-matrices
                matrix.size() != other.matrix.size() || //Ensure matrices can be compared
                !indexMap.equals(other.indexMap))       //Ensure the processes in the matrix are the same
            return false;
        for (int i = 0; i < matrix.size(); i++){        //Compare element wise
            if (!matrix.get(i).equals(other.matrix.get(i)))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode(){
        //It should create the hash from the hashes of its elements, so this should suffice
        return matrix.hashCode() * 31 + indexMap.hashCode();
    }


}
