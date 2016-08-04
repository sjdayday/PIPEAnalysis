package pipe.steadystate.algorithm;

import uk.ac.imperial.state.Record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Solves AX = 0 steady states by converting a CTMC matrix A into
 * a DTMC Q and solves for (I-Q)^Tx^T = 0
 * where Q = A/a + I where a = max |a_ii|
 * <p>
 * this is equivalent to solving (A/-a)^T = 0
 * </p>
 */
public abstract class AXEqualsBSolver extends AbstractSteadyStateSolver {
    /**
     * Class logger
     */
    private static final Logger LOGGER = Logger.getLogger(AXEqualsBSolver.class.getName());


    protected Map<Integer, Integer> stateToIndex = new HashMap<>();

    /**
     * Solves for a CTMC by first transforming it into a DTMC via uniformization:
     * Q = A/a + I
     * where a &gt; max |a_ii|
     * <p>
     * We then solve for a DTMC pQ = p
     *
     * NOTE: States must be labelled in increasing row order from 0 to N in +1 increments for this method to work
     *       That is all records and their transitions must have been labelled in +1 increments from 0
     *
     *
     * @param A matrix A.
     * @return solved matrix 
     */
    @Override
    public final Map<Integer, Double> solve(List<Record> A) {
        Map<Integer, Double> diagonals = calculateDiagonals(A);
        double a = geta(diagonals);
        List<Record> Q = divide(A, -a);
        Map<Integer, Double> newDiagonals = divide(-a, diagonals);
        Map<Integer, Map<Integer, Double>> QTranspose = transpose(Q);

        return timeAndSolve(QTranspose, newDiagonals);
    }

    /**
     *
     * Solves the steady state and logs timing results
     *
     * @param QTranspose
     * @param diagonals
     * @return the solved equation
     */
    private Map<Integer,Double> timeAndSolve(Map<Integer, Map<Integer, Double>> QTranspose, Map<Integer, Double> diagonals) {
        long startTime = System.nanoTime();
        List<Double> results = solve(QTranspose, diagonals);
        long finishTime = System.nanoTime();
        long duration = finishTime - startTime;
        Map<Integer, Double> x = normalize(results);
        LOGGER.log(Level.INFO, "Steady state solved in " + duration);
        return x;
    }


