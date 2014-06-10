package pipe.steadystate.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to solve multiple parallel implementations.
 *
 * Since Jacobi and the power method solve slightly different matrices they both
 * pass in runnable iteration methods which should modify x.
 */
class ParallelSubmitter {

    /**
     * Class logger
     */
    private static final Logger LOGGER = Logger.getLogger(ParallelSubmitter.class.getName());

    /**
     * If bounded = true this is the maximum number of iterations the solver is allowed
     */
    private final int maxIterations;

    /**
     * If this value is true then a maximum number of iterations is imposed on the solver
     */
    private boolean bounded = false;

    /**
     * Default constructor, does not bound the results
     */
    public ParallelSubmitter() {
        maxIterations = 0;
    }

    /**
     * Bounded results constructor
     * @param maxIterations the maximum number of iterations for the solver
     */
    public ParallelSubmitter(int maxIterations) {
        this.maxIterations = maxIterations;
        bounded = true;
    }

    /**
     * Solves the steady state using the methods in the utils. If the solver is bounded it will loop for a maximum
     * number of iterations
     * @param threads
     * @param executorService
     * @param firstGuess
     * @param records
     * @param diagonalElements
     * @param utils
     * @return unnormalized x.
     */
    public List<Double> solve(int threads, ExecutorService executorService, List<Double> firstGuess, Map<Integer, Map<Integer, Double>> records,
                    Map<Integer, Double> diagonalElements, ParallelUtils utils) {
        AtomicReferenceArray<Double> x = new AtomicReferenceArray<>(firstGuess.toArray(new Double[firstGuess.size()]));
        List<Double> previous = new ArrayList<>(firstGuess);
        boolean converged = false;
        int iterations = 0;
        while (!converged && canContinue(iterations)) {
            CountDownLatch latch = submitTasks(utils, threads, records, diagonalElements, executorService, x, previous);
            try {
                latch.await();
            } catch (InterruptedException e) {
                LOGGER.log(Level.ALL, e.getMessage());
            }
            converged = utils.isConverged(previous, x, records, diagonalElements);
            previous = atomicToList(x);
            iterations++;
        }
        LOGGER.log(Level.INFO, String.format("Took %d iterations to converge", iterations));
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < x.length(); i++){
            values.add(x.get(i));
        }
        return atomicToList(x);
    }

    private List<Double> atomicToList(AtomicReferenceArray<Double> x) {
        List<Double> result = new ArrayList<>();
        for (int i = 0; i < x.length(); i++) {
            result.add(x.get(i));
        }
        return result;

    }

    /**
     *
     * If bounded then this checks ot see if the iterations exceeds the maximum number
     * If unbounded then it will return true
     *
     * @param iterations next iteration number
     * @return true if the solver is allowed to continue for another iteration
     */
    private boolean canContinue(int iterations) {
        if (bounded && iterations >= maxIterations) {
            LOGGER.log(Level.INFO, "Reached maximum number of iterations. Cutting short!");
        }
        return !bounded || iterations < maxIterations;
    }

    /**
     * Splits the solving of x across the number of threads. For example if x is an 8x8 matrix and there are two
     * threads then each thread will solve 4 rows. If x is a 10x10 matrix and there are 3 threads then
     * two threads will solve 3 rows and one thread wills solve 4 rows.
     * @param utils
     * @param threads
     * @param records
     * @param diagonalElements
     * @param executorService
     * @param x
     * @param previous
     * @return
     */
    private CountDownLatch submitTasks(ParallelUtils utils, int threads, Map<Integer, Map<Integer, Double>> records, Map<Integer, Double> diagonalElements,
                                       ExecutorService executorService, AtomicReferenceArray<Double> x, List<Double> previous
    ) {

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
            executorService.submit(utils.createRunnable(from, to, latch, previous, x, records, diagonalElements));
            from = to + 1;
        }
        return latch;
    }


    /**
     * Since Java 7 does not support lambda functions this utilities class has to suffice as a method
     * for determining convergence of the steady state.
     *
     * Classes who use the {@link pipe.steadystate.algorithm.ParallelSubmitter} will need
     * to implement the methods in here.
     */
    public interface ParallelUtils {
        boolean isConverged(List<Double> previousX, AtomicReferenceArray<Double> x,
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
        Runnable createRunnable(int from, int to, CountDownLatch latch, List<Double> previousX, AtomicReferenceArray<Double> x, Map<Integer, Map<Integer, Double>> records,
                                Map<Integer, Double> diagonalElements);
    }
}
