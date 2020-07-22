package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr;

import de.hterhors.semanticmr.crf.structure.EntityType;

public class SCIOEntityTypes {

	public static final EntityType experimentalGroup = getLazy("ExperimentalGroup");
	public static final EntityType compoundTreatment = getLazy("CompoundTreatment");
	public static final EntityType definedExperimentalGroup = getLazy("DefinedExperimentalGroup");
	public static final EntityType analizedExperimentalGroup = getLazy("AnalyzedExperimentalGroup");
	public static final EntityType compound = getLazy("Compound");
	public static final EntityType treatment = getLazy("Treatment");
	public static final EntityType species = getLazy("OrganismSpecies");
	public static final EntityType organismModel = getLazy("OrganismModel");
	public static final EntityType injury = getLazy("Injury");
	public static final EntityType vehicle = getLazy("Vehicle");
	public static final EntityType groupName = getLazy("GroupName");
	public static final EntityType vertebralArea = getLazy("VertebralArea");
	public static final EntityType deliveryMethod = getLazy("DeliveryMethod");
	public static final EntityType investigationMethod = getLazy("InvestigationMethod");
	public static final EntityType observation = getLazy("Observation");
	public static final EntityType result = getLazy("Result");
	public static final EntityType trend = getLazy("Trend");
	public static final EntityType significance = getLazy("Significance");
	public static final EntityType pValue = getLazy("PValue");
	public static final EntityType observedDifference = getLazy("ObservedDifference");
	public static final EntityType anaesthetic = getLazy("Anaesthetic");
	public static final EntityType injuryDevice = getLazy("InjuryDevice");
	public static final EntityType vertebralLocation = getLazy("VertebralLocation");

	private static EntityType getLazy(String name) {
		try {
			return EntityType.get(name);
		} catch (Exception e) {
		}
		return null;
	}
}
