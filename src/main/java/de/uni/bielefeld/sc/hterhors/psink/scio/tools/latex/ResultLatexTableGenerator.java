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

public class ResultLatexTableGenerator {

	public static void main(String[] args) throws IOException {

		File f1File = new File("dissertation_results/result/evaluations/", "merged_cardinality_f1.csv");
		File precFile = new File("dissertation_results/result/evaluations/",
				"merged_cardinality_precision.csv");
		File recFile = new File("dissertation_results/result/evaluations/",
				"merged_cardinality_recall.csv");
		List<String> F1 = Files.readAllLines(f1File.toPath());
		List<String> precision = Files.readAllLines(precFile.toPath());
		List<String> recall = Files.readAllLines(recFile.toPath());

		Map<String, Map<String, Double>> f1Map = new HashMap<>();
		Map<String, Map<String, Double>> precMap = new HashMap<>();
		Map<String, Map<String, Double>> recallMap = new HashMap<>();

		fillMap(F1, f1Map);
		fillMap(precision, precMap);
		fillMap(recall, recallMap);

		Set<String> keys = new HashSet<>(f1Map.keySet());
		List<String> sortedKeys = new ArrayList<>(keys);

		Collections.sort(sortedKeys);

		Map<String, Score> goldScoreMap = new HashMap<>();
		Map<String, Score> predScoreMap = new HashMap<>();
		Map<String, Score> goldCovScoreMap = new HashMap<>();
		Map<String, Score> predCovScoreMap = new HashMap<>();

		toScoreMap(f1Map, precMap, recallMap, sortedKeys, "gold", goldScoreMap);
		toScoreMap(f1Map, precMap, recallMap, sortedKeys, "goldCov", goldCovScoreMap);
		toScoreMap(f1Map, precMap, recallMap, sortedKeys, "pred", predScoreMap);
		toScoreMap(f1Map, precMap, recallMap, sortedKeys, "predCov", predCovScoreMap);

		String lines = "";
		String type = "", cardinality = "";
		Object overall = "";
		for (String key : sortedKeys) {

			String line = key + "}$^{}$";

			line += "\t" + predScoreMap.get(key).toTSVString() + "\t\t" + predCovScoreMap.get(key).toTSVString()
					+ "\t\t" + goldScoreMap.get(key).toTSVString();
			line = line.replaceAll("\t", " & ");

			if (line.startsWith("Root")) {
				type = "\\emph{"+line.replaceFirst("Root", "type");
			} else if (line.startsWith("Cardinality")) {
				cardinality = "\\emph{"+line.replaceFirst("Cardinality", "cardinality");
			} else if (line.startsWith("Overall")) {
				overall = "\\emph{"+line.replaceFirst("Overall", "overall");
			} else {
				lines += "\\emph{"+line + "\\\\\n";
			}
		}
		String name = "Result";
		String t = "result";
		final String latexTable = String.format(template, name, t, name, type, lines, cardinality, overall);
		System.out.println(latexTable);
		System.out.println();
		System.out.println();
		System.out.println();
	}

	final static String template = "\\begin{table}\n" + "\\caption{Evaluation result of predicting the \\textsc{%s}.}\n"
			+ "\\label{tab:eval_%s}\n" + "\\centering\n" + "\\scriptsize\n" + "\\begin{tabular}{@{}lccccccccccc@{}}\n"
			+ "\\toprule\n" + "\\textsc{%s}$^C$  &  & joint &  &  &  & entity &  &  &  & relation &  \\\\ \\midrule\n"
			+ "Macro   & $F_1$ & P & R && $F_1$ & P & R & & $F_1$ & P & R \\\n"
			+ " \\cmidrule(lr){1-1} \\cmidrule(lr){2-4} \\cmidrule(lr){6-8} \\cmidrule(rl){10-12} \n" + "%s\\\\ \n"
			+ "%s" + "\\cmidrule(lr){1-1} \\cmidrule(lr){2-4} \\cmidrule(lr){6-8} \\cmidrule(rl){10-12} \n" + "%s\\\\"
			+ "\\cmidrule(lr){1-1} \\cmidrule(lr){2-4} \\cmidrule(lr){6-8} \\cmidrule(rl){10-12}\n" + "%s"
			+ " \\\\ \\bottomrule\n" + "\\end{tabular}\n" + "\\end{table}";

	private static void toScoreMap(Map<String, Map<String, Double>> f1Map, Map<String, Map<String, Double>> precMap,
			Map<String, Map<String, Double>> recallMap, List<String> keysSorted, String mode,
			Map<String, Score> goldScoreMap) {
		for (String key : keysSorted) {
			Score s = new Score(f1Map.get(key).get(mode), precMap.get(key).get(mode), recallMap.get(key).get(mode));
			goldScoreMap.put(key, s);
		}
	}

	private static void fillMap(List<String> F1, Map<String, Map<String, Double>> f1Map) {
		boolean skipFirst = true;
		for (String string : F1) {

			if (skipFirst) {
				skipFirst = false;
				continue;
			}
			if (string.trim().isEmpty())
				break;

			if (!string.startsWith("Sorted macro"))
				continue;

			String data[] = string.split("\t");

			String key = data[0].trim();
			if (key.equals("Sorted macro Both score"))
				continue;
			if (key.equals("Sorted macro Full score"))
				key = "Overall";
			if (key.equals("Sorted macro Cardinality score"))
				key = "Cardinality";
			if (key.equals("Sorted macro Investigation score"))
				key = "hasInvestigationMethod";
			if (key.equals("Sorted macro Reference score"))
				key = "hasReferenceGroup";
			if (key.equals("Sorted macro Target score"))
				key = "hasTargetGroup";
			if (key.equals("Sorted macro Trend score"))
				key = "hasTrend";

			double gold = Double.parseDouble(data[1]);
			double goldCov = Double.parseDouble(data[2]);
			double pred = Double.parseDouble(data[3]);
			double predCov = Double.parseDouble(data[4]);

			f1Map.put(key, new HashMap<>());
			f1Map.get(key).put("gold", gold);
			f1Map.get(key).put("pred", pred);
			f1Map.get(key).put("goldCov", goldCov);
			f1Map.get(key).put("predCov", predCov);
		}
	}




}
