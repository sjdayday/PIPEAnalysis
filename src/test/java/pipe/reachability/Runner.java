package pipe.reachability;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import pipe.reachability.algorithm.*;
import pipe.reachability.algorithm.parallel.MassiveParallelStateSpaceExplorer;
import pipe.reachability.algorithm.sequential.SequentialStateSpaceExplorer;
import pipe.reachability.algorithm.StateSpaceExplorer;
import pipe.steadystate.algorithm.*;
import uk.ac.imperial.io.EntireStateReader;
import uk.ac.imperial.io.KryoStateIO;
import uk.ac.imperial.io.MultiStateReader;
import uk.ac.imperial.io.StateProcessor;
import uk.ac.imperial.pipe.exceptions.InvalidRateException;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;
import uk.ac.imperial.pipe.parsers.UnparsableException;
import uk.ac.imperial.state.ClassifiedState;
import uk.ac.imperial.state.Record;
import utils.Utils;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Runner {

    private static final int ONE_MILLION = 1_000_000;

    public static void main(String[] args)
            throws JAXBException, UnparsableException, InterruptedException, ExecutionException, TimelessTrapException,
            IOException, InvalidRateException {
        run("/medium_complex_102400.xml");
//        run("/medium_complex3.xml");
//        run("/medium_complex_286720.xml");
//        PetriNet petriNet = Utils.readPetriNet("/steady_state_petri_nets/run5.xml");
//        Results r1 = processParallel(petriNet, 200);
//        Results r2 = processSequential(petriNet);
//        debugBizarreResults(r1, r2);

    }

    private static void debugBizarreResults(Results r1, Results r2) {
        Map<ClassifiedState, Map<ClassifiedState, Double>> full1 = fullStateSpace(r1);
        Map<ClassifiedState, Map<ClassifiedState, Double>> full2 = fullStateSpace(r2);

        int i = 0;
        for (Map.Entry<ClassifiedState, Map<ClassifiedState, Double>> e : full1.entrySet()) {
            ClassifiedState state = e.getKey();
            Map<ClassifiedState, Double> parallel = e.getValue();
            Map<ClassifiedState, Double> sequential = full2.get(state);
            if (parallel.size() != sequential.size()) {
                System.out.println("PARALLEL != SEQUENTIAL for state " + state);
                for (ClassifiedState succ : parallel.keySet()) {
                    System.out.println("SUCCESSORS: " + succ);
                }
                int debug_size = 1;
            }
            if (!parallel.equals(sequential)) {
                int debug_equality = 2;
            }
        }
        System.out.println(r1.equals(r2));
    }

    private static Map<ClassifiedState, Map<ClassifiedState, Double>> fullStateSpace(Results results) {
        Map<ClassifiedState, Map<ClassifiedState, Double>> full = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Double>> entry : results.transitions.entrySet()) {
            full.put(results.states.get(entry.getKey()), intToState(results.states, entry.getValue()));
        }

        return full;
    }

    private static Map<ClassifiedState, Double> intToState(Map<Integer, ClassifiedState> stateMappings, Map<Integer, Double> rates) {
        Map<ClassifiedState, Double> result = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : rates.entrySet()) {
            result.put(stateMappings.get(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private static Map<Integer, Map<Integer, Double>> recordToMap(List<Record> records) {
        Map<Integer, Map<Integer, Double>> results = new HashMap<>();
        for (Record record : records) {
            results.put(record.state, record.successors);
        }
        return results;
    }

    private static void foo()
            throws UnparsableException, InterruptedException, ExecutionException, TimelessTrapException, JAXBException,
            IOException, InvalidRateException {
        dataStructureExperiment("/medium_complex_5832.xml");
        System.out.println("~~~~~~~~~~~~~~~~");
        dataStructureExperiment("/medium_complex_28672.xml");
        System.out.println("~~~~~~~~~~~~~~~~");
        dataStructureExperiment("/medium_complex_102400.xml");
        System.out.println("~~~~~~~~~~~~~~~~");
        dataStructureExperiment("/medium_complex_286720.xml");
        System.out.println("~~~~~~~~~~~~~~~~");
    }

    private static void dataStructureExperiment(String s)
            throws JAXBException, UnparsableException, InterruptedException, ExecutionException, TimelessTrapException,
            IOException, InvalidRateException {
        System.out.println("Starting three runs of " + s);
        PetriNet petriNet = Utils.readPetriNet(s);
        for (int i = 0; i < 3; i++) {
            processSequential(petriNet);
        }
    }

    private static void run(String s)
            throws JAXBException, UnparsableException, InterruptedException, ExecutionException, TimelessTrapException,
            IOException, InvalidRateException {
        System.out.println("Results for " + s);
        System.out.println("================================");
        PetriNet petriNet = Utils.readPetriNet(s);
        processParallel(petriNet, 1000);
        System.gc();
        System.out.println("************************");
        processParallel(petriNet, 500);
        System.out.println("************************");
        processParallel(petriNet, 200);
        System.gc();
        System.out.println("************************");
        processParallel(petriNet, 100);
        System.gc();
        System.out.println("************************");
        processSequential(petriNet);
        System.gc();
        System.out.println("************************");
    }

    private static Results processSequential(PetriNet petriNet)
            throws IOException, InterruptedException, TimelessTrapException, ExecutionException, InvalidRateException {

        TangibleOnlyUtils utils = new TangibleOnlyUtils();
        KryoStateIO kryoIo = new KryoStateIO();
        Path transitions = Files.createTempFile("trans", ".tmp");
        Path state = Files.createTempFile("state", ".tmp");
        try (OutputStream transitionByteStream = Files.newOutputStream(transitions);
             OutputStream stateByteStream = Files.newOutputStream(state)) {
            try (Output transitionOutputStream = new Output(transitionByteStream); Output stateOutputStream = new Output(stateByteStream)) {
                StateProcessor processor = utils.getTangibleStateExplorer(kryoIo, transitionOutputStream, stateOutputStream);
                ExplorerUtilities explorerUtilities = new UnboundedExplorerUtilities(petriNet);
                VanishingExplorer vanishingExplorer = utils.getVanishingExplorer(explorerUtilities);

                SequentialStateSpaceExplorer stateSpaceExplorer =
                        new SequentialStateSpaceExplorer(explorerUtilities, vanishingExplorer, processor);

                explore(stateSpaceExplorer, explorerUtilities, " Sequential ");
            }
            try (InputStream transitionInputStream = Files.newInputStream(transitions);
                 InputStream stateStream = Files.newInputStream(state);
                 Input inputStream = new Input(transitionInputStream);
                 Input stateInputStream = new Input(stateStream)) {
                MultiStateReader reader = new EntireStateReader(kryoIo);
                List<Record> records = new ArrayList<>(reader.readRecords(inputStream));
                Map<Integer, ClassifiedState> mappings = reader.readStates(stateInputStream);


//                GaussSeidelSolver solver = new GaussSeidelSolver();
//                Map<Integer, Double> steadyState = solve(solver, records, "Gauss Seidel");
                return new Results(recordToMap(records), mappings);
            }
        }
    }

    private static Results processParallel(PetriNet petriNet, int statesPerThread)
            throws IOException, InterruptedException, TimelessTrapException, ExecutionException, InvalidRateException {

        TangibleOnlyUtils utils = new TangibleOnlyUtils();
        KryoStateIO kryoIo = new KryoStateIO();
        Path transitions = Files.createTempFile("trans", ".tmp");
        Path state = Files.createTempFile("state", ".tmp");
        try (OutputStream transitionByteStream = Files.newOutputStream(transitions);
             OutputStream stateByteStream = Files.newOutputStream(state)) {
            try (Output transitionOutputStream = new Output(transitionByteStream); Output stateOutputStream = new Output(stateByteStream)) {
                StateProcessor processor = utils.getTangibleStateExplorer(kryoIo, transitionOutputStream, stateOutputStream);
                ExplorerUtilities explorerUtilities = new UnboundedExplorerUtilities(petriNet);
                VanishingExplorer vanishingExplorer = utils.getVanishingExplorer(explorerUtilities);

                MassiveParallelStateSpaceExplorer stateSpaceExplorer =
                        new MassiveParallelStateSpaceExplorer(explorerUtilities, vanishingExplorer, processor, statesPerThread);

                explore(stateSpaceExplorer, explorerUtilities, " Parallel " + statesPerThread);

            }
            try (InputStream transitionInputStream = Files.newInputStream(transitions);
                 InputStream stateStream = Files.newInputStream(state);
                 Input inputStream = new Input(transitionInputStream);
                 Input stateInputStream = new Input(stateStream)) {
                EntireStateReader reader = new EntireStateReader(kryoIo);
                List<Record> records = new ArrayList<>(reader.readRecords(inputStream));
                Map<Integer, ClassifiedState> mappings = reader.readStates(stateInputStream);

//                SteadyStateBuilder builder = new SteadyStateBuilderImpl();
//                ParallelSteadyStateSolver solver = new ParallelSteadyStateSolver(8, builder);
//                Map<Integer, Double> steadyState = solve(solver, records, "Parallel");
//                System.out.println("----------------------");
//                GaussSeidelSolver gaussSeidelSolver = new GaussSeidelSolver();
//                solve(gaussSeidelSolver, records, "Gauss Seidel");

                return new Results(recordToMap(records), mappings);

            }
        }

    }

    private static Map<Integer, Double> solve(SteadyStateSolver solver, List<Record> records, String method) {
        System.out.println("Starting " + method + " stead state solving");
        long startTime = System.nanoTime();
        Map<Integer, Double> steadyState = solver.solve(records);
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        System.out.println("Solved steady state In time: " + duration);

//        for (Map.Entry<Integer, Double> entry : steadyState.entrySet()) {
//            System.out.println("State: " + entry.getKey() + " prob " + entry.getValue());
//        }
        return steadyState;
    }

    private static void explore(StateSpaceExplorer explorer,  ExplorerUtilities explorerUtilities, String name)
            throws InterruptedException, ExecutionException, IOException, TimelessTrapException, InvalidRateException {
        System.out.println("Starting " + name);
        System.out.println("========================");
        long startTime = System.nanoTime();
        StateSpaceExplorer.StateSpaceExplorerResults result = explorer.generate(explorerUtilities.getCurrentState());
        long endTime = System.nanoTime();

        long duration = endTime - startTime;
        System.out.println("Processed transitions: " + result.processedTransitions);
        System.out.println("In time: " + duration);
        System.out.println(result.numberOfStates + " different states");
    }

    private static class Results {
        final Map<Integer, Map<Integer, Double>> transitions;
        final Map<Integer, ClassifiedState> states;

        private Results(Map<Integer, Map<Integer, Double>> transitions, Map<Integer, ClassifiedState> states) {
            this.transitions = transitions;
            this.states = states;
        }
    }
}
