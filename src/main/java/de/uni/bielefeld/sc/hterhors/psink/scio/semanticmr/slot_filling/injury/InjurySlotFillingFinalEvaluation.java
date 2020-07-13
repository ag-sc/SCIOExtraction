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
import java.util.Map.Entry;
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
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.hterhors.semanticmr.projects.examples.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor.ENERModus;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.InjuryRestrictionProvider.EInjuryModifications;

/**
 * Slot filling for injuries.
 */
public class InjurySlotFillingFinalEvaluation {
	public static void main(String[] args) throws IOException {
		new InjurySlotFillingFinalEvaluation(1000L, args[0]);
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private File instanceDirectory;

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");
	ENERModus modus;

	public InjurySlotFillingFinalEvaluation(long randomSeed, String modusName) throws IOException {

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

//		List<String> names = Files.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath());

		Map<String, Score> scoreMap = new HashMap<>();

		EInjuryModifications rule = EInjuryModifications.ROOT_DEVICE_LOCATION_ANAESTHESIA;

		Random random = new Random(randomSeed);
		modus = ENERModus.valueOf(modusName);
		for (int i = 0; i < 10; i++) {
			log.info("RUN ID:" + i);

			long seed = random.nextLong();
			AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(seed)
					.setTrainingProportion(90).setDevelopmentProportion(10).setCorpusSizeFraction(1F).build();

//			AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder()
//					.setCorpusSizeFraction(1F).build();

			InjuryRestrictionProvider.applySlotTypeRestrictions(rule);

			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					InjuryRestrictionProvider.getByRule(rule));

			List<String> trainingInstanceNames = instanceProvider.getRedistributedTrainingInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> developInstanceNames = instanceProvider.getRedistributedDevelopmentInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> testInstanceNames = instanceProvider.getRedistributedTestInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			String modelName = "Injury_Final_Seed_" + seed;

			InjurySlotFillingPredictor predictor = new InjurySlotFillingPredictor(modelName, trainingInstanceNames,
					developInstanceNames, testInstanceNames, rule, modus);

			predictor.trainOrLoadModel();

			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

//			Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);

			Set<SlotType> slotTypesToConsider = new HashSet<>();

			slotTypesToConsider.add(SCIOSlotTypes.hasInjuryDevice);
			slotTypesToConsider.add(SCIOSlotTypes.hasInjuryLocation);
			slotTypesToConsider.add(SCIOSlotTypes.hasAnaesthesia);

			AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE,
					EEvaluationDetail.LITERAL);

			Map<Instance, State> coverageStates = predictor.coverageOnDevelopmentInstances(SCIOEntityTypes.injury,
					false);

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
		}

		log.info("\n\n\n*************************");

		for (Entry<String, Score> sm : scoreMap.entrySet()) {
			log.info(sm.getKey() + "\t" + sm.getValue().toTSVString());
		}

		log.info("	*************************");

	}

}
