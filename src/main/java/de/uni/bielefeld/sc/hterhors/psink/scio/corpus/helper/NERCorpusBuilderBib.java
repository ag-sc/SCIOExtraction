package de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper;

import java.io.File;

import de.hterhors.semanticmr.crf.structure.EntityType;

public class NERCorpusBuilderBib {

	public static final String NER_DIR_NAME = "/ner/";
	public static final String RAW_DATA_DIR_NAME = "/current_raw_data/";
	public static final String DATA_STRUCTURE_DIR_NAME = "/data_structure/";
	public static final String INSTANCES_DIR_NAME = "/instances/";

	public static final String ENTITIES_FILE_NAME = "entities.csv";
	public static final String SLOTS_FILE_NAME = "slots.csv";
	public static final String STRUCTURES_FILE_NAME = "structures.csv";
	public static final String HIERARCHIES_FILE_NAME = "hierarchies.csv";
	public static final File DATA_DIRECTORY = new File("data/");

	public final static File NER_DIR = new File(DATA_DIRECTORY, NER_DIR_NAME);

	public static File getDefaultInstanceDirectoryForEntity(EntityType entityType) {
		final String instancesDirName = toDirName(entityType.name) + NERCorpusBuilderBib.INSTANCES_DIR_NAME;
		File finalInstancesDir = new File(NERCorpusBuilderBib.NER_DIR, instancesDirName);
		return finalInstancesDir;
	}

	public static File buildEntitiesFile(EntityType entityType) {
		return buildEntitiesFile(entityType.name);
	}

	public static File buildHierarchiesFile(EntityType entityType) {
		return buildHierarchiesFile(entityType.name);
	}

	public static File buildStructuresFile(EntityType entityType) {
		return buildStructuresFile(entityType.name);
	}

	public static File buildSlotsFile(EntityType entityType) {
		return buildSlotsFile(entityType.name);
	}

	public static File buildEntitiesFile(String rootEntityTypeName) {
		final String dataStructureDirName = toDirName(rootEntityTypeName) + DATA_STRUCTURE_DIR_NAME;
		File finalDataStructureDir = new File(NER_DIR, dataStructureDirName);
		return new File(finalDataStructureDir, ENTITIES_FILE_NAME);
	}

	public static File buildHierarchiesFile(String rootEntityTypeName) {
		final String dataStructureDirName = toDirName(rootEntityTypeName) + DATA_STRUCTURE_DIR_NAME;
		File finalDataStructureDir = new File(NER_DIR, dataStructureDirName);
		return new File(finalDataStructureDir, HIERARCHIES_FILE_NAME);
	}

	public static File buildStructuresFile(String rootEntityTypeName) {
		final String dataStructureDirName = toDirName(rootEntityTypeName) + DATA_STRUCTURE_DIR_NAME;
		File finalDataStructureDir = new File(NER_DIR, dataStructureDirName);
		return new File(finalDataStructureDir, STRUCTURES_FILE_NAME);
	}

	public static File buildSlotsFile(String rootEntityTypeName) {
		final String dataStructureDirName = toDirName(rootEntityTypeName) + DATA_STRUCTURE_DIR_NAME;
		File finalDataStructureDir = new File(NER_DIR, dataStructureDirName);
		return new File(finalDataStructureDir, SLOTS_FILE_NAME);
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
