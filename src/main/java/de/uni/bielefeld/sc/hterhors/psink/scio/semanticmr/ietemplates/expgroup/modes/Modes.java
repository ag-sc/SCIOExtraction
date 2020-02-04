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

	public enum EGroupNamesPreProcessingMode {
		SAMPLE,

		KMEANS_CLUSTERING,

		WEKA_CLUSTERING,

		GOLD_CLUSTERING;

	}

	public enum EDistinctGroupNamesMode {
		/**
		 * Literals of GroupNames are distinct. Picks first occurrence of any groupName
		 * from the set of same literals.
		 */
		DISTINCT,

		/**
		 * Literals of GroupNames are not distinct. Takes all occurrences.
		 */
		NOT_DISTINCT;
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
		GOLD,
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
		PATTERN_NP_CHUNKS,
		/**
		 * Combine Pattern and NP-Chunks with Gold
		 */
		PATTERN_NP_CHUNKS_GOLD,

	}

	public enum ECardinalityMode {

		SAMPLE_CARDINALITY, GOLD_CARDINALITY, PREDICTED_CARDINALITY, MULTI_CARDINALITIES;

	}

	public enum EAssignmentMode {

		GROUP_NAME, TREATMENT, ALL, ORGANISM_MODEL, INJURY, INJURY_ORGANISM_MODEL, TREATMENT_ORGANISM_MODEL,
		INJURY_TREATMENT;

	}

	public enum EComplexityMode {
		ROOT, FULL;
	}
}
