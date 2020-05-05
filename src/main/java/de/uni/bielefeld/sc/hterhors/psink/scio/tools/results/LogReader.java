package de.uni.bielefeld.sc.hterhors.psink.scio.tools.results;

import java.io.File;
import java.io.PrintStream;
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

public class LogReader {

	public static void main(String[] args) throws Exception {

		new LogReader();

	}

	public final static Pattern modePattern = Pattern.compile(".*output(\\d{1,2})_(\\d{3})\\.log");
	public final static Pattern trainingTimePattern = Pattern
			.compile("CRFStatistics \\[context=Train, getTotalDuration\\(\\)=(\\d+)\\]");
	public final static Pattern testTimePattern = Pattern
			.compile("CRFStatistics \\[context=Test, getTotalDuration\\(\\)=(\\d+)\\]");
	public final static Pattern statesGeneratedPattern = Pattern.compile("States generated( in total)?: (\\d+)");

	public LogReader() throws Exception {

		File logDir = new File("results/experimentalgroupextratcion/iswc_run_high_recall_50/logs/");

		File output = new File("results/experimentalgroupextratcion/iswc_run_high_recall_50/logs/stats.log");
		PrintStream ps = new PrintStream(output);
		Map<String, Map<String, List<Double>>> data = new HashMap<>();

		for (File logCollection : logDir.listFiles()) {
		
			if (!logCollection.isDirectory())
				continue;
			
			System.out.println("Folder: " + logCollection);
			for (File log : logCollection.listFiles()) {
				System.out.println("File: " + log);

				Matcher modeMatcher = modePattern.matcher(log.getName());
				modeMatcher.find();
				String modeID = modeMatcher.group(1);

				Set<Double> statesGens = new HashSet<>();
				Set<Double> trainingTimes = new HashSet<>();
				Set<Double> testTimes = new HashSet<>();
				for (String line : Files.readAllLines(log.toPath())) {
					Matcher trainingTimeMatcher = trainingTimePattern.matcher(line);
					Matcher testTimeMatcher = testTimePattern.matcher(line);
					Matcher statesGenMatcher = statesGeneratedPattern.matcher(line);
					if (trainingTimeMatcher.find()) {
						// to seconds per document in training
						trainingTimes.add(Double.valueOf(trainingTimeMatcher.group(1)) / (1000 * 76));
					}
					if (testTimeMatcher.find()) {
						testTimes.add(Double.valueOf(testTimeMatcher.group(1)) / (1000 * 20));
					}
					// per document
					if (statesGenMatcher.find()) {
						statesGens.add(Double.valueOf(statesGenMatcher.group(2)) / (1000*96));
					}

				}

				data.putIfAbsent(modeID, new HashMap<>());
				data.get(modeID).putIfAbsent("trainingTime", new ArrayList<>());
				data.get(modeID).putIfAbsent("testTime", new ArrayList<>());
				data.get(modeID).putIfAbsent("statesGen", new ArrayList<>());

				data.get(modeID).get("trainingTime").addAll(trainingTimes);
				data.get(modeID).get("testTime").addAll(testTimes);
				data.get(modeID).get("statesGen").addAll(statesGens);

			}
		}
		Map<String, Map<String, Double>> mergedData = new HashMap<>();

		for (String modeID : data.keySet()) {
			mergedData.put(modeID, new HashMap<>());
			for (Entry<String, List<Double>> dataForMode : data.get(modeID).entrySet()) {

				double value = 0;
				for (Double dataValue : dataForMode.getValue()) {
					value += dataValue;
				}

				value /= dataForMode.getValue().size();
				mergedData.get(modeID).put(dataForMode.getKey(), value);
			}

		}

		for (Entry<String, Map<String, Double>> d : mergedData.entrySet()) {
			ps.println(d);
		}
		ps.flush();
		ps.close();
	}

}
