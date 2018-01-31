package pipe.reachability.algorithm;

import uk.ac.imperial.pipe.exceptions.InvalidRateException;
import uk.ac.imperial.pipe.models.petrinet.Transition;
import uk.ac.imperial.state.ClassifiedState;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;

/**
 * Abstract class that contains the shared code for vanishing explorers
 *
 *  * <p>
 * It performs on the fly vanishing state elimination, producing the reachability graph for tangible states.
 * A tangible state is one in which:
 * a) Has no enabled transitions
 * b) Has entirely timed transitions leaving it
 * </p><p>
 *
 * The difference between the explorers is what happens when a tangible state is found
 * The recorded state is either the last tanigble or the parent vanishin state.
 * </p>
 * Differing implementations can choose which state to explore
 */
public final class OnTheFlyVanishingExplorer implements VanishingExplorer {
    /**
     * The number of times that cyclic vanishing states are allowed to be explored before a
     * {@link pipe.reachability.algorithm.TimelessTrapException} is thrown
     */
    //TODO ASK WILL FOR THE SIZE?
    private static final int ALLOWED_ITERATIONS = 1000;

    /**
     * Value used to eliminate a vanishing state. We do not explore a state if the rate into it is
     * less than this value
     */
    private static final double EPSILON = 0.0000001;

    /**
     * Explorer utilities useful for state manipulations
     */
    private final ExplorerUtilities explorerUtilities;

    /**
     * Constructor that takes the exploration utilities for generating reachability/coverability graphs
     * @param explorerUtilities utilities 
     */
    public OnTheFlyVanishingExplorer(ExplorerUtilities explorerUtilities) {
        this.explorerUtilities = explorerUtilities;
    }

    /**
     * Explores a vanishing state by processing its successors until either no vanishing states
     * are left or ALLOWED_ITERATIONS has been reached in the case of a cycle
     * <p>
     * Whilst performing this processing, any tangible states seen are registered with the current
     * rate at which the state transitions into them. If we see a transition more than once then its rate
     * is summed.
     * </p>
     * @param vanishingState vanishing state to explore.
     * @param rate rate at which vanishingState is entered from the previous state
     * @return tangible transitions that the vanishing state transitions to.
     * @throws TimelessTrapException unable to exit cyclic vanishing state
     * @throws InvalidRateException functional rate expression invalid
     */
    @Override
    public Collection<StateRateRecord> explore(ClassifiedState vanishingState, double rate)
            throws TimelessTrapException, InvalidRateException {
        Deque<StateRateRecord> vanishingStack = new ArrayDeque<>();
        vanishingStack.push(new StateRateRecord(vanishingState, rate));
        int iterations = 0;
        Collection<StateRateRecord> tangibleStatesFound = new LinkedList<>();
        while (!vanishingStack.isEmpty() && iterations < ALLOWED_ITERATIONS) {
            StateRateRecord record = vanishingStack.pop();
            ClassifiedState previous = record.getState();
            for (ClassifiedState successor : explorerUtilities.getSuccessors(previous)) {
                double successorRate = record.getRate() * probability(previous, successor);
                if (successor.isTangible()) {
                    tangibleStatesFound.add(new StateRateRecord(successor, successorRate));
                } else {
                    if (successorRate > EPSILON) {
                        vanishingStack.push(new StateRateRecord(successor, successorRate));
                    }
                }
            }
            iterations++;
        }
        if (iterations == ALLOWED_ITERATIONS) {
            throw new TimelessTrapException();
        }
        return tangibleStatesFound;
    }

    /**
     * Works out what transitions would lead you to the successor state then divides the sum
     * of their rates by the total rates of all enabled transitions
     *
     * @param state     initial state
     * @param successor next state
     * @return the probability of transitioning to the successor state from state
     * @throws InvalidRateException functional rate expression invalid
     */
    private double probability(ClassifiedState state, ClassifiedState successor) throws InvalidRateException {
        Collection<Transition> marked = explorerUtilities.getTransitions(state, successor);
        if (marked.isEmpty()) {
            return 0;
        }
        double toSuccessorWeight = explorerUtilities.getWeightOfTransitions(state, marked);
        double totalWeight = explorerUtilities
                .getWeightOfTransitions(state, explorerUtilities.getAllEnabledTransitions(state));
        return toSuccessorWeight / totalWeight;
    }

}
