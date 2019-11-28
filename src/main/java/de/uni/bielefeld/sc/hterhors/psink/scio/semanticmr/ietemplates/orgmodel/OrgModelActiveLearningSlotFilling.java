package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.orgmodel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.activelearning.IActiveLearningDocumentRanker;
import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.OriginalCorpusDistributor;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.activelearning.DocumentAtomicChangeEntropyRanker;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.orgmodel.specs.OrgModelSpecs;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.AgeNormalization;
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
public class OrgModelActiveLearningSlotFilling {

	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new OrgModelActiveLearningSlotFilling();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("src/main/resources/slotfilling/organism_model/corpus/instances/");

	public OrgModelActiveLearningSlotFilling() throws IOException {

		/**
		 * Initialize the system.
		 * 
		 * The scope represents the specifications of the 4 defined specification files.
		 * The scope mainly affects the exploration.
		 */
		SystemScope scope = SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(OrgModelSpecs.systemsScopeReader)
				/**
				 * We apply the scope, so that we can add normalization functions for various
				 * literal entity types, if necessary.
				 */
				.apply()
				/**
				 * Now normalization functions can be added. A normalization function is
				 * especially used for literal-based annotations. In case a normalization
				 * function is provided for a specific entity type, the normalized value is
				 * compared during evaluation instead of the actual surface form. A
				 * normalization function normalizes different surface forms so that e.g. the
				 * weights "500 g", "0.5kg", "500g" are all equal. Each normalization function
				 * is bound to exactly one entity type.
				 */
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization())
				/**
				 * Finally, we build the systems scope.
				 */
				.build();

		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
				.build();

//		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
//				.setSeed(1000L).setDevelopmentProportion(20).setTestProportion(20).setCorpusSizeFraction(1F).build();

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		List<String> allTrainingInstanceNames = instanceProvider.getRedistributedTrainingInstances().stream()
				.map(t -> t.getName()).collect(Collectors.toList());

		List<String> developInstanceNames = instanceProvider.getRedistributedDevelopmentInstances().stream()
				.map(t -> t.getName()).collect(Collectors.toList());

		List<String> testInstanceNames = instanceProvider.getRedistributedTestInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		final List<String> trainingInstancesNames = new ArrayList<>();

		trainingInstancesNames.addAll(allTrainingInstanceNames.subList(0, 5));
		testInstanceNames.addAll(allTrainingInstanceNames);
		testInstanceNames.removeAll(trainingInstancesNames);

		String rand = String.valueOf(new Random().nextInt());

//		final IActiveLearningDocumentRanker ranker = new FullDocumentLengthRanker();

		for (int i = 0; i < 25; i++) {
			System.out.println("----TRAIN----");
			trainingInstancesNames.forEach(System.out::println);
			String modelName = "OrganismModel" + rand + "_AL_" + i;
			System.out.println(modelName);
			OrgModelSlotFillingPredictor predictor;

			predictor = new OrgModelSlotFillingPredictor(modelName, scope, trainingInstancesNames, developInstanceNames,
					testInstanceNames);

			predictor.trainOrLoadModel();

			List<Instance> remainingInstances = instanceProvider.getRedistributedTrainingInstances().stream()
					.filter(t -> !trainingInstancesNames.contains(t.getName())).collect(Collectors.toList());

//			final IActiveLearningDocumentRanker ranker = new DocumentRandomRanker();
//			final IActiveLearningDocumentRanker ranker = new DocumentModelScoreRanker(predictor);
//			final IActiveLearningDocumentRanker ranker = new DocumentEntropyRanker(predictor);
			final IActiveLearningDocumentRanker ranker = new DocumentAtomicChangeEntropyRanker(predictor);

			List<Instance> rankedInstances = ranker.rank(remainingInstances);

			List<String> newTrainingDataInstances = rankedInstances.stream().map(a -> a.getName()).limit(5)
					.collect(Collectors.toList());

			trainingInstancesNames.addAll(newTrainingDataInstances);
			testInstanceNames.removeAll(newTrainingDataInstances);

			predictor.evaluateOnDevelopment();
		}

	}
}
