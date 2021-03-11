package de.uni.bielefeld.sc.hterhors.psink.scio.tools.ddkg;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * The sCI-KG is just a set of Results. Each triple has its pubmed ID in the
 * name of the triples. This is used to cluster the results by their
 * publication.
 * 
 * <http://scio/data/101170XML2.json_Result_1754>
 * 
 * <http://scio/data/N005_DePaul,_Lin_et_al._2015_Experiment_5038>
 * <http://psink.de/scio/hasResult>
 * <http://scio/data/N005_DePaul,_Lin_et_al._2015_Result_1183> .
 * 
 * <http://scio/data/N005_DePaul,_Lin_et_al._2015_Publication_5037>
 * <http://psink.de/scio/describes>
 * <http://scio/data/N005_DePaul,_Lin_et_al._2015_Experiment_5038> .
 * <http://scio/data/N005_DePaul,_Lin_et_al._2015_Publication_5037>
 * <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://scio/data/N005
 * DePaul, Lin et al. 2015_Publication_5037> <http://psink.de/scio/hasPubmedID>
 * "N005 DePaul, Lin et al. 2015" .
 * 
 * @author hterhors
 *
 */
public class AddPublications {

	final static Pattern p = Pattern.compile(".+?(\\d+)XML[1,2].json_Result_\\d+>");

	
	
	public static void main(String[] args) throws Exception {
//		Model model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
//		model.read(new FileInputStream("DDKG/SCIKG_XML_pubmed_clustered.n-triples"), null, "N-TRIPLES");
	
		List<String> all = Files.readAllLines(new File("DDKG/SCIKG_XML.n-triples").toPath());

		Set<String> results = all.stream().map(s -> s.split(" ")[0].trim()).filter(s -> s.contains("Result"))
				.collect(Collectors.toSet());

		// pubmedid, list of results
		Map<String, Set<String>> map = new HashMap<>();

		for (String string : results) {
			Matcher m = p.matcher(string);

			m.find();

			String pubmedID = m.group(1);
			map.putIfAbsent(pubmedID, new HashSet<>());
			map.get(pubmedID).add(string);

		}

		List<String> additionalTripple = new ArrayList<>();
		for (Entry<String, Set<String>> pubs : map.entrySet()) {

			final String id = pubs.getKey();

			additionalTripple.add(String.format(
					"<http://scio/data/Publication_%s> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://psink.de/scio/Publication> .",
					id));
			additionalTripple.add(String
					.format("<http://scio/data/Publication_%s> <http://psink.de/scio/hasPubmedID> \"%s\" .", id, id));
			additionalTripple.add(String.format(
					"<http://scio/data/Publication_%s> <http://psink.de/scio/describes> <http://scio/data/Experiment_%s> .",
					id, id));

			for (String string : pubs.getValue()) {
				additionalTripple.add(String.format(
						"<http://scio/data/Experiment_%s> <http://psink.de/scio/hasResult> %s .", id, string));

			}

		}

		for (String string : additionalTripple) {
			System.out.println(string);
		}

	}

}
