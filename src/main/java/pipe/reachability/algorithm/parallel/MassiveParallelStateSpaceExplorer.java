package pipe.reachability.algorithm.parallel;

import pipe.reachability.algorithm.*;
import uk.ac.imperial.io.StateProcessor;
import uk.ac.imperial.pipe.exceptions.InvalidRateException;
import uk.ac.imperial.state.ClassifiedState;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performs a parallel state space exploration using 8 threads.
 * At every iteration of the exploration these threads are submitted with a
 * maximum number of states they can fully explore before returning with
 * their results.
 * <p>
 * In effect this state space exploration is a thread level map reduce where states are
 * mapped onto threads and at the end of their run the results are reduced down into a
 * single result.
 * </p><p>
 * The iteration then continues performing another map reduce with the left over states
 * to explore from the previous iteration. 
 * </p>
 */
public final class MassiveParallelStateSpaceExplorer extends AbstractStateSpaceExplorer {
    /**
     * The number of threads that this class will use to explore the state space
     */
    private final int threads;

    /**
     * Class logger
     */
    private static final Logger LOGGER = Logger.getLogger(MassiveParallelStateSpaceExplorer.class.getName());

    /**
     * Number of states to analyse sequentially per thread
     */
    private final int statesPerThread;

    /**
     * Executor service used to submit tasks to
     */
    protected ExecutorService executorService;

    private Queue<ClassifiedState> sharedIterationQueue = new ConcurrentLinkedQueue<>();
    private Map<ClassifiedState, Map<ClassifiedState, Double>> iterationTransitions = new ConcurrentHashMap<>();
    private Map<ClassifiedState, Boolean> sharedHashSeen = new ConcurrentHashMap<>();

    /**
     * Constructor for generating massive state space exploration
     *
     * @param explorerUtilities utilities
     * @param vanishingExplorer explorer
     * @param stateProcessor  processor
     * @param statesPerThread   the number of states to allow each thread to explore in a single iteration
     *                          before returning to join the results together
     * @param threads across which to spread work                           
     */
    public MassiveParallelStateSpaceExplorer(ExplorerUtilities explorerUtilities, VanishingExplorer vanishingExplorer,
            StateProcessor stateProcessor, int threads, int statesPerThread) {
        super(explorerUtilities, vanishingExplorer, stateProcessor);

        this.statesPerThread = statesPerThread;
        this.threads = threads;
    }

    /**
     * Performs state space exploration by spinning up threads and allowing them to process
     * states in parallel. The number of states that each thread processes is set in the constructor
     * and is statesPerThread.
     * <p>
     * Results are then merged together into the explored and explorationQueue data sets
     * and transitions are written to the output stream.
     * </p><p>
     * A possible extension to this is to have the threads ask for work
     * if they run out and/or dynamically scale the number of threads processed according to
     * how it benefits each different state space.
     * </p>
     * @throws InterruptedException  thread interrupted
     * @throws ExecutionException task aborted due to exception
     * @throws TimelessTrapException if vanishing states lead to a timeless trap
     */
    @Override
    protected void stateSpaceExploration()
            throws InterruptedException, ExecutionException, TimelessTrapException, IOException {
        executorService = Executors.newFixedThreadPool(threads);
        CompletionService<Collection<Void>> completionService = new ExecutorCompletionService<>(executorService);
        int iterations = 0;
        long duration = 0;
        List<MultiStateExplorer> explorers = initialiseExplorers();
        sharedIterationQueue.addAll(explorationQueue);
        while (!sharedIterationQueue.isEmpty() && explorerUtilities.canExploreMore(stateCount)) {
            int submitted = 0;
            while (submitted < threads && !sharedIterationQueue.isEmpty()) {
                MultiStateExplorer explorer = explorers.get(submitted);
                completionService.submit(explorer);
                submitted++;
            }

            long start = System.nanoTime();
            for (int i = 0; i < submitted; i++) {
                completionService.take().get();
            }
            long end = System.nanoTime();
            duration += end - start;

            markAsExplored(sharedHashSeen.keySet());

            for (Map.Entry<ClassifiedState, Map<ClassifiedState, Double>> entry : iterationTransitions.entrySet()) {
                writeStateTransitions(entry.getKey(), entry.getValue());
            }

            sharedHashSeen.clear();
            iterationTransitions.clear();
            explorerUtilities.clear();
            iterations++;
        }

        executorService.shutdownNow();
        LOGGER.log(Level.INFO, "Took " + iterations + " iterations to explore state space with " +
                duration / (double) iterations + " time for each iteration");
    }

