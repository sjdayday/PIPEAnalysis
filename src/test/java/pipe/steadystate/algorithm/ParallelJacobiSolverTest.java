package pipe.steadystate.algorithm;

import org.junit.Test;
import uk.ac.imperial.state.Record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParallelJacobiSolverTest {

    @Test
    public void solvesSimple() {
        List<Record> records = new ArrayList<>();
        Map<Integer, Double> successors0 = new HashMap<>();
        successors0.put(1, 1.0);
        records.add(new Record(0, successors0));

        Map<Integer, Double> successors1 = new HashMap<>();
        successors1.put(0, 1.0);
        records.add(new Record(1, successors1));

        ExecutorService service = Executors.newFixedThreadPool(2);
        ParallelJacobiSolver solver = new ParallelJacobiSolver(2, service);
        Map<Integer, Double> steadyState = solver.solve(records);
        System.out.println("Solved");
        service.shutdownNow();
    }

}