    /**
     * Normalizes x by dividing every value in it by its total sum
     * @param x list to be normalized
     * @return normalized x
     */
    protected final Map<Integer, Double> normalize(List<Double> x) {
        double sum = 0;
        for (double value : x) {
            sum += value;
        }
        Map<Integer, Double> normalized = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : stateToIndex.entrySet()) {
            Integer state = entry.getKey();
            Integer index = entry.getValue();
            if (sum == 0) {
                normalized.put(state, x.get(index));
            } else {
                normalized.put(state, x.get(index)/sum);
            }

        }
        return normalized;
    }


    /**
     * Abstract method which delegates solving to subclasses who solve in the form Ax = b
     * @param records to be solved 
     * @param diagonalElements diagonals
     * @return solved steady state
     */
    protected abstract List<Double> solve(Map<Integer, Map<Integer, Double>> records,
                                                  Map<Integer, Double> diagonalElements);


    /**
     * Performs row sums of successors and optionally ignores a self loop successor
     *
     * That is for the map we sum up each states rate multiplied by the guess for it at x
     *
     * @param row the current non-zero row values in A, note A should be 0 along the diagonal
     * @param x current guess for x
     * @return sum 
     */
    protected final double multiplyAndSum(Map<Integer, Double> row, List<Double> x) {
        double sum = 0;
        for (Map.Entry<Integer, Double> entry : row.entrySet()) {
            Integer state = entry.getKey();
            Double rate = entry.getValue();
            sum += rate * x.get(state);
        }
        return sum;
    }

    /**
     * Performs row sums of successors and optionally ignores a self loop successor
     *
     * That is for the map we sum up each states rate multiplied by the guess for it at x
     *
     * @param row the current non-zero row values in A, note A should be 0 along the diagonal
     * @param x current guess for x
     * @return sum 
     */
    protected final double multiplyAndSum(Map<Integer, Double> row, AtomicReferenceArray<Double> x) {
        double sum = 0;
        for (Map.Entry<Integer, Double> entry : row.entrySet()) {
            Integer state = entry.getKey();
            Double rate = entry.getValue();
            sum += rate * x.get(state);
        }
        return sum;
    }

    /**
     * Checks to see if gauss seidel has converged. That is if Ax &lt; EPSILON
     * <p>
     * It does this on a row level, returning false early if one of the
     * row sums is &gt;= EPSILON
     * </p><p>
     * If Ax &lt; EPSILON we finally check that the answer is plausible. That is that every value in
     * x &gt;= 0
     *
     * @param records to be evaluated
     * @param x converged list 
     * @param diagonals to process 
     * @return true if every value for x has converged
     */
    protected final boolean hasConverged(Map<Integer, Map<Integer, Double>> records, Map<Integer, Double> diagonals,
                                         List<Double> x) {
        for (Map.Entry<Integer, Map<Integer, Double>> entry : records.entrySet()) {
            int state = entry.getKey();
            double rowValue = multiplyAndSum(entry.getValue(), x);
            //Add the diagonal
            rowValue += diagonals.get(state) * x.get(state);
            if (rowValue >= EPSILON) {
                return false;
            }
        }
        return isPlausible(x);
    }


    /**
     * Checks to see if gauss seidel has converged. That is if Ax &lt; EPSILON
     * <p>
     * It does this on a row level, returning false early if one of the
     * row sums is &gt;= EPSILON
     * </p><p>
     * If Ax &lt; EPSILON we finally check that the answer is plausible. That is that every value in
     * x &gt;= 0
     * </p>
     * @param records to be evaluated
     * @param x converged list 
     * @param diagonals to process 
     * @return true if every value for x has converged
     */
    protected final boolean hasConverged(Map<Integer, Map<Integer, Double>> records, Map<Integer, Double> diagonals,
                                         AtomicReferenceArray<Double> x) {
        for (Map.Entry<Integer, Map<Integer, Double>> entry : records.entrySet()) {
            int state = entry.getKey();
            double rowValue = multiplyAndSum(entry.getValue(), x);
            //Add the diagonal
            rowValue += diagonals.get(state) * x.get(state);
            if (rowValue >= EPSILON) {
                return false;
            }
        }
        return isPlausible(x);
    }


    /**
     * Calculates if x is plausible for the steady state, that is every value of x
     * must be &gt;= 0
     *
     * @param x list to evaluate
     * @return true if x is a plausible answer for the steady state
     */
    private boolean isPlausible(List<Double> x) {
        for (double value : x) {
            if (value < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates if x is plausible for the steady state, that is every value of x
     * must be &gt;= 0
     *
     * @param x list to evaluate
     * @return true if x is a plausible answer for the steady state
     */
    private boolean isPlausible(AtomicReferenceArray<Double> x) {
        for (int i = 0; i < x.length(); i++) {
            double value = x.get(i);
            if (value < 0) {
                return false;
            }
        }
        return true;
    }


    /**
     * Performs a row calculation of the Gauss Seidel method
     *
     * @param state current row
     * @param row   contains the non zero row values in A
     * @param aii   the record's state's value on the diagonal of the A matrix
     * @param x     current x values
     * @return the value that should be entered in x for the state held by the record
     */
    protected final double getRowValue(Integer state, Map<Integer, Double> row, double aii, List<Double> x) {
        if (aii == 0) {
            return x.get(state);
        }

        double rowSum = multiplyAndSum(row, x);
        return -rowSum / aii;
    }
    /**
     * Performs a row calculation of the Gauss Seidel method
     *
     * @param state current row
     * @param row   contains the non zero row values in A
     * @param aii   the record's state's value on the diagonal of the A matrix
     * @param x     current x values
     * @return the value that should be entered in x for the state held by the record
     */
    protected final double getRowValue(Integer state, Map<Integer, Double> row, double aii, AtomicReferenceArray<Double> x) {
        if (aii == 0) {
            return x.get(state);
        }

        double rowSum = multiplyAndSum(row, x);
        return -rowSum / aii;
    }


    /**
     *
     * Initialises each value of x to a first guess of 1
     *
     * @param records to initialize
     * @return initial guess for x
     */
    protected final List<Double> initialiseXWithGuessList(Map<Integer, Map<Integer, Double>> records) {
        List<Double> x = new ArrayList<>();
        int index = 0;
        for (Integer state : records.keySet()) {
            stateToIndex.put(index, state);
            x.add(1.0);
            index++;
        }
        return x;
    }


}
