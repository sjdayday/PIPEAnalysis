package pipe.steadystate.algorithm;

/**
 *
 * Parallel solver attempts to solve the matrix via Jacobi.
 * However Jacobi is only guaranteed to converge for diagonally dominant matrices.
 *
 * If Jacobi is not guaranteed to converge then the power method and a sequential Gauss Seidel method are used.
 * It takes the first converging result.
 */

import uk.ac.imperial.state.Record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to solve the Jacobi and Power implementations for solving the steady state
 * Both methods use slightly different techniques to solve slightly different matrices
 * so they each define their own runnable class to call each iteration on.
 */
public final class ParallelSteadyStateSolver extends AbstractSteadyStateSolver {
    /**
     * Number of threads to solve with
     */
    private final int threads;

    /**
     * Builds different steady state solvers
     */
    private final SteadyStateBuilder builder;

    private static final Logger LOGGER = Logger.getLogger(ParallelSteadyStateSolver.class.getName());


    public ParallelSteadyStateSolver(int threads, SteadyStateBuilder builder) {
        this.threads = threads;
        this.builder = builder;
    }

    @Override
    public Map<Integer, Double> solve(List<Record> records) {
        Map<Integer, Double> diagonals = calculateDiagonals(records);
        double a = geta(diagonals);
        List<Record> Q = divide(records, -a);
        Map<Integer, Double> newDiagonals = divide(-a, diagonals);
        Map<Integer, Map<Integer, Double>> QTranspose = transpose(Q);

        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        try {
            if (isDiagonallyDominant(QTranspose, newDiagonals)) {
                return solveWithJacobi(records, executorService);
            }
            return solveWithJacobiAndGS(records, executorService);
        }   finally{
            executorService.shutdownNow();
        }
    }

    /**
     * Solves by running a sequential GS & a parallel jacobi implementation in parallel.
     * Sequential GS is run because there is no guarantee that Jacobi will converge
     * It takes the first result that is done
     * @param records
     * @param executorService
     * @return
     */
    private Map<Integer, Double> solveWithJacobiAndGS(List<Record> records, ExecutorService executorService) {
        SteadyStateSolver gsSolver = builder.buildGaussSeidel();
        if (threads == 1) {
            return gsSolver.solve(records);
        }

        SteadyStateSolver jacobiSolver = builder.buildJacobi(executorService, threads - 1);
        CompletionService<Map<Integer, Double>> completionService =
                new ExecutorCompletionService<>(executorService);
        List<Future<Map<Integer, Double>>> futures = submit(completionService, records, gsSolver, jacobiSolver);
        try {
            return completionService.take().get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            return new HashMap<>();
        }
        finally {
            for(Future<Map<Integer, Double>> future : futures) {
               future.cancel(true);
            }
        }
    }

    private Map<Integer, Double> solveWithJacobi(List<Record> records, ExecutorService executorService) {
        SteadyStateSolver solver = builder.buildJacobi(executorService, threads);
        return solver.solve(records);
    }

    /**
     * Matrix is diagonally dominant if |a_ii| >= sum_for_all_i_neq_j |a_ij|
     * @param records
     * @param diagonalElements
     * @return
     */
    private boolean isDiagonallyDominant(Map<Integer, Map<Integer, Double>> records,
                                         Map<Integer, Double> diagonalElements) {
        for (Map.Entry<Integer, Map<Integer, Double>> entry : records.entrySet()) {
            double rowSum = getAbsRowSum(entry);
            double a_ii = diagonalElements.get(entry.getKey());
            if (Math.abs(a_ii) < rowSum) {
                return false;
            }
        }
        return true;
    }

    private List<Future<Map<Integer, Double>>> submit(CompletionService<Map<Integer, Double>> completionService,
                                                      List<Record> records, SteadyStateSolver... solvers) {
        List<Future<Map<Integer, Double>>> futures = new ArrayList<>();
        for (SteadyStateSolver solver : solvers) {
            futures.add(completionService.submit(new CallableSteadyStateSolver(solver, records)));
        }
        return futures;
    }

    private double getAbsRowSum(Map.Entry<Integer, Map<Integer, Double>> entry) {
        double rowSum = 0;
        for (double value : entry.getValue().values()) {
            rowSum += Math.abs(value);
        }
        return rowSum;
    }

    private static final class CallableSteadyStateSolver implements Callable<Map<Integer, Double>> {

        private final SteadyStateSolver solver;

        private final List<Record> records;

        private CallableSteadyStateSolver(SteadyStateSolver solver, List<Record> records) {
            this.solver = solver;
            this.records = records;
        }

        @Override
        public Map<Integer, Double> call() {
            return solver.solve(records);
        }
    }
}