package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury_device;

import java.io.File;
import java.io.IOException;
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

public class InjuryDeviceFillingFinalEvaluation {

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
		new InjuryDeviceFillingFinalEvaluation(1000L, args[0]);
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("data/slot_filling/injury_device/instances/");

	ENERModus modus;

	public InjuryDeviceFillingFinalEvaluation(long randomSeed, String modusName) throws IOException {

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

		Map<String, Score> scoreMap = new HashMap<>();
		modus = ENERModus.valueOf(modusName);
		Random random = new Random(randomSeed);

		for (int i = 0; i < 10; i++) {
			log.info("RUN ID:" + i);

			long seed = random.nextLong();
			AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(seed)
					.setTrainingProportion(90).setDevelopmentProportion(10).setCorpusSizeFraction(1F).build();

			EInjuryDeviceModifications rule = EInjuryDeviceModifications.NO_MODIFICATION;
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

			String modelName = modusName + "_InjuryDevice_Final_" + seed;

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
					rule, modus);

			predictor.trainOrLoadModel();

			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

			Set<SlotType> slotTypesToConsider = new HashSet<>();
			slotTypesToConsider.add(SCIOSlotTypes.hasWeight);
			slotTypesToConsider.add(SCIOSlotTypes.hasForce);
			slotTypesToConsider.add(SCIOSlotTypes.hasDistance);
			slotTypesToConsider.add(SCIOSlotTypes.hasDuration);
			slotTypesToConsider.add(SCIOSlotTypes.hasVolume);

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
		}

		log.info("\n\n\n*************************");

		for (Entry<String, Score> sm : scoreMap.entrySet()) {
			log.info(sm.getKey() + "\t" + sm.getValue().toTSVString());
		}

		log.info("	*************************");

	}

}

//*************************
//hasForce-Relative	�	�	�
//Cardinality-Coverage	0.991	1.000	0.982
//hasVolume-Coverage	0.500	0.500	0.500
//hasDuration-Absolute	0.143	0.143	0.143
//hasDuration-Relative	�	�	�
//hasForce-Absolute	0.714	0.714	0.714
//Cardinality-Absolute	0.991	1.000	0.982
//hasWeight-Coverage	0.600	0.600	0.600
//Root-Coverage	0.990	1.000	0.979
//hasVolume-Absolute	0.250	0.250	0.250
//hasVolume-Relative	�	�	�
//hasDistance-Coverage	0.735	0.735	0.735
//Root-Absolute	0.734	0.738	0.731
//Overall-Relative	0.801	0.746	0.863
//hasWeight-Absolute	0.446	0.446	0.446
//hasWeight-Relative	0.754	0.754	0.754
//Root-Relative	0.741	0.737	0.745
//hasDistance-Absolute	0.436	0.436	0.436
//hasForce-Coverage	0.778	0.778	0.778
//Overall-Coverage	0.908	0.936	0.882
//hasDuration-Coverage	0.000	0.000	0.000
//Cardinality-Relative	1.000	1.000	1.000
//hasDistance-Relative	0.695	0.695	0.695
//Overall-Absolute	0.729	0.700	0.761
//	*************************
