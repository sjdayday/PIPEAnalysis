package pipe.reachability.algorithm;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class BoundedExplorerUtilitiesTest {

    @Mock
    PetriNet petriNet;

    private final int MAX_EXPLORE = 10;

    BoundedExplorerUtilities utilities;

    @Before
    public void setUp() {
        utilities = new BoundedExplorerUtilities(petriNet, MAX_EXPLORE);
    }

    @Test
    public void canExploreIfLessThanMax() {
        assertTrue(utilities.canExploreMore(MAX_EXPLORE - 5));
    }


    @Test
    public void canExploreIfEqualToMax() {
        assertTrue(utilities.canExploreMore(MAX_EXPLORE));
    }


    @Test
    public void cannotExploreIfAboveMax() {
        assertFalse(utilities.canExploreMore(MAX_EXPLORE + 10));
    }


}