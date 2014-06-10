package pipe.steadystate.algorithm;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Solves a matrix via the Jacobi iteration.
 * <p/>
 * Note that convergence is only guaranteed for diagonally dominant matrices, so this should
 * be checked before solve is called. Failing that solve may hang indefinitely.
 */
class ParallelJacobiSolver extends AXEqualsBSolver {

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
     * If bounded = true this is the maximum number of iterations the solver is allowed
     */
    private final int maxIterations;

    /**
     * If this value is true then a maximum number of iterations is imposed on the solver
     */
    private boolean bounded = false;



    /**
     * @param threads         Number of threads Jacobi should be solved with for each
     *                        iteration
     * @param executorService service to submit tasks to
     */
    public ParallelJacobiSolver(int threads, ExecutorService executorService) {
        this.threads = threads;
        this.executorService = executorService;
        maxIterations = 0;
    }

    /**
     * @param threads         Number of threads Jacobi should be solved with for each
     *                        iteration
     * @param executorService service to submit tasks to
     * @param maxIterations maximum number of iterations allowed
     */
    public ParallelJacobiSolver(int threads, ExecutorService executorService, int maxIterations) {
        this.threads = threads;
        this.executorService = executorService;
        this.maxIterations = maxIterations;
        bounded = true;
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
    protected List<Double> solve(Map<Integer, Map<Integer, Double>> records,
                                         Map<Integer, Double> diagonalElements) {
        ParallelSubmitter submitter = getSubmitter();
        List<Double> x =
                submitter.solve(threads, executorService, initialiseXWithGuessList(records), records, diagonalElements,
                        new ParallelSubmitter.ParallelUtils() {
                            @Override
                            public boolean isConverged(List<Double> previousX, AtomicReferenceArray<Double> x,
                                                       Map<Integer, Map<Integer, Double>> records,
                                                       Map<Integer, Double> diagonalElements) {
                                return hasConverged(records, diagonalElements, x);
                            }

                            @Override
                            public Runnable createRunnable(int from, int to, CountDownLatch latch,
                                                          List<Double> previousX, AtomicReferenceArray<Double> x,
                                                           Map<Integer, Map<Integer, Double>> records,
                                                           Map<Integer, Double> diagonalElements) {
                                return new ParallelSolver(from, to, latch, previousX, records, x, diagonalElements);
                            }
                        });

        return x;
    }

    /**
     *
     * @return a submitter that is possibly bounded
     */
    private ParallelSubmitter getSubmitter() {
        if (!bounded) {
            return new ParallelSubmitter();
        }
        return new ParallelSubmitter(maxIterations);
    }

    /**
     * Parallel solver task
     */
    private final class ParallelSolver implements Runnable {
        /**
         * Previous value of x
         */
        private final List<Double> previous;

        /**
         * Sparse matrix A, missing the diagonal elements
         */
        private final Map<Integer, Map<Integer, Double>> records;

        /**
         * Current value of x
         */
        private final AtomicReferenceArray<Double> x;

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
         * @param previous
         * @param records
         * @param x
         * @param diagonalElements
         */
        private ParallelSolver(int from, int to, CountDownLatch latch, List<Double> previous,
                               Map<Integer, Map<Integer, Double>> records, AtomicReferenceArray<Double> x,
                               Map<Integer, Double> diagonalElements) {
            this.previous = previous;
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
            try {
                for (int state = from; state <= to; state++) {
                    Map<Integer, Double> row = records.get(state);
                    double aii = diagonalElements.get(state);
                    double rowValue = getRowValue(state, row, aii, previous);
                    x.set(state, rowValue);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            } finally {
                latch.countDown();
            }
        }
    }
}
