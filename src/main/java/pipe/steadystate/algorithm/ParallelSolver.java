package pipe.steadystate.algorithm;

/**
 *
 * Parallel solver attempts to solve the matrix via Jacobi.
 * However Jacobi is only guaranteed to converge for diagonally dominent matricies.
 *
 * If Jacobi is not guaranteed to converge then the power method and a sequential guass sidel method are used.
 * It takes the first converging result.
 */

import java.util.Map;

public class ParallelSolver extends AXEqualsBSolver
{
    @Override
    protected Map<Integer, Double> solve(Map<Integer, Map<Integer, Double>> records,
                                         Map<Integer, Double> diagonalElements) {
        return null;
    }
}
