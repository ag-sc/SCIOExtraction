package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthesia;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.exploration.constraints.AbstractHardConstraint;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;

public class DistinctEntityTypeConstraint extends AbstractHardConstraint {

	final public AbstractEvaluator evaluator;

	public DistinctEntityTypeConstraint(AbstractEvaluator evaluator) {
		this.evaluator = evaluator;
	}

	@Override
	public boolean violatesConstraint(State currentState, EntityTemplate entityTemplate, int annotationIndex) {

		int c = 0;
		int index = 0;
		for (AbstractAnnotation currentPrediction : currentState.getCurrentPredictions().getAnnotations()) {

			boolean equals = index != annotationIndex
					&& currentPrediction.getEntityType() == entityTemplate.getEntityType();

			if (equals) {
				c++;
			}
			index++;
		}

		return c >= 1;

	}

	@Override
	public List<EntityTemplate> violatesConstraint(State currentState, List<EntityTemplate> candidateListToFilter,
			int annotationIndex) {

		if (currentState.getCurrentPredictions().getAbstractAnnotations().size() < 2)
			return candidateListToFilter;

		List<EntityTemplate> filteredList = new ArrayList<>(candidateListToFilter.size());

		filteredList = candidateListToFilter.parallelStream()
				.filter(candidate -> !violatesConstraint(currentState, candidate, annotationIndex))
				.collect(Collectors.toList());

		return filteredList;
	}

}
