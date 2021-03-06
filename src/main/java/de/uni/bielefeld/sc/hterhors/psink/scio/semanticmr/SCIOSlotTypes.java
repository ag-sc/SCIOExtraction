package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr;

import de.hterhors.semanticmr.crf.structure.annotations.SlotType;

public class SCIOSlotTypes {

	public static final SlotType hasOrganismModel = getLazy("hasOrganismModel");
	public static final SlotType hasOrganismSpecies = getLazy("hasOrganismSpecies");
	public static final SlotType hasGender = getLazy("hasGender");
	public static final SlotType hasAgeCategory = getLazy("hasAgeCategory");
	public static final SlotType hasAge = getLazy("hasAge");
	public static final SlotType hasWeight = getLazy("hasWeight");

	public static final SlotType hasCompound = getLazy("hasCompound");
	public static final SlotType hasGroupName = getLazy("hasGroupName");
	public static final SlotType hasTreatmentType = getLazy("hasTreatmentType");

	public static final SlotType hasApplicationInstrument = getLazy("hasApplicationInstrument");
	public static final SlotType hasDeliveryMethod = getLazy("hasDeliveryMethod");
	public static final SlotType hasDuration = getLazy("hasDuration");

	public static final SlotType hasElectricFieldStrength = getLazy("hasElectricFieldStrength");
	public static final SlotType hasVoltage = getLazy("hasVoltage");
	public static final SlotType hasRehabMedication = getLazy("hasRehabMedication");
	public static final SlotType hasDirection = getLazy("hasDirection");
	public static final SlotType hasDosage = getLazy("hasDosage");
	public static final SlotType hasTemperature = getLazy("hasTemperature");

	public static final SlotType hasInjuryModel = getLazy("hasInjuryModel");
	public static final SlotType hasInjuryDevice = getLazy("hasInjuryDevice");

	public static final SlotType hasVolume = getLazy("hasVolume");
	public static final SlotType hasForce = getLazy("hasForce");
	public static final SlotType hasDistance = getLazy("hasDistance");

	public static final SlotType hasLocations = getLazy("hasLocations");
	public static final SlotType hasLocation = getLazy("hasLocation");
	public static final SlotType hasUpperVertebrae = getLazy("hasUpperVertebrae");
	public static final SlotType hasLowerVertebrae = getLazy("hasLowerVertebrae");
	public static final SlotType hasAnaesthesia = getLazy("hasInjuryAnaesthesia");
	public static final SlotType hasInjuryIntensity = getLazy("hasInjuryIntensity");

	public static final SlotType hasNNumber = getLazy("hasNNumber");
	public static final SlotType hasTotalPopulationSize = getLazy("hasTotalPopulationSize");
	public static final SlotType hasGroupNumber = getLazy("hasGroupNumber");
	public static final SlotType hasEventBefore = getLazy("hasEventBefore");
	public static final SlotType hasEventAfter = getLazy("hasEventAfter");

	public static final SlotType hasInjuryLocation = getLazy("hasInjuryLocation");;
	public static final SlotType hasInjuryAnaesthesia = getLazy("hasInjuryAnaesthesia");
	public static final SlotType hasTrend = getLazy("hasTrend");
	public static final SlotType hasJudgement = getLazy("hasJudgement");
	public static final SlotType hasSignificance = getLazy("hasSignificance");
	public static final SlotType hasDifference = getLazy("hasDifference");
	public static final SlotType hasAlphaSignificanceNiveau = getLazy("hasAlphaSignificanceNiveau");
	public static final SlotType hasPValue = getLazy("hasPValue");

	public static final SlotType hasInvestigationMethod = getLazy("hasInvestigationMethod");
	public static final SlotType hasTargetGroup = getLazy("hasTargetGroup");
	public static final SlotType hasReferenceGroup = getLazy("hasReferenceGroup");
	public static final SlotType belongsTo = getLazy("belongsTo");

	private static SlotType getLazy(String name) {
		try {
			return SlotType.get(name);
		} catch (Exception e) {
		}
		return null;
	}
}