    private List<MultiStateExplorer> initialiseExplorers() {
        List<MultiStateExplorer> explorers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            explorers.add(new MultiStateExplorer());
        }
        return explorers;
    }

    /**
     * Callable implementation that explores a state and its successors up to a certain
     * depth.
     * <p>
     * It registers all transitions that it observes
     * </p>
     */
    private final class MultiStateExplorer implements Callable<Collection<Void>> {
        private MultiStateExplorer() {
        }

        /**
         * Performs sequential state space exploration using a BFS up to a certain number
         * of states
         *
         * @return the result of a BFS including any transitions seen, states that have not yet been explored
         * and those that have.
         * @throws TimelessTrapException unable to exit cyclic vanishing state
         * @throws InvalidRateException functional rate expression invalid
         */
        @Override
        public Collection<Void> call() throws TimelessTrapException, InvalidRateException {
            //            Collection<ClassifiedState> seen = new LinkedList<>();
            for (int explored = 0; explored < statesPerThread; explored++) {
                ClassifiedState state = sharedIterationQueue.poll();
                //Test to see if sharedIterationQueue is empty
                if (state == null) {
                    return null;
                }
                Map<ClassifiedState, Double> successorRates = new HashMap<>();
                for (ClassifiedState successor : explorerUtilities.getSuccessors(state)) {
                    double rate = explorerUtilities.rate(state, successor);
                    if (successor.isTangible()) {
                        registerStateRate(successor, rate, successorRates);
                        if (!seen(successor)) {
                            sharedIterationQueue.add(successor);
                            addToSharedSeen(successor);
                            //                            seen.add(successor);
                        }
                    } else {
                        Collection<StateRateRecord> explorableStates = vanishingExplorer.explore(successor, rate);
                        for (StateRateRecord record : explorableStates) {
                            registerStateRate(record.getState(), record.getRate(), successorRates);
                            if (!seen(record.getState())) {
                                sharedIterationQueue.add(record.getState());
                                addToSharedSeen(record.getState());
                                //                                seen.add(record.getState());
                            }
                        }
                    }
                }
                writeStateTransitions(state, successorRates);
            }
            return null;
        }

        private void addToSharedSeen(ClassifiedState state) {
            sharedHashSeen.put(state, true);
        }

        /**
         * Puts the successor and rate into the successor rates map.
         * If an entry already exists for the state then the rate is
         * summed within the rate.
         */
        /**
         * @param successor state
         * @param rate of the successor 
         * @param successorRates map of the successor rates
         */
        private void registerStateRate(ClassifiedState successor, double rate,
                Map<ClassifiedState, Double> successorRates) {
            if (successorRates.containsKey(successor)) {
                double previousRate = successorRates.get(successor);
                successorRates.put(successor, previousRate + rate);
            } else {
                successorRates.put(successor, rate);
            }
        }

        /**
         * @param state to evaluate
         * @return true if the state has already been explored
         */
        private boolean seen(ClassifiedState state) {
            return sharedHashSeen.containsKey(state) || explored.contains(state);
        }

        /**
         * Puts the state and its rates into the transitions data structure
         *
         * @param state to be written
         * @param successorRates to be written 
         */
        private void writeStateTransitions(ClassifiedState state, Map<ClassifiedState, Double> successorRates) {
            if (!iterationTransitions.containsKey(state)) {
                iterationTransitions.put(state, successorRates);
            }
        }
    }

}
