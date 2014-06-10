package pipe.steadystate.algorithm;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParallelGaussSeidel extends AXEqualsBSolver {

    /**
     * Class logger
     */
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
     * The number of sub iterations that each thread will do before testing for convergence results
     */
    private final int subIterations;



    /**
     * @param threads         Number of threads Jacobi should be solved with for each
     *                        iteration
     * @param executorService service to submit tasks to
     * @param subIterations  number of sub iterations that each thread will do before testing for convergence results
     */
    public ParallelGaussSeidel(int threads, ExecutorService executorService, int subIterations) {
        this.threads = threads;
        this.executorService = executorService;
        this.subIterations = subIterations;
    }


    /**
     * Solves the matrix via a parallel jacobi. The submitted tasks of each iteration each perform the new
     * calculation of x_i for a specified number of rows.
     *
     * Iterations continue until x converges
     *
     * @param records
     * @param diagonalElements
     * @return normalized x
     */
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
                                return new ParallelSolver(from, to, latch, records, x, diagonalElements);
                            }
                        });

        return normalize(x);
    }

    /**
     * Parallel solver task
     */
    private final class ParallelSolver implements Runnable {

        /**
         * Sparse matrix A, missing the diagonal elements
         */
        private final Map<Integer, Map<Integer, Double>> records;

        /**
         * Current value of x
         */
        private final Map<Integer, Double> x;

        /**
         * Diagonal elements of A
         */
        private final Map<Integer, Double> diagonalElements;

        /**
         * inclusive row start
         */
        private final int from;

        /**
         * inclusive row end
         */
        private final int to;

        /**
         * Latch to decrement when finished processing
         */
        private final CountDownLatch latch;

        /**
         * Initialises this task with the information it needs
         * @param from
         * @param to
         * @param latch
         * @param records
         * @param x
         * @param diagonalElements
         */
        private ParallelSolver(int from, int to, CountDownLatch latch, Map<Integer, Map<Integer, Double>> records, Map<Integer, Double> x,
                               Map<Integer, Double> diagonalElements) {
            this.records = records;
            this.latch = latch;
            this.x = x;
            this.diagonalElements = diagonalElements;
            this.from = from;
            this.to = to;
        }

        /**
         * Solves for the next value of x for the rows from to to
         * Decrements the latch on finishing
         */
        @Override
        public void run() {
            for (int i = 0; i < subIterations; i++) {
                try {
                    for (int state = from; state <= to; state++) {
                        Map<Integer, Double> row = records.get(state);
                        double aii = diagonalElements.get(state);
                        double rowValue = getRowValue(state, row, aii, x);
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
}
