package pipe.steadystate.metrics;

import uk.ac.imperial.state.ClassifiedState;

import java.util.HashMap;
import java.util.Map;

/**
 * Metrics utility class calculates useful metrics about tokens
 * once the steady state at equilibrium of a Petri net has been solved
 */
public final class TokenMetrics {

    /**
     * Private utility constructor
     */
    private TokenMetrics() {
    }

    /**
     *
     * Calculates the average number of tokens on each place by doing the following:
     * Loop through each state:
     *   Loop through each place in the state:
     *      Loop through each token class in the place: (e.g. Default, Red, Blue etc.)
     *        calculate the probability of this token being here
     *        Add this to the averages
     *
     *
     * @param stateSpace state space containing a states integer id to the classified state
     * @param steadyState steady state of the state space
     * @return the average number of tokens on each place
     */
    public static Map<String, Map<String, Double>> averageTokensOnPlace(Map<Integer, ClassifiedState> stateSpace,
            Map<Integer, Double> steadyState) {
        Map<String, Map<String, Double>> averages = new HashMap<>();
        for (Map.Entry<Integer, ClassifiedState> entry : stateSpace.entrySet()) {
            int id = entry.getKey();
            ClassifiedState state = entry.getValue();
            for (String place : state.getPlaces()) {
                Map<String, Double> tokenAverages = getTokenAverages(averages, place);
                for (Map.Entry<String, Integer> tokenEntry : state.getTokens(place).entrySet()) {
                    String token = tokenEntry.getKey();
                    Integer tokenCount = tokenEntry.getValue();
                    Double previous = tokenAverages.get(token);
                    double probability = tokenCount * steadyState.get(id);
                    double newAverage = probability + (previous == null ? 0 : previous);
                    tokenAverages.put(token, newAverage);
                }
            }
        }
        return averages;
    }

    /**
     *
     * Uses the existing averages to get the token id -> count map.
     *
     * If it doesn't exist in existingAverages it is created and added.
     *
     * @param existingAverages
     * @param place place id
     * @return the map token id -> count for the given place
     */
    private static Map<String, Double> getTokenAverages(Map<String, Map<String, Double>> existingAverages,
            String place) {
        if (!existingAverages.containsKey(place)) {
            existingAverages.put(place, new HashMap<String, Double>());
        }
        return existingAverages.get(place);
    }
}
