package de.uni.bielefeld.sc.hterhors.psink.scio.tools.visualization;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.init.reader.csv.CSVScopeReader;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.visualization.templates.ClassWithDataTypeProperties;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.visualization.templates.ClassWithOutDataTypeProperties;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.visualization.templates.DataTypeClass;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.visualization.templates.GroupNodes;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.visualization.templates.IGraphMLContent;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.visualization.templates.NamedIndividual;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.visualization.templates.ObjectTypePropertyEdge;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.visualization.templates.SubClassEdge;

public class OWL2GraphmlConverter {

	class Triple {
		final String t1;
		final String t2;
		final String t3;

		private Triple(String t1, String t2, String t3) {
			this.t1 = t1;
			this.t2 = t2;
			this.t3 = t3;
		}

	}

	class Tuple {
		final String t1;
		final String t2;

		private Tuple(String t1, String t2) {
			this.t1 = t1;
			this.t2 = t2;
		}

	}

	public static void main(String[] args) throws Exception {

		new OWL2GraphmlConverter();

	}

	private static final File entities = new File("src/main/resources/slotfilling/result/specifications/entities.csv");
	private static final File slots = new File("src/main/resources/slotfilling/result/specifications/slots.csv");
	private static final File structuresFile = new File(
			"src/main/resources/slotfilling/result/specifications/structures.csv");
	private static final File hierarchiesFile = new File(
			"src/main/resources/slotfilling/result/specifications/hierarchies.csv");
	public final static CSVScopeReader systemsScope = new CSVScopeReader(entities, hierarchiesFile, slots,
			structuresFile);

