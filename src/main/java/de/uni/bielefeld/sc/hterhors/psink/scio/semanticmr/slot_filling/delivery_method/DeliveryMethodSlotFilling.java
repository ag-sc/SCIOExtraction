package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
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
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DurationNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method.DeliveryMethodRestrictionProvider.EDeliveryMethodModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrgModelSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;

/**
 * GREEDY EVAL! Slot filling for delivery method.
 * 
 * ROOT Time: 8867 mean score: Score [getF1()=0.760, getPrecision()=0.760,
 * getRecall()=0.760, tp=38, fp=12, fn=12, tn=0] CRFStatistics [context=Train,
 * getTotalDuration()=95621] CRFStatistics [context=Test,
 * getTotalDuration()=8867] Compute coverage... Coverage Training: Score
 * [getF1()=1.000, getPrecision()=1.000, getRecall()=1.000, tp=143, fp=0, fn=0,
 * tn=0] Compute coverage... Coverage Development: Score [getF1()=0.920,
 * getPrecision()=0.920, getRecall()=0.920, tp=46, fp=4, fn=4, tn=0] modelName:
 * DeliveryMethod708621930
 * 
 * 
 * ROOT LOCATION Time: 13968 mean score: Score [getF1()=0.478,
 * getPrecision()=0.512, getRecall()=0.449, tp=44, fp=42, fn=54, tn=0]
 * CRFStatistics [context=Train, getTotalDuration()=325204] CRFStatistics
 * [context=Test, getTotalDuration()=13969] Compute coverage... Coverage
 * Training: Score [getF1()=0.994, getPrecision()=1.000, getRecall()=0.988,
 * tp=257, fp=0, fn=3, tn=0] Compute coverage... Coverage Development: Score
 * [getF1()=0.805, getPrecision()=0.958, getRecall()=0.694, tp=68, fp=3, fn=30,
 * tn=0] modelName: DeliveryMethod-1603701560
 * 
 * 
 * ROOT LOCATION DURATION
 * 
 * Time: 18560 mean score: Score [getF1()=0.480, getPrecision()=0.522,
 * getRecall()=0.443, tp=47, fp=43, fn=59, tn=0] CRFStatistics [context=Train,
 * getTotalDuration()=355932] CRFStatistics [context=Test,
 * getTotalDuration()=18560] Compute coverage... Coverage Training: Score
 * [getF1()=0.994, getPrecision()=1.000, getRecall()=0.989, tp=271, fp=0, fn=3,
 * tn=0] Compute coverage... Coverage Development: Score [getF1()=0.782,
 * getPrecision()=0.959, getRecall()=0.660, tp=70, fp=3, fn=36, tn=0] modelName:
 * DeliveryMethod1911796546
 * 
 * @author hterhors
 *
 *
 *         mean score: Score [getF1()=0.778, getPrecision()=0.778,
 *         getRecall()=0.778, tp=7, fp=2, fn=2, tn=0] CRFStatistics
 *         [context=Train, getTotalDuration()=173393] CRFStatistics
 *         [context=Test, getTotalDuration()=504] Compute coverage...
 * 
 * 
 * 
 *         Time: 1284 mean score: Score [getF1()=0.500, getPrecision()=0.500,
 *         getRecall()=0.500, tp=10, fp=10, fn=10, tn=0] CRFStatistics
 *         [context=Train, getTotalDuration()=584119] CRFStatistics
 *         [context=Test, getTotalDuration()=1284] Compute coverage...
 * 
 *         Time: 7664 mean score: Score [getF1()=0.554, getPrecision()=0.605,
 *         getRecall()=0.511, tp=23, fp=15, fn=22, tn=0] CRFStatistics
 *         [context=Train, getTotalDuration()=769097] CRFStatistics
 *         [context=Test, getTotalDuration()=7664] Compute coverage... Coverage
 *         Training: Score [getF1()=0.991, getPrecision()=1.000,
 *         getRecall()=0.983, tp=399, fp=0, fn=7, tn=0] Compute coverage...
 *         Coverage Development: Score [getF1()=0.816, getPrecision()=1.000,
 *         getRecall()=0.689, tp=31, fp=0, fn=14, tn=0] modelName:
 *         DeliveryMethod-668144621
 * 
 */
public class DeliveryMethodSlotFilling {
//	Compute coverage...
//	Coverage Training: Score [getF1()=0.981, getPrecision()=1.000, getRecall()=0.964, tp=371, fp=0, fn=14, tn=0]
//	Compute coverage...
//	Coverage Development: Score [getF1()=0.835, getPrecision()=1.000, getRecall()=0.716, tp=58, fp=0, fn=23, tn=0]
//	results: ROOT_LOCATION_DURATION	0.35	0.46	0.28
//	modelName: DeliveryMethod-1583580014
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
				.setSeed(1000L).setTrainingProportion(80).setDevelopmentProportion(20).build();

//		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
//				.build();

