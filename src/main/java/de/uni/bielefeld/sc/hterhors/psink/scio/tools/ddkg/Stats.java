package de.uni.bielefeld.sc.hterhors.psink.scio.tools.ddkg;

import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class Stats {
//	scio:hasOrganismModel
//	scio:hasInjuryAnaesthesia
//	scio:hasInjuryDevice
//	scio:hasInjuryLocation
//	scio:hasInjuryModel
//	scio:hasDeliveryMethod
//	scio:hasTreatmentType
//	scio:hasInvestigationMethod
	
	public static void main(String[] args) throws Exception {
		Model model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		model.read(new FileInputStream("DDKG/SCIKG_XML_pubmed_clustered.n-triples"), null, "N-TRIPLES");

		Set<String> organismModels = new HashSet<>();
		String queryString = "PREFIX xmls: <http://www.w3.org/2001/XMLSchema#>\n"
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "PREFIX rdfsyntax: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "PREFIX scio: <http://psink.de/scio/>\n" + "\n" + "select distinct ?s ?p ?o where {\n"
				+ " ?a scio:hasInvestigationMethod ?location . ?location (<>|!<>)* ?s .\n" + "  ?s ?p ?o .\n" + "}\n";
		Query query = QueryFactory.create(queryString);
		System.out.println(query);

		int i = 0;
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();
			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();

				try {
					System.out.println(querySolution.getResource("s") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getResource("o"));
					organismModels.add(querySolution.getResource("s") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getResource("o"));
					i++;
				} catch (Exception e) {
					organismModels.add(querySolution.getResource("s") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getLiteral("o"));
					System.out.println(querySolution.getResource("s") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getLiteral("o"));
					i++;
				}

			}
		}
		String queryString2 = "PREFIX xmls: <http://www.w3.org/2001/XMLSchema#>\n"
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "PREFIX rdfsyntax: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "PREFIX scio: <http://psink.de/scio/>\n" + "\n" + "select distinct ?s ?p ?o where {\n"
				+ " ?s scio:hasInvestigationMethod ?o . FILTER(STRSTARTS(STR(?o), \"http://psink.de/scio/\"))\n" + 
				"\n" + "}\n";
		Query query2 = QueryFactory.create(queryString2);
		System.out.println(query2);
		
		try (QueryExecution qexec = QueryExecutionFactory.create(query2, model)) {
			ResultSet results = qexec.execSelect();
			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();
				
				try {
					System.out.println(querySolution.getResource("s") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getResource("o"));
					organismModels.add(querySolution.getResource("s") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getResource("o"));
					i++;
//					organismModels.add(querySolution.getResource("dm2").toString());
				} catch (Exception e) {
					organismModels.add(querySolution.getResource("s") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getLiteral("o"));
					System.out.println(querySolution.getResource("s") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getLiteral("o"));
					i++;
//					System.out.println(querySolution.getLiteral("t"));
				}
				
			}
		}
		System.out.println(i);
		System.out.println(organismModels.size());

	}

}
