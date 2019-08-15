package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.investigationmethod.specs;

import java.io.File;

import de.hterhors.semanticmr.init.reader.csv.CSVScopeReader;

public class InvestigationMethodSpecs {
	/**
	 * The file that contains specifications about entities.
	 */
	private static final File entities = new File(
			"src/main/resources/slotfilling/investigationmethod/specifications/entities.csv");

	/**
	 * Specification file that contains information about slots.
	 **/
	private static final File slots = new File(
			"src/main/resources/slotfilling/investigationmethod/specifications/slots.csv");
	/**
	 * Specification file that contains information about slots of entities.
	 **/
	private static final File structures = new File(
			"src/main/resources/slotfilling/investigationmethod/specifications/structures.csv");

	/**
	 * Specification file of entity hierarchies.
	 */
	private static final File hierarchies = new File(
			"src/main/resources/slotfilling/investigationmethod/specifications/hierarchies.csv");

	public final static CSVScopeReader systemsScopeReader = new CSVScopeReader(entities, hierarchies, slots,
			structures);
}
