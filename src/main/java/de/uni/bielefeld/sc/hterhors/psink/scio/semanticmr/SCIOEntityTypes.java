package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr;

import de.hterhors.semanticmr.crf.structure.EntityType;

public class SCIOEntityTypes {

	public static final EntityType compoundTreatment = getLazy("CompoundTreatment");
	public static final EntityType definedExperimentalGroup = getLazy("DefinedExperimentalGroup");
	public static final EntityType compound = getLazy("Compound");
	public static final EntityType treatment = getLazy("Treatment");

	private static EntityType getLazy(String name) {
		try {
			return EntityType.get(name);
		} catch (Exception e) {
		}
		return null;
	}
}
