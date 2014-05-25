package pipe.steadystate.algorithm;

import java.util.concurrent.ExecutorService;

public interface SteadyStateBuilder {

    SteadyStateSolver buildGaussSeidel();

    SteadyStateSolver buildJacobi(ExecutorService executorService, int threads);

    SteadyStateSolver buildPower(ExecutorService executorService, int threads);
}