		PrintStream resultsOut = new PrintStream(new File("results/deliveryResults.csv"));
//		List<String> names = Files.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath());

		resultsOut.println(header);
		for (EDeliveryMethodModifications rule : EDeliveryMethodModifications.values()) {
//			DeliveryMethodFilling.rule =rule;
			rule = EDeliveryMethodModifications.ROOT_LOCATION_DURATION;
			SCIOSlotTypes.hasDuration.include();
			SCIOSlotTypes.hasLocations.include();

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

			trainingInstances = instanceProvider.getRedistributedTrainingInstances();
			devInstances = instanceProvider.getRedistributedDevelopmentInstances();
			testInstances = instanceProvider.getRedistributedTestInstances();

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
					rule);

//			predictor.setOrganismModel(organismModel);

			predictor.trainOrLoadModel();

			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

			Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);

			resultsOut.println(toResults(rule, score));

			Set<SlotType> slotTypesToConsider = new HashSet<>();
			slotTypesToConsider.add(SCIOSlotTypes.hasDuration);
			slotTypesToConsider.add(SCIOSlotTypes.hasLocations);

			AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE,
					EEvaluationDetail.LITERAL);

			Map<Instance, State> coverageStates = predictor.coverageOnDevelopmentInstances(false);

			System.out.println("---------------------------------------");

			PerSlotEvaluator.evalRoot(EScoreType.MICRO, finalStates, coverageStates, evaluator);

			PerSlotEvaluator.evalProperties(EScoreType.MICRO, finalStates, coverageStates, slotTypesToConsider,
					evaluator);

			PerSlotEvaluator.evalCardinality(EScoreType.MICRO, finalStates, coverageStates);

			PerSlotEvaluator.evalRoot(EScoreType.MACRO, finalStates, coverageStates, evaluator);

			PerSlotEvaluator.evalProperties(EScoreType.MACRO, finalStates, coverageStates, slotTypesToConsider,
					evaluator);

			PerSlotEvaluator.evalCardinality(EScoreType.MACRO, finalStates, coverageStates);

			PerSlotEvaluator.evalOverall(EScoreType.MACRO, finalStates, coverageStates, evaluator);

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

			log.info("results: " + toResults(rule, score));
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

	private Map<Instance, Set<AbstractAnnotation>> predictOrganismModel(List<Instance> instances) {

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
				"OrganismModel_DeliveryMethod_" + dataRandomSeed, trainingInstanceNames, developInstanceNames,
				testInstanceNames, rule);
		predictor.trainOrLoadModel();

		Map<Instance, Set<AbstractAnnotation>> organismModelAnnotations = predictor.predictInstances(instances, 1);

		SlotType.restoreExcludance(x);
		return organismModelAnnotations;
	}
//			MICRO	Root = 0.702	0.647	0.767	0.997	1.002	0.990
//			MICRO	hasDuration = 0.673	0.623	0.733	0.978	0.964	0.994
//			MICRO	hasLocations = 0.511	0.469	0.561	0.747	0.655	0.858
//			MICRO	Cardinality = 0.783	0.734	0.839	1.000	1.000	1.000
//			MACRO	Root = 0.605	0.581	0.631	0.855	0.937	0.767
//			MACRO	hasDuration = 0.594	0.565	0.626	0.852	0.911	0.786
//			MACRO	hasLocations = 0.541	0.453	0.671	0.757	0.653	0.912
//			MACRO	Cardinality = 0.818	0.734	0.924	1.000	1.000	1.000
//			MACRO	Overall = 0.537	0.449	0.669	0.759	0.647	0.926
//			results: ROOT_LOCATION_DURATION	0.55	0.51	0.61
//			CRFStatistics [context=Train, getTotalDuration()=193516]
//			CRFStatistics [context=Test, getTotalDuration()=3255]
//			modelName: DeliveryMethod1538226669
}
//results: ROOT_LOCATION_DURATION	0.56	0.52	0.61
//results scoreRoot: ROOT_LOCATION_DURATION	0.74	0.69	0.78
//Compute coverage...
//
//Coverage Training: Score [getF1()=0.839, getPrecision()=0.866, getRecall()=0.814, tp=240, fp=37, fn=55, tn=0]
//Compute coverage...
//Coverage Development: Score [getF1()=0.821, getPrecision()=0.873, getRecall()=0.775, tp=55, fp=8, fn=16, tn=0]
//results: ROOT_LOCATION_DURATION	0.56	0.52	0.61
//CRFStatistics [context=Train, getTotalDuration()=48490]
//CRFStatistics [context=Test, getTotalDuration()=1324]
//modelName: DeliveryMethod1519022817
