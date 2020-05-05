package de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper;

import java.io.File;

import de.hterhors.semanticmr.crf.structure.EntityType;

public class AnnotationsCorpusBuilderBib {

	public static final String ANNOTATIONS_DIR_NAME = "/annotations/";
	public static final String RAW_DATA_DIR_NAME = "/current_raw_data/";
	public static final String INSTANCES_DIR_NAME = "/instances/";

	public static final File DATA_DIRECTORY = new File("data/");
	
	public static final String DATA_STRUCTURE_DIR_NAME = "/data_structure/";

	public static final String ENTITIES_FILE_NAME = "entities.csv";
	public static final String SLOTS_FILE_NAME = "slots.csv";
	public static final String STRUCTURES_FILE_NAME = "structures.csv";
	public static final String HIERARCHIES_FILE_NAME = "hierarchies.csv";

	public final static File ANNOTATIONS_DIR = new File(DATA_DIRECTORY, ANNOTATIONS_DIR_NAME);

	public static File getDefaultInstanceDirectoryForEntity(EntityType entityType) {
		final String instancesDirName = toDirName(entityType.name) + AnnotationsCorpusBuilderBib.INSTANCES_DIR_NAME;
		File finalInstancesDir = new File(AnnotationsCorpusBuilderBib.ANNOTATIONS_DIR, instancesDirName);
		return finalInstancesDir;
	}

	public static String toDirName(EntityType entityType) {
		return toDirName(entityType.name);
	}

	public static String toDirName(String entityTypeName) {
		StringBuffer buffer = new StringBuffer();

		String[] parts = entityTypeName.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
		for (int i = 0; i < parts.length - 1; i++) {
			buffer.append(parts[i].toLowerCase());
			buffer.append("_");
		}
		buffer.append(parts[parts.length - 1].toLowerCase());
		return buffer.toString();
	}

}
