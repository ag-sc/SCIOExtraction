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

//		String queryString2 = "SELECT ?s ?p ?o " + "WHERE { ?s ?p ?o . }";
		subClassOf(model);
		relatedToClass(model);
		relatedToProperty(model);
		getObjectProperty(model);
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
				+ "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" + "\n" + "select distinct ?d ?p  {\n"
				
+"{?d a ?r.} UNION {?d <http://www.w3.org/2000/01/rdf-schema#subClassOf>* ?r } "

//				+ "?p a owl:ObjectProperty. "
//				
+ "scio:makesUseOf" + " rdfs:range ?r. "
//				
+ "scio:makesUseOf rdfs:domain scio:InvestigationMethod . " 

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
	public static void relatedToClass(Model model) {
		String queryString2 = "prefix owl:   <http://www.w3.org/2002/07/owl#> \n"
				+ "prefix scio:      <http://psink.de/scio/> \n"
				+ "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" + "\n" + "select distinct ?d ?p  {\n"

//				+"?d <http://www.w3.org/2000/01/rdf-schema#subClassOf>* scio:AgeCategory. "
				+"{?d a ?r.} UNION {?d <http://www.w3.org/2000/01/rdf-schema#subClassOf>* ?r } "
				
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
		String queryString2 = "SELECT * WHERE {"
				+ "  ?class <http://www.w3.org/2000/01/rdf-schema#subClassOf>* <http://psink.de/scio/AgeCategory>."
				+ "}";

		Query query2 = QueryFactory.create(queryString2);

		try (QueryExecution qexec = QueryExecutionFactory.create(query2, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();
				System.out.println(querySolution.getResource("class"));
			}
		}

	}

}
