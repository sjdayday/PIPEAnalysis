package pipe.reachability.algorithm.sequential;

import pipe.reachability.algorithm.*;
import uk.ac.imperial.io.StateProcessor;
import uk.ac.imperial.state.ClassifiedState;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

public class CoverabilityExplorer extends AbstractStateSpaceExplorer{

    public CoverabilityExplorer(ExplorerUtilities explorerUtilities, VanishingExplorer vanishingExplorer,
                                StateProcessor stateProcessor) {
        super(explorerUtilities, vanishingExplorer, stateProcessor);
    }

    @Override
    protected void stateSpaceExploration()
            throws InterruptedException, ExecutionException, TimelessTrapException, IOException {
        while (!explorationQueue.isEmpty()) {
            ClassifiedState state = explorationQueue.poll();
            successorRates.clear();
            for (ClassifiedState successor : explorerUtilities.getSuccessors(state)) {
                double rate = explorerUtilities.rate(state, successor);
                if (successor.isTangible()) {
                    registerStateTransition(successor, rate);
                } else {
                    Collection<StateRateRecord> explorableStates = vanishingExplorer.explore(successor, rate);
                    for (StateRateRecord record : explorableStates) {
                        registerStateTransition(record.getState(), record.getRate());
                    }
                }
            }
            writeStateTransitions(state, successorRates);
            explorerUtilities.clear();
        }
    }
}
