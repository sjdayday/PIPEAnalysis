package pipe.reachability.algorithm;

import uk.ac.imperial.pipe.models.petrinet.PetriNet;

/**
 * Unbounded exploring utilities has no bound on the number
 * of states that can be explored
 */
public final class UnboundedExplorerUtilities extends CachingExplorerUtilities {
    /**
     * Takes a copy of the Petri net to use for state space exploration so
     * not to affect the reference
     *
     * @param petriNet petri net to use for state space exploration
     */
    public UnboundedExplorerUtilities(PetriNet petriNet) {
        super(petriNet);
    }

    /**
     *
     * @param stateCount count of the states 
     * @return true because this is an unbounded implementation
     */
    @Override
    public boolean canExploreMore(int stateCount) {
        return true;
    }
}
