package pipe.steadystate.algorithm;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

public class ParallelSubmitter {

    public Map<Integer, Double> solve(int threads, ExecutorService executorService, Map<Integer, Double> firstGuess, Map<Integer, Map<Integer, Double>> records,
                    Map<Integer, Double> diagonalElements, ParallelUtils utils) {

        Map<Integer, Double> x = new ConcurrentHashMap<>(firstGuess);
        Map<Integer, Double> previous = new HashMap<>(x); //TODO: I dont think this needs to be concurrent, only ever doing 'get'
        boolean converged = false;
        while (!converged) {
            CountDownLatch latch = submitTasks(utils, threads, records, diagonalElements, executorService, x, previous);
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            converged = utils.isConverged(previous, x, records, diagonalElements);
            previous = new HashMap<>(x);
        }

        return x;
    }


    private Map<Integer, Double> normalizeConcurrent(Map<Integer, Double> x) {
        double sum = 0;
        for (double value : x.values()) {
            sum += value;
        }
        Map<Integer, Double> normalized = new ConcurrentHashMap<>();
        if (sum == 0) {
            normalized.putAll(x);
        } else {
            for (Map.Entry<Integer, Double> entry : x.entrySet()) {
                normalized.put(entry.getKey(), entry.getValue()/sum);
            }
        }
        return normalized;
    }

    private ConcurrentHashMap<Integer, BigDecimal> convertToBigDecimal(Map<Integer, Double> x) {
        ConcurrentHashMap<Integer, BigDecimal> result = new ConcurrentHashMap<>();
        for (Map.Entry<Integer, Double> entry : x.entrySet()) {
            result.put(entry.getKey(), new BigDecimal(entry.getValue()));
        }
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
            int to = from + split - 1 + remaining;
//            System.out.println("Thread " + thread + " from " + from + " to " + to);
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

        Runnable createRunnable(int from, int to, CountDownLatch latch, Map<Integer, Double> previousX, Map<Integer, Double> x, Map<Integer, Map<Integer, Double>> records,
                                Map<Integer, Double> diagonalElements);
    }
}
