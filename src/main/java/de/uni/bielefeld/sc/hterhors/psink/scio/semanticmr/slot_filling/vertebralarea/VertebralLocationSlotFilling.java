package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.VertebralAreaRestrictionProvider.EVertebralAreaModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.Stats;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 *
 */
public class VertebralLocationSlotFilling {
	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 0)
			new VertebralLocationSlotFilling(1000L, "PREDICT");
		else
			new VertebralLocationSlotFilling(1000L, args[0]);
	}

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("data/slot_filling/vertebral_location/instances/");

	ENERModus modus;

	public VertebralLocationSlotFilling(long randomSeed, final String modusName) throws IOException {
		InstanceProvider.removeEmptyInstances = false;

		/**
		 * Initialize the system.
		 * 
		 * The scope represents the specifications of the 4 defined specification files.
		 * The scope mainly affects the exploration.
		 */
		SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result"))
				/**
				 * We apply the scope, so that we can add normalization functions for various
				 * literal entity types, if necessary.
				 */
				.apply()
				/**
				 * Finally, we build the systems scope.
				 */
				.build();

		Random random = new Random(randomSeed);
		Map<String, Score> scoreMap = new HashMap<>();
		modus = ENERModus.valueOf(modusName);

		EVertebralAreaModifications rule = EVertebralAreaModifications.NO_MODIFICATION;

		long seed = random.nextLong();
		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(seed)
				.setTrainingProportion(100).setDevelopmentProportion(0).setCorpusSizeFraction(1F).build();

		/**
		 * The instance provider reads all json files in the given directory. We can set
		 * the distributor in the constructor. If not all instances should be read from
		 * the file system, we can add an additional parameter that specifies how many
		 * instances should be read. NOTE: in contrast to the corpusSizeFraction in the
		 * ShuffleCorpusDistributor, we initially set a limit to the number of files
		 * that should be read.
		 */
		InstanceProvider instanceProvider;

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
				VertebralAreaRestrictionProvider.getByRule(rule));

		String modelName = "VertebralLocation_PREDICT";

		List<String> trainingInstanceNames = instanceProvider
				.getTrainingInstances().stream().map(t -> t.getName()).filter(n -> !(n.startsWith("N255")
						|| n.startsWith("N256") || n.startsWith("N258") || n.startsWith("N259")))
				.collect(Collectors.toList());

		List<String> developInstanceNames = instanceProvider
				.getTrainingInstances().stream().map(t -> t.getName()).filter(n -> (n.startsWith("N255")
						|| n.startsWith("N256") || n.startsWith("N258") || n.startsWith("N259")))
				.collect(Collectors.toList());

		List<String> testInstanceNames = instanceProvider.getTestInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		VertebralLocationPredictor predictor = new VertebralLocationPredictor(modelName, trainingInstanceNames,
				developInstanceNames, testInstanceNames, rule, modus);
//		VertebralLocationPredictor predictor = new VertebralLocationPredictor(modelName,
//				instanceProvider.getTrainingInstances().stream().map(t -> t.getName()).collect(Collectors.toList()),
//				instanceProvider.getDevelopmentInstances().stream().map(t -> t.getName()).collect(Collectors.toList()),
//				instanceProvider.getTestInstances().stream().map(t -> t.getName()).collect(Collectors.toList()), rule,
//				modus);

		Set<SlotType> slotTypesToConsider = new HashSet<>();
		slotTypesToConsider.add(SCIOSlotTypes.hasUpperVertebrae);
		slotTypesToConsider.add(SCIOSlotTypes.hasLowerVertebrae);

//		Stats.countVariables(1,instanceProvider.getInstances(),slotTypesToConsider);
//		System.exit(1);
		//
		Stats.computeNormedVar(instanceProvider.getInstances(), SCIOEntityTypes.vertebralLocation);

		for (SlotType slotType : slotTypesToConsider) {
			Stats.computeNormedVar(instanceProvider.getInstances(), slotType);
		}
		System.exit(1);
		predictor.trainOrLoadModel();

		Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

		AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE, EEvaluationDetail.LITERAL);

		Map<Instance, State> coverageStates = predictor
				.coverageOnDevelopmentInstances(SCIOEntityTypes.vertebralLocation, false);

		System.out.println("---------------------------------------");

		PerSlotEvaluator.evalRoot(EScoreType.MACRO, finalStates, coverageStates, evaluator, scoreMap);

		PerSlotEvaluator.evalProperties(EScoreType.MACRO, finalStates, coverageStates, slotTypesToConsider, evaluator,
				scoreMap);

		PerSlotEvaluator.evalCardinality(EScoreType.MACRO, finalStates, coverageStates, scoreMap);

		PerSlotEvaluator.evalOverall(EScoreType.MACRO, finalStates, coverageStates, evaluator, scoreMap);

		/**
		 * Computes the coverage of the given instances. The coverage is defined by the
		 * objective mean score that can be reached relying on greedy objective function
		 * sampling strategy. The coverage can be seen as the upper bound of the system.
		 * The upper bound depends only on the exploration strategy, e.g. the provided
		 * NER-annotations during slot-filling.
		 */
		log.info("modelName: " + predictor.modelName);
		/**
		 * TODO: Compare results with results when changing some parameter. Implement
		 * more sophisticated feature-templates.
		 */

		log.info("\n\n\n*************************");

		for (Entry<String, Score> sm : scoreMap.entrySet()) {
			log.info(sm.getKey() + "\t" + sm.getValue().toTSVString());
		}

		log.info("	*************************");

	}

}
