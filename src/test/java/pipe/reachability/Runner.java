package pipe.reachability;

import com.esotericsoftware.kryo.io.Output;
import pipe.reachability.algorithm.*;
import pipe.reachability.algorithm.parallel.MassiveParallelStateSpaceExplorer;
import pipe.reachability.algorithm.sequential.SequentialStateSpaceExplorer;
import pipe.steadystate.algorithm.SteadyStateSolver;
import uk.ac.imperial.io.KryoStateIO;
import uk.ac.imperial.io.StateProcessor;
import uk.ac.imperial.pipe.exceptions.InvalidRateException;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;
import uk.ac.imperial.pipe.parsers.UnparsableException;
import uk.ac.imperial.state.Record;
import utils.Utils;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Runner {

    private static final int ONE_MILLION = 1_000_000;
    private static final int THREADS = 4;

    public static void main(String[] args)
            throws JAXBException, UnparsableException, InterruptedException, ExecutionException, TimelessTrapException,
            IOException, InvalidRateException {
//        run("/medium_complex_102400.xml");
//        run("/medium_complex_286720.xml");
//        PetriNet petriNet = Utils.readPetriNet("/pipe4_tests/4046.xml");
//        processSequential(petriNet);
//        processParallel(petriNet, 100);

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
        System.gc();
        System.out.println("************************");
        processParallel(petriNet, 100);
        System.gc();
        System.out.println("************************");
        processSequential(petriNet);
        System.gc();
        System.out.println("************************");
    }

    private static void processSequential(PetriNet petriNet)
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
//            try (InputStream transitionInputStream = Files.newInputStream(transitions);
//                 InputStream stateStream = Files.newInputStream(state);
//                 Input inputStream = new Input(transitionInputStream);
//                 Input stateInputStream = new Input(stateStream)) {
//                MultiStateReader reader = new EntireStateReader(kryoIo);
//                List<Record> records = new ArrayList<>(reader.readRecords(inputStream));
//                Map<Integer, ClassifiedState> mappings = reader.readStates(stateInputStream);
//
//
//                GaussSeidelSolver solver = new GaussSeidelSolver();
////                Map<Integer, Double> steadyState = solve(solver, records, "Gauss Seidel");
//
//
//            }
        }
    }

    private static void processParallel(PetriNet petriNet, int statesPerThread)
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
                        new MassiveParallelStateSpaceExplorer(explorerUtilities, vanishingExplorer, processor, THREADS, statesPerThread);

                explore(stateSpaceExplorer, explorerUtilities, " Parallel " + statesPerThread);

            }
//            try (InputStream transitionInputStream = Files.newInputStream(transitions);
//                 InputStream stateStream = Files.newInputStream(state);
//                 Input inputStream = new Input(transitionInputStream);
//                 Input stateInputStream = new Input(stateStream)) {
//                MultiStateReader reader = new EntireStateReader(kryoIo);
//                List<Record> records = new ArrayList<>(reader.readRecords(inputStream));
//                Map<Integer, ClassifiedState> mappings = reader.readStates(stateInputStream);
//
//                SteadyStateBuilder builder = new SteadyStateBuilderImpl();
//                ParallelSteadyStateSolver solver = new ParallelSteadyStateSolver(8, builder);
//                Map<Integer, Double> steadyState = solve(solver, records, "Parallel");
//                System.out.println("----------------------");
////                GaussSeidelSolver gaussSeidelSolver = new GaussSeidelSolver();
////                solve(gaussSeidelSolver, records, "Gauss Seidel");
//
//
//            }
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
}
