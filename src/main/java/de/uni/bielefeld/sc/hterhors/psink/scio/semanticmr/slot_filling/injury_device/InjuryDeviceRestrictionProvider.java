package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury_device;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor.IModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class InjuryDeviceRestrictionProvider {

	public enum EInjuryDeviceModifications implements IModificationRule {
		ROOT,  NO_MODIFICATION;

	}

	private static final SlotType deliveryMethod = SCIOSlotTypes.hasDeliveryMethod;
	private static final SlotType dosageSlot = SCIOSlotTypes.hasDosage;

	public static List<GoldModificationRule> getByRule(EInjuryDeviceModifications modelModifications) {

		switch (modelModifications) {
		case NO_MODIFICATION:
			return Collections.emptyList();
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

				if (!newGold.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
					return null;

				return newGold;
			}
		});
		return rules;

	}


}
