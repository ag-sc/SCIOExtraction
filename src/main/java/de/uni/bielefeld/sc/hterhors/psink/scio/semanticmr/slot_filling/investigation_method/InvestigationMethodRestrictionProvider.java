package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.investigation_method;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor.IModificationRule;

public class InvestigationMethodRestrictionProvider {

	public enum EInvestigationMethodModifications implements IModificationRule {
		ROOT;

	}

	public static List<GoldModificationRule> getByRule(EInvestigationMethodModifications modelModifications) {

		switch (modelModifications) {
		case ROOT:
			return getRoot();
		}
		return null;

	}

	public static List<GoldModificationRule> getRoot() {
		List<GoldModificationRule> rules = new ArrayList<>();

		return rules;

	}


}
