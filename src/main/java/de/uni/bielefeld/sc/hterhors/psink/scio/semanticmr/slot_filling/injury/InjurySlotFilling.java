package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury;

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
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.examples.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AnalyzeComplexity;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DistanceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DosageNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DurationNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ForceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.LengthNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.PressureNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ThicknessNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.VolumeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.InjuryRestrictionProvider.EInjuryModifications;

/**
 * Slot filling for injuries.
 * 
 * 
 * Mean Score: Score [getF1()=0.416, getPrecision()=0.521, getRecall()=0.347,
 * tp=76, fp=70, fn=143, tn=0] CRFStatistics [context=Train,
 * getTotalDuration()=200631] CRFStatistics [context=Test,
 * getTotalDuration()=6597] Compute coverage... Coverage Training: Score
 * [getF1()=0.950, getPrecision()=0.985, getRecall()=0.917, tp=719, fp=11,
 * fn=65, tn=0] Compute coverage... No states were generated for instance: N156
 * Kalincik 2010 2 Coverage Development: Score [getF1()=0.814,
 * getPrecision()=0.905, getRecall()=0.740, tp=162, fp=17, fn=57, tn=0]
 * Injury-520642072
 * 
 * 
 * @author hterhors
 *
 *         Use gold for training standard: Score [getF1()=0.307,
 *         getPrecision()=0.602, getRecall()=0.206, tp=62, fp=41, fn=239, tn=0]
 *         only root: Score [getF1()=0.548, getPrecision()=0.575,
 *         getRecall()=0.523, tp=23, fp=17, fn=21, tn=0] CRFStatistics
 *         [context=Train, getTotalDuration()=99561] CRFStatistics
 *         [context=Test, getTotalDuration()=238] modelName: Injury886
 *
 * 
 *         Use predicted for training
 *
 *         standard: Score [getF1()=0.302, getPrecision()=0.592,
 *         getRecall()=0.203, tp=61, fp=42, fn=240, tn=0] only root: Score
 *         [getF1()=0.548, getPrecision()=0.575, getRecall()=0.523, tp=23,
 *         fp=17, fn=21, tn=0] CRFStatistics [context=Train,
 *         getTotalDuration()=113709] CRFStatistics [context=Test,
 *         getTotalDuration()=341] modelName: Injury3652
 * 
 *         Use gold for predicting and training standard: Score [getF1()=0.311,
 *         getPrecision()=0.606, getRecall()=0.209, tp=63, fp=41, fn=238, tn=0]
 *         only root: Score [getF1()=0.548, getPrecision()=0.575,
 *         getRecall()=0.523, tp=23, fp=17, fn=21, tn=0] CRFStatistics
 *         [context=Train, getTotalDuration()=72071] CRFStatistics
 *         [context=Test, getTotalDuration()=288] modelName: Injury8174
 *
 */
public class InjurySlotFilling {

