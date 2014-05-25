package pipe.reachability;

import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import pipe.reachability.algorithm.TimelessTrapException;
import uk.ac.imperial.pipe.exceptions.PetriNetComponentNotFoundException;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;
import uk.ac.imperial.pipe.parsers.UnparsableException;
import uk.ac.imperial.state.ClassifiedState;
import uk.ac.imperial.state.Record;
import uk.ac.imperial.utils.StateUtils;
import utils.Utils;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class StateSpaceExplorerStepDefinitions {
    /**
     * Petri net to perform exploration on
     */
    private PetriNet petriNet;

    /**
     * State space exploration results
     */
    private Map<Integer, Map<Integer, Double>> results = new HashMap<>();

    /**
     * Record of state to integer representation
     */
    private Map<ClassifiedState, Integer> stateMappings = new HashMap<>();

    /**
     * Set to true if timeless trap is thrown
     */
    private boolean timelessTrap = false;

    /**
     * Auxillary state for registering with expected records
     */
    private ClassifiedState state;

    /**
     * Auxillary state for registering with expected records
     */
    private ClassifiedState successor;

    private StateExplorerUtils utils;

    private int processedTransitons = 0;

    @Before("@tangibleOnly")
    public void beforeTangibleScenario() {
        utils = new TangibleOnlyUtils();
    }

    @Before("@tangibleAndVanishing")
    public void beforeTanigbleAndVanishingScenario() {
        utils = new TangibleAndVanishingUtils();
    }


    @Given("^I use the Petri net located at (/[\\w/]+.xml)$")
    public void I_Use_the_Petri_net_located_at(String path) throws JAXBException, UnparsableException {
        petriNet = Utils.readPetriNet(path);
    }

    @When("^I generate the exploration graph (in parallel)$")
    public void I_generate_the_exploration_graph(String parallel) throws IOException, ExecutionException, InterruptedException {
        try {
            Utils.StateSpaceResult result;
            if (parallel.isEmpty()) {
                 result = Utils.performStateSpaceExplore(utils, petriNet);
            } else {
                result = Utils.performParallelStateSpaceExplore(utils, petriNet);
            }
            processedTransitons = result.processedTransitions;
            for (Record record : result.results) {
                results.put(record.state, record.successors);
            }

            for (Map.Entry<Integer, ClassifiedState> entry : result.states.entrySet()) {
                stateMappings.put(entry.getValue(), entry.getKey());
            }

        } catch (TimelessTrapException e) {
            timelessTrap = true;
        }
    }

    @Then("^I expect to see (\\d+) state transitions?")
    public void I_expect_transitions(int transitionCount) {
        assertEquals(transitionCount, processedTransitons);
    }

    @And("^I expect a record with state")
    public void I_expect_a_record_with_state(String jsonState) throws IOException, PetriNetComponentNotFoundException {
        state = StateUtils.tangibleStateFromJson(jsonState);
    }

    @And("^successor")
    public void successor(String jsonState) throws IOException, PetriNetComponentNotFoundException {
        successor = StateUtils.tangibleStateFromJson(jsonState);
    }

    @And("^rate (\\d+.\\d+)")
    public void rate(double rate) {
        int stateId = stateMappings.get(state);
        int successorId = stateMappings.get(successor);
        Map<Integer, Double> successors = results.get(stateId);
        Double actualRate = successors.get(successorId);
        assertNotNull("State transition not contained in results", actualRate);
        assertEquals("State transition rate not correct", rate, actualRate, 0.00001);
    }

    @And("^have thrown a TimelessTrapException$")
    public void have_thrown_a_TimelessTrapException() {
        assertTrue(timelessTrap);
    }
}
