package de.uni.bielefeld.sc.hterhors.psink.scio.tools.results;

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
 * excel . EXAMPLE INPUT:
 * 
 * samplingMode: TYPE_BASED
 * 
 * assignmentMode: TREATMENT_ORGANISM_MODEL_INJURY
 * 
 * cardinalityMode: GOLD_CARDINALITY
 * 
 * groupNameProcessingMode: GOLD_CLUSTERING
 * 
 * groupNameProviderMode: EMPTY
 * 
 * mainClassProviderMode: GOLD
 * 
 * distinctGroupNamesMode: NOT_DISTINCT
 * 
 * TREATMENTS MICRO: BOTH = Score [ getF1()=0.962, getPrecision()=1.000,
 * getRecall()=0.926, tp=50, fp=0, fn=4, tn=0]
 * 
 * TREATMENTS MICRO: Vehicle = Score [ getF1()=0.857, getPrecision()=1.000,
 * getRecall()=0.750, tp=12, fp=0, fn=4, tn=0]
 * 
 * TREATMENTS MICRO: Non Vehicle = Score [ getF1()=1.000, getPrecision()=1.000,
 * getRecall()=1.000, tp=38, fp=0, fn=0, tn=0]
 * 
 * ORGANISM MODEL MICRO: SCORE = Score [ getF1()=0.974, getPrecision()=1.000,
 * getRecall()=0.950, tp=19, fp=0, fn=1, tn=0]
 * 
 * INJURY MODEL MICRO: SCORE = Score [ getF1()=0.974, getPrecision()=1.000,
 * getRecall()=0.950, tp=19, fp=0, fn=1, tn=0]
 * 
 * EXP GROUP MICRO CARDINALITY = Score [ getF1()=1.000, getPrecision()=1.000,
 * getRecall()=1.000, tp=57, fp=0, fn=0, tn=0]
 * 
 * EXP GROUP MICRO INTERVALL CARDINALITY = 0:1.0 1:1.0 2:1.0 3:1.0
 * 
 * EXP GROUP MICRO CARDINALITY RMSE = 0.0
 * 
 * EXP GROUP MICRO SCORE = Score [ getF1()=0.797, getPrecision()=0.782,
 * getRecall()=0.813, tp=161, fp=45, fn=37, tn=0]
 * 
 * EXP GROUP MICRO: TREATMENT BOTH = Score [ getF1()=0.724,
 * getPrecision()=0.632, getRecall()=0.847, tp=72, fp=42, fn=13, tn=0]
 * 
 * EXP GROUP MICRO: TREATMENT Vehicle = Score [ getF1()=0.606,
 * getPrecision()=0.714, getRecall()=0.526, tp=10, fp=4, fn=9, tn=0]
 * 
 * EXP GROUP MICRO: TREATMENT Non Vehicle = Score [ getF1()=0.747,
 * getPrecision()=0.620, getRecall()=0.939, tp=62, fp=38, fn=4, tn=0]
 * 
 * EXP GROUP MICRO: ORG MODEL = Score [ getF1()=0.828, getPrecision()=0.976,
 * getRecall()=0.719, tp=41, fp=1, fn=16, tn=0]
 * 
 * EXP GROUP MICRO: INJURY MODEL = Score [ getF1()=0.906, getPrecision()=0.960,
 * getRecall()=0.857, tp=48, fp=2, fn=8, tn=0]
 * 
 * 
 * 
 * 
 * 
 * @author hterhors
 *
 */
public class ResultOutPutReaderISWC {

	final private static Pattern modePattern = Pattern.compile("(.*): (.*)");

	final private static Pattern microDataPattern = Pattern.compile(
			"(.*) = Score \\[getF1\\(\\)=(.+?), getPrecision\\(\\)=(.+?), getRecall\\(\\)=(.+?), tp=(.+?), fp=(.+?), fn=(.+?), tn=(.+?)\\]");

	final private static Pattern macroDataPattern = Pattern
			.compile("(.*) = Score \\[macroF1=(.+?), macroPrecision=(.+?), macroRecall=(.+?)\\]");

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

	static public EValue value = EValue.RECALL;

	/*
	 * FOR ISWC
	 */
//	static String folder = "iswc_baseline/results/";
	static String folder = "Final_Evaluation_Results/res/";

	public static void main(String[] args) throws Exception {

		PrintStream ps = new PrintStream(
				new File(folder + "/merged_cardinality_" + value.toString().toLowerCase() + ".csv"));
		Map<String, Map<String, List<Score>>> dataMap = new HashMap<>();
		Map<String, Map<String, String>> modePairsMap = new HashMap<>();
		Set<String> dataNames = new HashSet<>();
		Set<String> modeValueNames = new HashSet<>();
		for (int run = 1; run < 2; run++) {

			File dir = new File(folder + "/res" + run + "/");

			int modeCounter = 0;

			List<String> files = new ArrayList<>(Arrays.asList(dir.list())).stream()
					.filter(n -> n.matches(".*Results.*")).collect(Collectors.toList());

			Collections.sort(files, new Comparator<String>() {

				@Override
				public int compare(String o1, String o2) {
					return Integer.compare(Integer.parseInt(o1.split("_")[0]), Integer.parseInt(o2.split("_")[0]));
				}
			});

			for (String fileName : files) {
				File file = new File(dir, fileName);
				System.out.println(file);

				if (!file.getName().contains("Results"))
					continue;

				final String modeName = "Mode_" + modeCounter;
				modePairsMap.put(modeName, new HashMap<>());
				List<String> readAllLines = Files.readAllLines(file.toPath());
				for (int i = 2; i < 6; i++) {
					ModePair modePair = getModePattern(readAllLines.get(i));
					modePairsMap.get(modeName).put(modePair.mode, modePair.value);
					modeValueNames.add(modePair.mode);
				}
				for (int i = 8; i < readAllLines.size(); i++) {
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

	public static void singleFolder(int run) throws FileNotFoundException, IOException {
		File dir = new File("results/experimentalgroupextratcion/" + folder + "/" + run + "/");

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
				return Integer.compare(Integer.parseInt(o1.split("_")[0]), Integer.parseInt(o2.split("_")[0]));
			}
		});

		PrintStream ps = new PrintStream(new File("results/experimentalgroupextratcion/" + folder + "/" + run
				+ "/single_run_results_" + run + "_" + value.toString().toLowerCase() + ".csv"));
		for (String fileName : files) {
			File file = new File(dir, fileName);
			System.out.println(file);

			if (!file.getName().contains("Results"))
				continue;

			final String modeName = "Mode_" + modeCounter;
			modePairsMap.put(modeName, new HashMap<>());
			List<String> readAllLines = Files.readAllLines(file.toPath());
			for (int i = 2; i < 6; i++) {
				ModePair modePair = getModePattern(readAllLines.get(i));
				modePairsMap.get(modeName).put(modePair.mode, modePair.value);
				modeValueNames.add(modePair.mode);
			}
			for (int i = 8; i < readAllLines.size(); i++) {

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
				return new DataPair(macro.group(1), new Score(Double.parseDouble(macro.group(2)),
						Double.parseDouble(macro.group(3)), Double.parseDouble(macro.group(4))));
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
