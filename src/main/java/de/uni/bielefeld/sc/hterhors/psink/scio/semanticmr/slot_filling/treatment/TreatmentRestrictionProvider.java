package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor.IModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class TreatmentRestrictionProvider {

	public enum ETreatmentModifications implements IModificationRule {
		ROOT, DOSAGE, DOSAGE_DELIVERY_METHOD, DOSAGE_DELIVERY_METHOD_APPLICATION_INSTRUMENT,
		DOSAGE_DELIVERY_METHOD_APPLICATION_INSTRUMENT_DIRECTION;

	}

	public static List<GoldModificationRule> getByRule(ETreatmentModifications modelModifications) {

		switch (modelModifications) {
		case ROOT:
			return getRoot();
		case DOSAGE:
			return getPlusDosage();
		case DOSAGE_DELIVERY_METHOD:
			return getPlusDeliveryMethod();
		case DOSAGE_DELIVERY_METHOD_APPLICATION_INSTRUMENT:
			return getPlusApplicationInstrument();
		case DOSAGE_DELIVERY_METHOD_APPLICATION_INSTRUMENT_DIRECTION:
			return getPlusDirection();
		default:
			break;
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
					if (goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
							.containsSlotFiller())
						newGold.setSingleSlotFiller(SCIOSlotTypes.hasCompound,
								goldAnnotation.asInstanceOfEntityTemplate()
										.getSingleFillerSlot(SCIOSlotTypes.hasCompound).getSlotFiller()
										.asInstanceOfEntityTemplate().clearAllSlots());

					if (newGold.asInstanceOfEntityTemplate().isEmpty())
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

	public static List<GoldModificationRule> getPlusDosage() {
		List<GoldModificationRule> rules = new ArrayList<>();

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation().deepCopy());

				if (newGold.getEntityType() == SCIOEntityTypes.compoundTreatment) {
					if (goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
							.containsSlotFiller())
						newGold.setSingleSlotFiller(SCIOSlotTypes.hasCompound,
								goldAnnotation.asInstanceOfEntityTemplate()
										.getSingleFillerSlot(SCIOSlotTypes.hasCompound).getSlotFiller().deepCopy());

					if (newGold.asInstanceOfEntityTemplate().isEmpty())
						return null;
				} else {
					if (!newGold.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
						return null;
				}

				addSingleSlotOnExistance(SCIOSlotTypes.hasElectricFieldStrength, goldAnnotation, newGold);
				addSingleSlotOnExistance(SCIOSlotTypes.hasDosage, goldAnnotation, newGold);
				addSingleSlotOnExistance(SCIOSlotTypes.hasRehabMedication, goldAnnotation, newGold);
				addSingleSlotOnExistance(SCIOSlotTypes.hasTemperature, goldAnnotation, newGold);

				return newGold;
			}

		});
		return rules;

	}

	private static void addSingleSlotOnExistance(SlotType slotType, AbstractAnnotation goldAnnotation,
			EntityTemplate newGold) {
		if (goldAnnotation.asInstanceOfEntityTemplate().hasSlotOfType(slotType))
			newGold.setSingleSlotFiller(slotType,
					goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(slotType).getSlotFiller());
	}

	public static List<GoldModificationRule> getPlusDeliveryMethod() {
		List<GoldModificationRule> rules = new ArrayList<>();

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation().deepCopy());

				if (newGold.getEntityType() == SCIOEntityTypes.compoundTreatment) {
					if (goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
							.containsSlotFiller())
						newGold.setSingleSlotFiller(SCIOSlotTypes.hasCompound,
								goldAnnotation.asInstanceOfEntityTemplate()
										.getSingleFillerSlot(SCIOSlotTypes.hasCompound).getSlotFiller().deepCopy());

					if (newGold.asInstanceOfEntityTemplate().isEmpty())
						return null;
				} else {
					if (!newGold.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
						return null;
				}

				newGold.setSingleSlotFiller(SCIOSlotTypes.hasDeliveryMethod, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasDeliveryMethod).getSlotFiller());
				addSingleSlotOnExistance(SCIOSlotTypes.hasElectricFieldStrength, goldAnnotation, newGold);
				addSingleSlotOnExistance(SCIOSlotTypes.hasDosage, goldAnnotation, newGold);
				addSingleSlotOnExistance(SCIOSlotTypes.hasRehabMedication, goldAnnotation, newGold);
				addSingleSlotOnExistance(SCIOSlotTypes.hasTemperature, goldAnnotation, newGold);

				return newGold;
			}
		});
		return rules;

	}

	public static List<GoldModificationRule> getPlusApplicationInstrument() {
		List<GoldModificationRule> rules = new ArrayList<>();

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation().deepCopy());

				if (newGold.getEntityType() == SCIOEntityTypes.compoundTreatment) {
					if (goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
							.containsSlotFiller())
						newGold.setSingleSlotFiller(SCIOSlotTypes.hasCompound,
								goldAnnotation.asInstanceOfEntityTemplate()
										.getSingleFillerSlot(SCIOSlotTypes.hasCompound).getSlotFiller().deepCopy());

					if (newGold.asInstanceOfEntityTemplate().isEmpty())
						return null;
				} else {
					if (!newGold.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
						return null;
				}

				newGold.setSingleSlotFiller(SCIOSlotTypes.hasDeliveryMethod, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasDeliveryMethod).getSlotFiller());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasApplicationInstrument,
						goldAnnotation.asInstanceOfEntityTemplate()
								.getSingleFillerSlot(SCIOSlotTypes.hasApplicationInstrument).getSlotFiller());
				addSingleSlotOnExistance(SCIOSlotTypes.hasElectricFieldStrength, goldAnnotation, newGold);
				addSingleSlotOnExistance(SCIOSlotTypes.hasDosage, goldAnnotation, newGold);
				addSingleSlotOnExistance(SCIOSlotTypes.hasRehabMedication, goldAnnotation, newGold);
				addSingleSlotOnExistance(SCIOSlotTypes.hasTemperature, goldAnnotation, newGold);

				return newGold;
			}
		});
		return rules;

	}

	public static List<GoldModificationRule> getPlusDirection() {
		List<GoldModificationRule> rules = new ArrayList<>();

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation().deepCopy());
				if (newGold.getEntityType() == SCIOEntityTypes.compoundTreatment) {
					if (goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
							.containsSlotFiller())
						newGold.setSingleSlotFiller(SCIOSlotTypes.hasCompound,
								goldAnnotation.asInstanceOfEntityTemplate()
										.getSingleFillerSlot(SCIOSlotTypes.hasCompound).getSlotFiller().deepCopy());

					if (newGold.asInstanceOfEntityTemplate().isEmpty())
						return null;
				} else {
					if (!newGold.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
						return null;
				}

				newGold.setSingleSlotFiller(SCIOSlotTypes.hasDeliveryMethod, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasDeliveryMethod).getSlotFiller());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasApplicationInstrument,
						goldAnnotation.asInstanceOfEntityTemplate()
								.getSingleFillerSlot(SCIOSlotTypes.hasApplicationInstrument).getSlotFiller());
				addSingleSlotOnExistance(SCIOSlotTypes.hasElectricFieldStrength, goldAnnotation, newGold);
				addSingleSlotOnExistance(SCIOSlotTypes.hasDosage, goldAnnotation, newGold);
				addSingleSlotOnExistance(SCIOSlotTypes.hasRehabMedication, goldAnnotation, newGold);
				addSingleSlotOnExistance(SCIOSlotTypes.hasTemperature, goldAnnotation, newGold);

				for (AbstractAnnotation sf : goldAnnotation.asInstanceOfEntityTemplate()
						.getMultiFillerSlot(SCIOSlotTypes.hasDirection).getSlotFiller()) {
					newGold.addMultiSlotFiller(SCIOSlotTypes.hasDirection, sf.deepCopy());
				}

				return newGold;
			}
		});
		return rules;

	}

}
