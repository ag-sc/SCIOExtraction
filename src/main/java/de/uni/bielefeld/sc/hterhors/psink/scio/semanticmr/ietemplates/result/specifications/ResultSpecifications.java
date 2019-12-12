package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.result.specifications;

import java.io.File;

import de.hterhors.semanticmr.init.reader.csv.CSVScopeReader;

public class ResultSpecifications {
	public static final File entities = new File("src/main/resources/slotfilling/result/specifications/entities.csv");
	public static final File slots = new File("src/main/resources/slotfilling/result/specifications/slots.csv");
	public static final File structures = new File(
			"src/main/resources/slotfilling/result/specifications/structures.csv");
	public static final File hierarchies = new File(
			"src/main/resources/slotfilling/result/specifications/hierarchies.csv");

	public final static CSVScopeReader systemsScope = new CSVScopeReader(entities, hierarchies, slots, structures);

}
