package pipe.steadystate.algorithm;

import java.util.HashMap;
import java.util.Map;

public final class GaussianEliminationSolver extends AXEqualsBSolver {
    @Override
    protected Map<Integer, Double> solve(Map<Integer, Map<Integer, Double>> records,
                                         Map<Integer, Double> diagonalElements) {
        addDiagonalElements(records, diagonalElements);
        reduceToUpperTriangle(records);
        Map<Integer, Double> x = backSubstitute(records);
        return normalize(x);
    }

    /**
     * Adds all diagonal elements to the record
     * @param records
     * @param diagonalElements
     */
    private void addDiagonalElements(Map<Integer, Map<Integer, Double>> records,
                                     Map<Integer, Double> diagonalElements) {
        for (Map.Entry<Integer, Double> entry : diagonalElements.entrySet()) {
            records.get(entry.getKey()).put(entry.getKey(), entry.getValue());
        }

    }

    private Map<Integer, Double> backSubstitute(Map<Integer, Map<Integer, Double>> records) {
        Map<Integer, Double> x = new HashMap<>();
        x.put(records.size() -1 , 1.);
        for (int i = records.size() - 2; i >= 0; i--) {
            double sum = 0;
            for (int j = i+1; j < records.size(); j++) {
                double aij = get(i, j, records);
                double xj = x.get(j);
                sum += aij*xj;
            }
            double aii = get(i, i, records);
            double xi = -sum/aii;
            x.put(i, xi);
        }
        return x;
    }

    /**
     * Performs upper triangle reduction step of Guassian elimination where by for a given
     * row in the iteration, all rows below it are subtracted by an amount....
     *
     * In place modifies records
     * @param records
     */
    private void reduceToUpperTriangle(Map<Integer, Map<Integer, Double>> records) {
        for (int k = 0; k < records.size() - 1; k++) {

            for (int i = k + 1; i < records.size(); i++) {
                double aik = get(i, k, records);
                double akk = get(k, k, records);
                double mik =  aik / akk;
                for (int j = k+1; j < records.size(); j++) {
                    double aij = get(i, j, records);
                    double akj = get(k, j, records);
                    aij = aij - mik * akj;
                    // Replace
                    records.get(i).put(j, aij);
                }
            }
        }
    }

    /**
     * Gets the value at A[i][j] from records. Since records is a sparse
     * representation if it is not contained within records then 0 is returned.
     *
     * Assume i != j
     *
     * @param i
     * @param j
     * @param records
     */
    private double get(int i, int j, Map<Integer, Map<Integer, Double>>  records) {
        if (!records.containsKey(i) || !records.get(i).containsKey(j)) {
            return 0;
        }
        return records.get(i).get(j);
    }
}
