package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr;

import java.io.File;

import de.hterhors.semanticmr.init.reader.csv.CSVDataStructureReader;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;

public class DataStructureLoader {

	public static CSVDataStructureReader loadSlotFillingDataStructureReader(String entityTypeName) {

		File entities = SlotFillingCorpusBuilderBib.buildEntitiesFile(entityTypeName);
		File hierarchies = SlotFillingCorpusBuilderBib.buildHierarchiesFile(entityTypeName);
		File slots = SlotFillingCorpusBuilderBib.buildSlotsFile(entityTypeName);
		File structures = SlotFillingCorpusBuilderBib.buildStructuresFile(entityTypeName);

		return new CSVDataStructureReader(entities, hierarchies, slots, structures);
	}

	public static CSVDataStructureReader loadNERDataStructureReader(String entityTypeName) {

		File entities = NERCorpusBuilderBib.buildEntitiesFile(entityTypeName);
		File hierarchies = NERCorpusBuilderBib.buildHierarchiesFile(entityTypeName);
		File slots = NERCorpusBuilderBib.buildSlotsFile(entityTypeName);
		File structures = NERCorpusBuilderBib.buildStructuresFile(entityTypeName);

		return new CSVDataStructureReader(entities, hierarchies, slots, structures);
	}

}
