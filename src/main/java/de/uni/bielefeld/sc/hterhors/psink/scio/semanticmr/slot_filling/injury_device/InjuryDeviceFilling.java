package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury_device;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AnalyzeComplexity;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DistanceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DurationNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ForceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.LengthNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.PressureNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ThicknessNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.VolumeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury_device.InjuryDeviceRestrictionProvider.EInjuryDeviceModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.Stats;

public class InjuryDeviceFilling {

	/**
	 * Start the slot filling procedure.
	 * 
	 * 
	 * results: NO_MODIFICATION 0.75 0.74 0.75 results scoreRoot: Score
	 * [getF1()=0.836, getPrecision()=0.848, getRecall()=0.824, tp=28, fp=5, fn=6,
	 * tn=0]
	 * 
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new InjuryDeviceFilling();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("data/slot_filling/injury_device/instances/");

	public final String header = "Mode\tF1\tPrecision\tRecall";

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	public InjuryDeviceFilling() throws IOException {

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
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("InjuryDevice"))
				/**
				 * We apply the scope, so that we can add normalization functions for various
				 * literal entity types, if necessary.
				 */
				.apply().registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new DurationNormalization())
				.registerNormalizationFunction(new VolumeNormalization())
				.registerNormalizationFunction(new ForceNormalization())
				.registerNormalizationFunction(new ThicknessNormalization())
				.registerNormalizationFunction(new PressureNormalization())
				.registerNormalizationFunction(new LengthNormalization())
				.registerNormalizationFunction(new DistanceNormalization())
//				.registerNormalizationFunction(new TemperatureNormalization())
//				.registerNormalizationFunction(new LightIntensityNormalization())
				/**
				 * Finally, we build the systems scope.
				 */
				.build();

//		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setCorpusSizeFraction(1F)
//				.setSeed(1001L).setTrainingProportion(100).build();
		AbstractCorpusDistributor corpusDistributor = new TenFoldCrossCorpusDistributor.Builder().setCorpusSizeFraction(1F)
				.setSeed(1000L).setFold(0).setTrainingProportion(90).setDevelopmentProportion(10).build();

//		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
//				.build();

		PrintStream resultsOut = new PrintStream(new File("results/injuryDeviceResults.csv"));
//		List<String> names = Files.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath());

		resultsOut.println(header);
		Map<String, Score> scoreMap = new HashMap<>();
		Set<SlotType> slotTypesToConsider = new HashSet<>();
		slotTypesToConsider.add(SCIOSlotTypes.hasWeight);
		slotTypesToConsider.add(SCIOSlotTypes.hasForce);
		slotTypesToConsider.add(SCIOSlotTypes.hasDistance);
		slotTypesToConsider.add(SCIOSlotTypes.hasDuration);
		slotTypesToConsider.add(SCIOSlotTypes.hasVolume);

		for (EInjuryDeviceModifications rule : EInjuryDeviceModifications.values()) {
//			DeliveryMethodFilling.rule =rule;
			rule = EInjuryDeviceModifications.NO_MODIFICATION;
			/**
			 * The instance provider reads all json files in the given directory. We can set
			 * the distributor in the constructor. If not all instances should be read from
			 * the file system, we can add an additional parameter that specifies how many
			 * instances should be read. NOTE: in contrast to the corpusSizeFraction in the
			 * ShuffleCorpusDistributor, we initially set a limit to the number of files
			 * that should be read.
			 */
			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					InjuryDeviceRestrictionProvider.getByRule(rule));

//			String modelName = "InjuryDevice_PREDICT";
			String modelName = "InjuryDevice" + new Random().nextInt();

			InjuryDevicePredictor predictor = new InjuryDevicePredictor(modelName,
					instanceProvider.getTrainingInstances().stream().map(t -> t.getName())
//							.filter(n -> names.contains(n))
							.collect(Collectors.toList()),
					instanceProvider.getDevelopmentInstances().stream().map(t -> t.getName())
//							.filter(n -> names.contains(n))
							.collect(Collectors.toList()),
					instanceProvider.getTestInstances().stream().map(t -> t.getName())
//							.filter(n -> names.contains(n))
							.collect(Collectors.toList()),
					rule, ENERModus.PREDICT);

			for (Instance slotType : instanceProvider.getInstances()) {
				for (AbstractAnnotation slotType2 : slotType.getGoldAnnotations().getAbstractAnnotations()) {
					System.out.println(slotType2.getEntityType().name);
				}
			}

