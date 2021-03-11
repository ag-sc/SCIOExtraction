
package de.uni.bielefeld.sc.hterhors.psink.scio.tools.latex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;

public class GeneralLatexTableGenerator {

	public static void main(String[] args) throws IOException {

//		List<String> lines = Files.readAllLines(
//				new File("dissertation_results/treatment/Treatment_FinalEvaluationGOLD.log").toPath());
		String gold;
		String predict;
		String t;
		String name;
		t = "anaesthetic";
		name = "Anaesthetic";
		gold = ("dissertation_results/%s/%s_FinalEvaluationGOLD.log");
		predict = ("dissertation_results/%s/%s_FinalEvaluationPREDICT.log");
		new GeneralLatexTableGenerator(name, t, gold, predict);
//
//		t = "delivery_method";
//		name = "DeliveryMethod";
//		gold = ("dissertation_results/%s/%s_FinalEvaluationGOLD.log");
//		predict = ("dissertation_results/%s/%s_FinalEvaluationPREDICT.log");
//		new GeneralLatexTableGenerator(name, t, gold, predict);
//
//		t = "injury";
//		name = "Injury";
//		gold = ("dissertation_results/%s/%s_FinalEvaluationGOLD.log");
//		predict = ("dissertation_results/%s/%s_FinalEvaluationPREDICT.log");
//		new GeneralLatexTableGenerator(name, t, gold, predict);
//
//		t = "injury_device";
//		name = "InjuryDevice";
//		gold = ("dissertation_results/%s/%s_FinalEvaluationGOLD.log");
//		predict = ("dissertation_results/%s/%s_FinalEvaluationPREDICT.log");
//		new GeneralLatexTableGenerator(name, t, gold, predict);

//		t = "organism_model";
//		name = "OrganismModel";
//		gold = ("dissertation_results/%s/%s_FinalEvaluationGOLD.log");
//		predict = ("dissertation_results/%s/%s_FinalEvaluationPREDICT.log");
//		new GeneralLatexTableGenerator(name, t, gold, predict);

//		t = "treatment";
//		name = "Treatment";
//		gold = ("dissertation_results/%s/%s_FinalEvaluationGOLD.log");
//		predict = ("dissertation_results/%s/%s_FinalEvaluationPREDICT.log");
//		new GeneralLatexTableGenerator(name, t, gold, predict);

//		t = "vertebral_location";
//		name = "VertebralLocation";
//		gold = ("dissertation_results/%s/%s_FinalEvaluationGOLD.log");
//		predict = ("dissertation_results/%s/%s_FinalEvaluationPREDICT.log");
//		new GeneralLatexTableGenerator(name, t, gold, predict);

	}

	public GeneralLatexTableGenerator(String name, String t, String gold, String predict) throws IOException {
		List<String> goldData = new ArrayList<>();
		List<String> predictData = new ArrayList<>();

		getData(new File(String.format(gold, t, name)), goldData);
		getData(new File(String.format(predict, t, name)), predictData);

		Map<String, Score> goldScore = new HashMap<>();
		Map<String, Score> predictScore = new HashMap<>();

		mapData(goldData, goldScore);
		mapData(predictData, predictScore);

		Set<String> keys = new HashSet<>();

		for (String key : goldScore.keySet()) {
			key = key.replace("-Absolute", "");
			key = key.replace("-Coverage", "");
			key = key.replace("-Relative", "");
			keys.add(key);
		}

		List<String> sortedKeys = new ArrayList<>(keys);
		Collections.sort(sortedKeys);

		String lines = "";
		String type = "", cardinality = "";
		Object overall = "";
		for (String key : sortedKeys) {

			String line = key + "}$^{}$";

			line += "\t" + predictScore.get(key + "-Absolute").toTSVString() + "\t\t"
					+ predictScore.get(key + "-Coverage").toTSVString() + "\t\t"
					+ goldScore.get(key + "-Absolute").toTSVString();
			line = line.replaceAll("\t", " & ");

			if (line.startsWith("Root")) {
				type = "\\emph{" + line.replaceFirst("Root", "type");
			} else if (line.startsWith("Cardinality")) {
				cardinality = "\\emph{" + line.replaceFirst("Cardinality", "cardinality");
			} else if (line.startsWith("Overall")) {
				overall = "\\emph{" + line.replaceFirst("Overall", "overall");
			} else {
				lines += "\\emph{" + line + "\\\\\n";
			}
		}
		final String latexTable = String.format(template,   name, type, lines, cardinality, overall,name,t);
		System.out.println(latexTable);
		System.out.println();
		System.out.println();
		System.out.println();
	}

	final static String template = "\\begin{table}[H]\n" + "\\centering\n" + "\\scriptsize\n"
			+ "\\begin{tabular}{@{}lccccccccccc@{}}\n"
			+ "\\toprule\n"
			+ "\\textsc{%s}  &  \\multicolumn{3}{c}{real-world}   &  &  \\multicolumn{3}{c}{property-oracle}  &  &  \\multicolumn{3}{c}{candidate-oracle}  \\\\ \\midrule\n"
			+ "Macro   & $F_1$ & P & R && $F_1$ & P & R & & $F_1$ & P & R \\\\\n"
			+ " \\cmidrule(lr){1-1} \\cmidrule(lr){2-4} \\cmidrule(lr){6-8} \\cmidrule(rl){10-12} \n" + "%s\\\\ \n"
			+ "%s" + "\\cmidrule(lr){1-1} \\cmidrule(lr){2-4} \\cmidrule(lr){6-8} \\cmidrule(rl){10-12} \n" + "%s\\\\"
			+ "\\cmidrule(lr){1-1} \\cmidrule(lr){2-4} \\cmidrule(lr){6-8} \\cmidrule(rl){10-12}\n" + "%s"
			+ " \\\\ \\bottomrule\n" + "\\end{tabular}\n"
			+ "\\caption{Evaluation result for predicting instances of type \\textsc{%s}.}\n" + "\\label{tab:eval_%s}\n"
			+ "\\end{table}";

	private static void mapData(List<String> goldData, Map<String, Score> goldScore) {
		for (String line : goldData) {
			String[] data = line.split("\t");

			goldScore.put(data[0],
					new Score(Double.parseDouble(data[1]), Double.parseDouble(data[2]), Double.parseDouble(data[3])));

		}
	}

	private static void getData(File file, List<String> goldData) throws IOException {
		List<String> lines = Files.readAllLines(file.toPath());
		int c = 0;
		for (int i = lines.size() - 1; i >= 0; i--) {
			String line = lines.get(i);
			boolean data = false;

			c += (data = !line.trim().startsWith("*")) ? 0 : 1;
			if (c == 2)
				break;

			if (data) {
				goldData.add(line);
			}

		}
	}
}
