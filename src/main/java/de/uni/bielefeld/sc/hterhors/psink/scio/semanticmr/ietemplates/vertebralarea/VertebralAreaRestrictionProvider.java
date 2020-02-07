package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea;

import java.util.Collections;
import java.util.List;

import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor.IModificationRule;

public class VertebralAreaRestrictionProvider {

	public enum EVertebralAreaModifications implements IModificationRule{
		NO_MODIFICATION;
	}

	public static List<GoldModificationRule> getByRule(EVertebralAreaModifications modelModifications) {

		switch (modelModifications) {
		case NO_MODIFICATION:
			return Collections.emptyList();
		}
		return null;

	}

}
