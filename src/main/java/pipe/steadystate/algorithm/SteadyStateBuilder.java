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
     * @param executorService
     * @param threads
     * @return Jacobi parallel solver using threads
     */
    SteadyStateSolver buildJacobi(ExecutorService executorService, int threads);

    /**
     *
     * @param executorService
     * @param threads
     * @return Power method solver using threads
     */
    SteadyStateSolver buildPower(ExecutorService executorService, int threads);
}
