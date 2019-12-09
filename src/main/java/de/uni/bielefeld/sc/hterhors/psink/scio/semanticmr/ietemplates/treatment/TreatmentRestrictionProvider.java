package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.treatment;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class TreatmentRestrictionProvider {

	public enum ETreatmentModifications {
		ROOT;

	}

	private static final SlotType compoundSlot = SCIOSlotTypes.hasCompound;
	private static final SlotType deliveryMethodSlot = SlotType.get("hasDeliveryMethod");

	public static List<GoldModificationRule> getByRule(ETreatmentModifications modelModifications) {

		switch (modelModifications) {
		case ROOT:
			return getRoot();
		}
		return null;

	}

	public static List<GoldModificationRule> getRoot() {
		List<GoldModificationRule> rules = new ArrayList<>();

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation().deepCopy());

				if (newGold.getEntityType() == SCIOEntityTypes.compoundTreatment) {
					if (goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(compoundSlot)
							.containsSlotFiller())
						newGold.setSingleSlotFiller(compoundSlot,
								goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(compoundSlot)
										.getSlotFiller().asInstanceOfEntityTemplate().clearProperties());

					if (newGold.asInstanceOfEntityTemplate().getAllSlotFillerValues().isEmpty())
						return null;

				} else {

					if (!newGold.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
						return null;
				}

				return newGold;
			}
		});
		return rules;

	}

	public static List<GoldModificationRule> getPlusLocation() {
		List<GoldModificationRule> rules = new ArrayList<>();

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation().deepCopy());
				newGold.setSingleSlotFiller(deliveryMethodSlot, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(deliveryMethodSlot).getSlotFiller());

				if (!newGold.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
					return null;

				if (newGold.asInstanceOfEntityTemplate().getAllSlotFillerValues().isEmpty())
					return null;

				return newGold;
			}
		});
		return rules;

	}

}
