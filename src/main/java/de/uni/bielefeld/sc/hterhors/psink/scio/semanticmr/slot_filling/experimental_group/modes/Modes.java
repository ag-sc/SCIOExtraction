package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes;

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

		GOLD, SAMPLE, PRE_PREDICTED;

	}

	public enum EGroupNamesClusteringMode {
		NONE,

		KMEANS_CLUSTERING,

		WEKA_CLUSTERING,

		GOLD_CLUSTERING;

	}

	public enum EDistinctGroupNamesMode {
		/**
		 * Literals of GroupNames are distinct. Picks first occurrence of any groupName
		 * from the set of same literals.
		 */
		STRING_DISTINCT,

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

		PREDICTED,
		/**
		 * Add GroupName annotations from gold data. GroupNames however are not
		 * co-referenced and assigned to the correct DefindExperimentalGroup.
		 */
		GOLD,

		/**
		 * Constructs pattern from the training data and applies these to the documents.
		 */
		TRAINING_PATTERN,
		/**
		 * Add GroupName annotations from a set of predefined regular expression
		 * pattern. Annotations are not clustered yet, as they are can be wrong.
		 * Annotations however are used during sampling.
		 */
		MANUAL_PATTERN,
		/**
		 * Add GroupName annotations from NP-Chunks extracted with Stanford Core NLP.
		 * Annotations are not clustered yet, as they are can be wrong. Annotations
		 * however are used during sampling.
		 */

		NP_CHUNKS,
		/**
		 * Combine Pattern and NP-Chunks
		 */
		MANUAL_PATTERN_NP_CHUNKS,
		/**
		 * Combine training pattern and NP-Chunks
		 */
		TRAINING_PATTERN_NP_CHUNKS,
		/**
		 * Combine Pattern and NP-Chunks
		 */
		TRAINING_MANUAL_PATTERN,
		/**
		 * Combine training and manual Pattern and NP-Chunks
		 */
		TRAINING_MANUAL_PATTERN_NP_CHUNKS,

	}

	public enum ECardinalityMode {

		SAMPLE, GOLD, RSS_PREDICTED, RSS_PREDICTED_SAMPLE, PARALLEL, PARALLEL_MODEL_UPDATE;

	}

	public enum EAssignmentMode {

		GROUP_NAME, TREATMENT, TREATMENT_ORGANISM_MODEL_INJURY, ORGANISM_MODEL, INJURY, INJURY_ORGANISM_MODEL,
		TREATMENT_ORGANISM_MODEL, INJURY_TREATMENT;

	}

	public enum EComplexityMode {
		ROOT, FULL;
	}
}
