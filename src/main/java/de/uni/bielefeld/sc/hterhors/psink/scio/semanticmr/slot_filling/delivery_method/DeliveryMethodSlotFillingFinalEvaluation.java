package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method;

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
import de.hterhors.semanticmr.corpus.distributor.TenFoldCrossCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DurationNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method.DeliveryMethodRestrictionProvider.EDeliveryMethodModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrgModelSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;

/**
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * TODO: CHANGE INITALIZER TO GENERIC !!! FOR FINAL EVALUATION
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */

/**
 * MIX
 */
//MACRO	Root = 0.747	0.882	0.647	0.806	0.938	0.710	0.926	0.941	0.912
//MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//MACRO	hasLocations = 0.413	0.529	0.338	0.578	0.711	0.493	0.714	0.745	0.686
//MACRO	Cardinality = 0.847	1.000	0.735	0.915	1.062	0.806	0.926	0.941	0.912
//MACRO	Overall = 0.580	0.706	0.492	0.712	0.831	0.630	0.813	0.849	0.781
//CRFStatistics [context=Train, getTotalDuration()=63953]
//CRFStatistics [context=Test, getTotalDuration()=222]
//modelName: PREDICT_DeliveryMethod_Final_-470456323649602622
//
//
//

/**
 * Greedy model
 */
//MACRO	Root = 0.369	0.471	0.304	0.399	0.500	0.333	0.926	0.941	0.912
//MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//MACRO	hasLocations = 0.039	0.059	0.029	0.055	0.079	0.043	0.714	0.745	0.686
//MACRO	Cardinality = 0.431	0.529	0.363	0.465	0.562	0.398	0.926	0.941	0.912
//MACRO	Overall = 0.229	0.382	0.164	0.282	0.450	0.210	0.813	0.849	0.781
//CRFStatistics [context=Train, getTotalDuration()=60357]
//CRFStatistics [context=Test, getTotalDuration()=126]
//modelName: PREDICT_DeliveryMethod_Final_-470456323649602622
//

public class DeliveryMethodSlotFillingFinalEvaluation {
	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 0)
			new DeliveryMethodSlotFillingFinalEvaluation(1000L, "PREDICT");
		else
			new DeliveryMethodSlotFillingFinalEvaluation(1000L, args[0]);
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("data/slot_filling/delivery_method/instances/");

	List<Instance> trainingInstances;
	List<Instance> devInstances;
	List<Instance> testInstances;
