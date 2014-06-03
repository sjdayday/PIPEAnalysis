package pipe.steadystate.algorithm;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Solves a matrix via the Jacobi iteration.
 * <p/>
 * Note that convergence is only guaranteed for diagonally dominant matrices, so this should
 * be checked before solve is called. Failing that solve may hang indefinitely.
 */
class ParallelJacobiSolver extends AXEqualsBSolver {

    private static final Logger LOGGER = Logger.getLogger(ParallelJacobiSolver.class.getName());


    /**
     * Number of threads to run in parallel on the CPU
     */
    private final int threads;

    /**
     * Executor service for submitting runnable tasks to
     */
    private final ExecutorService executorService;

    /**
     * @param threads         Number of threads Jacobi should be solved with for each
     *                        iteration
     * @param executorService service to submit tasks to
     */
    public ParallelJacobiSolver(int threads, ExecutorService executorService) {
        this.threads = threads;
        this.executorService = executorService;
    }

    @Override
    protected Map<Integer, Double> solve(Map<Integer, Map<Integer, Double>> records,
                                         Map<Integer, Double> diagonalElements) {
        ParallelSubmitter submitter = new ParallelSubmitter();
        Map<Integer, Double> x =
                submitter.solve(threads, executorService, initialiseXWithGuess(records), records, diagonalElements,
                        new ParallelSubmitter.ParallelUtils() {
                            @Override
                            public boolean isConverged(Map<Integer, Double> previousX, Map<Integer, Double> x,
                                                       Map<Integer, Map<Integer, Double>> records,
                                                       Map<Integer, Double> diagonalElements) {
                                return hasConverged(records, diagonalElements, x);
                            }

                            @Override
                            public Runnable createRunnable(int from, int to, CountDownLatch latch,
                                                           Map<Integer, Double> previousX, Map<Integer, Double> x,
                                                           Map<Integer, Map<Integer, Double>> records,
                                                           Map<Integer, Double> diagonalElements) {
                                return new ParallelSolver(from, to, latch, previousX, records, x, diagonalElements);
                            }
                        });

        return normalize(x);
    }

    private final class ParallelSolver implements Runnable {
        /**
         * Previous value of x
         */
        private final Map<Integer, Double> previous;

        private final Map<Integer, Map<Integer, Double>> records;

        /**
         * Current value of x
         */
        private final Map<Integer, Double> x;

        private final Map<Integer, Double> diagonalElements;

        /**
         * inclusive row start
         */
        private final int from;

        /**
         * inclusive row end
         */
        private final int to;

        private final CountDownLatch latch;

        private ParallelSolver(int from, int to, CountDownLatch latch, Map<Integer, Double> previous,
                               Map<Integer, Map<Integer, Double>> records, Map<Integer, Double> x,
                               Map<Integer, Double> diagonalElements) {
            this.previous = previous;
            this.records = records;
            this.latch = latch;
            this.x = x;
            this.diagonalElements = diagonalElements;
            this.from = from;
            this.to = to;
        }

        @Override
        public void run() {
            try {
                for (int state = from; state <= to; state++) {
                    Map<Integer, Double> row = records.get(state);
                    double aii = diagonalElements.get(state);
                    double rowValue = getRowValue(state, row, aii, previous);
                    x.put(state, rowValue);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            } finally {
                latch.countDown();
            }
        }
    }
}
