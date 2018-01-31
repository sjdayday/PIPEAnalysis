package utils;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import pipe.reachability.StateExplorerUtils;
import pipe.reachability.algorithm.*;
import pipe.reachability.algorithm.parallel.MassiveParallelStateSpaceExplorer;
import pipe.reachability.algorithm.sequential.SequentialStateSpaceExplorer;
import pipe.reachability.algorithm.StateSpaceExplorer;
import uk.ac.imperial.io.EntireStateReader;
import uk.ac.imperial.io.KryoStateIO;
import uk.ac.imperial.io.MultiStateReader;
import uk.ac.imperial.io.StateProcessor;
import uk.ac.imperial.pipe.exceptions.InvalidRateException;
import uk.ac.imperial.pipe.io.PetriNetIOImpl;
import uk.ac.imperial.pipe.io.PetriNetReader;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;
import uk.ac.imperial.pipe.parsers.UnparsableException;
import uk.ac.imperial.state.ClassifiedState;
import uk.ac.imperial.state.Record;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Utility class used to help out step definitions for analysis testing
 */
public class Utils {
    private static final int THREADS = 4;

    private Utils() {
    }

    public static PetriNet readPetriNet(String path) throws JAXBException, FileNotFoundException {
        PetriNetReader io = new PetriNetIOImpl();
        return io.read(fileLocation(path));
    }

    public static String fileLocation(String path) {
        return Utils.class.getResource(path).getPath();
    }

    public static StateSpaceResult performStateSpaceExplore(StateExplorerUtils utils,
            ExplorerUtilities explorerUtilities)
            throws IOException, ExecutionException, InterruptedException, TimelessTrapException, InvalidRateException {
        KryoStateIO kryoIo = new KryoStateIO();
        int processedTransitons = 0;
        try (ByteArrayOutputStream transitionByteStream = new ByteArrayOutputStream();
                ByteArrayOutputStream stateByteStream = new ByteArrayOutputStream()) {
            try (Output transitionOutputStream = new Output(transitionByteStream);
                    Output stateOutputStream = new Output(stateByteStream)) {
                StateProcessor processor = utils
                        .getTangibleStateExplorer(kryoIo, transitionOutputStream, stateOutputStream);
                VanishingExplorer vanishingExplorer = utils.getVanishingExplorer(explorerUtilities);

                StateSpaceExplorer stateSpaceExplorer = new SequentialStateSpaceExplorer(explorerUtilities,
                        vanishingExplorer, processor);
                processedTransitons = stateSpaceExplorer
                        .generate(explorerUtilities.getCurrentState()).processedTransitions;
            }
            try (ByteArrayInputStream transitionInputStream = new ByteArrayInputStream(
                    transitionByteStream.toByteArray());
                    ByteArrayInputStream stateStream = new ByteArrayInputStream(stateByteStream.toByteArray());
                    Input inputStream = new Input(transitionInputStream);
                    Input stateInputStream = new Input(stateStream)) {
                MultiStateReader reader = new EntireStateReader(kryoIo);
                Collection<Record> records = reader.readRecords(inputStream);
                Map<Integer, ClassifiedState> mappings = reader.readStates(stateInputStream);
                return new StateSpaceResult(records, processedTransitons, mappings);
            }
        }
    }

    public static StateSpaceResult performParallelStateSpaceExplore(StateExplorerUtils utils,
            ExplorerUtilities explorerUtilities)
            throws IOException, ExecutionException, InterruptedException, TimelessTrapException, InvalidRateException {
        KryoStateIO kryoIo = new KryoStateIO();
        int processedTransitons = 0;
        try (ByteArrayOutputStream transitionByteStream = new ByteArrayOutputStream();
                ByteArrayOutputStream stateByteStream = new ByteArrayOutputStream()) {
            try (Output transitionOutputStream = new Output(transitionByteStream);
                    Output stateOutputStream = new Output(stateByteStream)) {
                StateProcessor processor = utils
                        .getTangibleStateExplorer(kryoIo, transitionOutputStream, stateOutputStream);
                VanishingExplorer vanishingExplorer = utils.getVanishingExplorer(explorerUtilities);

                StateSpaceExplorer stateSpaceExplorer = new MassiveParallelStateSpaceExplorer(explorerUtilities,
                        vanishingExplorer, processor, THREADS, 5);
                processedTransitons = stateSpaceExplorer
                        .generate(explorerUtilities.getCurrentState()).processedTransitions;
            }
            try (ByteArrayInputStream transitionInputStream = new ByteArrayInputStream(
                    transitionByteStream.toByteArray());
                    ByteArrayInputStream stateStream = new ByteArrayInputStream(stateByteStream.toByteArray());
                    Input inputStream = new Input(transitionInputStream);
                    Input stateInputStream = new Input(stateStream)) {
                MultiStateReader reader = new EntireStateReader(kryoIo);
                Collection<Record> records = reader.readRecords(inputStream);
                Map<Integer, ClassifiedState> mappings = reader.readStates(stateInputStream);
                return new StateSpaceResult(records, processedTransitons, mappings);
            }
        }
    }

    public static class StateSpaceResult {
        public StateSpaceResult(Collection<Record> results, int processedTransitions,
                Map<Integer, ClassifiedState> states) {
            this.results = results;
            this.processedTransitions = processedTransitions;
            this.states = states;
        }

        public final Collection<Record> results;

        public final int processedTransitions;

        public final Map<Integer, ClassifiedState> states;

    }
}
