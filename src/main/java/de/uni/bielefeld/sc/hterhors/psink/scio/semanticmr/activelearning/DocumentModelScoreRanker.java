package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.activelearning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.activelearning.IActiveLearningDocumentRanker;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;

public class DocumentModelScoreRanker implements IActiveLearningDocumentRanker {

	final private AbstractSlotFillingPredictor predictor;

	public DocumentModelScoreRanker(AbstractSlotFillingPredictor predictor) {
		this.predictor = predictor;
	}

	static class Pair implements Comparable<Pair> {
		public final Instance instance;

		public final double modelScore;

		public Pair(Instance instance, double modelScore) {
			this.instance = instance;
			this.modelScore = modelScore;
		}

		@Override
		public int compareTo(Pair o) {
			/*
			 * Smalles first.
			 */
			return Double.compare(modelScore, o.modelScore);
		}
	}

	@Override
	public List<Instance> rank(List<Instance> remainingInstances) {

		Map<Instance, State> results = predictor.crf.predict(remainingInstances, predictor.maxStepCrit,
				predictor.noModelChangeCrit);

		List<Pair> predictions = new ArrayList<>(results.entrySet().stream()
				.map(e -> new Pair(e.getKey(), e.getValue().getModelScore())).collect(Collectors.toList()));

		Collections.sort(predictions);

		return predictions.stream().map(p -> p.instance).collect(Collectors.toList());
	}

}
