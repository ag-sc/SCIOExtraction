package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.goldmodrules;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class OnlyDefinedExpGroupResults implements GoldModificationRule {

	@Override
	public AbstractAnnotation modify(AbstractAnnotation result) {

		AbstractAnnotation target = result.asInstanceOfEntityTemplate()
				.getSingleFillerSlot(SCIOSlotTypes.hasTargetGroup).getSlotFiller();
		AbstractAnnotation reference = result.asInstanceOfEntityTemplate()
				.getSingleFillerSlot(SCIOSlotTypes.hasReferenceGroup).getSlotFiller();

		if (target != null
				&& target.asInstanceOfEntityTemplate().getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
			return null;

		if (reference != null
				&& reference.asInstanceOfEntityTemplate().getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
			return null;

		return result;
	}
}
