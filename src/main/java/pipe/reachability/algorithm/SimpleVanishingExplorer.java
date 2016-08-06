package pipe.reachability.algorithm;

import java.util.Arrays;
import java.util.Collection;

import uk.ac.imperial.state.ClassifiedState;

/**
 * This state performs no computation of vanishing states and simply
 * returns them to the user to be explored normally
 */
public final class SimpleVanishingExplorer implements VanishingExplorer {
    /**
     *
     * @param vanishingState vanishing state to explore.
     * @param rate rate at which vanishingState is entered from the previous state
     * @return the exact vanishing state to explore
     * @throws TimelessTrapException unable to exit cyclic vanishing state
     */
    @Override
    public Collection<StateRateRecord> explore(ClassifiedState vanishingState, double rate)
            throws TimelessTrapException {
        return Arrays.asList(new StateRateRecord(vanishingState, rate));
    }
}