	public OWL2GraphmlConverter() throws IOException, Exception {

		List<IGraphMLContent> listOfUnGroupedContent = new ArrayList<>();

		SystemScope.Builder.getScopeHandler().addScopeSpecification(systemsScope).build();

		/*
		 * vis_group,ClassNames
		 */
		Map<String, Set<String>> visGroups = new HashMap<>();

		visGroups.put("Treatment_Group", new HashSet<>(Arrays.asList("Treatment", "StemCellSourceType")));
		visGroups.put("Investigation_Method_Group", new HashSet<>(Arrays.asList("Function", "Investigation",
				"InvestigationMethod", "Apparatus", "ApplicationInstrument")));
		visGroups.put("Experimental_Design_Group",
				new HashSet<>(Arrays.asList("Experiment", "Publication", "StudyDesign", "Person")));
		visGroups.put("Organism_Group",
				new HashSet<>(Arrays.asList("OrganismModel", "AgeCategory", "Gender", "OrganismSpecies")));
		visGroups.put("Result_Group", new HashSet<>(Arrays.asList("Judgement", "StatisticalTest", "Trend",
				"ObservedDifference", "Significance", "Result")));
		visGroups.put("Location_Group", new HashSet<>(Arrays.asList("Location")));
		visGroups.put("Delivery_Method_Group", new HashSet<>(Arrays.asList("DeliveryMethod")));
		visGroups.put("Observation_Group", new HashSet<>(Arrays.asList("Retransection", "Observation",
				"ObservedParameter", "Sacrifice", "NumericValue", "TemporalInterval")));
		visGroups.put("Injury_Device_Group", new HashSet<>(Arrays.asList("InjuryDevice")));
		visGroups.put("Compound_Treatment_Group",
				new HashSet<>(Arrays.asList("CompoundTreatment", "CompoundSource", "Compound")));
		visGroups.put("Experimental_Group", new HashSet<>(Arrays.asList("Constraint", "ExperimentalGroup")));
		visGroups.put("Injury_Group", new HashSet<>(Arrays.asList("InjuryByAccident", "PostSurgeryCondition", "Injury",
				"AnimalCareCondition", "InjuryArea", "InjuryByAccidentType", "InjuryIntensity")));
		visGroups.put("Direction_Group", new HashSet<>(Arrays.asList("SpatialDimensions")));

		/*
		 * classname, vis_group
		 */
		Map<String, String> classGroups = new HashMap<>();
		Set<String> dataTypeClasses = new HashSet<>();
		Set<String> classesWithoutDataTypeProperties = new HashSet<>();
		Set<String> namedIndividuals = new HashSet<>();
		Map<String, Set<String>> classesWithDataTypeProperties = new HashMap<>();
		Set<Tuple> subClassRelations = new HashSet<>();

		Set<Triple> objectTypeProperties = new HashSet<>();

		List<String[]> structures = structuresFile == null ? Collections.emptyList()
				: Files.readAllLines(structuresFile.toPath()).stream().filter(l -> !l.startsWith("#"))
						.filter(l -> !l.trim().isEmpty()).map(l -> l.split("\t")).collect(Collectors.toList());

		for (String[] d : structures) {

			if (EntityType.get(d[2]).isLiteral)
				continue;
			objectTypeProperties.add(new Triple(d[0], d[1], d[2]));
		}

		for (EntityType entity : EntityType.getEntityTypes()) {

			x: for (Entry<String, Set<String>> vg : visGroups.entrySet()) {
				for (EntityType superE : entity.getTransitiveClosureSuperEntityTypes()) {

					if (vg.getValue().contains(superE.name)) {
						classGroups.put(entity.name, vg.getKey());
						break x;
					}
				}

				if (vg.getValue().contains(entity.name)) {
					classGroups.put(entity.name, vg.getKey());
					break x;
				}
			}

			// ################################

			if (entity.isLiteral) {
				dataTypeClasses.add(entity.name);
			}

			// ################################

			Set<String> dataProps = new HashSet<>();
			for (SlotType s : entity.getSlots()) {
				for (EntityType et : s.getSlotFillerEntityTypes()) {

					if (et.isLiteral) {
						dataProps.add(s.name);
						break;
					}

				}

			}
			if (dataProps.isEmpty())
				classesWithoutDataTypeProperties.add(entity.name);
			else
				classesWithDataTypeProperties.put(entity.name, dataProps);

			// ################################

			if (entity.getTransitiveClosureSubEntityTypes().isEmpty())
				namedIndividuals.add(entity.name);

		}

		List<String[]> hierarchies = hierarchiesFile == null ? Collections.emptyList()
				: Files.readAllLines(hierarchiesFile.toPath()).stream().filter(l -> !l.startsWith("#"))
						.filter(l -> !l.trim().isEmpty()).map(l -> l.split("\t")).collect(Collectors.toList());

		for (String[] d : hierarchies) {
			subClassRelations.add(new Tuple(d[0], d[1]));
		}

		/*
		 * groupname, GroupContent
		 */
		Map<String, List<IGraphMLContent>> groupedContent = new HashMap<>();

		for (String className : classesWithoutDataTypeProperties) {
			IGraphMLContent content = new ClassWithOutDataTypeProperties.Builder(getIDForName(className))
					.setName(className).build();

			if (classGroups.containsKey(className)) {
				final String groupName = classGroups.get(className);
				groupedContent.putIfAbsent(groupName, new ArrayList<>());
				groupedContent.get(groupName).add(content);
			} else {
				listOfUnGroupedContent.add(content);
			}

		}
		for (String className : namedIndividuals) {
			IGraphMLContent content = new NamedIndividual.Builder(getIDForName(className)).setName(className).build();
			if (classGroups.containsKey(className)) {
				final String groupName = classGroups.get(className);
				groupedContent.putIfAbsent(groupName, new ArrayList<>());
				groupedContent.get(groupName).add(content);
			} else {
				listOfUnGroupedContent.add(content);
			}

		}

		for (String className : dataTypeClasses) {
			IGraphMLContent content = new DataTypeClass.Builder(getIDForName(className)).setName(className).build();
			if (classGroups.containsKey(className)) {
				final String groupName = classGroups.get(className);
				groupedContent.putIfAbsent(groupName, new ArrayList<>());
				groupedContent.get(groupName).add(content);
			} else {
				listOfUnGroupedContent.add(content);
			}
		}

		for (Entry<String, Set<String>> c : classesWithDataTypeProperties.entrySet()) {
			final String className = c.getKey();
			List<String> atts = new ArrayList<>(c.getValue());
			Collections.sort(atts);
			IGraphMLContent content = new ClassWithDataTypeProperties.Builder(getIDForName(className))
					.setName(className).setAttributes(atts).build();

			if (classGroups.containsKey(className)) {
				final String groupName = classGroups.get(className);
				groupedContent.putIfAbsent(groupName, new ArrayList<>());
				groupedContent.get(groupName).add(content);
			} else {
				listOfUnGroupedContent.add(content);
			}
		}

		for (Triple t : objectTypeProperties) {
			final String className = t.t1;
			final String relationName = t.t2;
			final String rangeName = t.t3;
			listOfUnGroupedContent.add(
					new ObjectTypePropertyEdge.Builder(relationID++, getIDForName(className), getIDForName(rangeName))
							.setLabel(relationName).build());
		}

		for (Tuple sc : subClassRelations) {
			final String scn = sc.t1;
			final String cn = sc.t2;

			listOfUnGroupedContent
					.add(new SubClassEdge.Builder(relationID++, getIDForName(cn), getIDForName(scn)).build());
		}

		for (Entry<String, List<IGraphMLContent>> grouped : groupedContent.entrySet()) {
			IGraphMLContent gC = new GroupNodes.Builder(getIDForName(grouped.getKey())).setContent(grouped.getValue())
					.setName(grouped.getKey()).build();
			listOfUnGroupedContent.add(gC);

		}

		listOfUnGroupedContent.forEach(System.out::println);
		GraphML graph = new GraphML(listOfUnGroupedContent);
		PrintStream ps = new PrintStream(new File("graphml/scio_v_" + 65 + ".graphml"));
		ps.println(graph);
		ps.close();
	}

	int relationID = 0;

	private Map<String, Integer> nameToIDMap = new HashMap<>();

	private int getIDForName(final String name) {
		final int id = nameToIDMap.getOrDefault(name, nameToIDMap.size());
		nameToIDMap.putIfAbsent(name, id);
		System.out.println("n" + id + " -- " + name);
		return id;
	}

}
