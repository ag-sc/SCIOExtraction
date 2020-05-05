package de.uni.bielefeld.sc.hterhors.psink.scio.playground.preprocessing.sentenceclassification;

import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;

public class Classification {

	public final int sentenceIndex;
	public final boolean isRelevant;
	public final double probability;
	public final Score s;

	public Classification(int sentenceIndex, boolean isRelevant, double probability, Score s) {
		this.sentenceIndex = sentenceIndex;
		this.isRelevant = isRelevant;
		this.probability = probability;
		this.s = s;
	}

	@Override
	public String toString() {
		return "Classification [sentenceIndex=" + sentenceIndex + ", isRelevant=" + isRelevant + ", probability="
				+ probability + ", s=" + s + "]";
	}

}
