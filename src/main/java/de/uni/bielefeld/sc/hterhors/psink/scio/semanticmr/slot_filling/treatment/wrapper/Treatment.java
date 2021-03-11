package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.wrapper;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class Treatment {
	final private EntityTemplate treatment;

	public EntityTemplate get() {
		return treatment;
	}

	public Treatment(EntityTemplate treatment) {
		this.treatment = treatment;
	}

	public AbstractAnnotation getDeliveryMethod() {

		if (SCIOSlotTypes.hasDeliveryMethod.isExcluded())
			return null;

		AbstractAnnotation deliveryMethod = treatment.getSingleFillerSlot(SCIOSlotTypes.hasDeliveryMethod)
				.getSlotFiller();

		return deliveryMethod;
	}

}