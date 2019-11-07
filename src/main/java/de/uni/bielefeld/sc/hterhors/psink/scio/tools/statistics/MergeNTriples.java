package de.uni.bielefeld.sc.hterhors.psink.scio.tools.statistics;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MergeNTriples {

	public static void main(String[] args) throws IOException {
		new MergeNTriples(new File("unroll/export_25092019/"), new File("unroll/merge_export_25092019.n-triples"));
	}

	final public static String resultType = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://psink.de/scio/Result> .";
	final public static String rdfsComment = "<http://www.w3.org/2000/01/rdf-schema#comment>";

	public MergeNTriples(File rawDataDirectory, File outputFile) throws IOException {

		if (!rawDataDirectory.isDirectory())
			throw new RuntimeException("Input File is not a directory: " + rawDataDirectory.getAbsolutePath());

		PrintStream outPS = new PrintStream(outputFile);
		Set<String> usedResultDomains = new HashSet<>();
		Arrays.stream(rawDataDirectory.listFiles()).filter(f -> f.getName().endsWith("n-triples")).forEach(file -> {
			try {
				Files.readAllLines(file.toPath()).forEach(line -> {

					String domain = line.split("> <", 2)[0] + ">";

					if (line.endsWith(resultType)) {
						if (!usedResultDomains.contains(domain)) {
							usedResultDomains.add(domain);
							outPS.println(
									domain + " " + rdfsComment + " \"" + file.getName().split("_Jessica")[0] + "\" .");
						}
					}
					outPS.println(line);
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		outPS.flush();
		outPS.close();
	}

}
