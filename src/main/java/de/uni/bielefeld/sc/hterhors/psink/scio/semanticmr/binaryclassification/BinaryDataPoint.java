package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.binaryclassification;

import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;

public class BinaryDataPoint {

	public final ELabel goldLabel;
	public final DocumentLinkedAnnotation annotation1;
	public final DocumentLinkedAnnotation annotation2;
	public final Instance instance;

	public BinaryDataPoint(Instance instance, DocumentLinkedAnnotation annotation1,
			DocumentLinkedAnnotation annotation2, ELabel label) {
		this.annotation1 = annotation1;
		this.annotation2 = annotation2;
		this.goldLabel = label;
		this.instance = instance;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotation1 == null) ? 0 : annotation1.hashCode());
		result = prime * result + ((annotation2 == null) ? 0 : annotation2.hashCode());
		result = prime * result + ((goldLabel == null) ? 0 : goldLabel.hashCode());
		result = prime * result + ((instance == null) ? 0 : instance.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BinaryDataPoint other = (BinaryDataPoint) obj;
		if (annotation1 == null) {
			if (other.annotation1 != null)
				return false;
		} else if (!annotation1.equals(other.annotation1))
			return false;
		if (annotation2 == null) {
			if (other.annotation2 != null)
				return false;
		} else if (!annotation2.equals(other.annotation2))
			return false;
		if (goldLabel != other.goldLabel)
			return false;
		if (instance == null) {
			if (other.instance != null)
				return false;
		} else if (!instance.equals(other.instance))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BinaryDataPoint [label=" + goldLabel + ", annotation1=" + annotation1.toPrettyString() + ", annotation2="
				+ annotation2.toPrettyString() + ", instance=" + instance.getName() + "]";
	}

}
