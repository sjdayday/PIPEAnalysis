package pipe.reachability.algorithm;

import pipe.steadystate.algorithm.AbstractSteadyStateSolver;
import uk.ac.imperial.io.StateProcessor;
import uk.ac.imperial.pipe.exceptions.InvalidRateException;
import uk.ac.imperial.state.ClassifiedState;
import uk.ac.imperial.state.State;
import uk.ac.imperial.utils.ExploredSet;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract state space explorer that contains useful methods for exploring the state space.
 * <p/>
 * The actual exploration method is delgated to subclasses
 */
public abstract class AbstractStateSpaceExplorer implements StateSpaceExplorer {
    /**
     * Prime number for the explored set size, trade off between wasted memory
     * and saturation avoidance
     */
    private static final int EXPLORED_SET_SIZE = 300007;

    private static final Logger LOGGER = Logger.getLogger(AbstractSteadyStateSolver.class.getName());

    /**
     * Used for processing transitions
     */
    protected final StateProcessor stateProcessor;

    /**
     * Used for exploring vanishing states
     */
    protected final VanishingExplorer vanishingExplorer;

    /**
     * Map to register successor states to their rate when exploring a state.
     * <p/>
     * When processing a tangible state it is possible that via multiple vanishing states
     * the same tangible state is the successor. In this case the rates must be summed.
     * <p/>
     * This map is therefore used to write transitions to temporarily whilst processing
     * all successors of a state. It is then used to write the records to the stateWriter
     * only once all successors have been processed.
     */
    protected final Map<ClassifiedState, Double> successorRates = new HashMap<>();

    /**
     * Performs useful state calculations
     */
    protected ExplorerUtilities explorerUtilities;

    /**
     * Queue for states yet to be explored
     */
    protected Deque<ClassifiedState> explorationQueue = new ArrayDeque<>();

    /**
     * Contains states that have already been explored.
     * Initialised in generate when initialState info is given
     */
    protected ExploredSet explored;

    /**
     * This value is used to give a unique number to each state seen
     */
    protected int stateCount = 0;

    /**
     * Number of states that have been processed during exploraton
     */
    private int processedCount = 0;

    public AbstractStateSpaceExplorer(ExplorerUtilities explorerUtilities, VanishingExplorer vanishingExplorer,
                                      StateProcessor stateProcessor) {
        this.explorerUtilities = explorerUtilities;
        this.vanishingExplorer = vanishingExplorer;
        this.stateProcessor = stateProcessor;
    }

    /**
     * Generates the state space from the initial state
     *
     * @param initialState starting state for exploration.
     * @return
     * @throws TimelessTrapException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws IOException
     * @throws InvalidRateException
     */
    @Override
    public final StateSpaceExplorerResults generate(ClassifiedState initialState)
            throws TimelessTrapException, InterruptedException, ExecutionException, IOException, InvalidRateException {
        long start = System.nanoTime();
        initialiseExplored(initialState);
        exploreInitialState(initialState);
        stateSpaceExploration();
        long end = System.nanoTime();
        long duration = end - start;
        LOGGER.log(Level.INFO, "Took " + duration + " to solve state space");
        return new StateSpaceExplorerResults(processedCount, stateCount);

    }

    /**
     * Initialises the explored set with the states place ordering and
     * the explored set size
     *
     * @param state
     */
    private void initialiseExplored(State state) {
        List<String> placeOrder = getPlaceNames(state);
        explored = new ExploredSet(EXPLORED_SET_SIZE, placeOrder);
    }

    /**
     * Populates tangibleQueue with all starting tangible states.
     * <p/>
     * In the case that initialState is tangible then this is just
     * added to the queue.
     * <p/>
     * Otherwise it must sort through vanishing states
     *
     * @param initialState starting state of the algorithm
     */
    protected final void exploreInitialState(ClassifiedState initialState)
            throws TimelessTrapException, InvalidRateException {
        if (initialState.isTangible()) {
            explorationQueue.add(initialState);
            markAsExplored(initialState);
        } else {
            Collection<StateRateRecord> explorableStates = vanishingExplorer.explore(initialState, 1.0);
            for (StateRateRecord record : explorableStates) {
                registerStateTransition(record.getState(), record.getRate());
            }
        }

    }

