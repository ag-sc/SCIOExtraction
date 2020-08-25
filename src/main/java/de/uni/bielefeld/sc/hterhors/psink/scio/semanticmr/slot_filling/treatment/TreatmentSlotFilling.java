package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AnalyzeComplexity;
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

/**
 * 
 * @author hterhors
 */
public class TreatmentSlotFilling {

//	--------------PREDICT MODUS------------------------
//	MACRO	Root = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//			MACRO	hasDeliveryMethod = 0.271	0.223	0.346	0.840	0.827	0.861	0.322	0.269	0.401
//			MACRO	hasRehabMedication = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//			MACRO	hasElectricFieldStrength = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//			MACRO	hasDirection = 0.072	0.096	0.058	0.075	0.096	0.063	0.957	1.000	0.917
//			MACRO	hasApplicationInstrument = 0.270	0.272	0.268	0.480	0.446	0.514	0.562	0.609	0.522
//			MACRO	hasVoltage = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//			MACRO	hasDosage = 0.644	0.641	0.647	0.990	0.877	1.000	0.651	0.731	0.647
//			MACRO	hasCompound = 0.416	0.497	0.357	0.458	0.497	0.431	0.907	1.000	0.830
//			MACRO	Cardinality = 0.861	0.796	0.938	0.861	0.796	0.938	1.000	1.000	1.000
//			MACRO	Overall = 0.360	0.370	0.350	0.485	0.406	0.559	0.742	0.911	0.626
//			modelName: Treatment1533266230

//	--------------GOLD MODUS------------------------
//	MACRO	Root = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//	MACRO	hasDeliveryMethod = 0.495	0.535	0.461	0.713	0.668	0.751	0.695	0.800	0.614
//	MACRO	hasRehabMedication = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//	MACRO	hasElectricFieldStrength = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//	MACRO	hasDirection = 0.116	0.154	0.093	0.121	0.154	0.101	0.957	1.000	0.917
//	MACRO	hasApplicationInstrument = 0.357	0.366	0.348	0.534	0.495	0.571	0.668	0.739	0.609
//	MACRO	hasVoltage = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//	MACRO	hasDosage = 0.644	0.628	0.660	0.778	0.681	0.880	0.828	0.923	0.751
//	MACRO	hasCompound = 0.475	0.556	0.414	0.523	0.556	0.499	0.907	1.000	0.830
//	MACRO	Cardinality = 0.861	0.796	0.938	0.861	0.796	0.938	1.000	1.000	1.000
//	MACRO	Overall = 0.429	0.444	0.416	0.503	0.444	0.558	0.854	1.000	0.745
//	modelName: Treatment-721030610
	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new TreatmentSlotFilling();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory;

	public final String header = "Mode\tF1\tPrecision\tRecall";

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");
	String dataRandomSeed;

	List<Instance> trainingInstances;
	List<Instance> devInstances;
	List<Instance> testInstances;

	public TreatmentSlotFilling() throws IOException {
		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).apply()
				.registerNormalizationFunction(new DosageNormalization()).build();

		instanceDirectory = SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.treatment);
		PrintStream resultsOut = new PrintStream(new File("results/treatmentResults.csv"));

		resultsOut.println(header);

		/**
		 * 
		 * 
		 * 
		 * 
		 * 
		 * TODO: REMOVE QUICK FIX IN EntityTemplate Line 359
		 * 
		 */
		Map<String, Score> scoreMap = new HashMap<>();

		Set<SlotType> slotTypesToConsider = new HashSet<>();
		slotTypesToConsider.add(SCIOSlotTypes.hasDeliveryMethod);
		slotTypesToConsider.add(SCIOSlotTypes.hasDirection);
		slotTypesToConsider.add(SCIOSlotTypes.hasApplicationInstrument);
		slotTypesToConsider.add(SCIOSlotTypes.hasDosage);
		slotTypesToConsider.add(SCIOSlotTypes.hasCompound);
		slotTypesToConsider.add(SCIOSlotTypes.hasVoltage);
		slotTypesToConsider.add(SCIOSlotTypes.hasRehabMedication);
		slotTypesToConsider.add(SCIOSlotTypes.hasElectricFieldStrength);

		for (ETreatmentModifications rule : ETreatmentModifications.values()) {
//			this.rule = rule;
			rule = ETreatmentModifications.DOSAGE_DELIVERY_METHOD_APPLICATION_INSTRUMENT_DIRECTION;

//			AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder()
//					.setCorpusSizeFraction(1F).build();

			AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(1000L)
					.setTrainingProportion(80).setDevelopmentProportion(20).setCorpusSizeFraction(1F).build();

			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					TreatmentRestrictionProvider.getByRule(rule));

			List<String> trainingInstanceNames = instanceProvider.getTrainingInstances().stream().map(t -> t.getName())
					.collect(Collectors.toList());

			List<String> developInstanceNames = instanceProvider.getDevelopmentInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> testInstanceNames = instanceProvider.getTestInstances().stream().map(t -> t.getName())
					.collect(Collectors.toList());

