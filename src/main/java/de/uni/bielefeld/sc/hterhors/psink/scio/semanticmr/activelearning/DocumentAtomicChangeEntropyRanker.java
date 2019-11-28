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
import de.hterhors.semanticmr.activelearning.RankedInstance;
import de.hterhors.semanticmr.crf.exploration.IExplorationStrategy;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;

public class DocumentAtomicChangeEntropyRanker implements IActiveLearningDocumentRanker {

	final Logger log = LogManager.getRootLogger();

	final private AbstractSlotFillingPredictor predictor;

	public DocumentAtomicChangeEntropyRanker(AbstractSlotFillingPredictor predictor) {
		this.predictor = predictor;
	}

	@Override
	public List<Instance> rank(List<Instance> remainingInstances) {

		List<RankedInstance> entropyInstances = new ArrayList<>();
//		List<RankedInstance> objectiveInstances = new ArrayList<>();

		log.info("Predict final states based on current model...");

		Map<Instance, State> results = predictor.crf.predict(remainingInstances, predictor.maxStepCrit,
				predictor.noModelChangeCrit);

		log.info("Compute variations for entropy...");
		for (Entry<Instance, State> predictedInstance : results.entrySet()) {
			State initialState = new State(predictedInstance.getValue());
			List<State> nextStates = new ArrayList<>();

			/*
			 * Create possible changes using explorers.
			 */
			for (IExplorationStrategy explorer : predictor.crf.explorerList) {
				nextStates.addAll(explorer.explore(initialState));
			}

			/*
			 * Score with model
			 */
			predictor.crf.scoreWithModel(nextStates);

//			runner.scoreWithObjectiveFunction(predictedInstance.getState());

			final double partition = nextStates.stream().map(s -> s.getModelScore()).reduce(0D, Double::sum);

			double entropy = 0;

			/*
			 * Compute entropy of state
			 */
			for (State obieState : nextStates) {

				final double modelProbability = obieState.getModelScore() / partition;
				entropy -= modelProbability * Math.log(modelProbability);

			}

			final double maxEntropy = Math.log(nextStates.size());

			/*
			 * Normalize by length
			 */
			entropy /= maxEntropy;

//			log.info("####FINAL STATE####");
//			log.info(initialState);
//			log.info("########");
//			nextStates.forEach(log::info);
//			log.info(initialState.getInstance().getName() + "\t" + nextStates.size() + "\t" + entropy);

//			log.info("___");
//			if(initialState.getInstance().getName().equals("Arvo_Kraam")) {
//				log.info("__");
//			}

			entropyInstances.add(new RankedInstance(entropy, predictedInstance.getKey()));
//			final double inverseObjectiveRank = 1 - predictedInstance.getState().getObjectiveScore();
//			objectiveInstances.add(new RankedInstance(inverseObjectiveRank, predictedInstance.getInstance()));

		}
		log.info("Sort based on entropy...");

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

//		log.info("Spearmans Correlation: " + correlation);
//		log.info("Mean F1 score (Objective): " + meanObjectiveF1Score);

//		System.out.println("Entropy:");
//		entropyInstances.forEach(System.out::println);
//		System.exit(1);

//		logStats(entropyInstances, "entropy");
//		logStats(objectiveInstances, "inverse-objective");

		return entropyInstances.stream().map(e -> e.instance).collect(Collectors.toList());
	}
//
//	final int n = 5;
//
//	private void logStats(List<RankedInstance> entropyInstances, String context) {
//
//		log.info("Next " + n + " " + context + ":");
//		entropyInstances.stream().limit(n).forEach(i -> log.info(i.instance.getName() + ":" + i.value));
//
//	}

}
