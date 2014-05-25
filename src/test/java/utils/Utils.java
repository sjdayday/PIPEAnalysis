package utils;


import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import pipe.reachability.StateExplorerUtils;
import pipe.reachability.algorithm.CachingExplorerUtilities;
import pipe.reachability.algorithm.ExplorerUtilities;
import pipe.reachability.algorithm.TimelessTrapException;
import pipe.reachability.algorithm.VanishingExplorer;
import pipe.reachability.algorithm.parallel.MassiveParallelStateSpaceExplorer;
import pipe.reachability.algorithm.sequential.SequentialStateSpaceExplorer;
import pipe.reachability.algorithm.state.StateSpaceExplorer;
import uk.ac.imperial.io.EntireStateReader;
import uk.ac.imperial.io.KryoStateIO;
import uk.ac.imperial.io.MultiStateReader;
import uk.ac.imperial.io.StateProcessor;
import uk.ac.imperial.pipe.io.PetriNetIOImpl;
import uk.ac.imperial.pipe.io.PetriNetReader;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;
import uk.ac.imperial.pipe.parsers.UnparsableException;
import uk.ac.imperial.state.ClassifiedState;
import uk.ac.imperial.state.Record;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Utility class used to help out step definitions for analysis testsing
 */
public class Utils {
    private Utils() {
    }

    public static PetriNet readPetriNet(String path) throws JAXBException, UnparsableException {
        PetriNetReader io = new PetriNetIOImpl();
        return io.read(fileLocation(path));
    }

    public static String fileLocation(String path) {
        return Utils.class.getResource(path).getPath();
    }

    public static StateSpaceResult performStateSpaceExplore(StateExplorerUtils utils, PetriNet petriNet)
            throws IOException, ExecutionException, InterruptedException, TimelessTrapException {
        KryoStateIO kryoIo = new KryoStateIO();
        int processedTransitons = 0;
        try (ByteArrayOutputStream transitionByteStream = new ByteArrayOutputStream();
             ByteArrayOutputStream stateByteStream = new ByteArrayOutputStream()) {
            try (Output transitionOutputStream = new Output(transitionByteStream); Output stateOutputStream = new Output(stateByteStream)) {
                StateProcessor processor = utils.getTangibleStateExplorer(kryoIo, transitionOutputStream, stateOutputStream);
                ExplorerUtilities explorerUtilities = new CachingExplorerUtilities(petriNet);
                VanishingExplorer vanishingExplorer = utils.getVanishingExplorer(explorerUtilities);

                StateSpaceExplorer stateSpaceExplorer =
                        new SequentialStateSpaceExplorer(explorerUtilities, vanishingExplorer, processor);
                processedTransitons = stateSpaceExplorer.generate(explorerUtilities.getCurrentState());
            }
            try (ByteArrayInputStream transitionInputStream = new ByteArrayInputStream(transitionByteStream.toByteArray());
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

    public static StateSpaceResult performParallelStateSpaceExplore(StateExplorerUtils utils, PetriNet petriNet)
            throws IOException, ExecutionException, InterruptedException, TimelessTrapException {
        KryoStateIO kryoIo = new KryoStateIO();
        int processedTransitons = 0;
        try (ByteArrayOutputStream transitionByteStream = new ByteArrayOutputStream();
             ByteArrayOutputStream stateByteStream = new ByteArrayOutputStream()) {
            try (Output transitionOutputStream = new Output(transitionByteStream); Output stateOutputStream = new Output(stateByteStream)) {
                StateProcessor processor = utils.getTangibleStateExplorer(kryoIo, transitionOutputStream, stateOutputStream);
                ExplorerUtilities explorerUtilities = new CachingExplorerUtilities(petriNet);
                VanishingExplorer vanishingExplorer = utils.getVanishingExplorer(explorerUtilities);

                StateSpaceExplorer stateSpaceExplorer =
                        new MassiveParallelStateSpaceExplorer(explorerUtilities, vanishingExplorer, processor, 5);
                processedTransitons = stateSpaceExplorer.generate(explorerUtilities.getCurrentState());
            }
            try (ByteArrayInputStream transitionInputStream = new ByteArrayInputStream(transitionByteStream.toByteArray());
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
