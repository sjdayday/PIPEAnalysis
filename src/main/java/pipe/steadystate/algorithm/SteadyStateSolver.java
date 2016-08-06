package pipe.steadystate.algorithm;

import uk.ac.imperial.state.Record;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Steady state solver interface that all implementing algorithms will adhere to
 */
public interface SteadyStateSolver {

    /**
     * Solves the steady state for the state space exploration file at the given
     * path. It solves Ax = 0
     *
     * @param path to file to be solved
     * @return x solution
     * @throws IOException if IO error occurs 
     */
    Map<Integer, Double> solve(String path) throws IOException;

    /**
     * Solves the steady state from a pre-processed set of records that
     * behave like the A matrix in Ax = 0
     * @param records of matrix 
     * @return x solution 
     */
    Map<Integer, Double> solve(List<Record> records);
}
