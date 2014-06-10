package pipe.steadystate.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReferenceArray;
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
    protected List<Double> solve(Map<Integer, Map<Integer, Double>> records,
                                         Map<Integer, Double> diagonalElements) {
        List<Double> firstGuess = initialiseXWithGuessList(records);
        AtomicReferenceArray<Double> x = new AtomicReferenceArray<>(firstGuess.toArray(new Double[firstGuess.size()]));
        boolean converged = false;
        int iterations = 0;
        while (!converged) {
            CountDownLatch latch = submitTasks(threads, records, diagonalElements, executorService, x);
            try {
                latch.await();
            } catch (InterruptedException e) {
                LOGGER.log(Level.ALL, e.getMessage());
            }
            converged = hasConverged(records, diagonalElements, x);
            iterations++;
        }
        LOGGER.log(Level.INFO, String.format("Took %d iterations to converge", iterations));
        return toArray(x);
    }

    private List<Double> toArray(AtomicReferenceArray<Double> x) {
        List<Double> result = new ArrayList<>();
        for (int i = 0; i < x.length(); i++) {
            result.add(x.get(i));
        }
        return result;
    }

    /**
     * Splits the solving of x across the number of threads. For example if x is an 8x8 matrix and there are two
     * threads then each thread will solve 4 rows. If x is a 10x10 matrix and there are 3 threads then
     * two threads will solve 3 rows and one thread wills solve 4 rows.
     * @param threads
     * @param records
     * @param diagonalElements
     * @param executorService
     * @param x
     * @return
     */
    private CountDownLatch submitTasks(int threads, Map<Integer, Map<Integer, Double>> records, Map<Integer, Double> diagonalElements,
                                       ExecutorService executorService, AtomicReferenceArray<Double> x) {

        int scheduledThreads = x.length() < threads ? x.length() : threads;
        int split = x.length() / scheduledThreads;
        int remaining = x.length() % scheduledThreads;

        CountDownLatch latch = new CountDownLatch(scheduledThreads);
        //TODO: THis wont do the last one, so we want inclusive too?
        int from  = 0;
        for (int thread = 0; thread < scheduledThreads; thread++) {
            int to = from + split - 1 + (remaining > 0 ? 1 : 0);
            if (remaining > 0) {
                remaining--;
            }
            executorService.submit(createRunnable(from, to, latch, x, records, diagonalElements));
            from = to + 1;
        }
        return latch;
    }

    public Runnable createRunnable(int from, int to, CountDownLatch latch, AtomicReferenceArray<Double> x, Map<Integer, Map<Integer, Double>> records,
                                   Map<Integer, Double> diagonalElements) {
        return new ParallelSolver(from, to, latch, records, x, diagonalElements);
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
         * @param records
         * @param x
         * @param diagonalElements
         */
        private ParallelSolver(int from, int to, CountDownLatch latch, Map<Integer, Map<Integer, Double>> records, AtomicReferenceArray<Double> x,
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
                    for (int index = from; index <= to; index++) {
                        Map<Integer, Double> row = records.get(index);
                        double aii = diagonalElements.get(index);
                        double rowValue = getRowValue(index, row, aii, x);
                        x.set(index, rowValue);
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
