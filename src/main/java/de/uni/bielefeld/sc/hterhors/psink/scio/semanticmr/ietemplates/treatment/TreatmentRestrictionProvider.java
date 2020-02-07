package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.treatment;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor.IModificationRule;

public class TreatmentRestrictionProvider {

	public enum ETreatmentModifications implements IModificationRule {
		ROOT;

	}

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

	public static List<GoldModificationRule> getPlusDeliveryMethod() {
		List<GoldModificationRule> rules = new ArrayList<>();

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation().deepCopy());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasDeliveryMethod, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasDeliveryMethod).getSlotFiller());

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
