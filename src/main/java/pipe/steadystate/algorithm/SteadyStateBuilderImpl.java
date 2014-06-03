package pipe.steadystate.algorithm;

import java.util.concurrent.ExecutorService;

public final class SteadyStateBuilderImpl implements SteadyStateBuilder

{
    @Override
    public SteadyStateSolver buildGaussSeidel() {
        return new GaussSeidelSolver();
    }

    @Override
    public SteadyStateSolver buildJacobi(ExecutorService executorService, int threads) {
        return new ParallelJacobiSolver(threads, executorService);
    }

    @Override
    public SteadyStateSolver buildPower(ExecutorService executorService, int threads) {
        return new PowerSolver(threads, executorService);
    }
}
