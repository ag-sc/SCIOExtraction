package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor.IModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class InjuryRestrictionProvider {

	public enum EInjuryModifications implements IModificationRule {
		ROOT, ROOT_DEVICE, ROOT_LOCATION, ROOT_LOCATION_DEVICE, ROOT_DEVICE_LOCATION_ANAESTHESIA;

	}

	public static List<GoldModificationRule> getByRule(EInjuryModifications modelModifications) {

		switch (modelModifications) {
		case ROOT:
			return getRoot();
		case ROOT_LOCATION:
			return getPlusLocation();
		case ROOT_DEVICE:
			return getPlusDevice();
		case ROOT_LOCATION_DEVICE:
			return getPlusLocationDevice();
		case ROOT_DEVICE_LOCATION_ANAESTHESIA:
			return getPlusAnaesthesia();
		}
		return null;

	}
	
	public static void applySlotTypeRestrictions(EInjuryModifications modelModifications) {

		SlotType.excludeAll();

		switch (modelModifications) {
		case ROOT:
			return;
		case ROOT_LOCATION:
			SCIOSlotTypes.hasLocation.include();
			SCIOSlotTypes.hasUpperVertebrae.include();
			SCIOSlotTypes.hasLowerVertebrae.include();
			return;
		case ROOT_DEVICE:
			SCIOSlotTypes.hasInjuryDevice.include();
			return;
		case ROOT_LOCATION_DEVICE:
			SCIOSlotTypes.hasInjuryDevice.include();
			SCIOSlotTypes.hasLocation.include();
			SCIOSlotTypes.hasUpperVertebrae.include();
			SCIOSlotTypes.hasLowerVertebrae.include();
			return;
		case ROOT_DEVICE_LOCATION_ANAESTHESIA:
			SCIOSlotTypes.hasInjuryDevice.include();
			SCIOSlotTypes.hasLocation.include();
			SCIOSlotTypes.hasUpperVertebrae.include();
			SCIOSlotTypes.hasLowerVertebrae.include();
			SCIOSlotTypes.hasAnaesthesia.include();
			return;
		}

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
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasLocation, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasLocation).getSlotFiller());

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
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasInjuryDevice, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasInjuryDevice).getSlotFiller());

				if (!newGold.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
					return null;

				if (newGold.asInstanceOfEntityTemplate().isEmpty())
					return null;

				return newGold;
			}
		});
		return rules;

	}

	public static List<GoldModificationRule> getPlusLocationDevice() {
		List<GoldModificationRule> rules = new ArrayList<>();

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation().deepCopy());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasInjuryDevice, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasInjuryDevice).getSlotFiller());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasLocation, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasLocation).getSlotFiller());

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
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasInjuryDevice, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasInjuryDevice).getSlotFiller());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasLocation, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasLocation).getSlotFiller());

				for (AbstractAnnotation sf : goldAnnotation.asInstanceOfEntityTemplate()
						.getMultiFillerSlot(SCIOSlotTypes.hasAnaesthesia).getSlotFiller()) {
					newGold.addMultiSlotFiller(SCIOSlotTypes.hasAnaesthesia, sf.deepCopy());
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
