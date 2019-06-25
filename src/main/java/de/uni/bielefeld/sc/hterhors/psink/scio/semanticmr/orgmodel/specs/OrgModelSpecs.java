package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.orgmodel.specs;

import java.io.File;

import de.hterhors.semanticmr.init.reader.csv.CSVScopeReader;

public class OrgModelSpecs {
//	/**
//	 * The file that contains specifications about entities.
//	 */
//	private static final File entities = new File("src/main/resources/slotfilling/result/specifications/entities.csv");
//
//	/**
//	 * Specification file that contains information about slots.
//	 **/
//	private static final File slots = new File("src/main/resources/slotfilling/result/specifications/slots.csv");
//	/**
//	 * Specification file that contains information about slots of entities.
//	 **/
//	private static final File structures = new File(
//			"src/main/resources/slotfilling/result/specifications/structures.csv");
//
//	/**
//	 * Specification file of entity hierarchies.
//	 */
//	private static final File hierarchies = new File(
//			"src/main/resources/slotfilling/result/specifications/hierarchies.csv");
//
//	public final static CSVScopeReader systemsScopeReader = new CSVScopeReader(entities, hierarchies, slots,
//			structures);
	/**
	 * The file that contains specifications about entities.
	 */
	private static final File entities = new File("src/main/resources/slotfilling/organism_model/specifications/entities.csv");
	
	/**
	 * Specification file that contains information about slots.
	 **/
	private static final File slots = new File("src/main/resources/slotfilling/organism_model/specifications/slots.csv");
	/**
	 * Specification file that contains information about slots of entities.
	 **/
	private static final File structures = new File(
			"src/main/resources/slotfilling/organism_model/specifications/structures.csv");
	
	/**
	 * Specification file of entity hierarchies.
	 */
	private static final File hierarchies = new File(
			"src/main/resources/slotfilling/organism_model/specifications/hierarchies.csv");
	
	public final static CSVScopeReader systemsScopeReader = new CSVScopeReader(entities, hierarchies, slots,
			structures);
}
