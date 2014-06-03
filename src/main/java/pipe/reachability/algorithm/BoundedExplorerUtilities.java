package pipe.reachability.algorithm;

import uk.ac.imperial.pipe.models.petrinet.PetriNet;

/**
 * Explorer utilities that sets a bound on the maximum number of states
 * that can be explored since it is possible for the reachability graph to be infinite
 */
public class BoundedExplorerUtilities extends CachingExplorerUtilities {


    /**
     * The approximate number of states that can be explored, after this
     * the utilities will return false to exploring more states
     */
    private final int maxNumberOfStates;

    public BoundedExplorerUtilities(PetriNet petriNet, int maxNumberOfStates) {
        super(petriNet);
        this.maxNumberOfStates = maxNumberOfStates;
    }

    /**
     * @param stateCount
     * @return if the state count is less than or equal to the maximum number of states
     */
    @Override
    public final boolean canExploreMore(int stateCount) {
        return stateCount <= maxNumberOfStates;
    }
}
