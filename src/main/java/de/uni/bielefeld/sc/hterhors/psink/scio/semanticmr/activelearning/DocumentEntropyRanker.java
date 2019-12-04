package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.activelearning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.activelearning.IActiveLearningDocumentRanker;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;

public class DocumentEntropyRanker implements IActiveLearningDocumentRanker {

	public static int N = 10;

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	final private AbstractSlotFillingPredictor runner;

	public DocumentEntropyRanker(AbstractSlotFillingPredictor runner) {
		this.runner = runner;
	}

	@Override
	public List<Instance> rank(List<Instance> remainingInstances) {

		List<HighestFirst> entropyInstances = new ArrayList<>();
//		List<RankedInstance> objectiveInstances = new ArrayList<>();

		log.info("Compute variations for entropy...");
		Map<Instance, List<State>> results = runner.crf.collectNBestStates(remainingInstances, N, runner.maxStepCrit);

		log.info("Compute entropy...");
		for (Entry<Instance, List<State>> predictedInstance : results.entrySet()) {

			final double partition = predictedInstance.getValue().stream().map(s -> -1 + s.getModelScore()).reduce(0D,
					Double::sum);

			double entropy = 0;

			/*
			 * Compute entropy of state
			 */
			for (State state : predictedInstance.getValue()) {

				final double modelProbability = (-1 + state.getModelScore()) / partition;
				entropy -= modelProbability * Math.log(modelProbability);

			}

			final double maxEntropy = Math.log(predictedInstance.getValue().size());

			/*
			 * Normalize by length
			 */
			entropy /= maxEntropy;

//			for (OBIEState rankedInstance : predictedInstance.getValue()) {
//				runner.scoreWithObjectiveFunction(rankedInstance);
//			}
//			predictedInstance.getValue().forEach(log::info);
//			log.info(
//					predictedInstance.getKey().getName() + "\t" + predictedInstance.getValue().size() + "\t" + entropy);
//			log.info("___");

			entropyInstances.add(new HighestFirst(predictedInstance.getKey(), entropy));
//			final double inverseObjectiveRank = 1 - predictedInstance.getValue().get(0).getObjectiveScore();
//			objectiveInstances.add(new RankedInstance(inverseObjectiveRank, predictedInstance.getKey()));

		}

//		log.info("Sort...");

		Collections.sort(entropyInstances);

//		Collections.sort(objectiveInstances);

//		SpearmansCorrelation s = new SpearmansCorrelation();
//
//		double[] entropy = entropyInstances.stream().map(i -> (double) i.instance.getName().hashCode())
//				.mapToDouble(Double::doubleValue).toArray();
//		double[] objective = objectiveInstances.stream().map(i -> (double) i.instance.getName().hashCode())
//				.mapToDouble(Double::doubleValue).toArray();
//
//		double correlation = s.correlation(entropy, objective);
//
//		final double meanObjectiveF1Score = objectiveInstances.stream().map(i -> i.value).reduce(0D, Double::sum)
//				/ objectiveInstances.size();
//
//		log.info("Spearmans Correlation: " + correlation);
//		log.info("Mean F1 score (Objective): " + meanObjectiveF1Score);

//		logStats(entropyInstances, "entropy");
//		logStats(objectiveInstances, "inverse-objective");

		return entropyInstances.stream().map(e -> e.instance).collect(Collectors.toList());

	}

	final int n = 20;

	private void logStats(List<HighestFirst> entropyInstances, String context) {

		log.info("Next " + n + " " + context + ":");
		entropyInstances.stream().limit(n).forEach(i -> log.info(i.instance.getName() + ":" + i.score));

	}
}
