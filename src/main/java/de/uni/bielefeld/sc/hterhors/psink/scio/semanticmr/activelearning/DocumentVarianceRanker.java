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
import de.hterhors.semanticmr.crf.SemanticParsingCRF;
import de.hterhors.semanticmr.crf.exploration.IExplorationStrategy;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;

/**
 * High Variance is bad! Chose data with highest variance first.
 * 
 * @author hterhors
 *
 */
public class DocumentVarianceRanker implements IActiveLearningDocumentRanker {

	final Logger log = LogManager.getRootLogger();

	final private AbstractSlotFillingPredictor predictor;

	public DocumentVarianceRanker(AbstractSlotFillingPredictor predictor) {
		this.predictor = predictor;
	}

	@Override
	public List<Instance> rank(List<Instance> remainingInstances) {

		List<HighestFirst> varianceInstances = new ArrayList<>();
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
			for (IExplorationStrategy explorer : ((SemanticParsingCRF) predictor.crf).explorerList) {
				nextStates.addAll(explorer.explore(initialState));
			}

			/*
			 * Score with model
			 */
			((SemanticParsingCRF) predictor.crf).scoreWithModel(nextStates);

//			runner.scoreWithObjectiveFunction(predictedInstance.getState());

			final double mean = nextStates.stream().map(s -> s.getModelScore()).reduce(0D, Double::sum)
					/ nextStates.size();

			final double variance = Math
					.sqrt(nextStates.stream().map(s -> Math.pow(mean - s.getModelScore(), 2)).reduce(0D, Double::sum)
							/ nextStates.size());

			varianceInstances.add(new HighestFirst(predictedInstance.getKey(), variance));

		}
		log.info("Sort based on variance...");

		Collections.sort(varianceInstances);
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

		return varianceInstances.stream().map(e -> e.instance).collect(Collectors.toList());
	}

}
