package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method;

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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AnalyzeComplexity;
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
 * @author hterhors
 *
 * 
 */
public class DeliveryMethodSlotFilling {
	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new DeliveryMethodSlotFilling();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("data/slot_filling/delivery_method/instances/");

	public final String header = "Mode\tF1\tPrecision\tRecall";

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");
	String dataRandomSeed;

	List<Instance> trainingInstances;
	List<Instance> devInstances;
	List<Instance> testInstances;

	public DeliveryMethodSlotFilling() throws IOException {

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

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setCorpusSizeFraction(1F)
				.setSeed(1000L).setTrainingProportion(90).setDevelopmentProportion(10).build();

//		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
//				.build();

		PrintStream resultsOut = new PrintStream(new File("results/deliveryResults.csv"));
//		List<String> names = Files.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath());
		Map<String, Score> scoreMap = new HashMap<>();

		resultsOut.println(header);
		for (EDeliveryMethodModifications rule : EDeliveryMethodModifications.values()) {
//			DeliveryMethodFilling.rule =rule;
			rule = EDeliveryMethodModifications.ROOT_LOCATION_DURATION;
			SCIOSlotTypes.hasDuration.include();
			SCIOSlotTypes.hasLocations.include();
			SCIOSlotTypes.hasOrganismSpecies.include();
			SCIOSlotTypes.hasGender.include();
			SCIOSlotTypes.hasWeight.include();
			SCIOSlotTypes.hasAgeCategory.include();
			SCIOSlotTypes.hasAge.include();
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

//			dataRandomSeed = "123";// + new Random().nextInt();
			dataRandomSeed = "" + new Random().nextInt();
			// String modelName = "DeliveryMethod1104810543";
			String modelName = "DeliveryMethod" + dataRandomSeed;

			trainingInstances = instanceProvider.getTrainingInstances();
			devInstances = instanceProvider.getDevelopmentInstances();
			testInstances = instanceProvider.getTestInstances();

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
					rule, ENERModus.GOLD);

			Set<SlotType> slotTypesToConsider = new HashSet<>();
			slotTypesToConsider.add(SCIOSlotTypes.hasDuration);
			slotTypesToConsider.add(SCIOSlotTypes.hasLocations);

//			predictor.setOrganismModel(predictOrganismModel(instanceProvider.getInstances()));
			
			AnalyzeComplexity.analyze(SCIOEntityTypes.deliveryMethod,slotTypesToConsider, predictor.instanceProvider.getInstances(),
					predictor.predictionObjectiveFunction.getEvaluator());

			
			
			
			
			predictor.trainOrLoadModel();

			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

//			Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);

//			resultsOut.println(toResults(rule, score));

			AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE,
					EEvaluationDetail.LITERAL);

			Map<Instance, State> coverageStates = predictor
					.coverageOnDevelopmentInstances(SCIOEntityTypes.deliveryMethod, false);

			System.out.println("---------------------------------------");

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
			 * Finally, we evaluate the produced states and print some statistics.
			 */

//			log.info("results: " + toResults(rule, score));
//			final Score trainCoverage = predictor.computeCoverageOnTrainingInstances(false);
//			log.info("Coverage Training: " + trainCoverage);
//
//			final Score devCoverage = predictor.computeCoverageOnDevelopmentInstances(false);
//			log.info("Coverage Development: " + devCoverage);

			/**
			 * Computes the coverage of the given instances. The coverage is defined by the
			 * objective mean score that can be reached relying on greedy objective function
			 * sampling strategy. The coverage can be seen as the upper bound of the system.
			 * The upper bound depends only on the exploration strategy, e.g. the provided
			 * NER-annotations during slot-filling.
			 */

//			log.info("results: " + toResults(rule, score));
			log.info(predictor.crf.getTrainingStatistics());
			log.info(predictor.crf.getTestStatistics());
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

	private String toResults(EDeliveryMethodModifications rule, Score score) {
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
				"OrganismModel_DeliveryMethod_" + dataRandomSeed, trainingInstanceNames, developInstanceNames,
				testInstanceNames, rule, ENERModus.GOLD);
		predictor.trainOrLoadModel();

		Map<String, Set<AbstractAnnotation>> organismModelAnnotations = predictor.predictInstances(instances, 1)
				.entrySet().stream().collect(Collectors.toMap(a -> a.getKey().getName(), a -> a.getValue()));

		SlotType.restoreExcludance(x);
		return organismModelAnnotations;
	}
}
//MACRO	Root = 0.604	0.750	0.505	0.604	0.750	0.505	1.000	1.000	1.000
//MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//MACRO	hasLocations = 0.470	0.562	0.403	0.819	0.931	0.739	0.574	0.604	0.546
//MACRO	Cardinality = 0.861	1.000	0.755	0.861	1.000	0.755	1.000	1.000	1.000
//MACRO	Overall = 0.527	0.656	0.440	0.707	0.840	0.618	0.745	0.781	0.712
//CRFStatistics [context=Train, getTotalDuration()=57283]
//CRFStatistics [context=Test, getTotalDuration()=268]
//modelName: DeliveryMethod-1421717905
