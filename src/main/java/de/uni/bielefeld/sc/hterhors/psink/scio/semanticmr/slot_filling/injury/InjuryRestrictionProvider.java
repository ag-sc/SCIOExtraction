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
			SCIOSlotTypes.hasInjuryLocation.include();
			SCIOSlotTypes.hasUpperVertebrae.include();
			SCIOSlotTypes.hasLowerVertebrae.include();
			SCIOSlotTypes.hasInjuryIntensity.include();
			return;
		case ROOT_DEVICE:
			SCIOSlotTypes.hasInjuryDevice.include();
			SCIOSlotTypes.hasWeight.include();
			SCIOSlotTypes.hasForce.include();
			SCIOSlotTypes.hasDistance.include();
			SCIOSlotTypes.hasDuration.include();
			SCIOSlotTypes.hasVolume.include();
			SCIOSlotTypes.hasInjuryIntensity.include();
			return;
		case ROOT_LOCATION_DEVICE:
			SCIOSlotTypes.hasInjuryDevice.include();
			SCIOSlotTypes.hasWeight.include();
			SCIOSlotTypes.hasForce.include();
			SCIOSlotTypes.hasDistance.include();
			SCIOSlotTypes.hasDuration.include();
			SCIOSlotTypes.hasVolume.include();
			SCIOSlotTypes.hasInjuryLocation.include();
			SCIOSlotTypes.hasUpperVertebrae.include();
			SCIOSlotTypes.hasLowerVertebrae.include();
			SCIOSlotTypes.hasInjuryIntensity.include();
			return;
		case ROOT_DEVICE_LOCATION_ANAESTHESIA:
			SCIOSlotTypes.hasInjuryDevice.include();
			SCIOSlotTypes.hasWeight.include();
			SCIOSlotTypes.hasForce.include();
			SCIOSlotTypes.hasDistance.include();
			SCIOSlotTypes.hasDuration.include();
			SCIOSlotTypes.hasVolume.include();
			SCIOSlotTypes.hasInjuryLocation.include();
			SCIOSlotTypes.hasUpperVertebrae.include();
			SCIOSlotTypes.hasLowerVertebrae.include();
			SCIOSlotTypes.hasAnaesthesia.include();
			SCIOSlotTypes.hasDosage.include();
			SCIOSlotTypes.hasDeliveryMethod.include();
			SCIOSlotTypes.hasDuration.include();
			SCIOSlotTypes.hasLocations.include();
			SCIOSlotTypes.hasInjuryIntensity.include();
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
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasInjuryLocation, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasInjuryLocation).getSlotFiller());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasInjuryIntensity, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasInjuryIntensity).getSlotFiller());

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
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasInjuryIntensity, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasInjuryIntensity).getSlotFiller());

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
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasInjuryLocation, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasInjuryLocation).getSlotFiller());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasInjuryIntensity, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasInjuryIntensity).getSlotFiller());

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
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasInjuryLocation, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasInjuryLocation).getSlotFiller());

				newGold.setSingleSlotFiller(SCIOSlotTypes.hasInjuryIntensity, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasInjuryIntensity).getSlotFiller());

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
