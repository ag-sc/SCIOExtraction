package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.binaryclassification;

import java.util.List;
import java.util.Set;

import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;

public class DLAPredictions implements Comparable<DLAPredictions> {
	final public Set<DocumentLinkedAnnotation> collection;
	final public List<Double> probabilities;

	public DLAPredictions(Set<DocumentLinkedAnnotation> collection, List<Double> probabilities) {
		this.collection = collection;
		this.probabilities = probabilities;
	}

	public double getProbability() {
		return probabilities.stream().reduce(0D, Double::sum) / probabilities.size();
	}

	@Override
	public int compareTo(DLAPredictions o) {
		return -Double.compare(getProbability(), o.getProbability());
	}

	@Override
	public String toString() {
		return "DLAPredictions [getProbability()=" + getProbability() + ", collection="
				+ collection.stream().map(a -> a.toPrettyString() + "\n").reduce("", String::concat).trim() + "]";
	}

}
