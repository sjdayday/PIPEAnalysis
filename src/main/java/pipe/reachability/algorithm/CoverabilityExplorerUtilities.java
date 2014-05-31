package pipe.reachability.algorithm;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import uk.ac.imperial.pipe.models.petrinet.Transition;
import uk.ac.imperial.state.ClassifiedState;
import uk.ac.imperial.state.HashedClassifiedState;
import uk.ac.imperial.state.HashedStateBuilder;

import java.util.*;

/**
 * This class wraps an ExplorerUtilities with coverability logic.
 *
 * Coverability logic follows that given a path x1, x2, ...., xn
 * if for any x < xn xn has tokens greater than or equal to all
 * other token counts then the Petri net will be unbounded and have an
 * infite state space.
 *
 * To kirb this explosion we bound the state to have MAX_INT tokens
 * and limit its successors
 */
public class CoverabilityExplorerUtilities implements ExplorerUtilities {


   private final ExplorerUtilities explorerUtilities;

   private final Multimap<ClassifiedState, ClassifiedState> parents = HashMultimap.create();

    /**
     * Takes a copy of the Petri net to use for state space exploration so
     * not to affect the reference
     *
     * @param  utilities explorer utility to wrap with bounded state info
     */
    public CoverabilityExplorerUtilities(ExplorerUtilities utilities) {
        explorerUtilities = utilities;
    }

    /**
     *
     * @param state state in the Petri net to find successors of
     * @return successors that have potentially been bounded
     */
    @Override
    public Map<ClassifiedState, Collection<Transition>> getSuccessorsWithTransitions(ClassifiedState state) {
        Map<ClassifiedState, Collection<Transition>> successors =  explorerUtilities.getSuccessorsWithTransitions(state);
        Map<ClassifiedState, Collection<Transition>> boundedSuccessors = lookForUnbounded(state, successors);
        registerParent(state, boundedSuccessors.keySet());
        return boundedSuccessors;
    }

    /**
     * A state is considered to be bounded if it has MAX_INT tokens
     * @param state
     * @return if the state is bounded or not
     */
    private boolean isBoundedState(ClassifiedState state) {
        for (String place : state.getPlaces()) {
            for (int value : state.getTokens(place).values()) {
                if (value == Integer.MAX_VALUE) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<ClassifiedState, Collection<Transition>> lookForUnbounded(ClassifiedState state, Map<ClassifiedState, Collection<Transition>> successors) {
        Map<ClassifiedState, Collection<Transition>> boundedSuccessors = new HashMap<>();
        for (Map.Entry<ClassifiedState, Collection<Transition>> entry : successors.entrySet()) {
            ClassifiedState successor = entry.getKey();
            ClassifiedState bounded = getBoundedState(state, successor);
            boundedSuccessors.put(bounded, entry.getValue());
        }
        return boundedSuccessors;
    }

    private ClassifiedState getBoundedState(ClassifiedState parent, ClassifiedState state) {
        Queue<ClassifiedState> ancestors = new ArrayDeque<>();
        Set<ClassifiedState> exploredAncestors = new HashSet<>();

        ancestors.add(parent);
        while (!ancestors.isEmpty()) {
            ClassifiedState ancestor = ancestors.poll();
            if (isUnbounded(state, ancestor)) {
                return boundState(state, ancestor);
            }
            exploredAncestors.add(ancestor);
            for (ClassifiedState p : parents.get(ancestor)) {
                if (!exploredAncestors.contains(p)) {
                    ancestors.add(p);
                }
            }
        }
        return state;
    }

    private boolean isUnbounded(ClassifiedState state, ClassifiedState ancestor) {
        for (String place : state.getPlaces()) {
            for (Map.Entry<String, Integer> entry : state.getTokens(place).entrySet()) {
                String token = entry.getKey();
                int stateCount = entry.getValue();
                int ancestorCount = ancestor.getTokens(place).get(token);
                if (ancestorCount > stateCount) {
                    return false;
                }
            }
        }
        return true;
    }

    private ClassifiedState boundState(ClassifiedState state, ClassifiedState ancestor) {
        HashedStateBuilder builder = new HashedStateBuilder();
        for (String place : state.getPlaces()) {
            for (Map.Entry<String, Integer> entry : state.getTokens(place).entrySet()) {
                String token = entry.getKey();
                int stateCount = entry.getValue();
                int ancestorCount = ancestor.getTokens(place).get(token);
                if (ancestorCount >= stateCount) {
                    builder.placeWithToken(place, token, stateCount);
                } else {
                    builder.placeWithToken(place, token, Integer.MAX_VALUE);
                }
            }
        }
        if (state.isTangible()) {
            return HashedClassifiedState.tangibleState(builder.build());
        }
        return HashedClassifiedState.vanishingState(builder.build());
    }

    /**
     * Registers state as parents successors
     * @param state
     * @param successors
     */
    private void registerParent(ClassifiedState state, Collection<ClassifiedState> successors) {
        for (ClassifiedState successor : successors) {
            parents.put(successor, state);
        }
    }

    @Override
    public Collection<ClassifiedState> getSuccessors(ClassifiedState state) {
        return getSuccessorsWithTransitions(state).keySet();
    }

    @Override
    public double rate(ClassifiedState state, ClassifiedState successor) {
        return explorerUtilities.rate(state, successor);
    }

    @Override
    public ClassifiedState getCurrentState() {
        return explorerUtilities.getCurrentState();
    }

    @Override
    public Collection<Transition> getTransitions(ClassifiedState state, ClassifiedState successor) {
        return explorerUtilities.getTransitions(state, successor);
    }

    @Override
    public double getWeightOfTransitions(ClassifiedState state, Iterable<Transition> transitions) {
        return explorerUtilities.getWeightOfTransitions(state, transitions);
    }

    @Override
    public Collection<Transition> getAllEnabledTransitions(ClassifiedState state) {
        return explorerUtilities.getAllEnabledTransitions(state);
    }

    @Override
    public void clear() {
        explorerUtilities.clear();
    }

    /**
     * Coverability graph turns an infinite state space into a finite
     * space via bounding states so it is always possible that a state can continue
     *
     * @param stateCount
     * @return true
     */
    @Override
    public boolean canExploreMore(int stateCount) {
        return true;
    }
}
