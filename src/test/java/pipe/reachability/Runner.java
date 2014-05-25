package pipe.reachability;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import pipe.reachability.algorithm.CachingExplorerUtilities;
import pipe.reachability.algorithm.ExplorerUtilities;
import pipe.reachability.algorithm.TimelessTrapException;
import pipe.reachability.algorithm.VanishingExplorer;
import pipe.reachability.algorithm.parallel.MassiveParallelStateSpaceExplorer;
import pipe.reachability.algorithm.sequential.SequentialStateSpaceExplorer;
import pipe.steadystate.algorithm.GaussSeidelSolver;
import pipe.steadystate.algorithm.PowerSolver;
import uk.ac.imperial.io.EntireStateReader;
import uk.ac.imperial.io.KryoStateIO;
import uk.ac.imperial.io.MultiStateReader;
import uk.ac.imperial.io.StateProcessor;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Runner {

    public static void main(String[] args)
            throws JAXBException, UnparsableException, InterruptedException, ExecutionException, TimelessTrapException,
            IOException {
        PetriNet petriNet = Utils.readPetriNet("/simple_vanishing.xml");


//        processSequential(petriNet);
        processParallel(petriNet, 1000);
    }

    private static void processSequential(PetriNet petriNet)
            throws IOException, InterruptedException, TimelessTrapException, ExecutionException {

        TangibleOnlyUtils utils = new TangibleOnlyUtils();
        KryoStateIO kryoIo = new KryoStateIO();
        Path transitions = Files.createTempFile("trans", ".tmp");
        Path state = Files.createTempFile("state", ".tmp");
        try (OutputStream transitionByteStream = Files.newOutputStream(transitions);
             OutputStream stateByteStream = Files.newOutputStream(state)) {
            try (Output transitionOutputStream = new Output(transitionByteStream); Output stateOutputStream = new Output(stateByteStream)) {
                StateProcessor processor = utils.getTangibleStateExplorer(kryoIo, transitionOutputStream, stateOutputStream);
                ExplorerUtilities explorerUtilities = new CachingExplorerUtilities(petriNet);
                VanishingExplorer vanishingExplorer = utils.getVanishingExplorer(explorerUtilities);

                SequentialStateSpaceExplorer stateSpaceExplorer =
                        new SequentialStateSpaceExplorer(explorerUtilities, vanishingExplorer, processor);

                System.out.println("Starting sequential ");
                long startTime = System.nanoTime();
                int processedTransitions = stateSpaceExplorer.generate(explorerUtilities.getCurrentState());
                long endTime = System.nanoTime();

                long duration = endTime - startTime;
                System.out.println("Processed transitions: " + processedTransitions);
                System.out.println("In time: " + duration);
                System.out.println(stateSpaceExplorer.stateCount + " different states");
            }
            try (InputStream transitionInputStream = Files.newInputStream(transitions);
                 InputStream stateStream = Files.newInputStream(state);
                 Input inputStream = new Input(transitionInputStream);
                 Input stateInputStream = new Input(stateStream)) {
                MultiStateReader reader = new EntireStateReader(kryoIo);
                List<Record> records = new ArrayList<>(reader.readRecords(inputStream));
                Map<Integer, ClassifiedState> mappings = reader.readStates(stateInputStream);


                GaussSeidelSolver solver = new GaussSeidelSolver();
                Map<Integer, Double> steadyState = solver.solve(records);
                for (Map.Entry<Integer, Double> entry : steadyState.entrySet()) {
                    System.out.println("State: " + entry.getKey() + " prob " + entry.getValue());
                }

            }
        }
    }

    private static void processParallel(PetriNet petriNet, int statesPerThread)
            throws IOException, InterruptedException, TimelessTrapException, ExecutionException {

        TangibleOnlyUtils utils = new TangibleOnlyUtils();
        KryoStateIO kryoIo = new KryoStateIO();
        Path transitions = Files.createTempFile("trans", ".tmp");
        Path state = Files.createTempFile("state", ".tmp");
        try (OutputStream transitionByteStream = Files.newOutputStream(transitions);
             OutputStream stateByteStream = Files.newOutputStream(state)) {
            try (Output transitionOutputStream = new Output(transitionByteStream); Output stateOutputStream = new Output(stateByteStream)) {
                StateProcessor processor = utils.getTangibleStateExplorer(kryoIo, transitionOutputStream, stateOutputStream);
                ExplorerUtilities explorerUtilities = new CachingExplorerUtilities(petriNet);
                VanishingExplorer vanishingExplorer = utils.getVanishingExplorer(explorerUtilities);

                MassiveParallelStateSpaceExplorer stateSpaceExplorer =
                        new MassiveParallelStateSpaceExplorer(explorerUtilities, vanishingExplorer, processor, statesPerThread);

                System.out.println("Starting parallel " + statesPerThread);
                long startTime = System.nanoTime();
                int processedTransitions = stateSpaceExplorer.generate(explorerUtilities.getCurrentState());
                long endTime = System.nanoTime();

                long duration = endTime - startTime;
                System.out.println("Processed transitions: " + processedTransitions);
                System.out.println("In time: " + duration);
                System.out.println(stateSpaceExplorer.stateCount + " different states");
            }
            try (InputStream transitionInputStream = Files.newInputStream(transitions);
                 InputStream stateStream = Files.newInputStream(state);
                 Input inputStream = new Input(transitionInputStream);
                 Input stateInputStream = new Input(stateStream)) {
                MultiStateReader reader = new EntireStateReader(kryoIo);
                List<Record> records = new ArrayList<>(reader.readRecords(inputStream));
                Map<Integer, ClassifiedState> mappings = reader.readStates(stateInputStream);

                PowerSolver solver = new PowerSolver(1);
                Map<Integer, Double> steadyState = solver.solve(records);
                for (Map.Entry<Integer, Double> entry : steadyState.entrySet()) {
                    System.out.println("State: " + entry.getKey() + " prob " + entry.getValue());
                }
            }
        }

    }
}
