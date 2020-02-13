package de.uni.bielefeld.sc.hterhors.psink.scio.playground.preprocessing.sentenceclassification;

public class Classification {

	public final int sentenceIndex;
	public final boolean isRelevant;
	public final double probability;

	public Classification(int sentenceIndex, boolean isRelevant, double probability) {
		this.sentenceIndex = sentenceIndex;
		this.isRelevant = isRelevant;
		this.probability = probability;
	}

	@Override
	public String toString() {
		return "Classification [sentenceIndex=" + sentenceIndex + ", isRelevant=" + isRelevant + ", probability="
				+ probability + "]";
	}

}
