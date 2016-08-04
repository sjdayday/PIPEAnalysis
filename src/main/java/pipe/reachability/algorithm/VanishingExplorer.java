package pipe.reachability.algorithm;

import java.util.Collection;

import uk.ac.imperial.pipe.exceptions.InvalidRateException;
import uk.ac.imperial.state.ClassifiedState;

/**
 * Interface used to explore all vanishing states.
 *
 * Further implementations can choose to eliminate them from the steady state exploration
 * or incorporate them into the exploration
 */
public interface VanishingExplorer {

    /**
     *
     * @param vanishingState vanishing state to explore.
     * @param rate rate at which vanishingState is entered from the previous state
     * @return Collection of states found to explore whilst processing the vanishing state
     * @throws TimelessTrapException unable to exit cyclic vanishing state
     * @throws InvalidRateException functional rate expression invalid
     */
    Collection<StateRateRecord> explore(ClassifiedState vanishingState, double rate)
            throws TimelessTrapException, InvalidRateException;
}
