package pipe.reachability.algorithm;

import java.util.Collection;
import java.util.Map;

import uk.ac.imperial.pipe.exceptions.InvalidRateException;
import uk.ac.imperial.pipe.models.petrinet.Transition;
import uk.ac.imperial.state.ClassifiedState;

/**
 * Useful class for performing state calculations on a Petri net
 */
public interface ExplorerUtilities {
    /**
     *
     * Finds successors of the given state. A successor is a state that occurs
     * when one of the enabled transitions in the current state is fired.
     *
     * @param state state in the Petri net to find successors of
     * @return map of successor states to the transitions that caused them
     */
    Map<ClassifiedState, Collection<Transition>> getSuccessorsWithTransitions(ClassifiedState state);

    /**
     *
     * Finds successors of the given state. A successor is a state that occurs
     * when one of the enabled transitions in the current state is fired.
     *
     * @param state state in the Petri net to find successors of
     * @return map of successor states to the transitions that caused them
     */
    Collection<ClassifiedState> getSuccessors(ClassifiedState state);

    /**
     * Calculates the rate of a  transition from a tangible state to the successor state.
     * It does this by calculating the transitions that are enabled at the given state,
     * the transitions that can be reached from that state and performs the intersection of the two.
     * <p>
     * It then sums the firing rates of this intersection and divides by the sum of the firing rates
     * of the enabled transition
     * </p>
     * @param state to evaluate
     * @param successor  rate to the successor state
     * @return rate of transition from tangible state to successor 
     * @throws InvalidRateException functional rate expression invalid
     */
    double rate(ClassifiedState state, ClassifiedState successor) throws InvalidRateException;

    /**
     *
     * Calculates the current underling state of the Petri net
     * and creates a new state.
     * <p>
     * It determines if it is a vanishing or tangible transition and returns
     * the correct implementation accordingly.
     * </p>
     * @return underlying state of the Petri net
     */
    ClassifiedState getCurrentState();

    /**
     * Calculates the set of transitions that will take you from one state to the successor.
     *
     * @param state     initial state
     * @param successor successor state, must be directly reachable from the state
     * @return enabled transitions that take you from state to successor, if it is not directly reachable then
     * an empty Collection will be returned
     */
    Collection<Transition> getTransitions(ClassifiedState state, ClassifiedState successor);

    /**
     *
     * Sums up the weights of the transitions. Transitions may have functional rates
     *
     *
     * @param state to evaluate
     * @param transitions whose weights are to be summed
     * @return summed up the weight of the transitions specified
     * @throws InvalidRateException functional rate expression invalid
     */
    double getWeightOfTransitions(ClassifiedState state, Iterable<Transition> transitions) throws InvalidRateException;

    /**
     *
     * @param state state in the Petri net to determine enabled transitions of
     * @return all enabled transitions for the specified state
     */
    Collection<Transition> getAllEnabledTransitions(ClassifiedState state);

    /**
     * Clear any saved states
     */
    void clear();

    /**
     * Since it is possible for reachability graphs to be infinite this method provides
     * a way to determine if the algorithm should continue past a certain size state count
     * @param stateCount count of states 
     * @return if the state space exploration can continue
     */
    boolean canExploreMore(int stateCount);

}
