package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.hardconstraints;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.exploration.constraints.AbstractHardConstraint;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;

public class DistinctEntityTemplateConstraint extends AbstractHardConstraint {

	final public AbstractEvaluator evaluator;

	public DistinctEntityTemplateConstraint(AbstractEvaluator evaluator) {
		this.evaluator = evaluator;
	}

	@Override
	public boolean violatesConstraint(State currentState, EntityTemplate entityTemplate) {
		boolean violates = false;

		for (AbstractAnnotation currentPrediction : currentState.getCurrentPredictions().getAnnotations()) {
			if (currentPrediction.evaluateEquals(evaluator, entityTemplate)) {
				violates = true;
				break;
			}
		}

		return violates;

	}

	@Override
	public List<EntityTemplate> violatesConstraint(State currentState, List<EntityTemplate> candidateListToFilter) {

		List<EntityTemplate> filteredList = new ArrayList<>(candidateListToFilter.size());

		filteredList = candidateListToFilter.parallelStream()
				.filter(candidate -> !violatesConstraint(currentState, candidate)).collect(Collectors.toList());

		return filteredList;
	}

}
