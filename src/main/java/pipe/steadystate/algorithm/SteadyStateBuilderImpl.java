package pipe.steadystate.algorithm;

import java.util.concurrent.ExecutorService;

/**
 * Builds the underlying implementations of the steady state that this package contains
 * <p/>
 * Note that a Jacobi parallel solver is preferred to the power method
 */
public final class SteadyStateBuilderImpl implements SteadyStateBuilder

{
    /**
     * @return Gauss Seidel sequential solver
     */
    @Override
    public SteadyStateSolver buildGaussSeidel() {
        return new GaussSeidelSolver();
    }

    /**
     * @return sequential Jacobi solver
     */
    @Override
    public SteadyStateSolver buildSequentialJacobi() {
        return new SequentialJacobiSolver();
    }

    /**
     * @param maxIterations the maximum number of iterations Jacobi can run for
     * @return a bounded sequential Jacobi solver
     */
    @Override
    public SteadyStateSolver buildBoundedSequentialJacobi(int maxIterations) {
        return new SequentialJacobiSolver(maxIterations);
    }

    /**
     * @param executorService executor service for submitting tasks
     * @param threads         number of parallel tasks that can be submitted
     * @return Jacobi parallel solver using threads
     */
    @Override
    public SteadyStateSolver buildJacobi(ExecutorService executorService, int threads) {
        return new ParallelJacobiSolver(threads, executorService);
    }

    /**
     * @param executorService executor service for submitting tasks
     * @param threads         number of parallel tasks that can be submitted
     * @param maxIterations   the maximum number of iterations Jacobi can run for
     * @return Jacobi parallel bounded solver
     */
    @Override
    public SteadyStateSolver buildBoundedParallelJacobiSolver(ExecutorService executorService, int threads,
                                                              int maxIterations) {
        return new ParallelJacobiSolver(threads, executorService, maxIterations);
    }

    /**
     *
     * @param executorService executor service for submitting tasks
     * @param threads the number of worker threads to use
     * @param subIterations the number of sub iterations each worker will perform before testing for convergence
     * @return asynchronous Gauss-Seidel implementation
     */
    @Override
    public SteadyStateSolver buildAsynchronousGaussSeidel(ExecutorService executorService, int threads, int subIterations) {
        return new ParallelGaussSeidel(threads, executorService, subIterations);
    }
}
