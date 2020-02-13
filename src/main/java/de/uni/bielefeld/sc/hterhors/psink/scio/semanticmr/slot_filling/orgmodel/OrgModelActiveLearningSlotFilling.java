package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.activelearning.ActiveLearningProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.AgeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;

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
	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	public OrgModelActiveLearningSlotFilling() throws IOException {

		/**
		 * Initialize the system.
		 * 
		 */
		SystemScope scope = SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("OrganismModel")).apply()
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization()).build();

//		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
//				.build();

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
				.setSeed(1000L).setDevelopmentProportion(20).setCorpusSizeFraction(1F).build();

		EActiveLearningStrategies[] activeLearningStrategies = new EActiveLearningStrategies[] {
				EActiveLearningStrategies.DocumentRandomRanker, EActiveLearningStrategies.DocumentModelScoreRanker,
				EActiveLearningStrategies.DocumentMarginBasedRanker, EActiveLearningStrategies.DocumentEntropyRanker };

		EOrgModelModifications rule = EOrgModelModifications.SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE;
		PrintStream resultOut = new PrintStream("results/activeLearning/OrganismModel_full_plusfive.csv");
		for (EActiveLearningStrategies strategy : activeLearningStrategies) {
			log.info(strategy);

			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					OrganismModelRestrictionProvider.getByRule(rule));

			List<String> allTrainingInstanceNames = instanceProvider.getRedistributedTrainingInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> developInstanceNames = instanceProvider.getRedistributedDevelopmentInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> testInstanceNames = instanceProvider.getRedistributedTestInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			final List<String> trainingInstancesNames = new ArrayList<>();

			int numberOfInstanceToBegin = 5;
			long numberOfInstancePerStep = 5;
			int numOfMaxSteps = 10;

			trainingInstancesNames.addAll(allTrainingInstanceNames.subList(0, numberOfInstanceToBegin));
			testInstanceNames.addAll(allTrainingInstanceNames);
			testInstanceNames.removeAll(trainingInstancesNames);

			String rand = String.valueOf(new Random().nextInt());

			List<String> newTrainingDataInstances = null;

			int i = 0;

			while (i++ != numOfMaxSteps && (newTrainingDataInstances == null || !newTrainingDataInstances.isEmpty())) {
				String modelName = "OrganismModel_" + rand + "_" + strategy.name() + "_" + i;
				log.info("model name: " + modelName);
				log.info("#Training instances: " + trainingInstancesNames.size());
				log.info("Strategy: " + strategy);

				OrgModelSlotFillingPredictor predictor = new OrgModelSlotFillingPredictor(modelName, scope,
						trainingInstancesNames, developInstanceNames, testInstanceNames, rule);

				predictor.trainOrLoadModel();

				List<Instance> remainingInstances = instanceProvider.getRedistributedTrainingInstances().stream()
						.filter(t -> !trainingInstancesNames.contains(t.getName())).collect(Collectors.toList());

				Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

				Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);

				resultOut.println(toResult(score, strategy, trainingInstancesNames, rule));

				final IActiveLearningDocumentRanker ranker = ActiveLearningProvider.getActiveLearningInstance(strategy,
						predictor);

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
			EOrgModelModifications rule) {
		return strategy.name() + "\t" + rule.name() + "\t" + trainingInstancesNames.size() + "\t"
				+ resultFormatter.format(score.getF1()) + "\t" + resultFormatter.format(score.getPrecision()) + "\t"
				+ resultFormatter.format(score.getRecall());
	}
}
