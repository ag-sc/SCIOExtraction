package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthesia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor.IModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class AnaestheticRestrictionProvider {

	public enum EAnaestheticModifications implements IModificationRule {
		ROOT,ROOT_DOSAGE, ROOT_DELIVERY_METHOD, ROOT_DELIVERY_METHOD_DOSAGE, NO_MODIFICATION;

	}

	private static final SlotType deliveryMethod = SCIOSlotTypes.hasDeliveryMethod;
	private static final SlotType dosageSlot = SCIOSlotTypes.hasDosage;

	public static List<GoldModificationRule> getByRule(EAnaestheticModifications modelModifications) {

		switch (modelModifications) {
		case NO_MODIFICATION:
			return Collections.emptyList();
		case ROOT:
			return getRoot();
		case ROOT_DELIVERY_METHOD:
			return getDeliveryMethod();
		case ROOT_DELIVERY_METHOD_DOSAGE:
			return getDosage();
		case ROOT_DOSAGE:
			return getOnlyDosage();
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

	public static List<GoldModificationRule> getDeliveryMethod() {
		List<GoldModificationRule> rules = new ArrayList<>();

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation().deepCopy());

				newGold.setSingleSlotFiller(deliveryMethod, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(deliveryMethod).getSlotFiller());

				if (!newGold.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
					return null;

				if (newGold.asInstanceOfEntityTemplate().isEmpty())
					return null;

				return newGold;
			}
		});
		return rules;

	}

	public static List<GoldModificationRule> getDosage() {
		List<GoldModificationRule> rules = new ArrayList<>();

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation().deepCopy());
				newGold.setSingleSlotFiller(dosageSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(dosageSlot).getSlotFiller());
				newGold.setSingleSlotFiller(deliveryMethod, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(deliveryMethod).getSlotFiller());

				if (!newGold.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
					return null;

				if (newGold.asInstanceOfEntityTemplate().isEmpty())
					return null;

				return newGold;
			}
		});
		return rules;

	}
	public static List<GoldModificationRule> getOnlyDosage() {
		List<GoldModificationRule> rules = new ArrayList<>();
		
		rules.add(new GoldModificationRule() {
			
			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {
				
				EntityTemplate newGold = new EntityTemplate(
						goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation().deepCopy());
				
				newGold.setSingleSlotFiller(dosageSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(dosageSlot).getSlotFiller());
				
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
