package pipe.reachability.io;

import uk.ac.imperial.pipe.exceptions.InvalidRateException;
import uk.ac.imperial.state.Record;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import pipe.reachability.algorithm.TimelessTrapException;

/**
 * Reads the results from generating the state space exploration
 */
public interface StateSpaceExplorationReader {
    /**
     * Process the entire stream and build up a collection of Records.
     *
     * @param stream input stream
     * @return Collection of all state transitions with rates
     * @throws IOException error doing IO
     */
    Collection<Record> getRecords(ObjectInputStream stream) throws IOException;
}
