package pipe.steadystate.algorithm;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import pipe.reachability.TangibleOnlyUtils;
import pipe.reachability.algorithm.TimelessTrapException;
import pipe.reachability.algorithm.UnboundedExplorerUtilities;
import uk.ac.imperial.pipe.exceptions.InvalidRateException;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;
import uk.ac.imperial.pipe.parsers.UnparsableException;
import uk.ac.imperial.state.ClassifiedState;
import uk.ac.imperial.state.Record;
import uk.ac.imperial.utils.StateUtils;
import utils.Utils;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

public class SteadyStateStepDefinitions {

    private PetriNet petriNet;

    /**
     * Number of threads to run parallel solvers with
     */
    private static final int THREADS = 4;

    private boolean timelessTrap;

    /**
     * State for checking probabilities. Used in the Then and And clauses
     * to mark a state for probability checking
     */
    private ClassifiedState state;

    /**
     * Steady state probabilities
     */
    private Map<Integer, Double> probabilities = new HashMap<>();

    private Map<ClassifiedState, Integer> stateMappings = new HashMap<>();

    @Given("^I use the Petri net located at (/[\\w/]+.xml)$")
    public void I_Use_the_Petri_net_located_at(String path)
            throws JAXBException, UnparsableException, FileNotFoundException {
        petriNet = Utils.readPetriNet(path);
    }

    @When("^I calculate the steady state using (a|an) (sequential|parallel) (jacobi|gauss-seidel|parallel power) solver")
    public void I_calculate_the_steady_state(String a, String type, String method)
            throws InterruptedException, ExecutionException, IOException, InvalidRateException {
        try {
            TangibleOnlyUtils utils = new TangibleOnlyUtils();
            Utils.StateSpaceResult result = Utils
                    .performStateSpaceExplore(utils, new UnboundedExplorerUtilities(petriNet));
            List<Record> records = new ArrayList<>(result.results);
            for (Map.Entry<Integer, ClassifiedState> entry : result.states.entrySet()) {
                stateMappings.put(entry.getValue(), entry.getKey());
            }

            ExecutorService executorService = null;
            SteadyStateBuilder builder = new SteadyStateBuilderImpl();
            SteadyStateSolver solver;
            if (type.equals("sequential")) {
                if (method.equals("gauss-seidel")) {
                    solver = builder.buildGaussSeidel();
                } else {
                    solver = builder.buildBoundedSequentialJacobi(100_000);
                }
            } else {
                executorService = Executors.newFixedThreadPool(THREADS);
                if (method.equals("gauss-seidel")) {
                    solver = builder.buildAsynchronousGaussSeidel(executorService, THREADS, 5);
                } else {
                    solver = builder.buildBoundedParallelJacobiSolver(executorService, THREADS, 100_000);
                }
            }
            probabilities.putAll(solver.solve(records));
            if (executorService != null) {
                executorService.shutdownNow();
            }
        } catch (TimelessTrapException e) {
            timelessTrap = true;
        }
    }

    @Then("^I expect a record for")
    public void I_expect_a_record_for(String jsonState) throws IOException {
        state = StateUtils.tangibleStateFromJson(jsonState);
    }

    @And("^I expect another record for")
    public void I_expect_another_record_for(String jsonState) throws IOException {
        state = StateUtils.tangibleStateFromJson(jsonState);
    }

    @And("^its probability to be (\\d+.\\d+)")
    public void Its_probability_to_be(double probability) {
        int stateId = stateMappings.get(state);
        assertEquals(probability, probabilities.get(stateId), 0.000_1);
    }
}