//			String modelName = "Treatment-1842612192";
			dataRandomSeed = "" + new Random().nextInt();
			String modelName = "Treatment" + dataRandomSeed;

			trainingInstances = instanceProvider.getTrainingInstances();
			devInstances = instanceProvider.getDevelopmentInstances();
			testInstances = instanceProvider.getTestInstances();

			TreatmentSlotFillingPredictor predictor = new TreatmentSlotFillingPredictor(modelName,
					trainingInstanceNames, developInstanceNames, testInstanceNames, rule, ENERModus.PREDICT);
			SCIOSlotTypes.hasDirection.slotMaxCapacity = 3;
//
//			predictor.trainOrLoadModel();
//
//			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();
//
//			Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);
//
//			resultsOut.println(toResults(rule, score));
//			log.info("results: " + toResults(rule, score));
//			/**
//			 * Finally, we evaluate the produced states and print some statistics.
//			 */
//
//			final Score trainCoverage = predictor.computeCoverageOnTrainingInstances(false);
//			log.info("Coverage Training: " + trainCoverage);
//
//			final Score devCoverage = predictor.computeCoverageOnDevelopmentInstances(false);
//			log.info("Coverage Development: " + devCoverage);
//
//			log.info("results: " + toResults(rule, score));
			AnalyzeComplexity.analyze(slotTypesToConsider, predictor.instanceProvider.getInstances(),
					predictor.predictionObjectiveFunction.getEvaluator());

			predictor.setOrganismModel(predictOrganismModel(instanceProvider.getInstances()));

			Map<Instance, State> coverageStates = predictor
					.coverageOnDevelopmentInstances(SCIOEntityTypes.compoundTreatment, true);

			predictor.trainOrLoadModel();

			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

//			Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);

			AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE,
					EEvaluationDetail.LITERAL);

			log.info("---------------------------------------");

//			PerSlotEvaluator.evalRoot(EScoreType.MICRO, finalStates, coverageStates, evaluator);
//
//			PerSlotEvaluator.evalProperties(EScoreType.MICRO, finalStates, coverageStates, slotTypesToConsider,
//					evaluator);
//
//			PerSlotEvaluator.evalCardinality(EScoreType.MICRO, finalStates, coverageStates);

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
			break;
		}

		resultsOut.flush();
		resultsOut.close();

	}

	private String toResults(ETreatmentModifications rule, Score score) {
		return rule.name() + "\t" + resultFormatter.format(score.getF1()) + "\t"
				+ resultFormatter.format(score.getPrecision()) + "\t" + resultFormatter.format(score.getRecall());
	}

	private Map<String, Set<AbstractAnnotation>> predictOrganismModel(List<Instance> instances) {

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
				"OrganismModel_Treatment_" + dataRandomSeed, trainingInstanceNames, developInstanceNames,
				testInstanceNames, rule, ENERModus.PREDICT);
		predictor.trainOrLoadModel();

		Map<String, Set<AbstractAnnotation>> organismModelAnnotations = predictor.predictInstances(instances, 1)
				.entrySet().stream().collect(Collectors.toMap(a -> a.getKey().getName(), a -> a.getValue()));

		SlotType.restoreExcludance(x);
		return organismModelAnnotations;
	}
}
