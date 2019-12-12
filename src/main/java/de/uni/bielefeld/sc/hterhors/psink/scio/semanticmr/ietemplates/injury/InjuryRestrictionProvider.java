package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;

public class InjuryRestrictionProvider {

	public enum EInjuryModificationRules {
		ROOT, ROOT_LOCATION, ROOT_LOCATION_DEVICE, ROOT_LOCATION_DEVICE_ANAESTHESIA;

	}

	private static final SlotType deviceSlot = SlotType.get("hasInjuryDevice");
	private static final SlotType locationSlot = SlotType.get("hasInjuryLocation");
	private static final SlotType anaesthesiaSlot = SlotType.get("hasInjuryAnaesthesia");

	public static List<GoldModificationRule> getByRule(EInjuryModificationRules modelModifications) {

		switch (modelModifications) {
		case ROOT:
			return getRoot();
		case ROOT_LOCATION:
			return getPlusLocation();
		case ROOT_LOCATION_DEVICE:
			return getPlusDevice();
		case ROOT_LOCATION_DEVICE_ANAESTHESIA:
			return getPlusAnaesthesia();
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
				newGold.setSingleSlotFiller(locationSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(locationSlot).getSlotFiller());

				if (!newGold.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
					return null;

				if (newGold.asInstanceOfEntityTemplate().isEmpty())
					return null;

				return newGold;
			}
		});
		return rules;

	}

	public static List<GoldModificationRule> getPlusDevice() {
		List<GoldModificationRule> rules = new ArrayList<>();

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation().deepCopy());
				newGold.setSingleSlotFiller(deviceSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(deviceSlot).getSlotFiller());
				newGold.setSingleSlotFiller(locationSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(locationSlot).getSlotFiller());

				if (!newGold.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
					return null;

				if (newGold.asInstanceOfEntityTemplate().isEmpty())
					return null;

				return newGold;
			}
		});
		return rules;

	}

	public static List<GoldModificationRule> getPlusAnaesthesia() {
		List<GoldModificationRule> rules = new ArrayList<>();

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation().deepCopy());
				newGold.setSingleSlotFiller(deviceSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(deviceSlot).getSlotFiller());
				newGold.setSingleSlotFiller(locationSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(locationSlot).getSlotFiller());

				for (AbstractAnnotation sf : goldAnnotation.asInstanceOfEntityTemplate()
						.getMultiFillerSlot(anaesthesiaSlot).getSlotFiller()) {
					newGold.addMultiSlotFiller(anaesthesiaSlot, sf.deepCopy());
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
