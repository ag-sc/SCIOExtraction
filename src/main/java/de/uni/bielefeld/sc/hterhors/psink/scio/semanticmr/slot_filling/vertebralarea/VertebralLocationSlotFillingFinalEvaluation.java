package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AnalyzeComplexity;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.VertebralAreaRestrictionProvider.EVertebralAreaModifications;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 *
 */
public class VertebralLocationSlotFillingFinalEvaluation {
	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 0)
			new VertebralLocationSlotFillingFinalEvaluation(1001L, "GOLD");
		else
			new VertebralLocationSlotFillingFinalEvaluation(1000L, args[0]);
	}

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("data/slot_filling/vertebral_location/instances/");

	ENERModus modus;

	public VertebralLocationSlotFillingFinalEvaluation(long randomSeed, final String modusName) throws IOException {
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
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("VertebralLocation"))
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

			String modelName = modusName + "_VertebralLocation_Final_" + seed;

			VertebralLocationPredictor predictor = new VertebralLocationPredictor(modelName,
					instanceProvider.getTrainingInstances().stream().map(t -> t.getName()).collect(Collectors.toList()),
					instanceProvider.getDevelopmentInstances().stream().map(t -> t.getName())
							.collect(Collectors.toList()),
					instanceProvider.getTestInstances().stream().map(t -> t.getName()).collect(Collectors.toList()),
					rule, modus);

			Set<SlotType> slotTypesToConsider = new HashSet<>();
			slotTypesToConsider.add(SCIOSlotTypes.hasUpperVertebrae);
			slotTypesToConsider.add(SCIOSlotTypes.hasLowerVertebrae);

			predictor.trainOrLoadModel();

			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

			AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE,
					EEvaluationDetail.LITERAL);

			Map<Instance, State> coverageStates = predictor
					.coverageOnDevelopmentInstances(SCIOEntityTypes.vertebralLocation, false);

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
// GOLD
//*************************
//hasLowerVertebrae-Absolute	0.500	0.500	0.500
//Cardinality-Coverage	0.987	1.000	0.975
//Cardinality-Absolute	0.986	1.000	0.973
//Root-Coverage	0.351	0.360	0.343
//hasLowerVertebrae-Relative	0.549	0.549	0.549
//Root-Absolute	0.786	0.800	0.772
//Overall-Relative	1.000	1.000	1.000
//Root-Relative	1.000	1.000	1.000
//hasUpperVertebrae-Relative	0.609	0.609	0.609
//hasUpperVertebrae-Coverage	0.971	0.971	0.971
//Overall-Coverage	0.347	0.360	0.335
//Cardinality-Relative	0.999	1.000	0.997
//hasUpperVertebrae-Absolute	0.600	0.600	0.600
//hasLowerVertebrae-Coverage	0.899	0.899	0.899
//Overall-Absolute	0.727	0.735	0.719
//	*************************

//PREDICT

//*************************
//hasLowerVertebrae-Absolute	0.257	0.257	0.257
//Cardinality-Coverage	0.987	1.000	0.975
//Cardinality-Absolute	0.986	1.000	0.973
//Root-Coverage	0.351	0.360	0.343
//hasLowerVertebrae-Relative	0.306	0.306	0.306
//Root-Absolute	0.726	0.740	0.713
//Overall-Relative	1.000	1.000	1.000
//Root-Relative	1.000	1.000	1.000
//hasUpperVertebrae-Relative	0.512	0.512	0.512
//hasUpperVertebrae-Coverage	0.899	0.899	0.899
//Overall-Coverage	0.337	0.360	0.316
//Cardinality-Relative	0.999	1.000	0.997
//hasUpperVertebrae-Absolute	0.471	0.471	0.471
//hasLowerVertebrae-Coverage	0.812	0.812	0.812
//Overall-Absolute	0.643	0.652	0.635
//	*************************
