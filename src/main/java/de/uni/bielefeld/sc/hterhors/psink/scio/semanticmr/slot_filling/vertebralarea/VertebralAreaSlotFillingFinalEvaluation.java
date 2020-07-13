package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
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
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.VertebralAreaRestrictionProvider.EVertebralAreaModifications;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 *
 */
public class VertebralAreaSlotFillingFinalEvaluation {
	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new VertebralAreaSlotFillingFinalEvaluation(1000L, args[0]);
	}

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("data/slot_filling/vertebral_area/instances/");

	ENERModus modus;

	public VertebralAreaSlotFillingFinalEvaluation(long randomSeed, final String modusName) throws IOException {
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
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("VertebralArea"))
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

		for (int i = 0; i < 10; i++) {
			log.info("RUN ID:" + i);

//		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
//				.build();

			EVertebralAreaModifications rule = EVertebralAreaModifications.NO_MODIFICATION;

			long seed = random.nextLong();
			AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(seed)
					.setTrainingProportion(90).setDevelopmentProportion(10).setCorpusSizeFraction(1F).build();

//		InstanceProvider.removeEmptyInstances = false;

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

			String modelName = "VertebralArea_Final_Seed_" + seed;

			VertebralAreaPredictor predictor = new VertebralAreaPredictor(modelName,
					instanceProvider.getRedistributedTrainingInstances().stream().map(t -> t.getName())
							.collect(Collectors.toList()),
					instanceProvider.getRedistributedDevelopmentInstances().stream().map(t -> t.getName())
							.collect(Collectors.toList()),
					instanceProvider.getRedistributedTestInstances().stream().map(t -> t.getName())
							.collect(Collectors.toList()),
					rule, modus);

			predictor.trainOrLoadModel();

			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

			Set<SlotType> slotTypesToConsider = new HashSet<>();
			slotTypesToConsider.add(SCIOSlotTypes.hasUpperVertebrae);
			slotTypesToConsider.add(SCIOSlotTypes.hasLowerVertebrae);

			AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE,
					EEvaluationDetail.LITERAL);

			Map<Instance, State> coverageStates = predictor
					.coverageOnDevelopmentInstances(SCIOEntityTypes.vertebralArea, false);

			System.out.println("---------------------------------------");

			PerSlotEvaluator.evalRoot(EScoreType.MACRO, finalStates, coverageStates, evaluator, scoreMap);

			PerSlotEvaluator.evalProperties(EScoreType.MACRO, finalStates, coverageStates, slotTypesToConsider,
					evaluator, scoreMap);

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
		}

		log.info("\n\n\n*************************");

		for (Entry<String, Score> sm : scoreMap.entrySet()) {
			log.info(sm.getKey() + "\t" + sm.getValue().toTSVString());
		}

		log.info("	*************************");

	}

}

//*************************
//hasLowerVertebrae-Absolute	0.470	0.470	0.470
//Cardinality-Coverage	1.000	1.000	1.000
//Cardinality-Absolute	1.000	1.000	1.000
//Root-Coverage	1.000	1.000	1.000
//hasLowerVertebrae-Relative	0.641	0.641	0.641
//Root-Absolute	1.000	1.000	1.000
//Overall-Relative	0.754	0.740	0.769
//Root-Relative	1.000	1.000	1.000
//hasUpperVertebrae-Relative	0.625	0.625	0.625
//hasUpperVertebrae-Coverage	0.939	0.939	0.939
//Overall-Coverage	0.944	1.000	0.894
//Cardinality-Relative	1.000	1.000	1.000
//hasUpperVertebrae-Absolute	0.606	0.606	0.606
//hasLowerVertebrae-Coverage	0.742	0.742	0.742
//Overall-Absolute	0.716	0.742	0.692
//	*************************

//*************************
//hasLowerVertebrae-Absolute	0.485	0.485	0.485
//Cardinality-Coverage	1.000	1.000	1.000
//Cardinality-Absolute	1.000	1.000	1.000
//Root-Coverage	1.000	1.000	1.000
//hasLowerVertebrae-Relative	0.663	0.663	0.663
//Root-Absolute	1.000	1.000	1.000
//Overall-Relative	0.796	0.760	0.836
//Root-Relative	1.000	1.000	1.000
//hasUpperVertebrae-Relative	0.800	0.800	0.800
//hasUpperVertebrae-Coverage	0.939	0.939	0.939
//Overall-Coverage	0.944	1.000	0.894
//Cardinality-Relative	1.000	1.000	1.000
//hasUpperVertebrae-Absolute	0.758	0.758	0.758
//hasLowerVertebrae-Coverage	0.742	0.742	0.742
//Overall-Absolute	0.752	0.758	0.747
//	*************************
