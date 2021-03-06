package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment;

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
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DosageNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrgModelSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.TreatmentRestrictionProvider.ETreatmentModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.Stats;

/**
 * 
 * @author hterhors
 */
public class TreatmentSlotFillingFinalEvaluation {

	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 0)
			new TreatmentSlotFillingFinalEvaluation(1000L, "PREDICT");
		else
			new TreatmentSlotFillingFinalEvaluation(1000L, args[0]);
//		new TreatmentSlotFillingFinalEvaluation(1000L, "GOLD");
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory;

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");
	String dataRandomSeed;

	List<Instance> trainingInstances;
	List<Instance> devInstances;
	List<Instance> testInstances;
	Map<Instance, State> coverageStates;

	private ENERModus modus;

	public TreatmentSlotFillingFinalEvaluation(long randomSeed, String modusName) throws IOException {
		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).apply()
				.registerNormalizationFunction(new DosageNormalization()).build();

		instanceDirectory = SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.treatment);

		/**
		 * QUICK FIX in File : remove 9th annotation
		 * 
		 * 
		 * 
		 * 
		 * TODO: REMOVE QUICK FIX IN EntityTemplate Line 359
		 * 
		 */
		Map<String, Score> scoreMap = new HashMap<>();

		modus = ENERModus.valueOf(modusName);
		for (int i = 0; i < 10; i++) {
			log.info("RUN ID:" + i);
			ETreatmentModifications rule = ETreatmentModifications.DOSAGE_DELIVERY_METHOD_APPLICATION_INSTRUMENT_DIRECTION;

			SCIOSlotTypes.hasDirection.slotMaxCapacity = Integer.MAX_VALUE;
			AbstractCorpusDistributor corpusDistributor = new TenFoldCrossCorpusDistributor.Builder()
					.setSeed(randomSeed).setFold(i).setTrainingProportion(90).setDevelopmentProportion(10)
					.setCorpusSizeFraction(1F).build();

			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					TreatmentRestrictionProvider.getByRule(rule));
			
//			Stats.countVariables(0, instanceProvider.getInstances());
//			System.exit(1);
			
			List<String> trainingInstanceNames = instanceProvider.getTrainingInstances().stream().map(t -> t.getName())
					.collect(Collectors.toList());

			List<String> developInstanceNames = instanceProvider.getDevelopmentInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> testInstanceNames = instanceProvider.getTestInstances().stream().map(t -> t.getName())
					.collect(Collectors.toList());
//			dataRandomSeed = "" + seed;
			String modelName = modusName + "_Treatment_DissFinal_" + randomSeed+"_fold_"+i;

			trainingInstances = instanceProvider.getTrainingInstances();
			devInstances = instanceProvider.getDevelopmentInstances();
			testInstances = instanceProvider.getTestInstances();

//			for (Instance string : instanceProvider.getInstances()) {
//				System.out.println(string.getGoldAnnotations().getAbstractAnnotations().size());
//			}

			TreatmentSlotFillingPredictor predictor = new TreatmentSlotFillingPredictor(modelName,
					trainingInstanceNames, developInstanceNames, testInstanceNames, rule, modus);

			SCIOSlotTypes.hasDirection.slotMaxCapacity = 3;

			predictor.setOrganismModel(predictOrganismModel(instanceProvider.getInstances(),modelName));

			coverageStates = predictor.coverageOnDevelopmentInstances(SCIOEntityTypes.compoundTreatment, true);

			predictor.trainOrLoadModel();

			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

//			Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);

			Set<SlotType> slotTypesToConsider = new HashSet<>();
			slotTypesToConsider.add(SCIOSlotTypes.hasDeliveryMethod);
			slotTypesToConsider.add(SCIOSlotTypes.hasDirection);
			slotTypesToConsider.add(SCIOSlotTypes.hasApplicationInstrument);
			slotTypesToConsider.add(SCIOSlotTypes.hasDosage);
			slotTypesToConsider.add(SCIOSlotTypes.hasCompound);
			slotTypesToConsider.add(SCIOSlotTypes.hasVoltage);
			slotTypesToConsider.add(SCIOSlotTypes.hasRehabMedication);
			slotTypesToConsider.add(SCIOSlotTypes.hasElectricFieldStrength);

			AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE,
					EEvaluationDetail.LITERAL);

			log.info("---------------------------------------");

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

	private Map<String, Set<AbstractAnnotation>> predictOrganismModel(List<Instance> instances, String modelName) {

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
				"OrganismModel_" + modelName, trainingInstanceNames, developInstanceNames,
				testInstanceNames, rule, modus);
		predictor.trainOrLoadModel();

		Map<String, Set<AbstractAnnotation>> organismModelAnnotations = predictor.predictInstances(instances, 1)
				.entrySet().stream().collect(Collectors.toMap(a -> a.getKey().getName(), a -> a.getValue()));

		SlotType.restoreExcludance(x);
		return organismModelAnnotations;
	}
}
