package de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper;

import de.hterhors.semanticmr.crf.structure.EntityType;

public class CorpusBuilderHelper {

	public static String toDirName(EntityType entityType) {
		StringBuffer buffer = new StringBuffer();

		String[] parts = entityType.name.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
		for (int i = 0; i < parts.length - 1; i++) {
			buffer.append(parts[i].toLowerCase());
			buffer.append("_");
		}
		buffer.append(parts[parts.length - 1].toLowerCase());
		return buffer.toString();
	}
}