    /**
     * Abstratc method which performs the actual state space exploration algorithm.
     * This algorithm should make use of the explorationQueue and will write the results out
     * via the stateProcessor
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimelessTrapException
     * @throws IOException
     * @throws InvalidRateException
     */
    protected abstract void stateSpaceExploration()
            throws InterruptedException, ExecutionException, TimelessTrapException, IOException, InvalidRateException;

    /**
     * @param state
     * @return List of place names contained in state
     */
    private List<String> getPlaceNames(State state) {
        return new LinkedList<>(state.getPlaces());
    }

    /**
     * Adds a compressed version of a tangible state to exploredStates
     * Writes state with id to file
     *
     * @param state state that has been explored
     */
    protected final void markAsExplored(ClassifiedState state) {
        int uniqueNumber = getUniqueStateNumber();
        if (!explored.contains(state)) {
            stateProcessor.processState(state, uniqueNumber);
            explored.add(state, uniqueNumber);
        }
    }

    /**
     * registers a transition to the successor in stateRecords and
     * adds the successor to the exploredQueue if it is not already contained in it.
     *
     * @param successor state that is possible via an enabled transition from state
     * @param rate      rate at which state transitions to successor
     */
    protected final void registerStateTransition(ClassifiedState successor, double rate) {
        registerStateRate(successor, rate);
        if (!explored.contains(successor)) {
            explorationQueue.add(successor);
            markAsExplored(successor);
        }
    }

    /**
     * @return a unique number for every state
     */
    private int getUniqueStateNumber() {
        int number = stateCount;
        stateCount++;
        return number;
    }

    /**
     * Register the successor into successorRates map
     * <p/>
     * If successor already exists then the rate is summed, if not it
     * is added as a new entry
     *
     * @param successor key to successor rates
     * @param rate      rate at which successor is entered via some transition
     */
    protected final void registerStateRate(ClassifiedState successor, double rate) {
        if (successorRates.containsKey(successor)) {
            double previousRate = successorRates.get(successor);
            successorRates.put(successor, previousRate + rate);
        } else {
            successorRates.put(successor, rate);
        }
    }

    /**
     * Marks each state in explored as explored if it is not already in the explored set.
     * <p/>
     * It must do the contains check to ensure it does not get two different unique numbers
     *
     * @param exploredStates
     */
    protected final void markAsExplored(Collection<ClassifiedState> exploredStates) {
        for (ClassifiedState state : exploredStates) {
            if (!explored.contains(state)) {
                markAsExplored(state);
            }
        }
    }

    /**
     * This method writes all state transitions in the map stateTransitions out to the
     * state writer.
     * <p/>
     * It is assumed that no duplicate transitions exist by this point and that their rates
     * have been summed up. If this is not the case then multiple transitions will be written to
     * disk and must be dealt with accordingly.
     *
     * @param state          the current state that successors belong to
     * @param successorRates
     */
    protected final void writeStateTransitions(ClassifiedState state, Map<ClassifiedState, Double> successorRates) {
        Map<Integer, Double> transitions = getIntegerTransitions(successorRates);
        int stateId = explored.getId(state);
        stateProcessor.processTransitions(stateId, transitions);
        processedCount += successorRates.size();
    }

    /**
     * Modifies the successorRates map to replace the key with it's integer representation
     * that is stored in the explored set
     *
     * @param successorRates
     * @return a map of the successors integer id to the successor rate
     */
    private Map<Integer, Double> getIntegerTransitions(Map<ClassifiedState, Double> successorRates) {
        Map<Integer, Double> transitions = new HashMap<>();
        for (Map.Entry<ClassifiedState, Double> entry : successorRates.entrySet()) {
            int id = explored.getId(entry.getKey());
            transitions.put(id, entry.getValue());
        }
        return transitions;
    }
}
