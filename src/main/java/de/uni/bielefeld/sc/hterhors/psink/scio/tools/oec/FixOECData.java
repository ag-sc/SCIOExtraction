package de.uni.bielefeld.sc.hterhors.psink.scio.tools.oec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class FixOECData {

	Set<String> neutralResults = new HashSet<>(Arrays.asList("Result_1306", "Result_946", "Result_906", "Result_834",
			"Result_8", "Result_788", "Result_6", "Result_551", "Result_5", "Result_494", "Result_37", "Result_35",
			"Result_33", "Result_25", "Result_1985", "Result_1977", "Result_1973", "Result_1970", "Result_1967",
			"Result_1966", "Result_1956", "Result_1955", "Result_1951", "Result_195", "Result_1948", "Result_1940",
			"Result_1939", "Result_1936", "Result_1934", "Result_1932", "Result_193", "Result_192", "Result_189",
			"Result_1873", "Result_186", "Result_1803", "Result_180", "Result_18", "Result_1799", "Result_1798",
			"Result_1791", "Result_1790", "Result_1772", "Result_1766", "Result_1765", "Result_1763", "Result_1699",
			"Result_1698", "Result_1697", "Result_1696", "Result_1695", "Result_1666", "Result_1620", "Result_1605",
			"Result_1604", "Result_1602", "Result_1600", "Result_1599", "Result_1595", "Result_1592", "Result_1584",
			"Result_1579", "Result_1576", "Result_1460", "Result_1457", "Result_1449", "Result_1448", "Result_1447",
			"Result_1444", "Result_1440", "Result_1438", "Result_1437", "Result_1436", "Result_1435", "Result_1433",
			"Result_1432", "Result_1430", "Result_1428", "Result_1427", "Result_1423", "Result_1302", "Result_1301",
			"Result_1260", "Result_1240", "Result_1208", "Result_12", "Result_1110", "Result_1106", "Result_107",
			"Result_1010", "Result_1004"));

	public static void main(String[] args) throws Exception {
		Model model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		model.read(new FileInputStream("oec/OEC_grading_BBB.n-triples"), null, "N-TRIPLES");

//		new FixOECData();

	}

	public FixOECData() throws Exception {

		/**
		 * Fix whitespaces
		 * 
		 * fix Pubmed IDs
		 * 
		 * Add BBBScores
		 */
		fix();
	}

	static class Triple {

		final public String TargetScore;
		final public String ReferenceScore;
		final public String timepoint;

		public Triple(String targetScore, String referenceScore, String timepoint) {
			super();
			TargetScore = targetScore;
			ReferenceScore = referenceScore;
			this.timepoint = timepoint;
		}

		@Override
		public String toString() {
			return "Triple [TargetScore=" + TargetScore + ", ReferenceScore=" + ReferenceScore + ", timepoint="
					+ timepoint + "]";
		}

	}
	// <http://scio/data/Result_55271> <http://psink.de/scio/hasObservation>
	// <http://scio/data/Observation_90467>
//	<http://scio/data/Observation_90467> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://psink.de/scio/Observation> .
	// <http://scio/data/MeanValue_90470>
	// <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>
	// <http://psink.de/scio/MeanValue> .

//	<http://scio/data/Observation_90467> <http://psink.de/scio/belongsTo> <http://scio/data/DefinedExperimentalGroup_28473> .
//	<http://scio/data/Observation_90467> <http://psink.de/scio/hasNumericValue> <http://scio/data/MeanValue_90470> .
//	<http://scio/data/Observation_28500> <http://psink.de/scio/hasTemporalInterval> <http://scio/data/TemporalInterval_28502> .
//	<http://scio/data/TemporalInterval_28502> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://psink.de/scio/TemporalInterval> .
//		<http://scio/data/TemporalInterval_28502> <http://psink.de/scio/hasDuration> "1 week after SCI" .

//		<http://scio/data/TemporalInterval_28502> <http://psink.de/scio/hasEventBefore> <http://scio/data/Compression_2853> .
//		<http://scio/data/TemporalInterval_28502> <http://psink.de/scio/hasEventAfter> <http://scio/data/CompoundTreatment_28384> .

	public final static String rdfTypeTemporalInterval = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://psink.de/scio/TemporalInterval> .";
	public final static String rdfTypeObservation = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://psink.de/scio/Observation> .";
	public final static String rdfTypeMeanValue = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://psink.de/scio/MeanValue> .";
	public final static String hasObservation = "<http://psink.de/scio/hasObservation>";
	public final static String hasNumericValue = "<http://psink.de/scio/hasNumericValue>";
	public final static String hasValue = "<http://psink.de/scio/hasValue>";
	public final static String hasTemporalInterval = "<http://psink.de/scio/hasTemporalInterval>";
	public final static String hasDuration = "<http://psink.de/scio/hasDuration>";
	public final static String hasEventBefore = "<http://psink.de/scio/hasEventBefore>";
	public final static String hasEventAfter = "<http://psink.de/scio/hasEventAfter>";
	public final static String belongsTo = "<http://psink.de/scio/belongsTo>";
	public final static String observationTemplate = "<http://psink.de/scio/hasObservation>";

	private void fix() throws IOException {

		/**
		 * add bbb scores to results
		 */
		List<String[]> bbbScores = Files.readAllLines(new File("oec/OEC BBB nachannotiert.csv").toPath()).stream()
				.map(l -> l.split(";")).filter(d -> d.length > 4 && d[2].equals("BBBTest"))
				.filter(d -> !d[3].trim().isEmpty() || !d[4].trim().isEmpty() || !d[5].trim().isEmpty())
				.collect(Collectors.toList());
//result, observation triple
		Map<String, Triple> bbbMap = new HashMap<>();

		for (String[] string : bbbScores) {

			String key = string[0].split("/")[4].replaceAll(">", "");
			String targetScore = string[3].trim();
			String referenceScore = string[4].trim();
			String timepoint = string[5].trim();
			Triple value = new Triple(targetScore, referenceScore, timepoint);
			bbbMap.put(key, value);
		}

		/**
		 * 
		 */
		List<String> pubmedIds = Files.readAllLines(new File("oec/PMID match N Gold standard.csv").toPath());

		/*
		 * N number, Pubmedid
		 */
		Map<String, String> pubmedIdMap = new HashMap<>();
		for (String string : pubmedIds) {
			String[] data = string.split(";");

			pubmedIdMap.put(data[0], data[1]);

		}

		/**
		 * 
		 */
		List<String> triples = Files.readAllLines(new File("oec/OEC_grading_switch_fix.n-triples").toPath());

		int observationCounter = 0;

		List<String> observationtriple = new ArrayList<>();
		Set<String> observedResultSubjects = new HashSet<>();

		/*
		 * Result / investigationM
		 */
		Map<String, String> investMap = new HashMap<>();

		/*
		 * Result / Injury
		 */
		Map<String, String> injuryMap = new HashMap<>();

		/*
		 * Result / ExpGroup
		 */
		Map<String, String> ExpGroupMap = new HashMap<>();

		/**
		 * Results , all other
		 */
		for (String string : triples) {
			string = string.replaceAll(" ", "_");
			string = string.replaceAll(">_<", "> <");
			string = string.replaceAll("_\\.", " .");
			String resultSubject = string.split(" ")[0];

			for (String resultname : bbbMap.keySet()) {

				if (string.contains("_" + resultname + "> <")) {
					if (string.contains("hasTargetGroup")) {

						ExpGroupMap.put(resultSubject, string.split(" ")[2]);
					}
				}
			}
		}

		for (String string : triples) {
			string = string.replaceAll(" ", "_");
			string = string.replaceAll(">_<", "> <");
			string = string.replaceAll("_\\.", " .");

			for (Entry<String, String> ExpGroupname : ExpGroupMap.entrySet()) {

				if (string.startsWith(ExpGroupname.getValue())) {
					if (string.contains("hasInjury")) {

						injuryMap.put(ExpGroupname.getKey(), string.split(" ")[2]);
					}
				}
			}
		}
//		
		for (String string : triples) {
			string = string.replaceAll(" ", "_");
			string = string.replaceAll(">_<", "> <");
			string = string.replaceAll("_\\.", " .");
			String resultSubject = string.split(" ")[0];

			for (String resultname : bbbMap.keySet()) {

				if (string.contains("_" + resultname + "> <")) {
					if (string.contains("hasInvestigationMethod")) {

						investMap.put(resultSubject, string.split(" ")[2]);
					}
				}
			}
		}

		for (String string : triples) {

			if (string.contains("\"")) {
				String d[] = string.split("> ", 3);

				string = "";
				string += (d[0].replaceAll(" +", "_") + "> ");
				string += (d[1].replaceAll(" +", "_") + "> ");
				/**
				 * Replace name by pubmed id
				 */
				if (d[1].contains("hasPubmedID")) {
					String key = d[2].substring(1, 5);
					if (pubmedIdMap.get(key) == null)
						throw new IllegalArgumentException("can not find pubmed id mapping.");
					string += "\"" + pubmedIdMap.get(key) + "\" .";
				} else {
					if (d[2].contains("\\")) {
//						System.out.println(d[2]);
						string += d[2].replace("\\", "\\\\");
//						System.out.println("#####" + string);
					}else {
						string += d[2];
					}
				}

			} else {

				string = string.replaceAll(" ", "_");
				string = string.replaceAll(">_+<", "> <");
				string = string.replaceAll("_\\.", " .");
//				

				/**
				 * add obersvations
				 */

				for (String resultname : bbbMap.keySet()) {

					String resultSubject = string.split(" ")[0];

					if (observedResultSubjects.contains(resultSubject))
						continue;

					if (string.contains("_" + resultname + "> <")) {

						observedResultSubjects.add(resultSubject);

						Triple observationTriple = bbbMap.get(resultname);

						if (!observationTriple.TargetScore.isEmpty()) {
							observationCounter++;
							/**
							 * hasObservationtriple
							 */
							String hasObtriple = "";
							hasObtriple += resultSubject + " ";
							hasObtriple += hasObservation + " ";
							String observation = "<http://scio/data/Observation_" + observationCounter + ">";
							hasObtriple += observation + ". ";
							observationtriple.add(hasObtriple);

							/**
							 * type observation triple
							 */
							observationtriple.add(observation + " " + rdfTypeObservation);

							String meanVal = "<http://scio/data/MeanValue_" + observationCounter + ">";
							/**
							 * mean triple
							 */
							observationtriple.add(observation + " " + hasNumericValue + " " + meanVal + " .");
							observationtriple.add(meanVal + " " + rdfTypeMeanValue);
							observationtriple.add(meanVal + " " + hasValue + " \""
									+ observationTriple.TargetScore.replaceAll(",", ".") + "\" . ");
							if (!observationTriple.timepoint.isEmpty()) {

								String temporalInterval = "<http://scio/data/TemporalInterval" + observationCounter
										+ ">";
								observationtriple
										.add(observation + " " + hasTemporalInterval + " " + temporalInterval + " . ");
								observationtriple.add(temporalInterval + " " + rdfTypeTemporalInterval);
								observationtriple.add(temporalInterval + " " + hasDuration + " \""
										+ observationTriple.timepoint.replaceAll(",", ".") + "\" . ");
								String eventBefore = injuryMap.get(resultSubject);
								String eventAfter = investMap.get(resultSubject);
								observationtriple
										.add(temporalInterval + " " + hasEventAfter + " " + eventAfter + " . ");
								observationtriple
										.add(temporalInterval + " " + hasEventBefore + " " + eventBefore + " . ");
							}

						}
						if (!observationTriple.ReferenceScore.isEmpty()) {
							/**
							 * hasObservationtriple
							 */
							observationCounter++;
							String observation = "<http://scio/data/Observation_" + observationCounter + ">";
							String hasObtriple = "";
							hasObtriple += resultSubject + " ";
							hasObtriple += hasObservation + " ";
							hasObtriple += observation + " . ";
							observationtriple.add(hasObtriple);
							/**
							 * type observation triple
							 */
							observationtriple.add(observation + " " + rdfTypeObservation);

							String meanVal = "<http://scio/data/MeanValue_" + observationCounter + ">";
							/**
							 * mean triple
							 */
							observationtriple.add(observation + " " + hasNumericValue + " " + meanVal + " .");
							observationtriple.add(meanVal + " " + rdfTypeMeanValue);
							observationtriple.add(meanVal + " " + hasValue + " \""
									+ observationTriple.ReferenceScore.replaceAll(",", ".") + "\" . ");
							if (!observationTriple.timepoint.isEmpty()) {

								String temporalInterval = "<http://scio/data/TemporalInterval_" + observationCounter
										+ ">";
								observationtriple
										.add(observation + " " + hasTemporalInterval + " " + temporalInterval + " . ");
								observationtriple.add(temporalInterval + " " + rdfTypeTemporalInterval);
								observationtriple.add(temporalInterval + " " + hasDuration + " \""
										+ observationTriple.timepoint.replaceAll(",", ".") + "\" . ");
								String eventBefore = injuryMap.get(resultSubject);
								String eventAfter = investMap.get(resultSubject);
								observationtriple
										.add(temporalInterval + " " + hasEventAfter + " " + eventAfter + " . ");
								observationtriple
										.add(temporalInterval + " " + hasEventBefore + " " + eventBefore + " . ");
							}

						}
						break;
					}

				}

				/**
				 * Change judgements
				 */
				for (String string2 : neutralResults) {

					if (string.contains("_" + string2 + ">")) {
						if (string.contains("hasJud")) {
							string = string.replaceAll("Positive", "Neutral");
							string = string.replaceAll("Negative", "Neutral");
						}
					}
				}

			}

			System.out.println(string);

		}

		observationtriple.forEach(System.out::println);
	}
}
