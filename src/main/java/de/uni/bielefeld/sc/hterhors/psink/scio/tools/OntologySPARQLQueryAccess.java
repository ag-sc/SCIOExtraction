package de.uni.bielefeld.sc.hterhors.psink.scio.tools;

import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class OntologySPARQLQueryAccess {

	public static void main(String[] args) throws Exception {

		String path = "src/main/resources/scio/SCIO_65.owl";
		Model model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		model.read(path);
//		model.write(System.out);

		String queryString2 = "PREFIX xmls: <http://www.w3.org/2001/XMLSchema#>\n" + 
				"PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" + 
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + 
				"PREFIX rdfsyntax: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + 
				"PREFIX scio: <http://psink.de/scio/>\n" + 
				"\n" + 
				"SELECT\n" + 
				"    ?pubmedID ?result ?judgement ?target\n" + 
				"WHERE {\n" + 
				"    ?publication <http://psink.de/scio/hasPubmedID> ?pubmedID .\n" + 
				"    ?publication a <http://psink.de/scio/Publication> .\n" + 
				"    ?publication <http://psink.de/scio/describes> ?experiment .\n" + 
				"    ?experiment <http://psink.de/scio/hasResult> ?result .\n" + 
				"    ?result <http://psink.de/scio/hasInvestigationMethod> ?investigationMethodInstance .\n" + 
				"    ?result <http://psink.de/scio/hasJudgement> ?judgementInstance .\n" + 
				"    ?judgementInstance a ?judgement .\n" + 
				"    ?investigationMethodInstance a/rdfs:subClassOf* <http://psink.de/scio/FunctionalTest> .\n" + 
				"    ?result <http://psink.de/scio/hasTargetGroup> ?treatmentGroup .\n" + 
				"    ?treatmentGroup <http://psink.de/scio/hasTreatmentType> ?treatmentType .\n" + 
				"    ?treatmentType a ?treatmentTypeName .\n" + 
				"    ?treatmentGroup <http://psink.de/scio/hasOrganismModel> ?organismModel .\n" + 
				"    ?organismModel <http://psink.de/scio/hasOrganismSpecies> ?target .\n" + 
				"}\n" + 
				"";
		
		Query query2 = QueryFactory.create(queryString2);
		System.out.println(query2);

//		String queryString2 = "SELECT ?s ?p ?o " + "WHERE { ?s ?p ?o . }";
//		entityTypes(model);
//		subClassOf(model);
//		superClassOf(model);
//		relatedToClass(model);
//		propertiesRelatedToProperty(model);
//		relatedToProperty(model);
//		getObjectProperty(model);
	}

	public static void entityTypes(Model model) {
		String queryString2 = "prefix owl:   <http://www.w3.org/2002/07/owl#> \n"
				+ "prefix scio:      <http://psink.de/scio/> \n"
				+ "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" + "\n" + "select distinct ?d  {\n"
				+ "{?d a* owl:Class} " + "FILTER(STRSTARTS(STR(?d), \"http://psink.de/scio/\"))}";

		Query query2 = QueryFactory.create(queryString2);
		System.out.println(query2);
		try (QueryExecution qexec = QueryExecutionFactory.create(query2, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();

				try {

					System.out.println(querySolution.getResource("d") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getResource("r"));
				} catch (Exception e) {
					System.out.println(querySolution.getResource("d") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getLiteral("r"));
				}

			}
		}

	}

	public static void getObjectProperty(Model model) {
		String queryString2 = "prefix owl:   <http://www.w3.org/2002/07/owl#> \n"
				+ "prefix scio:      <http://psink.de/scio/> \n"
				+ "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" + "\n" + "select distinct ?p  {\n"

				+ "?p a owl:ObjectProperty. "
//				
				+ "?p rdfs:domain scio:OrganismModel . "

				+ "}";

		Query query2 = QueryFactory.create(queryString2);

		try (QueryExecution qexec = QueryExecutionFactory.create(query2, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();

				try {

					System.out.println(querySolution.getResource("d") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getResource("r"));
				} catch (Exception e) {
					System.out.println(querySolution.getResource("d") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getLiteral("r"));
				}

			}
		}
	}

	public static void relatedToProperty(Model model) {
		String queryString2 = "prefix owl:   <http://www.w3.org/2002/07/owl#> \n"
				+ "prefix scio:      <http://psink.de/scio/> \n"
				+ "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" + "\n" + "select distinct ?d ?p ?r {\n"

				+ "{?d a ?r.} UNION {?d <http://www.w3.org/2000/01/rdf-schema#subClassOf>* ?r } "

//				+ "?p a owl:ObjectProperty. "
//				
				+ "?p" + " rdfs:range ?r. "
//+ "scio:makesUseOf" + " rdfs:range ?r. "
//				
//+ "scio:makesUseOf rdfs:domain scio:InvestigationMethod . " 

				+ "}";

		Query query2 = QueryFactory.create(queryString2);
		System.out.println(query2);
		try (QueryExecution qexec = QueryExecutionFactory.create(query2, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();

				try {

					System.out.println(querySolution.getResource("d") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getResource("r"));
				} catch (Exception e) {
					System.out.println(querySolution.getResource("d") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getLiteral("r"));
				}

			}
		}

	}

	public static void propertiesRelatedToProperty(Model model) {
		String queryString2 = "prefix owl:   <http://www.w3.org/2002/07/owl#> \n"
				+ "prefix scio:      <http://psink.de/scio/> \n"
				+ "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" + "\n" + "select distinct ?p ?r ?d {\n"
				+ "?p rdfs:domain ?d. " + "{?d a ?r} UNION {?d <http://www.w3.org/2000/01/rdf-schema#subClassOf>* ?r } "

				+ "scio:hasReferenceGroup" + " rdfs:range ?r. "

				+ "}";

		Query query2 = QueryFactory.create(queryString2);
		System.out.println(query2);
		try (QueryExecution qexec = QueryExecutionFactory.create(query2, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();

				try {

					System.out.println(querySolution.getResource("d") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getResource("r"));
				} catch (Exception e) {
					System.out.println(querySolution.getResource("d") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getLiteral("r"));
				}

			}
		}

	}

	public static void relatedToClass(Model model) {
		String queryString2 = "prefix owl:   <http://www.w3.org/2002/07/owl#> \n"
				+ "prefix scio:      <http://psink.de/scio/> \n"
				+ "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" + "\n" + "select distinct ?d ?p  {\n"

//				+"?d <http://www.w3.org/2000/01/rdf-schema#subClassOf>* scio:AgeCategory. "
				+ "{?d a ?r.} UNION {?d <http://www.w3.org/2000/01/rdf-schema#subClassOf>* ?r } "

//				+ "?p a owl:ObjectProperty. "
//				
				+ "?p" + " rdfs:range ?r. "
//				
				+ "?p rdfs:domain scio:InvestigationMethod . "

				+ "}";

		Query query2 = QueryFactory.create(queryString2);
		System.out.println(query2);
		try (QueryExecution qexec = QueryExecutionFactory.create(query2, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();

				try {

					System.out.println(querySolution.getResource("d") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getResource("r"));
				} catch (Exception e) {
					System.out.println(querySolution.getResource("d") + "\t" + querySolution.getResource("p") + "\t"
							+ querySolution.getLiteral("r"));
				}

			}
		}

	}

	public static void subClassOf(Model model) {
		String queryString2 = "prefix owl:   <http://www.w3.org/2002/07/owl#> \n"
				+ "prefix scio:      <http://psink.de/scio/> \n"
				+ "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" + "SELECT * WHERE {"
				+ " {?class a* <http://psink.de/scio/AgeCategory>} UNION { ?class rdfs:subClassOf* <http://psink.de/scio/AgeCategory>}."
				+ "FILTER(STRSTARTS(STR(?class), \"http://psink.de/scio/\"))" + "}";

		Query query2 = QueryFactory.create(queryString2);
		System.out.println(query2);
		try (QueryExecution qexec = QueryExecutionFactory.create(query2, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();
				System.out.println(querySolution.getResource("class"));
			}
		}

	}

	public static void superClassOf(Model model) {
		String queryString2 = "prefix owl:   <http://www.w3.org/2002/07/owl#> \n"
				+ "prefix scio:      <http://psink.de/scio/> \n"
				+ "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" + " SELECT ?class WHERE {"
				+ "{<http://psink.de/scio/Male> a* ?class}  UNION { <http://psink.de/scio/Male> rdfs:subClassOf ?class}"
				+ "FILTER(STRSTARTS(STR(?class), \"http://psink.de/scio/\"))" + "}";

		Query query2 = QueryFactory.create(queryString2);
		System.out.println(query2);
		try (QueryExecution qexec = QueryExecutionFactory.create(query2, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();
				System.out.println(querySolution.getResource("class"));
			}
		}

	}

}
