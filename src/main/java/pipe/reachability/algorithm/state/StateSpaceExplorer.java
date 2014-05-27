package pipe.reachability.algorithm.state;

import pipe.reachability.algorithm.TimelessTrapException;
import uk.ac.imperial.state.ClassifiedState;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Performs state space exploration searching for the states that can be
 * reached from the given Petri net set up.
 *
 * There are numerous options for exploring the state space. One of which is
 * whether tangible states will be written out to the writer or if
 * vanishing states will be written too.
 *
 * Implementations of this interface should deal as flexibly as possible with different
 * scenarios for generating the state space.
 */
public interface StateSpaceExplorer {

    /**
     * Performs steady state exploration
     *
     * @param initialState starting state for exploration.
     * @throws pipe.reachability.algorithm.TimelessTrapException
     * @return number of transitions explored
     */
    StateSpaceExplorerResults generate(ClassifiedState initialState)
            throws TimelessTrapException, InterruptedException, ExecutionException, IOException;

    /**
     * Results to return from state space exploration
     */
    public class StateSpaceExplorerResults {
        public final int processedTransitions;
        public final int numberOfStates;

        public StateSpaceExplorerResults(int processedTransitions, int numberOfStates) {
            this.processedTransitions = processedTransitions;
            this.numberOfStates = numberOfStates;
        }
    }
}
