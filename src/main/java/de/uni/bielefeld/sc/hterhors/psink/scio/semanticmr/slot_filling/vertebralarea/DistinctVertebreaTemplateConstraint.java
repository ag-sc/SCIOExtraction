package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea;

import de.hterhors.semanticmr.crf.exploration.constraints.AbstractHardConstraint;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class DistinctVertebreaTemplateConstraint extends AbstractHardConstraint {

	public DistinctVertebreaTemplateConstraint() {
	}

	@Override
	public boolean violatesConstraint(State currentState, EntityTemplate entityTemplate) {

		AbstractAnnotation upper = entityTemplate.asInstanceOfEntityTemplate()
				.getSingleFillerSlot(SCIOSlotTypes.hasUpperVertebrae).getSlotFiller();
		AbstractAnnotation lower = entityTemplate.asInstanceOfEntityTemplate()
				.getSingleFillerSlot(SCIOSlotTypes.hasLowerVertebrae).getSlotFiller();
		return upper != null && lower != null && upper.getEntityType().equals(lower.getEntityType());

	}

}
