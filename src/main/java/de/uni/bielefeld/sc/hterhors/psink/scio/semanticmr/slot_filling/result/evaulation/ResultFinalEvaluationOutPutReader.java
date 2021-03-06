package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.evaulation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;

/**
 * Small helper class to read the output files of the experimental group
 * extraction.
 * 
 * Reads and converts to other file format e.g. to csv for a better import in
 * ==> result_final_evaluation_GOLD_1000.log <== Unsorted Score: Score
 * [getF1()=0.737, getPrecision()=0.752, getRecall()=0.722, tp=10971, fp=3622,
 * fn=4219, tn=0] Sorted macro Result Sentence Score = Score [macroF1=0.842,
 * macroPrecision=0.838, macroRecall=0.846, macroAddCounter=20] Sorted micro
 * Full score = Score [getF1()=0.756, getPrecision()=0.770, getRecall()=0.743,
 * tp=11264, fp=3359, fn=3902, tn=0] Sorted macro Full score = Score
 * [macroF1=0.851, macroPrecision=0.856, macroRecall=0.846, macroAddCounter=20]
 * Sorted macro Reference score = Score [macroF1=0.851, macroPrecision=0.860,
 * macroRecall=0.842, macroAddCounter=20] Sorted macro Target score = Score
 * [macroF1=0.867, macroPrecision=0.864, macroRecall=0.870, macroAddCounter=20]
 * Sorted macro Trend score = Score [macroF1=0.643, macroPrecision=0.691,
 * macroRecall=0.601, macroAddCounter=20] Sorted macro Investigation score =
 * Score [macroF1=0.706, macroPrecision=0.713, macroRecall=0.700,
 * macroAddCounter=20] Sorted macro Cardinality score = Score [macroF1=0.879,
 * macroPrecision=0.885, macroRecall=0.873, macroAddCounter=20] Sorted macro
 * Both score = Score [macroF1=0.790, macroPrecision=0.802, macroRecall=0.777,
 * macroAddCounter=100] MACRO CoarseGrained overallTrend: Score [macroF1=0.716,
 * macroPrecision=0.771, macroRecall=0.668, macroAddCounter=20] MACRO
 * CoarseGrained overallInvest: Score [macroF1=0.680, macroPrecision=0.633,
 * macroRecall=0.735, macroAddCounter=20] MACRO CoarseGrained overallGroups:
 * Score [macroF1=0.832, macroPrecision=0.843, macroRecall=0.821,
 * macroAddCounter=20] MACRO CoarseGrained overallResult: Score [macroF1=0.769,
 * macroPrecision=0.772, macroRecall=0.767, macroAddCounter=20]
 * CoarseGrainedResultEvaluation Score: Score [macroF1=0.769,
 * macroPrecision=0.772, macroRecall=0.767, macroAddCounter=20]
 * 
 * ==> result_final_evaluation_GOLD_1001.log <== Unsorted Score: Score
 * [getF1()=0.789, getPrecision()=0.798, getRecall()=0.780, tp=12963, fp=3290,
 * fn=3647, tn=0] Sorted macro Result Sentence Score = Score [macroF1=0.910,
 * macroPrecision=0.929, macroRecall=0.891, macroAddCounter=20]
 * 
 * 
 * 
 * @author hterhors
 *
 */
public class ResultFinalEvaluationOutPutReader {

	final private static Pattern modePattern = Pattern.compile("(.*): ?(.*)");

	final private static Pattern microDataPattern = Pattern.compile(
			"(.*) = Score \\[getF1\\(\\)=(.+?), getPrecision\\(\\)=(.+?), getRecall\\(\\)=(.+?), tp=(.+?), fp=(.+?), fn=(.+?), tn=(.+?)\\]");

	final private static Pattern macroDataPattern = Pattern.compile(
			"(.*)(=|:) Score \\[macroF1=(.+?), macroPrecision=(.+?), macroRecall=(.+?), macroAddCounter=.*?\\]");

	final private static Pattern intervalCardinalityPattern = Pattern
			.compile("EXP GROUP MICRO  INTERVALL CARDINALITY = (.*?)	(.*?)	(.*?)	(.*?)");

	static class ModePair {

		final public String mode;
		final public String value;

		public ModePair(String mode, String value) {
			this.mode = mode;
			this.value = value;
		}

		@Override
		public String toString() {
			return "ModePair [mode=" + mode + ", value=" + value + "]";
		}

	}

	static class DataPair {

		final public String mode;
		final public Score score;

		public DataPair(String mode, Score score) {
			this.mode = mode;
			this.score = score;
		}

		@Override
		public String toString() {
			return "DataPair [mode=" + mode + ", score=" + score + "]";
		}

	}

	enum EValue {

		PRECISION, RECALL, F1;

	}

//	static public EValue value = EValue.F1;
//	static public EValue value = EValue.RECALL;
	static public EValue value = EValue.PRECISION;