	/**
	 * Start the slot filling procedure. standard: Score [getF1()=0.432,
	 * getPrecision()=0.608, getRecall()=0.335, tp=62, fp=40, fn=123, tn=0] only
	 * root: Score [getF1()=0.571, getPrecision()=0.600, getRecall()=0.545, tp=24,
	 * fp=16, fn=20, tn=0] CRFStatistics [context=Train, getTotalDuration()=95537]
	 * CRFStatistics [context=Test, getTotalDuration()=168] modelName: Injury8892
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new InjurySlotFilling();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private File instanceDirectory;

	public final String header = "Mode\tF1\tPrecision\tRecall";

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	public InjurySlotFilling() throws IOException {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Injury")).apply()
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new DosageNormalization())
				.registerNormalizationFunction(new DurationNormalization())
				.registerNormalizationFunction(new VolumeNormalization())
				.registerNormalizationFunction(new ForceNormalization())
				.registerNormalizationFunction(new ThicknessNormalization())
				.registerNormalizationFunction(new PressureNormalization())
				.registerNormalizationFunction(new LengthNormalization())
				.registerNormalizationFunction(new DistanceNormalization())

				.build();

		instanceDirectory = SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.injury);

		PrintStream resultsOut = new PrintStream(new File("results/injuryResults.csv"));

		resultsOut.println(header);
//		List<String> names = Files.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath());

		Map<String, Score> scoreMap = new HashMap<>();

		Set<SlotType> slotTypesToConsider = new HashSet<>();
		slotTypesToConsider.add(SCIOSlotTypes.hasInjuryDevice);
		slotTypesToConsider.add(SCIOSlotTypes.hasInjuryLocation);
		slotTypesToConsider.add(SCIOSlotTypes.hasAnaesthesia);

		for (EInjuryModifications rule : EInjuryModifications.values()) {
			rule = EInjuryModifications.ROOT_DEVICE_LOCATION_ANAESTHESIA;
//			rule = rule;

			AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(1000L)
					.setTrainingProportion(80).setDevelopmentProportion(20).setCorpusSizeFraction(1F).build();

//			AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder()
//					.setCorpusSizeFraction(1F).build();

			InjuryRestrictionProvider.applySlotTypeRestrictions(rule);

			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					InjuryRestrictionProvider.getByRule(rule));

			List<String> trainingInstanceNames = instanceProvider.getTrainingInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> developInstanceNames = instanceProvider.getDevelopmentInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> testInstanceNames = instanceProvider.getTestInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			String modelName = "Injury" + new Random().nextInt();

			InjurySlotFillingPredictor predictor = new InjurySlotFillingPredictor(modelName, trainingInstanceNames,
					developInstanceNames, testInstanceNames, rule, ENERModus.GOLD);

			AnalyzeComplexity.analyze(SCIOEntityTypes.injury,slotTypesToConsider, predictor.instanceProvider.getInstances(),predictor.predictionObjectiveFunction.getEvaluator());

			predictor.trainOrLoadModel();

			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

//			Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);


			AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE,
					EEvaluationDetail.LITERAL);

			Map<Instance, State> coverageStates = predictor.coverageOnDevelopmentInstances(SCIOEntityTypes.injury,
					false);

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

//			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();
//
//			Score standard = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);
//
//			Map<SlotType, Boolean> x = SlotType.storeExcludance();
//			SlotType.excludeAll();
//			Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);
//			SlotType.restoreExcludance(x);
//
//			log.info("standard: " + standard);
//			log.info("only root: " + score);
//
//			log.info(predictor.crf.getTrainingStatistics());
//			log.info(predictor.crf.getTestStatistics());
//
//			resultsOut.println(toResults(rule, standard, "standard"));
//			resultsOut.println(toResults(rule, score, "onyl root"));
//
//			final Score trainCoverage = predictor.computeCoverageOnTrainingInstances(false);
//			log.info("Coverage Training: " + trainCoverage);
//			resultsOut.println(toResults(rule, trainCoverage, "coverage on train"));
//			final Score devCoverage = predictor.computeCoverageOnDevelopmentInstances(false);
//			log.info("Coverage Development: " + devCoverage);
//			resultsOut.println(toResults(rule, devCoverage, "coverage on dev"));
////			Gold Anaest + Location
////			Coverage Training: Score [getF1()=0.943, getPrecision()=1.000, getRecall()=0.892, tp=1128, fp=0, fn=137, tn=0]
////			Coverage Development: Score [getF1()=0.924, getPrecision()=0.981, getRecall()=0.874, tp=263, fp=5, fn=38, tn=0]
//
//
////			Predicted Anaest + Location
////			Compute coverage...
////			Coverage Training: Score [getF1()=0.767, getPrecision()=0.959, getRecall()=0.639, tp=819, fp=35, fn=462, tn=0]
////					Compute coverage...
////					Coverage Development: Score [getF1()=0.648, getPrecision()=0.895, getRecall()=0.508, tp=153, fp=18, fn=148, tn=0]
////					modelName: Injury7788
//
//			log.info(predictor.crf.getTrainingStatistics());
//			log.info(predictor.crf.getTestStatistics());
			/**
			 * Computes the coverage of the given instances. The coverage is defined by the
			 * objective mean score that can be reached relying on greedy objective function
			 * sampling strategy. The coverage can be seen as the upper bound of the system.
			 * The upper bound depends only on the exploration strategy, e.g. the provided
			 * NER-annotations during slot-filling.
			 */
			log.info("modelName: " + predictor.modelName);
			break;
		}

		resultsOut.flush();
		resultsOut.close();
	}

	private String toResults(EInjuryModifications rule, Score score, String context) {
		return rule.name() + "\t" + resultFormatter.format(score.getF1()) + "\t"
				+ resultFormatter.format(score.getPrecision()) + "\t" + resultFormatter.format(score.getRecall()) + "\t"
				+ context;
	}
}

//train & predict = false

//standard: Score [getF1()=0.444, getPrecision()=0.623, getRecall()=0.346, tp=104, fp=63, fn=197, tn=0]
//only root: Score [getF1()=0.476, getPrecision()=0.500, getRecall()=0.455, tp=20, fp=20, fn=24, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=108748]
//CRFStatistics [context=Test, getTotalDuration()=423]
//Compute coverage...
//Coverage Training: Score [getF1()=0.763, getPrecision()=0.990, getRecall()=0.621, tp=786, fp=8, fn=479, tn=0]
//Compute coverage...
//
//Coverage Development: Score [getF1()=0.747, getPrecision()=0.944, getRecall()=0.618, tp=186, fp=11, fn=115, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=108748]
//CRFStatistics [context=Test, getTotalDuration()=423]
//modelName: Injury5350

//train = true &  predict = false

//standard: Score [getF1()=0.449, getPrecision()=0.629, getRecall()=0.349, tp=105, fp=62, fn=196, tn=0]
//only root: Score [getF1()=0.476, getPrecision()=0.500, getRecall()=0.455, tp=20, fp=20, fn=24, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=125053]
//CRFStatistics [context=Test, getTotalDuration()=370]
//Compute coverage...
//Coverage Training: Score [getF1()=0.943, getPrecision()=1.000, getRecall()=0.892, tp=1128, fp=0, fn=137, tn=0]
//Compute coverage...
//
//Coverage Development: Score [getF1()=0.747, getPrecision()=0.944, getRecall()=0.618, tp=186, fp=11, fn=115, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=125053]
//CRFStatistics [context=Test, getTotalDuration()=370]
//modelName: Injury8537

//train & predict = true

//standard: Score [getF1()=0.520, getPrecision()=0.715, getRecall()=0.409, tp=123, fp=49, fn=178, tn=0]
//only root: Score [getF1()=0.500, getPrecision()=0.525, getRecall()=0.477, tp=21, fp=19, fn=23, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=95241]
//CRFStatistics [context=Test, getTotalDuration()=413]
//Compute coverage...
//Coverage Training: Score [getF1()=0.943, getPrecision()=1.000, getRecall()=0.892, tp=1128, fp=0, fn=137, tn=0]
//Compute coverage...
//
//Coverage Development: Score [getF1()=0.924, getPrecision()=0.981, getRecall()=0.874, tp=263, fp=5, fn=38, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=95241]
//CRFStatistics [context=Test, getTotalDuration()=413]
//modelName: Injury3062
