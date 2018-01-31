package pipe.reachability;

import com.esotericsoftware.kryo.io.Output;
import pipe.reachability.algorithm.ExplorerUtilities;
import pipe.reachability.algorithm.VanishingExplorer;
import pipe.reachability.algorithm.SimpleVanishingExplorer;
import uk.ac.imperial.io.StateIOProcessor;
import uk.ac.imperial.io.StateProcessor;
import uk.ac.imperial.io.StateWriter;

public class TangibleAndVanishingUtils implements StateExplorerUtils {
    @Override
    public StateProcessor getTangibleStateExplorer(StateWriter stateWriter, Output transitionStream,
            Output stateStream) {
        return new StateIOProcessor(stateWriter, transitionStream, stateStream);
    }

    @Override
    public VanishingExplorer getVanishingExplorer(ExplorerUtilities explorerUtilities) {
        return new SimpleVanishingExplorer();
    }
}
