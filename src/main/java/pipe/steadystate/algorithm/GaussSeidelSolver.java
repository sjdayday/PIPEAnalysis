package pipe.steadystate.algorithm;

import java.util.Map;

/**
 * Solves the Steady state (Ax = 0) using Gauss Seidel iterative techniques
 */
public class GaussSeidelSolver extends AXEqualsBSolver {

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
        boolean converged = false;
        int iterations = 0;
        while(!converged) {
            for (Map.Entry<Integer, Map<Integer, Double>> entry : records.entrySet()) {
                Integer state = entry.getKey();
                double aii = diagonalElements.get(state);
                double rowValue = getRowValue(state, entry.getValue(), aii, x);
                x.put(state, rowValue);
            }
            converged = hasConverged(records, diagonalElements, x);
            iterations++;
        }
        System.out.println("took " + iterations + " iterations to converge!");
        return normalize(x);
    }



}