//	long seed;
	ENERModus modus;

	public DeliveryMethodSlotFillingFinalEvaluation(long randomSeed, String modusName) throws IOException {

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
				.apply().registerNormalizationFunction(new DurationNormalization())
				/**
				 * Finally, we build the systems scope.
				 */
				.build();

		SlotType.excludeAll();

		Map<String, Score> scoreMap = new HashMap<>();
		EDeliveryMethodModifications rule = EDeliveryMethodModifications.ROOT_LOCATION_DURATION;

		SCIOSlotTypes.hasDuration.include();
		SCIOSlotTypes.hasLocations.include();
		modus = ENERModus.valueOf(modusName);
//		Random random = new Random(randomSeed);

		for (int i = 0; i < 10; i++) {
			log.info("RUN ID:" + i);

//			seed = random.nextLong();
			AbstractCorpusDistributor corpusDistributor = new TenFoldCrossCorpusDistributor.Builder()
					.setSeed(randomSeed).setFold(i).setTrainingProportion(90).setDevelopmentProportion(10)
					.setCorpusSizeFraction(1F).build();

			/**
			 * The instance provider reads all json files in the given directory. We can set
			 * the distributor in the constructor. If not all instances should be read from
			 * the file system, we can add an additional parameter that specifies how many
			 * instances should be read. NOTE: in contrast to the corpusSizeFraction in the
			 * ShuffleCorpusDistributor, we initially set a limit to the number of files
			 * that should be read.
			 */
			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					DeliveryMethodRestrictionProvider.getByRule(rule));

			String modelName = modusName + "_DeliveryMethod_DissFinal_" + randomSeed + "_fold_" + i;

			trainingInstances = instanceProvider.getTrainingInstances();
			devInstances = instanceProvider.getDevelopmentInstances();
			testInstances = instanceProvider.getTestInstances();

//			Map<Instance, Set<AbstractAnnotation>> organismModel = predictOrganismModel(
//					instanceProvider.getInstances());

			DeliveryMethodPredictor predictor = new DeliveryMethodPredictor(modelName,
					trainingInstances.stream().map(t -> t.getName())
//							.filter(n -> names.contains(n))
							.collect(Collectors.toList()),
					devInstances.stream().map(t -> t.getName())
//							.filter(n -> names.contains(n))
							.collect(Collectors.toList()),
					testInstances.stream().map(t -> t.getName())
//							.filter(n -> names.contains(n))
							.collect(Collectors.toList()),
					rule, modus);

//			predictor.setOrganismModel(organismModel);

			predictor.trainOrLoadModel();

			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

			Set<SlotType> slotTypesToConsider = new HashSet<>();
			slotTypesToConsider.add(SCIOSlotTypes.hasDuration);
			slotTypesToConsider.add(SCIOSlotTypes.hasLocations);

			AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE,
					EEvaluationDetail.LITERAL);

			Map<Instance, State> coverageStates = predictor
					.coverageOnDevelopmentInstances(SCIOEntityTypes.deliveryMethod, false);

			System.out.println("---------------------------------------");

			PerSlotEvaluator.evalRoot(EScoreType.MACRO, finalStates, coverageStates, evaluator, scoreMap);

			PerSlotEvaluator.evalProperties(EScoreType.MACRO, finalStates, coverageStates, slotTypesToConsider,
					evaluator, scoreMap);

			PerSlotEvaluator.evalCardinality(EScoreType.MACRO, finalStates, coverageStates, scoreMap);

			PerSlotEvaluator.evalOverall(EScoreType.MACRO, finalStates, coverageStates, evaluator, scoreMap);

			log.info(predictor.crf.getTrainingStatistics());
			log.info(predictor.crf.getTestStatistics());
			log.info("modelName: " + predictor.modelName);
		}

		log.info("\n\n\n*************************");

		for (Entry<String, Score> sm : scoreMap.entrySet()) {
			log.info(sm.getKey() + "\t" + sm.getValue().toTSVString());
		}

		log.info("	*************************");

	}

	private Map<Instance, Set<AbstractAnnotation>> predictOrganismModel(List<Instance> instances, long randomSeed,
			int i) {

		/**
		 * TODO: FULL MODEL EXTRACTION CAUSE THIS IS BEST TO PREDICT SPECIES
		 */
		EOrgModelModifications rule = EOrgModelModifications.SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE;

		/**
		 * Predict OrganismModels
		 */
		Map<SlotType, Boolean> x = SlotType.storeExcludance();
		OrganismModelRestrictionProvider.applySlotTypeRestrictions(rule);

		List<String> trainingInstanceNames = trainingInstances.stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> developInstanceNames = devInstances.stream().map(t -> t.getName()).collect(Collectors.toList());

		List<String> testInstanceNames = testInstances.stream().map(t -> t.getName()).collect(Collectors.toList());
//	+ modelName
		OrgModelSlotFillingPredictor predictor = new OrgModelSlotFillingPredictor(
				"OrganismModel_DeliveryMethod_" + randomSeed + "_fold_" + i, trainingInstanceNames,
				developInstanceNames, testInstanceNames, rule, modus);
		predictor.trainOrLoadModel();

		Map<Instance, Set<AbstractAnnotation>> organismModelAnnotations = predictor.predictInstances(instances, 1);

		SlotType.restoreExcludance(x);
		return organismModelAnnotations;
	}
}
