package pipe.reachability.algorithm;

import uk.ac.imperial.state.ClassifiedState;

import java.util.Arrays;
import java.util.Collection;

/**
 * This state performs no computation of vanishing states and simply
 * returns them to the user to be explored normally
 */
public final class SimpleVanishingExplorer implements VanishingExplorer {
    @Override
    public Collection<StateRateRecord> explore(ClassifiedState vanishingState, double rate)
            throws TimelessTrapException {
        return Arrays.asList(new StateRateRecord(vanishingState, rate));
    }
}
