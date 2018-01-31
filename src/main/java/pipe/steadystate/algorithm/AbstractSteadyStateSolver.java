package pipe.steadystate.algorithm;

import com.esotericsoftware.kryo.io.Input;
import uk.ac.imperial.io.EntireStateReader;
import uk.ac.imperial.io.KryoStateIO;
import uk.ac.imperial.io.MultiStateReader;
import uk.ac.imperial.state.Record;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class used to solve the steady state, contains useful methods and constants
 * that subclasses may use.
 */
public abstract class AbstractSteadyStateSolver implements SteadyStateSolver {
    /**
     * Epsilon value used for testing if gauss seidel has converged.
     * We are trying to solve for Ax = 0, but we will accept
     * Ax &lt; EPSILON
     */
    protected static final double EPSILON = 0.000001;

    /**
     * Performs the transpose of records.
     *
     * This is because for the steady state we wish to solve xA = 0 which is equivalent to
     * A^t x^t = 0
     *
     *
     * @param records to be transposed
     * @return transpose of records with each integer state mapping to its successors
     */
    protected final Map<Integer, Map<Integer, Double>> transpose(List<Record> records) {
        Map<Integer, Map<Integer, Double>> transpose = new HashMap<>();
        for (Record record : records) {
            Integer state = record.state;
            createAndGet(transpose, state);
            for (Map.Entry<Integer, Double> entry : record.successors.entrySet()) {
                Integer successor = entry.getKey();
                Double rate = entry.getValue();
                Map<Integer, Double> successors = createAndGet(transpose, successor);
                successors.put(state, rate);
            }
        }
        return transpose;
    }

    /**
     * Searches for the largest absolute diagonal and then returns q the dividor in
     * turning a CTMC matrix A, into a DTMC matrix Q using
     * Q = A/a + I
     * <p>
     * a is calculated by adding 1 to the largest value, since the largest &gt;= 0 we never
     * run the risk of returning 0
     * </p>
     * @param diagonals to evaluate
     * @return a &gt; max |a_ii|
     */
    protected final double geta(Map<Integer, Double> diagonals) {
        List<Double> values = new ArrayList<>(diagonals.values());
        double largest = Math.abs(values.get(0));
        for (double value : values) {
            double abs = Math.abs(value);
            if (abs > largest) {
                largest = abs;
            }
        }
        return largest + 2;
    }

    /**
     *
     * Returns the map for the successor rates, if it does not exist it is created and added
     * to transitions before returning
     * @param transpose of the matrix
     * @param state to evaluate
     * @return successor map for state in the transpose matrix
     */
    private Map<Integer, Double> createAndGet(Map<Integer, Map<Integer, Double>> transpose, Integer state) {
        if (!transpose.containsKey(state)) {
            transpose.put(state, new HashMap<Integer, Double>());
        }
        return transpose.get(state);
    }

    @Override
    public final Map<Integer, Double> solve(String path) throws IOException {
        try {
            List<Record> records = loadRecords(path);
            return solve(records);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private List<Record> loadRecords(String path) throws URISyntaxException, IOException {
        KryoStateIO io = new KryoStateIO();
        Path p = Paths.get(this.getClass().getResource(path).toURI());
        try (InputStream fileStream = Files.newInputStream(p);
                Input inputStream = new Input(fileStream)) {
            MultiStateReader reader = new EntireStateReader(io);
            return new ArrayList<>(reader.readRecords(inputStream));
        }
    }

    /**
     * Normalizes x by dividing every value in it by its total sum
     * @param x to be normalized
     * @return normalized x
     */
    protected final Map<Integer, Double> normalize(Map<Integer, Double> x) {
        double sum = 0;
        for (double value : x.values()) {
            sum += value;
        }
        Map<Integer, Double> normalized = new HashMap<>();
        if (sum == 0) {
            normalized.putAll(x);
        } else {
            for (Map.Entry<Integer, Double> entry : x.entrySet()) {
                normalized.put(entry.getKey(), entry.getValue() / sum);
            }
        }
        return normalized;
    }

    /**
     *
     * Initializes each value of x to a first guess of 1
     *
     * @param records to be initialized
     * @return initial guess for x
     */
    protected final Map<Integer, Double> initialiseXWithGuess(Map<Integer, Map<Integer, Double>> records) {
        Map<Integer, Double> x = new HashMap<>();
        for (Integer state : records.keySet()) {
            x.put(state, 1.0);
        }
        return x;
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
    protected final double multiplyAndSum(Map<Integer, Double> row, Map<Integer, Double> x) {
        double sum = 0;
        for (Map.Entry<Integer, Double> entry : row.entrySet()) {
            Integer state = entry.getKey();
            Double rate = entry.getValue();
            sum += rate * x.get(state);
        }
        return sum;
    }

    /**
     * The diagonal elements of the A matrix are those where an element maps
     * to itself, since no self loops are allowed.
     * <p>
     * The diagonal element is the negated sum of all other row elements
     * </p><p>
     * It is calculated via summing a states successors and negating the value
     * </p>
     * @param records to be calculated
     * @return diagonal values of the A matrix
     */
    protected final Map<Integer, Double> calculateDiagonals(List<Record> records) {
        Map<Integer, Double> diagonals = new HashMap<>();
        for (Record record : records) {
            double rowSum = 0;
            //TODO: What if there is a self loop?
            for (Double rate : record.successors.values()) {
                rowSum += rate;
            }
            diagonals.put(record.state, -rowSum);
        }
        return diagonals;
    }

    /**
     * Divdes every value in matrix A by a
     * @param A matrix
     * @param a scalar
     * @return divided matrix
     */
    protected final List<Record> divide(List<Record> A, double a) {
        List<Record> results = new ArrayList<>();
        for (Record record : A) {
            results.add(new Record(record.state, divide(a, record.successors)));
        }
        return results;
    }

    /**
     * Divides the specified row by a
     *
     * @param a scalar
     * @param row matrix row
     * @return divided row
     */
    protected final Map<Integer, Double> divide(double a, Map<Integer, Double> row) {
        Map<Integer, Double> divided = new HashMap<>();
        for (Map.Entry<Integer, Double> entry1 : row.entrySet()) {
            divided.put(entry1.getKey(), entry1.getValue() / a);
        }
        return divided;
    }

}
