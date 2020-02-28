package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.hardconstraints;

import de.hterhors.semanticmr.crf.exploration.constraints.AbstractHardConstraint;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class DistinctExperimentalGroupConstraint extends AbstractHardConstraint {

	final public AbstractEvaluator evaluator;

	public DistinctExperimentalGroupConstraint(AbstractEvaluator evaluator) {
		this.evaluator = evaluator;
	}

	@Override
	public boolean violatesConstraint(State currentState, EntityTemplate entityTemplate) {
		boolean violates = false;
		boolean inclTmp = SCIOSlotTypes.hasGroupName.isIncluded();
		SCIOSlotTypes.hasGroupName.exclude();
		for (AbstractAnnotation currentPrediction : currentState.getCurrentPredictions().getAnnotations()) {
			if (currentPrediction.evaluateEquals(evaluator, entityTemplate)) {
				violates = true;
				break;
			}
		}
		if (inclTmp)
			SCIOSlotTypes.hasGroupName.include();

		return violates;
	}

}
