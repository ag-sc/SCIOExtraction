package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.activelearning;

import java.util.Objects;

import de.hterhors.semanticmr.activelearning.IActiveLearningDocumentRanker;
import de.hterhors.semanticmr.activelearning.ranker.DocumentRandomRanker;
import de.hterhors.semanticmr.activelearning.ranker.EActiveLearningStrategies;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;

public class ActiveLearningProvider {

	public static IActiveLearningDocumentRanker getActiveLearningInstance(EActiveLearningStrategies strategy,
			AbstractSlotFillingPredictor predictor) {

		if (strategy != EActiveLearningStrategies.DocumentRandomRanker)
			Objects.nonNull(predictor);

		switch (strategy) {
		case DocumentHighVariatyRanker:
			return new DocumentHighVariatyRanker(predictor);
		case DocumentAtomicChangeEntropyRanker:
			return new DocumentAtomicChangeEntropyRanker(predictor);
		case DocumentMarginBasedRanker:
			return new DocumentMarginBasedRanker(predictor);
		case DocumentEntropyRanker:
			return new DocumentEntropyRanker(predictor);
		case DocumentModelScoreRanker:
			return new DocumentModelScoreRanker(predictor);
		case DocumentObjectiveScoreRanker:
			return new DocumentObjectiveScoreRanker(predictor);
		case DocumentRandomRanker:
			return new DocumentRandomRanker();
		case DocumentVarianceRanker:
			return new DocumentVarianceRanker(predictor);
		default:
			throw new IllegalArgumentException("Unkown active learning strategy...");
		}

	}

}
