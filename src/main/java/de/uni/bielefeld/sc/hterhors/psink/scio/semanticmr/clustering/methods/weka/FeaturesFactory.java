package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.methods.weka;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.hterhors.semanticmr.crf.templates.helper.LevenShteinSimilarities;
import de.hterhors.semanticmr.crf.variables.Document;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.helper.GroupNamePair;

public class FeaturesFactory {
	private static GroupNamePair groupNamePair;
	private static Map<String, Double> features;

	public static void set(Map<String, Double> features, GroupNamePair groupNamePair) {
		FeaturesFactory.groupNamePair = groupNamePair;
		FeaturesFactory.features = features;
	}

	/**
	 * Levensthein between both terms
	 */
	public static void levenshtein() {
		double sim = LevenShteinSimilarities.levenshteinSimilarity(groupNamePair.groupName1.getSurfaceForm(),
				groupNamePair.groupName2.getSurfaceForm(), 100);

		features.put("LEVENSHTEIN", sim);
	}

	public static void overlap() {
		/**
		 * Term overlap
		 */
		String[] g1 = groupNamePair.groupName1.getSurfaceForm().split("\\W");
		String[] g2 = groupNamePair.groupName2.getSurfaceForm().split("\\W");
		double countOverlap = 0;

		for (int i = 0; i < g1.length; i++) {
			for (int j = 0; j < g2.length; j++) {
				if (g1[i].isEmpty() || g2[j].isEmpty())
					continue;
				countOverlap += g1[i].equals(g2[j]) ? 1 : 0;
				features.put(g1[i] + ":" + g2[j], 1D);
			}
		}
		features.put("COUNT_OVERLAP", countOverlap);

	}

	private static final String TOKEN_SPLITTER_SPACE = "";

	private static final String END_SIGN = "$";

	private static final String START_SIGN = "^";

	private static final int MIN_TOKEN_LENGTH = 2;

	public static void charBasedNGrams() {

		List<String> g1 = getCharNgrams(groupNamePair.groupName1.getSurfaceForm());
		List<String> g2 = getCharNgrams(groupNamePair.groupName2.getSurfaceForm());
		for (int i = 0; i < g1.size(); i++) {
			for (int j = 0; j < g2.size(); j++) {
				if (g1.get(i).isEmpty() || g2.get(j).isEmpty())
					continue;
//				countOverlap +=g1.get(i).equals(g2.get(j)) ? 1 : 0;
				features.put(g1.get(i) + ":" + g2.get(j), 1D);
			}
		}
	}

	private static List<String> getCharNgrams(String surfaceForm) {

		List<String> nGrams = new ArrayList<>();

		final String cM = START_SIGN + TOKEN_SPLITTER_SPACE + surfaceForm + TOKEN_SPLITTER_SPACE + END_SIGN;

		final String[] tokens = cM.split("");

		final int maxNgramSize = Math.min(10, tokens.length);

		for (int ngram = 3; ngram <= maxNgramSize; ngram++) {
			for (int i = 0; i < maxNgramSize - 1; i++) {

				/*
				 * Do not include start symbol.
				 */
				if (i + ngram == 1)
					continue;

				/*
				 * Break if size exceeds token length
				 */
				if (i + ngram > maxNgramSize)
					break;

				final StringBuffer fBuffer = new StringBuffer();
				for (int t = i; t < i + ngram; t++) {

					if (tokens[t].isEmpty())
						continue;

					if (Document.getStopWords().contains(tokens[t].toLowerCase()))
						continue;

					fBuffer.append(tokens[t]).append(TOKEN_SPLITTER_SPACE);

				}

				final String featureName = fBuffer.toString().trim();

				if (featureName.length() < MIN_TOKEN_LENGTH)
					continue;

				if (featureName.isEmpty())
					continue;

				nGrams.add(featureName);

			}
		}
		return nGrams;
	}
}
