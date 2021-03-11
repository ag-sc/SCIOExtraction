package de.uni.bielefeld.sc.hterhors.psink.scio.tools.oec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class NeutralZeroSigFix {

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
//		Model model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
//		model.read(new FileInputStream("oec/OEC_grading_switch_fix_s_fix_neutral_fix.n-triples"), null, "N-TRIPLES");

		new NeutralZeroSigFix();

	}

	public NeutralZeroSigFix() throws Exception {

		whitespaceFix();
	}

	private void whitespaceFix() throws IOException {
		List<String> triples = Files
				.readAllLines(new File("oec/OEC_grading_switch_fix.n-triples").toPath());

		for (String string : triples) {

			if (string.contains("\"")) {
				String d[] = string.split("> ", 3);

				if (d.length != 3) {
					System.out.println(Arrays.toString(d));
					System.exit(1);
				}
				string = "";
				string += (d[0].replaceAll(" ", "_") + "> ");
				string += (d[1].replaceAll(" ", "_") + "> ");
				string += d[2];
			} else {
				string = string.replaceAll(" ", "_");
				string = string.replaceAll(">_<", "> <");
				string = string.replaceAll("_\\.", " .");
//				
				for (String string2 : neutralResults) {
					if(string.contains("_"+string2+">"))
					{
						if(string.contains("hasJud")) {
					string = string.replaceAll("Positive", "Neutral");		
					string = string.replaceAll("Negative", "Neutral");		
						}
					}
				}
//				

			}
			System.out.println(string);

		}
	}

}
