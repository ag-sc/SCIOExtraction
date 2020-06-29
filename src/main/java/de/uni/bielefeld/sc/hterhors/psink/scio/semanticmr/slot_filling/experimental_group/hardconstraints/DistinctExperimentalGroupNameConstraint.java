package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.hardconstraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.exploration.constraints.AbstractHardConstraint;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class DistinctExperimentalGroupNameConstraint extends AbstractHardConstraint {

	final public AbstractEvaluator evaluator;

	public DistinctExperimentalGroupNameConstraint(AbstractEvaluator evaluator) {
		this.evaluator = evaluator;
	}

	@Override
	public boolean violatesConstraint(State currentState, EntityTemplate entityTemplate) {

		if (SCIOSlotTypes.hasGroupName.isExcluded() || SCIOSlotTypes.hasGroupName.isFrozen())
			return false;

		Set<AbstractAnnotation> newGNs = entityTemplate.getMultiFillerSlot(SCIOSlotTypes.hasGroupName).getSlotFiller();

		for (AbstractAnnotation ngn : newGNs) {

			int countOverlap = 0;

			group: for (AbstractAnnotation currentPrediction : currentState.getCurrentPredictions().getAnnotations()) {

				Set<AbstractAnnotation> existingGNs = currentPrediction.asInstanceOfEntityTemplate()
						.getMultiFillerSlot(SCIOSlotTypes.hasGroupName).getSlotFiller();

				for (AbstractAnnotation egn : existingGNs) {
					if (egn.evaluateEquals(evaluator, ngn)) {
						countOverlap++;
						continue group;
					}

				}
			}

			if (countOverlap > 1)
				return true;

		}

		return false;
	}

	@Override
	public List<EntityTemplate> violatesConstraint(State currentState, List<EntityTemplate> candidateListToFilter) {

		if (SCIOSlotTypes.hasGroupName.isExcluded() || SCIOSlotTypes.hasGroupName.isFrozen())
			return candidateListToFilter;

		List<EntityTemplate> filteredList = new ArrayList<>(candidateListToFilter.size());

		filteredList = candidateListToFilter.parallelStream()
				.filter(candidate -> !violatesConstraint(currentState, candidate)).collect(Collectors.toList());

		return filteredList;
	}

}
