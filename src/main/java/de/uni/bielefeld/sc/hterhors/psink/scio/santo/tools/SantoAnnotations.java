package de.uni.bielefeld.sc.hterhors.psink.scio.santo.tools;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SantoAnnotations {

	private static int annodbid = 0;

	final private Set<String> rdf;
	final private Map<String, Set<String>> annodb;

	public SantoAnnotations(Set<String> rdf, Map<String, Set<String>> annodb) {
		this.rdf = rdf;
		this.annodb = annodb;
	}

	public Set<String> getRdf() {
		return rdf;
	}

	public Set<String> getAnnodb() {
		return annodb.entrySet().stream()
				.map(e -> new String(annodbid++ + ", " + e.getKey() + "\"" + toWSSepList(e.getValue()) + "\""))
				.collect(Collectors.toSet());
	}

	public void addInstanceToAnnotation(final String annotation, String instance) {
		if (!annodb.containsKey(annotation))
			annodb.put(annotation, new HashSet<>());
		annodb.get(annotation).add(instance);
	}

	private String toWSSepList(Set<String> value) {
		String x = "";
		for (String string : value) {
			x += string + " ";
		}

		return x.trim().replaceAll("\"", "\\\\\"");
	}
}