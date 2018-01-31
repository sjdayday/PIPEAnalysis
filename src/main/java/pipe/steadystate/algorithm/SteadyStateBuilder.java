package pipe.steadystate.algorithm;

import java.util.concurrent.ExecutorService;

/**
 * Builds the underlying implementations of the steady state that this package contains
 *
 * Note that a Jacobi parallel solver is preferred to the power method
 */
public interface SteadyStateBuilder {

    /**
     *
     * @return Gauss Seidel sequential solver
     */
    SteadyStateSolver buildGaussSeidel();

    /**
     *
     * @return sequential Jacobi solver
     */
    SteadyStateSolver buildSequentialJacobi();

    /**
     *
     * @param maxIterations the maximum number of iterations Jacobi can run for
     * @return a bounded sequential Jacobi solver
     */
    SteadyStateSolver buildBoundedSequentialJacobi(int maxIterations);

    /**
     *
     * @param executorService executor service for submitting tasks
     * @param threads number of parallel tasks that can be submitted
     * @return Jacobi parallel solver using threads
     */
    SteadyStateSolver buildJacobi(ExecutorService executorService, int threads);

    /**
     *
     * @param executorService executor service for submitting tasks
     * @param threads number of parallel tasks that can be submitted
     * @param maxIterations the maximum number of iterations Jacobi can run for
     * @return Jacobi parallel bounded solver
     */
    SteadyStateSolver buildBoundedParallelJacobiSolver(ExecutorService executorService, int threads, int maxIterations);

    /**
     *
     * @param executorService executor service for submitting tasks
     * @param threads number of parallel tasks that can be submitted
     * @param subIterations the number of sub iterations each worker will perform before testing for convergence
     * @return asynchronous Gauss-Seidel method solver using threads
     */
    SteadyStateSolver buildAsynchronousGaussSeidel(ExecutorService executorService, int threads, int subIterations);
}
