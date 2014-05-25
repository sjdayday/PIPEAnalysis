package pipe.steadystate.algorithm;

import uk.ac.imperial.state.Record;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Solves AX = 0 steady states by converting a CTMC matrix A into
 * a DTMC Q and solves for (I-Q)^Tx^T = 0
 * where Q = A/a + I where a = max |a_ii|
 * <p/>
 * this is equivalent to solving (A/-a)^T = 0
 * w
 */
public abstract class AXEqualsBSolver extends AbstractSteadyStateSolver {

    /**
     * Solves for a CTMC by first transforming it into a DTMC via uniformization:
     * Q = A/a + I
     * where a > max |a_ii|
     * <p/>
     * We then solve for a DTMC pQ = p
     *
     * @param A
     * @return
     */
    @Override
    public Map<Integer, Double> solve(List<Record> A) {
        Map<Integer, Double> diagonals = calculateDiagonals(A);
        double a = geta(diagonals);
        List<Record> Q = divide(A, -a);
        Map<Integer, Double> newDiagonals = divide(-a, diagonals);
        Map<Integer, Map<Integer, Double>> QTranspose = transpose(Q);
        return solve(QTranspose, newDiagonals);
    }






    //TODO: What if a state had no transitions? Ask Will to check this method
    protected abstract Map<Integer, Double> solve(Map<Integer, Map<Integer, Double>> records,
                                                  Map<Integer, Double> diagonalElements);



    private Map<Integer, Map<Integer, Double>> uniformize(Map<Integer, Map<Integer, Double>> A, double a) {
        Map<Integer, Map<Integer, Double>> result = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Double>> entry : A.entrySet()) {
            int key = entry.getKey();
            Map<Integer, Double> row = divide(a, entry.getValue());
            result.put(key, row);
        }
        return result;
    }

    /**
     * Checks to see if gauss seidel has converged. That is if Ax < EPSILON
     * <p/>
     * It does this on a row level, returning false early if one of the
     * row sums is >= EPSILON
     * <p/>
     * If Ax < EPSILON we finally check that the answer is plausible. That is that every value in
     * x >= 0
     *
     * @param records
     * @param x
     * @return true if every value for x has converged
     */

    //TODO: Ask will about the plausible check, could it not converge?
    protected boolean hasConverged(Map<Integer, Map<Integer, Double>> records, Map<Integer, Double> diagonals,
                                   Map<Integer, Double> x) {
        for (Map.Entry<Integer, Map<Integer, Double>> entry : records.entrySet()) {
            int state = entry.getKey();
            double rowValue = multiplyAndSum(entry.getValue(), x);
            rowValue += diagonals.get(state) * x.get(state); //Add the diagonal
            if (rowValue >= EPSILON) {
                return false;
            }
        }
        return isPlausible(x);
    }

    /**
     * Calculates if x is plausible for the steady state, that is every value of x
     * must be >= 0
     *
     * @param x
     * @return true if x is a plausible answer for the steady state
     */
    private boolean isPlausible(Map<Integer, Double> x) {
        for (double value : x.values()) {
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
    protected double getRowValue(Integer state, Map<Integer, Double> row, double aii, Map<Integer, Double> x) {
        if (aii == 0) {
            return x.get(state);
        }

        double rowSum = multiplyAndSum(row, x);
        return -rowSum / aii;
    }

}