	/*
	 * for run for F1 RECALL and PRECISION
	 */
//	static String folder = "final_evaluation_results/result/reg_ex_results/evaluations";
//	static String folder = "final_evaluation_results/result/fast_text_results/evaluations";
//	static String folder = "final_evaluation_results/result_new/evaluations";
	static String folder = "dissertation_results/result/evaluations";

	public static void main(String[] args) throws Exception {

		preProcess();

		PrintStream ps = new PrintStream(
				new File(folder + "/merged_cardinality_" + value.toString().toLowerCase() + ".csv"));
		Map<String, Map<String, List<Score>>> dataMap = new HashMap<>();
		Map<String, Map<String, String>> modePairsMap = new HashMap<>();
		Set<String> dataNames = new HashSet<>();
		Set<String> modeValueNames = new HashSet<>();
		for (int run = 0; run < 10; run++) {

			File dir = new File(folder + "/" + run);

			int modeCounter = 0;

			List<String> files = new ArrayList<>(Arrays.asList(dir.list())).stream()
					.filter(n -> n.matches(".*Results.*")).collect(Collectors.toList());

			Collections.sort(files, new Comparator<String>() {

				@Override
				public int compare(String o1, String o2) {
					return o1.split("_")[0].compareTo("" + (o2.split("_")[0]));
				}
			});

			for (String fileName : files) {
				File file = new File(dir, fileName);
				System.out.println(file);

				final String modeName = "Mode_" + modeCounter;
				modePairsMap.put(modeName, new HashMap<>());
				List<String> readAllLines = Files.readAllLines(file.toPath());
				for (int i = 0; i < 1; i++) {
					ModePair modePair = getModePattern(readAllLines.get(i));
					modePairsMap.get(modeName).put(modePair.mode, modePair.value);
					modeValueNames.add(modePair.mode);
				}
				for (int i = 1; i < readAllLines.size(); i++) {
					DataPair dataPair = getDataPattern(readAllLines.get(i));

					if (dataPair == null)
						continue;

					dataMap.putIfAbsent(dataPair.mode, new HashMap<>());
					if (!dataMap.get(dataPair.mode).containsKey(modeName)) {
						dataMap.get(dataPair.mode).put(modeName, new ArrayList<>());
					}
					dataMap.get(dataPair.mode).get(modeName).add(dataPair.score);

					dataNames.add(dataPair.mode);
				}
				modeCounter++;
			}

			singleFolder(run);
		}
		List<String> modeNames = new ArrayList<>(modePairsMap.keySet());
		Collections.sort(modeNames, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				return Integer.compare(Integer.parseInt(o1.split("_")[1]), Integer.parseInt(o2.split("_")[1]));
			}
		});

		ps.println(toModeNamesHeaderLine(modeNames));

		List<String> dataNameList = new ArrayList<>(dataNames);
		Collections.sort(dataNameList);

		for (String dataName : dataNameList) {
			StringBuffer buffer = new StringBuffer(dataName);
			buffer.append("\t");
			for (String modeName : modeNames) {
				Score s = null;
				for (Score score : dataMap.get(dataName).get(modeName)) {
					if (s == null)
						s = score;
					s.add(score);
				}
				if (value == EValue.PRECISION)
					buffer.append(s.getPrecision(Score.SCORE_FORMAT));

				else if (value == EValue.F1)
					buffer.append(s.getF1(Score.SCORE_FORMAT));
				else
					buffer.append(s.getRecall(Score.SCORE_FORMAT));

				buffer.append("\t");

			}

			ps.println(buffer.toString().trim());

		}

		for (int i = 0; i < 20; i++) {
			ps.println();
		}

		List<String> mnvpl = new ArrayList<>(modeValueNames);
		Collections.sort(mnvpl);

		// header
		ps.print("Mode \t");
		for (String modeName : mnvpl) {
			ps.print(modeName + "\t");
		}

		ps.println();
		for (String modeName : modeNames) {
			ps.print(modeName + "\t");

			for (String mn : mnvpl) {

				ps.print(modePairsMap.get(modeName).get(mn) + "\t");

			}
			ps.println();
		}

		ps.close();

	}

	private static void preProcess() throws IOException {

		List<String> lines = Files.readAllLines(new File(folder, "results").toPath());

		for (int i = 0; i < 10; i++) {
			new File(folder, "" + i).mkdirs();
		}

		int counter = 0;
		PrintStream ps = null;

		for (String line : lines) {

			if (line.contains("==>")) {

				String prefix = "";

				if (line.contains("GOLD_COVERAGE")) {
					prefix = "GOLD-COVERAGE";
				} else if (line.contains("GOLD")) {
					prefix = "GOLD";
				} else if (line.contains("PREDICT_COVERAGE")) {
					prefix = "PREDICT-COVERAGE";
				} else {
					prefix = "PREDICT";
				}

				if (counter == 10)
					counter = 0;
				ps = new PrintStream(new File(folder, counter + "/" + prefix + "_Results_" + counter));

				ps.println("mode:" + prefix);

				counter++;
			} else
				ps.println(line);

		}

	}

	public static void singleFolder(int run) throws FileNotFoundException, IOException {
		File dir = new File(folder + "/" + run);

		int modeCounter = 0;

		// ModeName, modeNameValuename modeNameValue
		Map<String, Map<String, String>> modePairsMap = new HashMap<>();

		// DataName, <ModeName , Score>
		Map<String, Map<String, Score>> dataMap = new HashMap<>();
		Set<String> dataNames = new HashSet<>();
		Set<String> modeValueNames = new HashSet<>();
		List<String> files = new ArrayList<>(Arrays.asList(dir.list())).stream().filter(n -> n.matches(".*Results.*"))
				.collect(Collectors.toList());

		Collections.sort(files, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				return o1.split("_")[0].compareTo("" + (o2.split("_")[0]));
			}
		});

		PrintStream ps = new PrintStream(
				new File(folder + "/" + run + "/single_run_results_" + value.toString().toLowerCase() + ".csv"));

		for (String fileName : files) {
			File file = new File(dir, fileName);
			System.out.println("single folder: " + file);

			final String modeName = "Mode_" + modeCounter;
			modePairsMap.put(modeName, new HashMap<>());
			List<String> readAllLines = Files.readAllLines(file.toPath());
			for (int i = 0; i < 1; i++) {
				ModePair modePair = getModePattern(readAllLines.get(i));
				modePairsMap.get(modeName).put(modePair.mode, modePair.value);
				modeValueNames.add(modePair.mode);
			}
			for (int i = 1; i < readAllLines.size(); i++) {

				DataPair dataPair = getDataPattern(readAllLines.get(i));
				if (dataPair == null)
					continue;
				dataMap.putIfAbsent(dataPair.mode, new HashMap<>());
				dataMap.get(dataPair.mode).put(modeName, dataPair.score);
				dataNames.add(dataPair.mode);
			}
			modeCounter++;
		}
		List<String> modeNames = new ArrayList<>(modePairsMap.keySet());
		Collections.sort(modeNames, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				return Integer.compare(Integer.parseInt(o1.split("_")[1]), Integer.parseInt(o2.split("_")[1]));
			}
		});

		ps.println(toModeNamesHeaderLine(modeNames));

		List<String> dataNameList = new ArrayList<>(dataNames);
		Collections.sort(dataNameList);

		for (String dataName : dataNameList) {
			StringBuffer buffer = new StringBuffer(dataName);
			buffer.append("\t");
			for (String modeName : modeNames) {

				if (value == EValue.PRECISION)
					buffer.append(dataMap.get(dataName).get(modeName).getPrecision(Score.SCORE_FORMAT));

				else if (value == EValue.F1)
					buffer.append(dataMap.get(dataName).get(modeName).getF1(Score.SCORE_FORMAT));
				else
					buffer.append(dataMap.get(dataName).get(modeName).getRecall(Score.SCORE_FORMAT));
				buffer.append("\t");
			}

			ps.println(buffer.toString().trim());

		}

		for (int i = 0; i < 20; i++) {
			ps.println();
		}

		List<String> mnvpl = new ArrayList<>(modeValueNames);
		Collections.sort(mnvpl);

		// header
		ps.print("Mode \t");
		for (String modeName : mnvpl) {
			ps.print(modeName + "\t");
		}

		ps.println();
		for (String modeName : modeNames) {
			ps.print(modeName + "\t");

			for (String mn : mnvpl) {

				ps.print(modePairsMap.get(modeName).get(mn) + "\t");

			}
			ps.println();
		}

		ps.close();
	}

	private static String toModeNamesHeaderLine(List<String> l) {

		StringBuffer line = new StringBuffer("DataName");
		line.append("\t");
		for (String string : l) {
			line.append(string);
			line.append("\t");
		}
		return line.toString().trim();
	}

	private static DataPair getDataPattern(String string) {
		Matcher micro = microDataPattern.matcher(string);

		if (micro.find()) {
			return new DataPair(micro.group(1),
					new Score(Integer.parseInt(micro.group(5)), Integer.parseInt(micro.group(6)),
							Integer.parseInt(micro.group(7)), Integer.parseInt(micro.group(8))));
		} else {
			Matcher macro = macroDataPattern.matcher(string);
			if (macro.find()) {
				return new DataPair(macro.group(1), new Score(Double.parseDouble(macro.group(3)),
						Double.parseDouble(macro.group(4)), Double.parseDouble(macro.group(5))));
			} else {
				return null;
			}
		}

	}

	private static ModePair getModePattern(String string) {
		Matcher m = modePattern.matcher(string);
		m.find();
		return new ModePair(m.group(1), m.group(2));
	}
}
