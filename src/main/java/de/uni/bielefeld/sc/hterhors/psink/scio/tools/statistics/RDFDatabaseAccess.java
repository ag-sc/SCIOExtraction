package de.uni.bielefeld.sc.hterhors.psink.scio.tools.statistics;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class RDFDatabaseAccess {

	public static void main(String[] args) throws IOException {

		new RDFDatabaseAccess(new File("unroll/merge_export_25092019.n-triples"));
	}

	public static class SPARQLQuery {
		public final String queryName;
		public final String queryString;
		public final String[] varNames;

		public SPARQLQuery(String queryName, String queryString, String... varNames) {
			this.queryName = queryName;
			this.queryString = queryString;
			this.varNames = varNames;
		}
	}

	public RDFDatabaseAccess(File inputFile) throws IOException {

		if (!inputFile.exists())
			throw new IllegalArgumentException("The provided file das not exists: " + inputFile.getName());

		Model model = ModelFactory.createDefaultModel();
		model.read(new FileInputStream(inputFile), null, "N-TRIPLES");

		SPARQLQuery testQuery = new SPARQLQuery("Query1", "PREFIX scio: <http://psink.de/scio/>\n" + 
				"PREFIX sciodata: <http://scio/data/>\n" + 
				"PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + 
				"PREFIX rdf2: <http://www.w3.org/2000/01/rdf-schema#>\n" + 
				"\n" + 
				"SELECT ?Document ?Animal ?Gender  ?Animal2 ?Gender2 #?InvestigationMethod\n" + 
				"WHERE {\n" + 
				"\n" + 
				"#get the results and studies\n" + 
				"OPTIONAL{?Result rdf2:comment ?Document.}\n" + 
				"\n" + 
				"#animal filter\n" + 
				"{\n" + 
				"?Result scio:hasTargetGroup ?TargetGroup.\n" + 
				"?TargetGroup scio:hasOrganismModel ?OrganismModel.                            \n" + 
				"?OrganismModel scio:hasOrganismSpecies ?Animal.\n" + 
				"?OrganismModel scio:hasGender ?Gender.\n" + 
				"}\n" + 
				"UNION\n" + 
				"{\n" + 
				"?Result scio:hasReferenceGroup ?ReferenceGroup.\n" + 
				"?ReferenceGroup scio:hasOrganismModel ?OrganismModel.\n" + 
				"?OrganismModel2 scio:hasOrganismSpecies ?Animal2.\n" + 
				"?OrganismModel2 scio:hasGender ?Gender2.\n" + 
				"}"
				+ "\n" + 
				"\n" + 
				"}"
				,  "Document", "Animal", "Gender");
//		SPARQLQuery testQuery = new SPARQLQuery("Query1", "PREFIX scio: <http://psink.de/scio/>\n"
//				+ "PREFIX sciodata: <http://scio/data/>\n"
//				+ "PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
//				+ "PREFIX rdf2: <http://www.w3.org/2000/01/rdf-schema#>\n" + "\n"
//				+ "SELECT ?Document ?Compound ?Difference ?Animal ?Gender #?InvestigationMethod\n" + "WHERE {\n" + "\n"
//				+ "#get the results and studies\n" + "OPTIONAL{?Result rdf2:comment ?Document.}\n" + "\n"
//				+ "#animal filter\n" + "OPTIONAL{\n" + "{?Result scio:hasTargetGroup ?TargetGroup.\n"
//				+ "?TargetGroup scio:hasOrganismModel ?OrganismModel.                            \n"
//				+ "?OrganismModel scio:hasOrganismSpecies ?Animal.\n" + "?OrganismModel scio:hasGenderrg/1999/02/22-rdf-syntax-ns#type ?Gender.\n"
//				+ "}\n" + "UNION\n" + "{\n" + "?Result scio:hasReferenceGroup ?ReferenceGroup.\n"
//				+ "?ReferenceGroup scio:hasOrganismModel ?OrganismModel.\n"
//				+ "?OrganismModel scio:hasOrganismSpecies ?Animal.\n" + "?OrganismModel scio:hasGender ?Gender.\n"
//				+ "}}\n" + "#oec filter\n" + "OPTIONAL{{\n" + "?Result scio:hasTargetGroup ?TargetGroup.\n"
//				+ "?TargetGroup scio:hasTreatmentType ?TreatmentType.\n"
//				+ "?TreatmentType scio:hasCompound ?Compound.}\n"
//				+ "#?Compound rdf:type scio:OlfactoryEnsheathingGliaCell.\n" + "#}\n" + "UNION\n" + "{\n"
//				+ "?Result scio:hasReferenceGroup ?ReferenceGroup.\n"
//				+ "?ReferenceGroup scio:hasTreatmentType ?TreatmentType.\n"
//				+ "?TreatmentType scio:hasCompound ?Compound.}}\n"
//				+ "#?Compound rdf:type scio:OlfactoryEnsheathingGliaCell.\n" + "#}\n" + "\n" + "#methoden wie BBB\n"
//				+ "#?Result scio:hasInvestigation ?Investigation.\n"
//				+ "#?Investigation scio:hasInvestigationMethod ?InvestigationMethod.\n"
//				+ "#?InvestigationMethod rdf:type scio:BBBTest.}\n" + "\n" + "#lower higher angabe\n"
//				+ "OPTIONAL{?Result scio:hasTrend ?Trend.\n" + "?Trend scio:hasDifference ?Difference.}\n" + "\n" + "}",
//				"Document", "Compound", "Difference", "Animal", "Gender");

		Query query = QueryFactory.create(testQuery.queryString);
		ResultSet subGraph = null;
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			subGraph = qexec.execSelect();
			printResults(subGraph, testQuery);
		}
	}

	private void printResults(ResultSet results, SPARQLQuery sparqlQuery) {
		System.out.println(sparqlQuery.queryName + ":");
		QuerySolution solution;
		int counter;
		for (counter = 0; results.hasNext(); counter++) {
			solution = results.nextSolution();

			for (String varName : sparqlQuery.varNames) {
				try {
					System.out.print(solution.getResource(varName) + "\t");
				} catch (ClassCastException e) {
					System.out.print(solution.getLiteral(varName) + "\t");
				}
			}
			System.out.println();
		}
		System.out.println("--> " + counter + " results found!");
	}
}
