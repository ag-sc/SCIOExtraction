package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr;

import de.hterhors.semanticmr.crf.structure.annotations.SlotType;

public class SCIOSlotTypes {

	public static final SlotType hasCompound = getLazy("hasCompound");
	public static final SlotType hasGroupName = getLazy("hasGroupName");
	public static final SlotType hasTreatmentType = getLazy("hasTreatmentType");
	public static final SlotType hasOrganismModel = getLazy("hasOrganismModel");
	public static final SlotType hasInjuryModel = getLazy("hasInjuryModel");

	private static SlotType getLazy(String name) {
		try {
			return SlotType.get(name);
		} catch (Exception e) {
		}
		return null;
	}
}
