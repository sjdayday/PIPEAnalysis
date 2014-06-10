package pipe.steadystate.algorithm;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Solves the Steady state (Ax = 0) using Gauss Seidel iterative techniques
 */
public final class GaussSeidelSolver extends AXEqualsBSolver {
    private static final Logger LOGGER = Logger.getLogger(GaussSeidelSolver.class.getName());

    /**
     * Solves Ax = b where b is all zeros in the case of steady state.
     *
     * A is the records which represent a sparse matrix.
     *
     * @param records
     * @param diagonalElements
     * @return unnormalized x
     */
    @Override
    protected List<Double> solve(Map<Integer, Map<Integer, Double>> records,
                                         Map<Integer, Double> diagonalElements) {

        List<Double> x = initialiseXWithGuessList(records);
        boolean converged = false;
        int iterations = 0;
        while(!converged) {
            for (Map.Entry<Integer, Map<Integer, Double>> entry : records.entrySet()) {
                Integer state = entry.getKey();
                double aii = diagonalElements.get(state);
                double rowValue = getRowValue(state, entry.getValue(), aii, x);
                x.set(stateToIndex.get(state), rowValue);
            }
            converged = hasConverged(records, diagonalElements, x);
            iterations++;
        }

        LOGGER.log(Level.INFO, String.format("Took %d iterations to converge", iterations));
        return x;
    }



}
