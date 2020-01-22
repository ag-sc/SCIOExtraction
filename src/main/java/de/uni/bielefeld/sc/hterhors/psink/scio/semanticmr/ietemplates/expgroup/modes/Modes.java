package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes;

public class Modes {
	public enum ESimpleEvaluationMode {
		/**
		 * Only vehicle
		 */
		VEHICLE,
		/**
		 * ONLY NON_VEHICLE
		 */
		NON_VEHICLE,
		/**
		 * 
		 */
		BOTH;
	}
	public enum EMainClassMode {

		GOLD, PREDICT, GOLD_PREDICTED;

	}

	public enum EExtractGroupNamesMode {

		/**
		 * Add no GroupNames for at all. Neither for sampling nor for gold data.
		 */
		EMPTY,
		/**
		 * Add GroupName annotations from gold data. GroupNames however are not
		 * co-referenced and assigned to the correct DefindExperimentalGroup.
		 */
		GOLD_UNCLUSTERED,
		/**
		 * Add GroupName annotations from gold data and assign the groupNames to the
		 * correct DefinedExperimentalGroup. If this is chosen, no annotations are
		 * created for GroupNames and the slot "hasgroupNme" is excluded from sampling.
		 */
		GOLD_CLUSTERED,
		/**
		 * Add GroupName annotations from a set of predefined regular expression
		 * pattern. Annotations are not clustered yet, as they are can be wrong.
		 * Annotations however are used during sampling.
		 */
		PATTERN,
		/**
		 * Add GroupName annotations from NP-Chunks extracted with Stanford Core NLP.
		 * Annotations are not clustered yet, as they are can be wrong. Annotations
		 * however are used during sampling.
		 */

		NP_CHUNKS,
		/**
		 * Combine Pattern and NP-Chunks
		 */
		PATTERN_NP_CHUNKS;
	}

	public enum ECardinalityMode {

		SAMPLE_CARDINALITY, GOLD_CARDINALITY, PREDICTED_CARDINALITY, MULTI_CARDINALITIES;

	}

	public enum EAssignmentMode {

		TREATMENT, ALL, ORGANISM_MODEL, INJURY, INJURY_ORGANISM_MODEL, TREATMENT_ORGANISM_MODEL, INJURY_TREATMENT;

	}

	public enum EComplexityMode {
		ROOT, FULL;
	}
}