//			Stats.countVariables(1, instanceProvider.getInstances());
//			Stats.computePropsVar(instanceProvider.getInstances(), slotTypesToConsider);
//			Stats.countVariables(0,instanceProvider.getInstances(),slotTypesToConsider);
			Stats.countVariables(0,instanceProvider.getInstances(),slotTypesToConsider);

			System.exit(1);
			
			
			
			
			
//			Stats.computeNormedVar(instanceProvider.getInstances(), SCIOEntityTypes.injuryDevice);

//			AnalyzeComplexity.analyze(SCIOEntityTypes.injuryDevice,slotTypesToConsider, predictor.instanceProvider.getInstances(),predictor.predictionObjectiveFunction.getEvaluator());

			predictor.trainOrLoadModel();

			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

//			Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);

			AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE,
					EEvaluationDetail.LITERAL);

			Map<Instance, State> coverageStates = predictor.coverageOnDevelopmentInstances(SCIOEntityTypes.injuryDevice,
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

//			resultsOut.println(toResults(rule, score));
//
//			SlotType.excludeAll();
//			Score scoreRoot = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);
//			SlotType.includeAll();

			/**
			 * Finally, we evaluate the produced states and print some statistics.
			 */

//			log.info("results: " + toResults(rule, score));
//			log.info("results scoreRoot: " + scoreRoot);
			/**
			 * Finally, we evaluate the produced states and print some statistics.
			 */

//			final Score trainCoverage = predictor.computeCoverageOnTrainingInstances(false);
//			log.info("Coverage Training: " + trainCoverage);

//			final Score devCoverage = predictor.computeCoverageOnDevelopmentInstances(false);
//			log.info("Coverage Development: " + devCoverage);
//			Compute coverage...
//			Coverage Training: Score [getF1()=0.993, getPrecision()=1.000, getRecall()=0.986, tp=142, fp=0, fn=2, tn=0]
//			Compute coverage...
//			Coverage Development: Score [getF1()=0.697, getPrecision()=0.821, getRecall()=0.605, tp=23, fp=5, fn=15, tn=0]
//			modelName: InjuryDevice1094075021

			log.info(predictor.crf.getTrainingStatistics());
			log.info(predictor.crf.getTestStatistics());
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

	private String toResults(EInjuryDeviceModifications rule, Score score) {
		return rule.name() + "\t" + resultFormatter.format(score.getF1()) + "\t"
				+ resultFormatter.format(score.getPrecision()) + "\t" + resultFormatter.format(score.getRecall());
	}

}

/*
 * Coverage is recall is lower because through errors we can reach higher recall
 * if e.g. the properties appear for different classes
 */

//results: NO_MODIFICATION	0.68	0.64	0.74
//Compute coverage...
//No states were generated in explorer SlotFillingExplorer for instance: N007 Ishikawa 2015
//No states were generated in explorer SlotFillingExplorer for instance: N200 Sasaki 2004
//
//No states were generated in explorer SlotFillingExplorer for instance: N120 Xia, Huang et al. 2017
//No states were generated in explorer SlotFillingExplorer for instance: N238 Janzedeh
//No states were generated in explorer SlotFillingExplorer for instance: N171 L+¦pez Vales 2007
//Coverage Development: Score [getF1()=0.787, getPrecision()=0.889, getRecall()=0.706, tp=24, fp=3, fn=10, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=11541]
//CRFStatistics [context=Test, getTotalDuration()=397]
//modelName: InjuryDevice-684221447

//Nice for error analysis

//***********************************************************
//|======Final Evaluation======_____________N008 Jin 2016 26852702_____________|
//Final State  Model[1.31668] Objective[0.40000] {
//GOLD [1]:
//NYUImpactor	"NYU impactor"	120	13905
//	hasForce	Force	"10 g, 25 mm"	120	13919
//}
//PREDICT [1]:
//NYUImpactor	"NYU impactor"	120	13905
//	hasWeight	Weight	"10 g"	120	13919
//	hasDistance	Distance	"25 mm"	("2.5 cm")	120	13925
//}
//Score [getF1()=0.400, getPrecision()=0.333, getRecall()=0.500, tp=1, fp=2, fn=1, tn=0]
//Score [macroF1=0.400, macroPrecision=0.333, macroRecall=0.500]
//***********************************************************
