package pipe.steadystate.algorithm;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

class SequentialJacobiSolver extends AXEqualsBSolver {
    private static final Logger LOGGER = Logger.getLogger(GaussSeidelSolver.class.getName());

    /**
     * Solves Ax = b where b is all zeros in the case of steady state.
     *
     * A is the records which represent a sparse matrix.
     *
     * @param records
     * @param diagonalElements
     * @return
     */
    @Override
    protected Map<Integer, Double> solve(Map<Integer, Map<Integer, Double>> records,
                                         Map<Integer, Double> diagonalElements) {

        Map<Integer, Double> x = initialiseXWithGuess(records);
        Map<Integer, Double> previous = new HashMap<>(x);
        boolean converged = false;
        int iterations = 0;
        while(!converged) {
//        while (iterations < 1) {
            for (Map.Entry<Integer, Map<Integer, Double>> entry : records.entrySet()) {
                Integer state = entry.getKey();
                double aii = diagonalElements.get(state);
                double rowValue = getRowValue(state, entry.getValue(), aii, previous);
                x.put(state, rowValue);
            }
            converged = hasConverged(records, diagonalElements, x);
            iterations++;
            previous.clear();
            previous.putAll(x);
        }

        LOGGER.log(Level.INFO, String.format("Sequential Jacobi took %d iterations to converge", iterations));
        return normalize(x);
    }

}
