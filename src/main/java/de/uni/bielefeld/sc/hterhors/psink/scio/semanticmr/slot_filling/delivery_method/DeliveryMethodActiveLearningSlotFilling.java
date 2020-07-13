package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.activelearning.ActiveLearningProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DosageNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DurationNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method.DeliveryMethodRestrictionProvider.EDeliveryMethodModifications;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 * 
 * 
 *         modelName: OrganismModel930148736
 *
 */
public class DeliveryMethodActiveLearningSlotFilling {

	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new DeliveryMethodActiveLearningSlotFilling();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("src/main/resources/slotfilling/injury/corpus/instances/");

	public DeliveryMethodActiveLearningSlotFilling() throws IOException {

		/**
		 * Initialize the system.
		 * 
		 */
		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("DeliveryMethod")).apply()
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new DosageNormalization())
				.registerNormalizationFunction(new DurationNormalization()).build();

//		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
//				.build();

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
				.setSeed(1000L).setDevelopmentProportion(20).setCorpusSizeFraction(1F).build();

		EActiveLearningStrategies[] activeLearningStrategies = new EActiveLearningStrategies[] {
				EActiveLearningStrategies.DocumentRandomRanker, EActiveLearningStrategies.DocumentModelScoreRanker,
				EActiveLearningStrategies.DocumentMarginBasedRanker, EActiveLearningStrategies.DocumentEntropyRanker };

		EDeliveryMethodModifications rule = EDeliveryMethodModifications.ROOT_LOCATION_DURATION;

		for (EActiveLearningStrategies strategy : activeLearningStrategies) {
			log.info(strategy);

			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

			List<String> allTrainingInstanceNames = instanceProvider.getRedistributedTrainingInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> developInstanceNames = instanceProvider.getRedistributedDevelopmentInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> testInstanceNames = instanceProvider.getRedistributedTestInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			final List<String> trainingInstancesNames = new ArrayList<>();

			int numberOfInstanceToBegin = 1;
			long numberOfInstancePerStep = 1;
			int numOfMaxSteps = 25;

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

				DeliveryMethodPredictor predictor = new DeliveryMethodPredictor(modelName, trainingInstancesNames,
						developInstanceNames, testInstanceNames, rule, ENERModus.GOLD);

				predictor.trainOrLoadModel();

				List<Instance> remainingInstances = instanceProvider.getRedistributedTrainingInstances().stream()
						.filter(t -> !trainingInstancesNames.contains(t.getName())).collect(Collectors.toList());

				predictor.evaluateOnDevelopment();

				final IActiveLearningDocumentRanker ranker = ActiveLearningProvider.getActiveLearningInstance(strategy,
						predictor);

				List<Instance> rankedInstances = ranker.rank(remainingInstances);

				newTrainingDataInstances = rankedInstances.stream().map(a -> a.getName()).limit(numberOfInstancePerStep)
						.collect(Collectors.toList());

				trainingInstancesNames.addAll(newTrainingDataInstances);
				testInstanceNames.removeAll(newTrainingDataInstances);

			}
		}

	}
}
