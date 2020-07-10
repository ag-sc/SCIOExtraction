package de.uni.bielefeld.sc.hterhors.psink.scio.tools.specifications;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni.bielefeld.sc.hterhors.psink.scio.tools.owl.OWLToSANTOConverter;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.owl.owlreader.SCIOEnvironment;

public class OWL2SpecsFormat {

	final public static int version = 65;

//	public static void main(String[] args) throws IOException {
//		new OWL2SpecsFormat(new File("test/specs_format"),
//				new File("src/main/resources/scio/SCIO_" + version + ".owl"));
//	}

	File parentCSVDirectory;

	public File entitiesFile;
	public File hierarchiesFile;
	public File structuresFile;
	public File slotsFile;

	public OWL2SpecsFormat(File parentCSVDirectory, File owlFile) throws IOException {

		this.parentCSVDirectory = parentCSVDirectory;
		this.entitiesFile = new File(parentCSVDirectory, "entities.csv");
		this.hierarchiesFile = new File(parentCSVDirectory, "hierachies.csv");
		this.structuresFile = new File(parentCSVDirectory, "structures.csv");
		this.slotsFile = new File(parentCSVDirectory, "slots.csv");

		OWLToSANTOConverter x = new OWLToSANTOConverter(SCIOEnvironment.getInstance(owlFile, version));
		parentCSVDirectory.mkdirs();

		List<String> entities = x.toClassesFile();

		List<String> hierachies = x.toSubClassesFile();

		List<String> slots = x.toRelationsFile();

		convertToEntitiesSpecsFile(entities, slots, hierachies);
		convertToHierachiesSpecsFile(hierachies);
		convertToStructuresSpecsFile(slots);
		convertToSlotsSpecsFile(slots);

	}

	private void convertToEntitiesSpecsFile(List<String> entities, List<String> slots, List<String> hierachies)
			throws IOException {

		Map<String, String> qudtMap = new HashMap<>();

		for (String line : hierachies) {
			if (line.startsWith("#"))
				continue;

			String[] data = line.split("\t");
			qudtMap.put(data[1], data[0]);
		}

		Map<String, Boolean> map = new HashMap<>();

		for (String line : slots) {
			if (line.startsWith("#"))
				continue;
			String[] data = line.split("\t");

			map.put(data[2], (qudtMap.containsKey(data[2]) && qudtMap.get(data[2]).equals("qudt#Quantity"))
					|| new Boolean(data[5]));
		}

		PrintStream ps = new PrintStream(entitiesFile);

		ps.println("#Class\tIsLiteral");

		for (String line : entities) {
			if (line.startsWith("#"))
				continue;

			String[] data = line.split("\t");

			boolean isLiteral = map.containsKey(data[0]) && map.get(data[0]);

			String d = data[0] + "\t" + isLiteral;

			ps.println(d);

		}

		ps.close();

	}

	private void convertToHierachiesSpecsFile(List<String> hierachies) throws IOException {

		PrintStream ps = new PrintStream(hierarchiesFile);
		ps.println("#SuperClass	SubClass");

		for (String line : hierachies) {
			if (line.startsWith("#"))
				continue;

			ps.println(line);

		}

		ps.close();

	}

	private void convertToStructuresSpecsFile(List<String> slots) throws IOException {

		PrintStream ps = new PrintStream(structuresFile);
		ps.println("#Class\tSlot\\tSlotSuperClassType");
		Set<String> ds = new HashSet<>();

		for (String line : slots) {
			if (line.startsWith("#"))
				continue;

			String[] data = line.split("\t");

			String d = data[0] + "\t" + data[1] + "\t" + data[2];
			ds.add(d);
		}

		ds.forEach(ps::println);
		ps.close();

	}

	private void convertToSlotsSpecsFile(List<String> slots) throws IOException {

		PrintStream ps = new PrintStream(slotsFile);
		ps.println("#Slot\tMaxCardinality");

		Set<String> ds = new HashSet<>();
		for (String line : slots) {
			if (line.startsWith("#"))
				continue;

			String[] data = line.split("\t");

			String d = data[1] + "\t" + (data[4].equals("m") ? "-1" : "1");
			ds.add(d);

		}
		ds.forEach(ps::println);

		ps.close();

	}

}
