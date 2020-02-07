package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates;

import java.io.File;

import de.hterhors.semanticmr.init.reader.csv.CSVDataStructureReader;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.CorpusBuilderBib;

public class DataStructureLoader {

	public static CSVDataStructureReader loadDataStructureReader(String entityTypeName) {

		File entities = CorpusBuilderBib.buildEntitiesFile(entityTypeName);
		File hierarchies = CorpusBuilderBib.buildHierarchiesFile(entityTypeName);
		File slots = CorpusBuilderBib.buildSlotsFile(entityTypeName);
		File structures = CorpusBuilderBib.buildStructuresFile(entityTypeName);

		return new CSVDataStructureReader(entities, hierarchies, slots, structures);
	}

}
