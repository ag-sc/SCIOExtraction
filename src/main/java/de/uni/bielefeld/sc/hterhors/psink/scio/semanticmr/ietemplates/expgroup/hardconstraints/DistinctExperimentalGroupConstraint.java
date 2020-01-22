package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.hardconstraints;

import de.hterhors.semanticmr.crf.exploration.constraints.AbstractHardConstraint;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;

public class DistinctExperimentalGroupConstraint extends AbstractHardConstraint {

	final public AbstractEvaluator evaluator;

	public DistinctExperimentalGroupConstraint(AbstractEvaluator evaluator) {
		this.evaluator = evaluator;
	}

	@Override
	public boolean violatesConstraint(State currentState, EntityTemplate entityTemplate) {

		for (AbstractAnnotation currentPrediction : currentState.getCurrentPredictions().getAnnotations()) {

			if (currentPrediction.evaluate(evaluator, entityTemplate).getF1() != 1.0D)
				return false;
		}

		return true;
	}

}
