package de.uni.bielefeld.sc.hterhors.psink.scio.tools.owl.owlreader;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SCIOEnvironment {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static SCIOEnvironment instance = new SCIOEnvironment();

	private final String ontologyBasePackage = "de.uni.bielefeld.sc.hterhors.psink.obie.projects.scio.ontology.";

	public static int version;
	public static File ONTOLOGY_FILE;
	public static final String DEFAULT_DATA_NAMESPACE = "http://scio/data/";
	public static final String ONTOLOGY_NAME = "SCIO";

	public int getOntologyVersion() {
		return version;
	}

	public String getDataNameSpace() {
		return DEFAULT_DATA_NAMESPACE;
	}

	public File getOntologyFile() {
		return ONTOLOGY_FILE;
	}

	public static SCIOEnvironment getInstance(File owlFile, int version) {
		SCIOEnvironment.ONTOLOGY_FILE = owlFile;
		SCIOEnvironment.version = version;
		return instance;
	}

	public String getOntologyName() {
		return ONTOLOGY_NAME;
	}

	public IClassFilter getOwlClassFilter() {
		return new IClassFilter() {

			@Override
			public boolean matchesCondition(OntologyClass ocd) {

				if (ocd.documentation.containsKey("clinicalTerm")
						&& ocd.documentation.get("clinicalTerm").get(0).equals("true"))
					return false;

				return recSuperClassCheck(ocd);

			}

			private boolean recSuperClassCheck(OntologyClass ocd) {
				for (OntologyClass superClass : ocd.superclasses) {
					boolean b = matchesCondition(superClass);
					if (!b)
						return false;
				}
				return true;
			}
		};
	}

	public Set<String> getCollectiveClasses() {
		return Collections.unmodifiableSet(new HashSet<>(Arrays.asList("http://psink.de/scio/Event",
				"http://data.nasa.gov/qudt/owl/qudt#Quantity", "http://www.w3.org/2006/time#TemporalEntity")));
	}

	public String getAdditionalPrefixes() {
		return "PREFIX scio: <http://psink.de/scio/>" + "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>";
	}

	public List<String> getAdditionalPropertyNames() {
		return additionalInfoVariableNames;
	}

	final static String VARIABLE_NAME_EXAMPLE = "scio:example";
	final static String VARIABLE_NAME_EXAMPLE_SOURCE = "scio:exampleSource";
	final static String VARIABLE_NAME_LABEL = "rdfs:label";
	final static String VARIABLE_NAME_CLINICAL_TERM = "scio:clinicalTerm";
	final static String VARIABLE_NAME_VISUALIZATION_CONTAINER = "scio:visualizationContainer";
	final static String VARIABLE_NAME_DESCRIPTION_SOURCE = "scio:descriptionSource";
	final static String VARIABLE_NAME_DESCRIPTION = "rdfs:description";
	final static String VARIABLE_NAME_COMMENT = "rdfs:comment";
	final static String VARIABLE_NAME_CLOSE_MATCH = "skos:closeMatch";

	final static List<String> additionalInfoVariableNames = Arrays.asList(VARIABLE_NAME_EXAMPLE,
			VARIABLE_NAME_EXAMPLE_SOURCE, VARIABLE_NAME_LABEL, VARIABLE_NAME_CLINICAL_TERM,
			VARIABLE_NAME_VISUALIZATION_CONTAINER, VARIABLE_NAME_DESCRIPTION_SOURCE, VARIABLE_NAME_COMMENT,
			VARIABLE_NAME_DESCRIPTION, VARIABLE_NAME_CLOSE_MATCH);

	public String getBasePackage() {
		return ontologyBasePackage;
	}

}
