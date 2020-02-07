package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.investigation_method;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
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
