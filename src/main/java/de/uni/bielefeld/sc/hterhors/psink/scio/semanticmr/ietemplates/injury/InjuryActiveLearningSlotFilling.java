package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.activelearning.IActiveLearningDocumentRanker;
import de.hterhors.semanticmr.activelearning.ranker.EActiveLearningStrategies;
import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.activelearning.ActiveLearningProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.InjuryRestrictionProvider.EInjuryModificationRules;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.specs.InjurySpecs;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.DosageNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.DurationNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.WeightNormalization;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 * 
 * 
 *         modelName: OrganismModel930148736
 *
 */
public class InjuryActiveLearningSlotFilling {

	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new InjuryActiveLearningSlotFilling();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("src/main/resources/slotfilling/injury/corpus/instances/");
	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	public InjuryActiveLearningSlotFilling() throws IOException {

		/**
		 * Initialize the system.
		 * 
		 */
		SystemScope scope = SystemScope.Builder.getScopeHandler().addScopeSpecification(InjurySpecs.systemsScopeReader)
				.apply().registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new DosageNormalization())
				.registerNormalizationFunction(new DurationNormalization()).build();

//		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
//				.build();

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
				.setSeed(1000L).setDevelopmentProportion(20).setCorpusSizeFraction(1F).build();

		EActiveLearningStrategies[] activeLearningStrategies = new EActiveLearningStrategies[] {
				EActiveLearningStrategies.DocumentRandomRanker, EActiveLearningStrategies.DocumentModelScoreRanker,
				EActiveLearningStrategies.DocumentMarginBasedRanker, EActiveLearningStrategies.DocumentEntropyRanker };

		InjurySlotFilling.rule = EInjuryModificationRules.ROOT;
		PrintStream resultOut = new PrintStream("results/activeLearning/InjuryModel_full_plusone.csv");

		for (EActiveLearningStrategies strategy : activeLearningStrategies) {
			log.info(strategy);

			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					InjuryRestrictionProvider.getByRule(InjurySlotFilling.rule));

			List<String> allTrainingInstanceNames = instanceProvider.getRedistributedTrainingInstances().stream()
					.map(t -> t.getName()).sorted().collect(Collectors.toList());

			allTrainingInstanceNames.forEach(System.out::println);
			List<String> developInstanceNames = instanceProvider.getRedistributedDevelopmentInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> testInstanceNames = instanceProvider.getRedistributedTestInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			final List<String> trainingInstancesNames = new ArrayList<>();

			int numberOfInstanceToBegin = 1;
			long numberOfInstancePerStep = 1;
			int numOfMaxSteps = 300;

			trainingInstancesNames.addAll(allTrainingInstanceNames.subList(0, numberOfInstanceToBegin));
			testInstanceNames.addAll(allTrainingInstanceNames);
			testInstanceNames.removeAll(trainingInstancesNames);

			String rand = String.valueOf(new Random().nextInt());

			List<String> newTrainingDataInstances = null;

			int i = 0;

			while (i++ != numOfMaxSteps && (newTrainingDataInstances == null || !newTrainingDataInstances.isEmpty())) {
				String modelName = "Injury" + rand + "_" + strategy.name() + "_" + i;
				log.info("model name: " + modelName);
				log.info("#Training instances: " + trainingInstancesNames.size());
				log.info("Strategy: " + strategy);

				InjurySlotFillingPredictor predictor = new InjurySlotFillingPredictor(modelName, scope,
						trainingInstancesNames, developInstanceNames, testInstanceNames);

				predictor.trainOrLoadModel();

				Score score = predictor.evaluateOnDevelopment();
				resultOut.println(toResult(score, strategy, trainingInstancesNames, InjurySlotFilling.rule));

				final IActiveLearningDocumentRanker ranker = ActiveLearningProvider.getActiveLearningInstance(strategy,
						predictor);

				List<Instance> remainingInstances = instanceProvider.getRedistributedTrainingInstances().stream()
						.filter(t -> !trainingInstancesNames.contains(t.getName())).collect(Collectors.toList());
				List<Instance> rankedInstances = ranker.rank(remainingInstances);

				newTrainingDataInstances = rankedInstances.stream().map(a -> a.getName()).limit(numberOfInstancePerStep)
						.collect(Collectors.toList());

				trainingInstancesNames.addAll(newTrainingDataInstances);
				testInstanceNames.removeAll(newTrainingDataInstances);

			}
		}
		resultOut.close();
	}

	private String toResult(Score score, EActiveLearningStrategies strategy, List<String> trainingInstancesNames,
			EInjuryModificationRules rule) {
		return strategy.name() + "\t" + rule.name() + "\t" + trainingInstancesNames.size() + "\t"
				+ resultFormatter.format(score.getF1()) + "\t" + resultFormatter.format(score.getPrecision()) + "\t"
				+ resultFormatter.format(score.getRecall());
	}
}
