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

/**
 * Ranks the remaining instances according to the oracles objective score.
 * 
 * This ranker is just used for analyzing, can not be used in a productive
 * session.
 * 
 * @author hterhors
 *
 */
public class DocumentObjectiveScoreRanker implements IActiveLearningDocumentRanker {

	final private AbstractSlotFillingPredictor predictor;

	public DocumentObjectiveScoreRanker(AbstractSlotFillingPredictor predictor) {
		this.predictor = predictor;
	}

	@Override
	public List<Instance> rank(List<Instance> remainingInstances) {

		Map<Instance, State> results = predictor.crf.predict(remainingInstances, predictor.maxStepCrit,
				predictor.noModelChangeCrit);

		List<SmallestFirst> predictions = new ArrayList<>(
				results.entrySet().stream().map(e -> new SmallestFirst(e.getKey(), e.getValue().getObjectiveScore()))
						.collect(Collectors.toList()));

		Collections.sort(predictions);

		return predictions.stream().map(p -> p.instance).collect(Collectors.toList());
	}

}
