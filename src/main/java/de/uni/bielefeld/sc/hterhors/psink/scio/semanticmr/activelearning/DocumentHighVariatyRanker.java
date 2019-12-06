package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.activelearning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.activelearning.IActiveLearningDocumentRanker;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;

public class DocumentHighVariatyRanker implements IActiveLearningDocumentRanker {

	final private AbstractSlotFillingPredictor predictor;

	public DocumentHighVariatyRanker(AbstractSlotFillingPredictor predictor) {
		this.predictor = predictor;
	}

	@Override
	public List<Instance> rank(List<Instance> remainingInstances) {

		List<HighestFirst> predictions = new ArrayList<>();
		Map<SlotType, Set<String>> trainData = extractAnnotationsFromTrainingData();


		Map<Instance, State> results = predictor.crf.predict(remainingInstances, predictor.maxStepCrit,
				predictor.noModelChangeCrit);

		for (Entry<Instance, State> prediction : results.entrySet()) {

			Map<SlotType, Set<String>> predictData = new HashMap<>();
			for (AbstractAnnotation goldAnnotation : prediction.getValue().getCurrentPredictions()
					.getAbstractAnnotations()) {

				for (Entry<SlotType, Set<AbstractAnnotation>> filler : goldAnnotation.asInstanceOfEntityTemplate()
						.filter().docLinkedAnnoation().entityTypeAnnoation().singleSlots().multiSlots().merge()
						.nonEmpty().build().getMergedAnnotations().entrySet()) {

					predictData.putIfAbsent(filler.getKey(), new HashSet<>());

					Set<String> annData = predictData.get(filler.getKey());

					for (AbstractAnnotation ann : filler.getValue()) {

						if (ann.isInstanceOfDocumentLinkedAnnotation() && ann.getEntityType().isLiteral)
							annData.add(ann.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm());
						else
							annData.add(ann.getEntityType().name);

					}
				}
			}
			double score = 0;

			for (SlotType d : predictData.keySet()) {
				score += !trainData.containsKey(d) ? 1 : 0;
			}
			for (SlotType d : trainData.keySet()) {
				score += !predictData.containsKey(d) ? 1 : 0;
			}

			for (Entry<SlotType, Set<String>> d : trainData.entrySet()) {
				if (!predictData.containsKey(d.getKey()))
					continue;

				for (String predVal : predictData.get(d.getKey())) {
					if (!d.getValue().contains(predVal)) {
						score++;
					}
				}
			}

			predictions.add(new HighestFirst(prediction.getKey(), score));

		}

		Collections.sort(predictions);

		return predictions.stream().map(p -> p.instance).collect(Collectors.toList());
	}

	public Map<SlotType, Set<String>> extractAnnotationsFromTrainingData() {
		Map<SlotType, Set<String>> trainData = new HashMap<>();

		for (Instance trainInstance : predictor.getTrainingInstances()) {
			for (AbstractAnnotation goldAnnotation : trainInstance.getGoldAnnotations().getAbstractAnnotations()) {

				for (Entry<SlotType, Set<AbstractAnnotation>> filler : goldAnnotation.asInstanceOfEntityTemplate()
						.filter().docLinkedAnnoation().entityTypeAnnoation().singleSlots().multiSlots().merge()
						.nonEmpty().build().getMergedAnnotations().entrySet()) {

					trainData.putIfAbsent(filler.getKey(), new HashSet<>());

					Set<String> annData = trainData.get(filler.getKey());

					for (AbstractAnnotation ann : filler.getValue()) {

						if (ann.isInstanceOfDocumentLinkedAnnotation() && ann.getEntityType().isLiteral)
							annData.add(ann.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm());
						else
							annData.add(ann.getEntityType().name);

					}
				}
			}
		}
		return trainData;
	}

}
