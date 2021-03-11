package de.uni.bielefeld.sc.hterhors.psink.scio.tools.oec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class JudgmentTripleBuilder {

	final public static String objectPrefix = "<http://psink.de/scio/";
	final public static String objectSuffix = "> .";
	final public static String IDPrefix = "<http://scio/data/";
	final public static String predicate = "<http://psink.de/scio/hasJudgement>";

	public static void main(String[] args) throws IOException {

		List<String> data = Files.readAllLines(new File("oec/extractedJudgementsFromJulia.csv").toPath());

		sanityCheck(data);

		data.stream().skip(1).forEach(line -> {

			String[] d = line.split("\t");

			String judgement = getJudgement(line, d);

			if (judgement != null) {

				String subjectID = d[0].split("/")[4];
				String subjectname = d[1];

				String triple = buildTriple(subjectID, subjectname, judgement);
				System.out.println(triple);
			}
		});

	}

	private static String buildTriple(String subjectID, String subjectname, String judgement) {
		return IDPrefix + subjectname + "_" + subjectID + " " + predicate + " " + objectPrefix + judgement
				+ objectSuffix;
	}

	private static String getJudgement(String line, String[] d) {
		String judgement = "";
		if (d.length == 5 ) {
			// Julia judgement exists
			judgement = d[4];
		} else if (d.length == 4) {
			if (!d[2].trim().equals("-")) {
				// SANTO judgement exists
				judgement = d[2];
			} else {
				if (!d[3].trim().equals("-")) {
					// Auto judgement exists
					judgement = d[3];
				} else {
					return null;
				}
			}

		} else {
			throw new IllegalArgumentException("Line does not contain enough data: " + line);
		}
		return judgement;
	}

	private static void sanityCheck(List<String> data) {
		System.out.println("First 10 lines of read data:");
		data.stream().limit(10).forEach(System.out::println);
		System.out.println();
	}
}
