package pipe.steadystate.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

class SequentialJacobiSolver extends AXEqualsBSolver {
    private static final Logger LOGGER = Logger.getLogger(GaussSeidelSolver.class.getName());


    /**
     * If bounded = true this is the maximum number of iterations the solver is allowed
     */
    private final int maxIterations;

    /**
     * If this value is true then a maximum number of iterations is imposed on the solver
     */
    private boolean bounded = false;


    /**
     * Unbounded constructor
     */
    public SequentialJacobiSolver() {
        maxIterations = 0;
    }
    /**
     * Bounded constructor
     * @param maxIterations maximum number of iterations allowed
     */
    public SequentialJacobiSolver(int maxIterations) {
        this.maxIterations = maxIterations;
        bounded = true;
    }


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
    protected List<Double> solve(Map<Integer, Map<Integer, Double>> records,
                                         Map<Integer, Double> diagonalElements) {

        List<Double> x = initialiseXWithGuessList(records);
        List<Double> previous = new ArrayList<>(x);
        boolean converged = false;
        int iterations = 0;
        while(!converged && canContinue(iterations)) {
            for (Map.Entry<Integer, Map<Integer, Double>> entry : records.entrySet()) {
                Integer state = entry.getKey();
                double aii = diagonalElements.get(state);
                double rowValue = getRowValue(state, entry.getValue(), aii, previous);
                x.set(state, rowValue);
            }
            converged = hasConverged(records, diagonalElements, x);
            iterations++;
            previous.clear();
            previous.addAll(x);
        }

        LOGGER.log(Level.INFO, String.format("Sequential Jacobi took %d iterations to converge", iterations));
        return x;
    }

    /**
     *
     * Determines if the next iteration can run, this will be true
     * if the solver is unbounded and bounded by the maximum number of iterations otherwise
     *
     * @param iterations
     * @return true if the next iteration is allowed to continue
     */
    private boolean canContinue(int iterations) {
        if (bounded && iterations >= maxIterations) {
            LOGGER.log(Level.INFO, "Reached maximum number of iterations. Cutting short!");
        }

        return !bounded || iterations < maxIterations;
    }

}
