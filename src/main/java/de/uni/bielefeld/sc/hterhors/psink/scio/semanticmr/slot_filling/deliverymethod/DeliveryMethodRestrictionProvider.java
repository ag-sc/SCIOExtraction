package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.deliverymethod;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor.IModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class DeliveryMethodRestrictionProvider {

	public enum EDeliveryMethodModifications implements IModificationRule {
		ROOT, ROOT_LOCATION, ROOT_LOCATION_DURATION;

	}

	private static final SlotType durationSlot = SCIOSlotTypes.hasDuration;
	private static final SlotType locationsSlot = SCIOSlotTypes.hasLocations;

	public static List<GoldModificationRule> getByRule(EDeliveryMethodModifications modelModifications) {

		switch (modelModifications) {
		case ROOT:
			return getRoot();
		case ROOT_LOCATION:
			return getPlusLocation();
		case ROOT_LOCATION_DURATION:
			return getPlusDuration();
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

				if (!newGold.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
					return null;

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

				for (AbstractAnnotation sf : goldAnnotation.asInstanceOfEntityTemplate()
						.getMultiFillerSlot(locationsSlot).getSlotFiller()) {
					newGold.addMultiSlotFiller(locationsSlot, sf.deepCopy());
				}

				if (!newGold.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
					return null;

				if (newGold.asInstanceOfEntityTemplate().isEmpty())
					return null;

				return newGold;
			}
		});
		return rules;

	}

	public static List<GoldModificationRule> getPlusDuration() {
		List<GoldModificationRule> rules = new ArrayList<>();

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation().deepCopy());
				newGold.setSingleSlotFiller(durationSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(durationSlot).getSlotFiller());

				for (AbstractAnnotation sf : goldAnnotation.asInstanceOfEntityTemplate()
						.getMultiFillerSlot(locationsSlot).getSlotFiller()) {
					newGold.addMultiSlotFiller(locationsSlot, sf.deepCopy());
				}

				if (!newGold.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
					return null;

				if (newGold.asInstanceOfEntityTemplate().isEmpty())
					return null;

				return newGold;
			}
		});
		return rules;

	}

}
