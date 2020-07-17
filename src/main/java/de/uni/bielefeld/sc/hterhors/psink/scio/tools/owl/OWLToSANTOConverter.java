package de.uni.bielefeld.sc.hterhors.psink.scio.tools.owl;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import de.uni.bielefeld.sc.hterhors.psink.scio.tools.owl.owlreader.ECardinalityType;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.owl.owlreader.JavaClassNamingTools;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.owl.owlreader.OWLReader;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.owl.owlreader.OntologyClass;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.owl.owlreader.OntologySlotData;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.owl.owlreader.SCIOEnvironment;

public class OWLToSANTOConverter {

	private static final List<String> DEFAULT_DESCRIPTION = Arrays.asList("No description provided");
	private static final Object DOCUMENTATION_DESCRIPTION_IDENTIFIER = "http://www.w3.org/2000/01/rdf-schema#description";
	private static final String SPLITTER = "\t";

	private final OWLReader dataProvider;

	SCIOEnvironment environment;

	public OWLToSANTOConverter(SCIOEnvironment environment) {

		this.environment = environment;

		this.dataProvider = new OWLReader(environment);

	}

	public void writeRelationsFile(File parentDirectory, List<String> relationsFile) throws IOException {
		File parent = new File(parentDirectory, "version" + environment.getOntologyVersion() + "/");

		if (!parent.exists())
			parent.mkdirs();
		PrintStream ps = new PrintStream(
				parent.getPath() + "/scio_v_" + environment.getOntologyVersion() + "_relations.csv");

		ps.println("#domainClass" + SPLITTER + "relation" + SPLITTER + "rangeClass" + SPLITTER + "from" + SPLITTER
				+ "to" + SPLITTER + "isDataTypeProperty" + SPLITTER + "mergedName" + SPLITTER + "description");

		relationsFile.forEach(ps::println);

		ps.close();

	}

	public void writeSubClassesFile(File parentDirectory, List<String> subClassesFilen) throws IOException {
		File parent = new File(parentDirectory, "version" + environment.getOntologyVersion() + "/");

		if (!parent.exists())
			parent.mkdirs();
		PrintStream ps = new PrintStream(
				parent.getPath() + "/scio_v_" + environment.getOntologyVersion() + "_subclasses.csv");

		ps.println("#superClass" + SPLITTER + "subClass");

		subClassesFilen.forEach(ps::println);
		ps.close();

	}

	public void writeClassesFile(File parentDirectory, List<String> classesFile) throws IOException {
		File parent = new File(parentDirectory, "version" + environment.getOntologyVersion() + "/");

		if (!parent.exists())
			parent.mkdirs();
		PrintStream ps = new PrintStream(
				parent.getPath() + "/scio_v_" + environment.getOntologyVersion() + "_classes.csv");

		ps.println("#class" + SPLITTER + "isNamedIndividual" + SPLITTER + "Description");
		classesFile.forEach(ps::println);

		ps.close();
	}

	public List<String> toClassesFile() {

		List<String> lines = new ArrayList<>();

		for (OntologyClass dataClass : dataProvider.classes) {

			final String description;

			if (!dataClass.documentation.isEmpty()) {
				if (dataClass.documentation.containsKey(DOCUMENTATION_DESCRIPTION_IDENTIFIER)) {

					description = docToString(dataClass.documentation.get(DOCUMENTATION_DESCRIPTION_IDENTIFIER));
				} else {
					System.err.println("WARN! No description found for class: " + dataClass.ontologyClassName);
					description = DEFAULT_DESCRIPTION.get(0);
				}
			} else {
				System.err.println("WARN! Class not found in ontology: " + dataClass.ontologyClassName);
				description = DEFAULT_DESCRIPTION.get(0);
			}
			boolean isNamedIndividual = dataClass.isNamedIndividual;// dataProvider.namedIndividuals.contains(dataClass);
			lines.add(dataClass.ontologyClassName + "" + SPLITTER + "" + (isNamedIndividual ? "true" : "false") + ""
					+ SPLITTER + description);
		}
		return lines;
	}

	public List<String> toSubClassesFile() {
		List<String> lines = new ArrayList<>();

		for (OntologyClass superClass : dataProvider.classes) {

			if (superClass.subclasses.isEmpty())
				continue;

			// }
			// for (Entry<DataClass, Set<DataClass>> superClass :
			// dataProvider.subclasses.entrySet()) {

			for (OntologyClass subClass : superClass.subclasses) {

				lines.add(superClass.ontologyClassName + "" + SPLITTER + "" + subClass.ontologyClassName);

			}
		}

		return lines;

	}

	public List<String> toRelationsFile() {
		List<String> lines = new ArrayList<>();

		for (OntologyClass domain : dataProvider.classes) {

			if (domain.domainRangeRelations.isEmpty())
				continue;

			for (Entry<OntologySlotData, Set<OntologyClass>> rangeRelations : domain.domainRangeRelations.entrySet()) {

				ECardinalityType cardinality = rangeRelations.getKey().cardinalityType;
				cardinality = cardinality == null ? ECardinalityType.UNDEFINED : cardinality;
				for (OntologyClass range : rangeRelations.getValue()) {
					lines.add(domain.ontologyClassName + "" + SPLITTER + ""
							+ rangeRelations.getKey().ontologyPropertyName + "" + SPLITTER + ""
							+ range.ontologyClassName + "" + SPLITTER + "" + cardinality.simpleName.split(":")[0] + ""
							+ SPLITTER + "" + cardinality.simpleName.split(":")[1] + "" + SPLITTER + ""
							/**
							 * TODO: data type is buggy for SCIo because weight etc extends QUDT quantity
							 * but should be datatype classes.
							 */

							+ (range.isDataType ? "true" : "false") + "" + SPLITTER + ""
							+ (cardinality == ECardinalityType.SINGLE || cardinality == ECardinalityType.UNDEFINED
									? JavaClassNamingTools.combineRelationWithClassNameAsClassName(
											rangeRelations.getKey().javaClassPropertyName, range.ontologyClassName)
									: JavaClassNamingTools.combineRelationWithClassNameAsPluralClassName(
											rangeRelations.getKey().javaClassPropertyName, range.ontologyClassName))
							+ "" + SPLITTER + "" + docToString(rangeRelations.getKey().documentation
									.getOrDefault(DOCUMENTATION_DESCRIPTION_IDENTIFIER, DEFAULT_DESCRIPTION)));
				}
			}
		}
		return lines;
	}

	/**
	 * Converts documentation to simple String. If a doc type has multiple values
	 * they are concatenated with <code>num</code> +")"
	 * 
	 * @param docs
	 * @return
	 */
	private String docToString(List<String> docs) {

		StringBuilder docBuilder = new StringBuilder();
		Iterator<String> docIt = docs.iterator();
		int num = 1;
		while (docIt.hasNext()) {
			docBuilder.append(docIt.next().replaceAll("\\s", " "));
			if (docIt.hasNext())
				docBuilder.append(num + ")");
			num++;
		}
		return docBuilder.toString();

	}

}
