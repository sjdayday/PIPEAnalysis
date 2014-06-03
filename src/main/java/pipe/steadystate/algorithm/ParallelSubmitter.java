package pipe.steadystate.algorithm;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to solve multiple parallel implementations.
 *
 * Since Jacobi and the power method solve slightly different matricies they both
 * pass in runnable iteration methods which should modify x.
 */
class ParallelSubmitter {

    private static final Logger LOGGER = Logger.getLogger(ParallelSubmitter.class.getName());

    /**
     * Solves the steady state using the methods in the utils.
     * @param threads
     * @param executorService
     * @param firstGuess
     * @param records
     * @param diagonalElements
     * @param utils
     * @return unnormalized x.
     */
    public Map<Integer, Double> solve(int threads, ExecutorService executorService, Map<Integer, Double> firstGuess, Map<Integer, Map<Integer, Double>> records,
                    Map<Integer, Double> diagonalElements, ParallelUtils utils) {
        Map<Integer, Double> x = new ConcurrentHashMap<>(firstGuess);
        Map<Integer, Double> previous = new HashMap<>(x);
        boolean converged = false;
        int iterations = 0;
        while (!converged) {
            CountDownLatch latch = submitTasks(utils, threads, records, diagonalElements, executorService, x, previous);
            try {
                latch.await();
            } catch (InterruptedException e) {
                LOGGER.log(Level.ALL, e.getMessage());
            }
            converged = utils.isConverged(previous, x, records, diagonalElements);
            previous = new HashMap<>(x);
            iterations++;
        }
        LOGGER.log(Level.INFO, String.format("Took %d iterations to converge", iterations));
        return x;
    }

    private CountDownLatch submitTasks(ParallelUtils utils, int threads, Map<Integer, Map<Integer, Double>> records, Map<Integer, Double> diagonalElements,
                                       ExecutorService executorService, Map<Integer, Double> x, Map<Integer, Double> previous
    ) {

        int scheduledThreads = x.size() < threads ? x.size() : threads;
        int split = x.size() / scheduledThreads;
        int remaining = x.size() % scheduledThreads;

        CountDownLatch latch = new CountDownLatch(scheduledThreads);
        //TODO: THis wont do the last one, so we want inclusive too?
        int from  = 0;
        for (int thread = 0; thread < scheduledThreads; thread++) {
            int to = from + split - 1 + (remaining > 0 ? 1 : 0);
            if (remaining > 0) {
                remaining--;
            }
            executorService.submit(utils.createRunnable(from, to, latch, previous, x, records, diagonalElements));
            from = to + 1;
        }
        return latch;
    }



    public interface ParallelUtils {
        boolean isConverged(Map<Integer, Double> previousX, Map<Integer, Double> x,
                            Map<Integer, Map<Integer, Double>> records, Map<Integer, Double> diagonalElements);

        /**
         * Create a runnable item that perfoms a single iteration in the
         * parallel implementation
         * @param from
         * @param to
         * @param latch
         * @param previousX
         * @param x
         * @param records
         * @param diagonalElements
         * @return
         */
        Runnable createRunnable(int from, int to, CountDownLatch latch, Map<Integer, Double> previousX, Map<Integer, Double> x, Map<Integer, Map<Integer, Double>> records,
                                Map<Integer, Double> diagonalElements);
    }
}
