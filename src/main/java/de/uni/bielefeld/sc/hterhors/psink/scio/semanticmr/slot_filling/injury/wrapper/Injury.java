package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.wrapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class Injury {

	final private EntityTemplate injury;

	public EntityTemplate get() {
		return injury;
	}

	public Injury(EntityTemplate injury) {
		this.injury = injury;
	}

	public AbstractAnnotation getInjuryDevice() {

		if (SCIOSlotTypes.hasInjuryDevice.isExcluded())
			return null;

		AbstractAnnotation injuryDevice = injury.getSingleFillerSlot(SCIOSlotTypes.hasInjuryDevice).getSlotFiller();

		if (injuryDevice == null)
			return null;

		return injuryDevice;

	}

	public AbstractAnnotation getInjuryLocation() {

		if (SCIOSlotTypes.hasInjuryLocation.isExcluded())
			return null;

		AbstractAnnotation injuryDevice = injury.getSingleFillerSlot(SCIOSlotTypes.hasInjuryLocation).getSlotFiller();

		if (injuryDevice == null)
			return null;

		return injuryDevice;
	}

	public Set<AbstractAnnotation> getAnaesthetics() {

		if (SCIOSlotTypes.hasAnaesthesia.isExcluded())
			return Collections.emptySet();

		Set<AbstractAnnotation> anaestehtic = injury.getMultiFillerSlot(SCIOSlotTypes.hasAnaesthesia).getSlotFiller();

		return anaestehtic;
	}

	public Set<AbstractAnnotation> getDeliveryMethods() {
		if (SCIOSlotTypes.hasDeliveryMethod.isExcluded())
			return Collections.emptySet();
		Set<AbstractAnnotation> deliveryMethods = new HashSet<>();

		for (AbstractAnnotation iterable_element : getAnaesthetics()) {
			if (!iterable_element.isInstanceOfEntityTemplate())
				continue;

			AbstractAnnotation deliveryMethod = iterable_element.asInstanceOfEntityTemplate()
					.getSingleFillerSlot(SCIOSlotTypes.hasDeliveryMethod).getSlotFiller();

			if (deliveryMethod == null)
				continue;

			deliveryMethods.add(deliveryMethod);

		}

		return deliveryMethods;
	}

}
