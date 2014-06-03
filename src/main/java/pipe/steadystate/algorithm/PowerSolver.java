package pipe.steadystate.algorithm;

import uk.ac.imperial.state.Record;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

class PowerSolver extends AbstractSteadyStateSolver {
    private final int threads;

    private final ExecutorService executorService;

    public PowerSolver(int threads, ExecutorService executorService) {
        this.threads = threads;
        this.executorService = executorService;
    }

    /**
     *
     * @param recordsTransposed we need the records transposed to be able to access the matrix in a columnar fashion
     *                          to perform vector matrix multiplications
     * @param diagonalElements
     * @return
     */
    protected Map<Integer, Double> solve(Map<Integer, Map<Integer, Double>> recordsTransposed, Map<Integer, Double> diagonalElements) {
        ParallelSubmitter submitter = new ParallelSubmitter();
        Map<Integer, Double> x = submitter.solve(threads, executorService, initialiseXWithGuess(recordsTransposed), recordsTransposed, diagonalElements, new ParallelSubmitter.ParallelUtils() {
            @Override
            public boolean isConverged(Map<Integer, Double> previousX, Map<Integer, Double> x,
                                       Map<Integer, Map<Integer, Double>> records,
                                       Map<Integer, Double> diagonalElements) {
                return hasConverged(previousX, x);
            }

            @Override
            public Runnable createRunnable(int from, int to, CountDownLatch latch, Map<Integer, Double> previousX,
                                           Map<Integer, Double> x, Map<Integer, Map<Integer, Double>> records,
                                           Map<Integer, Double> diagonalElements) {
                return new ParallelSolver(from, to, latch, previousX, records, x, diagonalElements);
            }
        });

        return normalize(x);
    }
    /**
     * Adds the I matrix to A/a which corresponds to just adding one to the
     * diagonals which are kept separately.
     *
     * @param diagonals
     * @return diagonals + 1
     */
    private Map<Integer, Double> addI(Map<Integer, Double> diagonals) {
        Map<Integer, Double> result = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : diagonals.entrySet()) {
            result.put(entry.getKey(), entry.getValue() + 1);
        }
        return result;
    }

    @Override
    public Map<Integer, Double> solve(List<Record> records) {

        Map<Integer, Double> diagonals = calculateDiagonals(records);
        double a = geta(diagonals);
        List<Record> Q = divide(records, a);
        Map<Integer, Double> dividedDiagonals = divide(a, diagonals);
        Map<Integer, Double> dtmcDiagonals = addI(dividedDiagonals);

        Map<Integer, Map<Integer, Double>> QTranspose = transpose(Q);
        return solve(QTranspose, dtmcDiagonals);
    }

    /**
     * Since this method solves xA = x we check to see if
     * the previous x and x are significantly close to each other for convergence
     * @param previousX previous value of x from the last iteration
     * @param x current value of x from this iteration
     * @return true if x has converged
     */
    private boolean hasConverged(Map<Integer, Double> previousX, Map<Integer, Double> x) {
        Map<Integer, Double> subtracted = subtract(previousX, x);
        double subtractedNorm = euclidianNorm(subtracted);
        double previousNorm = euclidianNorm(previousX);
        return (subtractedNorm/previousNorm) < EPSILON;
    }

    private Map<Integer, Double> subtract(Map<Integer, Double> a, Map<Integer,Double>b ) {
        Map<Integer, Double> subtracted = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : a.entrySet()) {
            int key = entry.getKey();
            double previous = entry.getValue();
            double current = b.get(key);
            subtracted.put(key, current - previous);
        }
        return subtracted;
    }

    private double euclidianNorm(Map<Integer, Double> x) {
        double sum = 0;
        for (double value : x.values()) {
            sum += Math.pow(value, 2);
        }
        return Math.sqrt(sum);
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
            for (int col = from; col <= to; col++) {
                Map<Integer, Double> column = records.get(col);
                double aii = diagonalElements.get(col);
                double rowValue = multiplyAndSum(column, previous);
                rowValue += aii * previous.get(col);
                x.put(col, rowValue);
            }
            latch.countDown();
        }
    }
}
