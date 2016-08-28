package pipe.steadystate.metrics;

import uk.ac.imperial.pipe.animation.AnimationLogic;
import uk.ac.imperial.pipe.animation.PetriNetAnimationLogic;
import uk.ac.imperial.pipe.models.petrinet.ExecutablePetriNet;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;
import uk.ac.imperial.pipe.models.petrinet.Transition;
import uk.ac.imperial.state.ClassifiedState;

import java.util.HashMap;
import java.util.Map;

/**
 * Metrics utility class calculates useful metrics about transitions
 * once the steady state at equilibrium of a Petri net has been solved
 */
public final class TransitionMetrics {

    /**
     * Private constructor of utility class
     */
    private TransitionMetrics() {}

    public static Map<String, Double> getTransitionThroughput(Map<Integer, ClassifiedState> stateSpace, Map<Integer, Double> steadyState, PetriNet petriNet) {
    	ExecutablePetriNet executablePetriNet = petriNet.getExecutablePetriNet(); 
    	AnimationLogic animationLogic = new PetriNetAnimationLogic(executablePetriNet);
        Map<String, Double> throughputs = new HashMap<>();
        for (Map.Entry<Integer, ClassifiedState> entry : stateSpace.entrySet()) {
            int id = entry.getKey();
            ClassifiedState state = entry.getValue();

            for (Transition transition : animationLogic.getEnabledTransitions(state)) {
                String transitionId = transition.getId();
                double throughput = transition.getActualRate(executablePetriNet) * steadyState.get(id);
                double previous = throughputs.containsKey(transitionId) ? throughputs.get(transitionId) : 0;
                throughputs.put(transitionId, throughput + previous);
            }
        }
        return throughputs;
    }
}
