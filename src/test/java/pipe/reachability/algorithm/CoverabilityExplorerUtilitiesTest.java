package pipe.reachability.algorithm;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.ac.imperial.pipe.models.petrinet.Transition;
import uk.ac.imperial.state.ClassifiedState;
import uk.ac.imperial.state.HashedClassifiedState;
import uk.ac.imperial.state.HashedStateBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CoverabilityExplorerUtilitiesTest {

    @Mock
    ExplorerUtilities explorerUtilities;

    CoverabilityExplorerUtilities classifiedUtilities;

    @Before
    public void setUp() {
      classifiedUtilities  = new CoverabilityExplorerUtilities(explorerUtilities);
    }


    @Test
    public void boundsUnboundState() {

        Collection<Transition> transitions = new LinkedList<>();
        Map<ClassifiedState, Collection<Transition>> succs = new HashMap<>();
        succs.put(buildState(1,2), transitions);

        ClassifiedState state = buildState(1,1);
        when(explorerUtilities.getSuccessorsWithTransitions(state)).thenReturn(succs);
        Collection<ClassifiedState> actualSuccs = classifiedUtilities.getSuccessors(state);
        assertEquals(1, actualSuccs.size());
        ClassifiedState actual = actualSuccs.iterator().next();
        ClassifiedState expected = buildState(1, Integer.MAX_VALUE);
        assertEquals(expected, actual);
    }

//    @Test
//    public void boundedStatesHaveNoSelfSuccessors() {
//        ClassifiedState bounded = buildState(1, 2, Integer.MAX_VALUE);
//        Collection<ClassifiedState> actualSuccs = classifiedUtilities.getSuccessors(bounded);
//        assertTrue(actualSuccs.isEmpty());
//        verifyNoMoreInteractions(explorerUtilities);
//    }



    /**
     * Builds a single token state with place counts
     * @param counts
     * @return
     */
    ClassifiedState buildState(int...counts) {
        HashedStateBuilder stateBuilder = new HashedStateBuilder();
        int i = 0;
        for(int count : counts) {
            stateBuilder.placeWithToken("P"+i, "Default", count);
            i++;
        }
        return HashedClassifiedState.tangibleState(stateBuilder.build());
    }

}