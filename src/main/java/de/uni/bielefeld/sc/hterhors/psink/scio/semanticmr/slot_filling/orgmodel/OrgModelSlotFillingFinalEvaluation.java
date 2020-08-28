package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel;

import java.io.File;
import java.io.IOException;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.AgeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 * 
 *         Score: SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE 0.94 0.97 0.91
 *         modelName: OrganismModel-522582779 CRFStatistics [context=Train,
 *         getTotalDuration()=16064] CRFStatistics [context=Test,
 *         getTotalDuration()=617]
 *
 * 
 */
public class OrgModelSlotFillingFinalEvaluation {

	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 0)
			new OrgModelSlotFillingFinalEvaluation(1000L, "GOLD");
		else
			new OrgModelSlotFillingFinalEvaluation(1000L, args[0]);
//		new OrgModelSlotFillingFinalEvaluation(1000L, args[0]);
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory;

	Map<Instance, State> coverageStates;

	public OrgModelSlotFillingFinalEvaluation(long randomSeed, String modusName) throws IOException {

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
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("OrganismModel"))
				/**
				 * We apply the scope, so that we can add normalization functions for various
				 * literal entity types, if necessary.
				 */
				.apply()
				/**
				 * Now normalization functions can be added. A normalization function is
				 * especially used for literal-based annotations. In case a normalization
				 * function is provided for a specific entity type, the normalized value is
				 * compared during evaluation instead of the actual surface form. A
				 * normalization function normalizes different surface forms so that e.g. the
				 * weights "500 g", "0.5kg", "500g" are all equal. Each normalization function
				 * is bound to exactly one entity type.
				 */
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization())
				/**
				 * Finally, we build the systems scope.
				 */
				.build();
		instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.organismModel);

		Set<SlotType> slotTypesToConsider = new HashSet<>();
		slotTypesToConsider.add(SCIOSlotTypes.hasAge);
		slotTypesToConsider.add(SCIOSlotTypes.hasAgeCategory);
		slotTypesToConsider.add(SCIOSlotTypes.hasWeight);
		slotTypesToConsider.add(SCIOSlotTypes.hasOrganismSpecies);
		slotTypesToConsider.add(SCIOSlotTypes.hasGender);

		Map<String, Score> scoreMap = new HashMap<>();
		Random random = new Random(randomSeed);
		ENERModus modus = ENERModus.valueOf(modusName);

		for (int i = 0; i < 10; i++) {
			log.info(modus + " RUN ID:" + i);
			EOrgModelModifications rule = EOrgModelModifications.SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE;

			OrganismModelRestrictionProvider.applySlotTypeRestrictions(rule);

			long seed = random.nextLong();
			AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(seed)
					.setTrainingProportion(90).setDevelopmentProportion(10).setCorpusSizeFraction(1F).build();

			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					OrganismModelRestrictionProvider.getByRule(rule));

			List<String> trainingInstanceNames = instanceProvider.getTrainingInstances().stream().map(t -> t.getName())
					.collect(Collectors.toList());

			List<String> developInstanceNames = instanceProvider.getDevelopmentInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> testInstanceNames = instanceProvider.getTestInstances().stream().map(t -> t.getName())
					.collect(Collectors.toList());

			String modelName = modusName + "_OrganismModel_Final_" + seed;

			OrgModelSlotFillingPredictor predictor = new OrgModelSlotFillingPredictor(modelName, trainingInstanceNames,
					developInstanceNames, testInstanceNames, rule, modus);

			predictor.trainOrLoadModel();

			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

			AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE,
					EEvaluationDetail.LITERAL);

			coverageStates = predictor.coverageOnDevelopmentInstances(SCIOEntityTypes.organismModel, false);

			System.out.println("---------------------------------------");

//			PerSlotEvaluator.evalRoot(EScoreType.MICRO, finalStates, coverageStates, evaluator,scores);
//
//			PerSlotEvaluator.evalProperties(EScoreType.MICRO, finalStates, coverageStates, slotTypesToConsider,
//					evaluator,scores);
//
//			PerSlotEvaluator.evalCardinality(EScoreType.MICRO, finalStates, coverageStates,scores);

			PerSlotEvaluator.evalRoot(EScoreType.MACRO, finalStates, coverageStates, evaluator, scoreMap);

			PerSlotEvaluator.evalProperties(EScoreType.MACRO, finalStates, coverageStates, slotTypesToConsider,
					evaluator, scoreMap);

			PerSlotEvaluator.evalCardinality(EScoreType.MACRO, finalStates, coverageStates, scoreMap);

			PerSlotEvaluator.evalOverall(EScoreType.MACRO, finalStates, coverageStates, evaluator, scoreMap);

			/**
			 * Finally, we evaluate the produced states and print some statistics.
			 */

			/**
			 * Computes the coverage of the given instances. The coverage is defined by the
			 * objective mean score that can be reached relying on greedy objective function
			 * sampling strategy. The coverage can be seen as the upper bound of the system.
			 * The upper bound depends only on the exploration strategy, e.g. the provided
			 * NER-annotations during slot-filling.
			 */
			log.info("modelName: " + predictor.modelName);

			log.info(predictor.crf.getTrainingStatistics());
			log.info(predictor.crf.getTestStatistics());
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
//Cardinality-Coverage	0.990	1.000	0.980
//hasAge-Relative	0.726	0.731	0.721
//hasGender-Relative	0.977	0.977	0.977
//Cardinality-Absolute	0.990	1.000	0.980
//hasWeight-Coverage	0.948	0.955	0.942
//Root-Coverage	0.000	0.000	0.000
//hasAgeCategory-Coverage	0.995	1.000	0.990
//hasGender-Absolute	0.962	0.972	0.952
//hasOrganismSpecies-Coverage	0.892	0.900	0.885
//Root-Absolute	0.000	0.000	0.000
//hasOrganismSpecies-Absolute	0.864	0.870	0.858
//hasAgeCategory-Absolute	0.890	0.895	0.886
//Overall-Relative	0.951	0.949	0.952
//hasWeight-Absolute	0.885	0.891	0.878
//hasWeight-Relative	0.931	0.931	0.930
//Root-Relative	�	�	�
//hasAgeCategory-Relative	0.894	0.895	0.894
//hasGender-Coverage	0.984	0.994	0.974
//Overall-Coverage	0.962	0.995	0.930
//Cardinality-Relative	1.000	1.000	1.000
//hasAge-Coverage	0.917	0.957	0.880
//hasAge-Absolute	0.662	0.692	0.635
//hasOrganismSpecies-Relative	0.965	0.964	0.967
//Overall-Absolute	0.914	0.944	0.886
//
