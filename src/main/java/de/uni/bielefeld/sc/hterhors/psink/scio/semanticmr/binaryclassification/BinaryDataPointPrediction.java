package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.binaryclassification;

public class BinaryDataPointPrediction {

	public final double confidence;
	public final ELabel predictedLabel;
	public final BinaryDataPoint binaryDataPoint;

	public BinaryDataPointPrediction(BinaryDataPoint binaryDataPoint, ELabel prediction, double confidence) {
		this.binaryDataPoint = binaryDataPoint;
		this.predictedLabel = prediction;
		this.confidence = confidence;
	}

	public boolean correctClassified() {
		return binaryDataPoint.goldLabel.equals(predictedLabel);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((binaryDataPoint == null) ? 0 : binaryDataPoint.hashCode());
		long temp;
		temp = Double.doubleToLongBits(confidence);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((predictedLabel == null) ? 0 : predictedLabel.hashCode());
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
		BinaryDataPointPrediction other = (BinaryDataPointPrediction) obj;
		if (binaryDataPoint == null) {
			if (other.binaryDataPoint != null)
				return false;
		} else if (!binaryDataPoint.equals(other.binaryDataPoint))
			return false;
		if (Double.doubleToLongBits(confidence) != Double.doubleToLongBits(other.confidence))
			return false;
		if (predictedLabel != other.predictedLabel)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BinaryDataPointPrediction [correctClassified()=" + correctClassified() + ", confidence=" + confidence
				+ ", prediction=" + predictedLabel + ", binaryDataPoint=" + binaryDataPoint + "]";
	}

}
