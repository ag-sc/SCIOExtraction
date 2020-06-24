package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.AbstractNumericInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ISingleUnit;

public class TemperatureInterpreter extends AbstractNumericInterpreter {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static public enum ETemperatureUnits implements ISingleUnit {

		undef(0), c(1d);

		final public double factor;

		private ETemperatureUnits(double factor) {
			this.factor = factor;
		}

		public static ETemperatureUnits getDefault() {
			return c;
		}

		@Override
		public double getFactor() {
			return factor;
		}

		@Override
		public String getName() {
			return this.name();
		}
	}

	final static String aboutGroupName = "about";
	final static String pattern1GrouName = "pattern1GrouName";
	final static String pattern2GrouName = "pattern2GrouName";
	final static String pattern3GrouName = "pattern3GrouName";

	final private static String p1Numbers1 = "p1Numbers1";
	final private static String p1Numbers2 = "p1Numbers2";
	final private static String p1Unit1 = "p1Unit1";
	final private static String p1Unit2 = "p1Unit2";

	final private static String p2Numbers1 = "p2Numbers1";
	final private static String p2Numbers2 = "p2Numbers2";
	final private static String p2Unit1 = "p2Unit1";

	final private static String p3Numbers1 = "p3Numbers1";
	final private static String p3Unit1 = "p3Unit1";

	final static String numbers_ = "((\\d+\\.\\d+)|\\d+)";
	final static String freeSpace_ = "[^\\d\\w\\.,]";
	final static String freeSpaceQuestionMark_ = "[^\\d\\w\\.,]?";
	final static String unit_ = "(C(elsius)?)";
	final static String about_ = "(~|" + PRE_BOUNDS + "about" + freeSpace_ + ")?";
	final static String connection_ = "(" + freeSpace_ + "(to|and)" + freeSpace_ + "|" + freeSpaceQuestionMark_
			+ "((\\+?(/|\\\\)?-)|±|" + freeSpace_ + ")" + freeSpaceQuestionMark_ + ")";

	final static String pattern1_ = "(?<" + p1Numbers1 + ">" + numbers_ + ")" + freeSpaceQuestionMark_ + "(?<" + p1Unit1
			+ ">" + unit_ + ")" + connection_ + "(?<" + p1Numbers2 + ">" + numbers_ + ")" + freeSpaceQuestionMark_
			+ "(?<" + p1Unit2 + ">" + unit_ + ")?";

	final static String pattern2_ = "(?<" + p2Numbers1 + ">" + numbers_ + ")" + connection_ + "(?<" + p2Numbers2 + ">"
			+ numbers_ + ")" + freeSpaceQuestionMark_ + "(?<" + p2Unit1 + ">" + unit_ + ")";
	final static String pattern3_ = "(?<" + p3Numbers1 + ">" + numbers_ + ")" + freeSpaceQuestionMark_ + "(?<" + p3Unit1
			+ ">" + unit_ + ")";

	public final static Pattern PATTERN = Pattern.compile("(?<" + aboutGroupName + ">" + about_ + ")((?<"
			+ pattern1GrouName + ">" + pattern1_ + ")|(?<" + pattern2GrouName + ">" + pattern2_ + ")|(?<"
			+ pattern3GrouName + ">" + pattern3_ + "))" + POST_BOUNDS, PATTERN_BITMASK);

	final public ETemperatureUnits unit;
	final public double meanValue;
	final public boolean about;
	final public double fromValue;
	final public double toValue;

	private TemperatureInterpreter(String surfaceForm, ETemperatureUnits unit, double meanValue, boolean about,
			double fromValue, double toValue) {
		super(surfaceForm);
		this.unit = unit;
		this.meanValue = meanValue;
		this.about = about;
		this.fromValue = fromValue;
		this.toValue = toValue;
	}

	public TemperatureInterpreter convertTo(ETemperatureUnits toWeightUnit) {
		return new TemperatureInterpreter(surfaceForm, toWeightUnit, convertValue(meanValue, toWeightUnit), about,
				convertValue(fromValue, toWeightUnit), convertValue(toValue, toWeightUnit));
	}

	public static double convertValue(double value, ETemperatureUnits fromWeightUnit, ETemperatureUnits toWeightUnit) {
		return (value * fromWeightUnit.factor) / toWeightUnit.factor;
	}

	private double convertValue(double value, ETemperatureUnits toWeightUnit) {
		return (value * unit.factor) / toWeightUnit.factor;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (about ? 1231 : 1237);
		long temp;
		temp = Double.doubleToLongBits(fromValue);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(meanValue);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(toValue);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
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
		TemperatureInterpreter other = (TemperatureInterpreter) obj;
		if (about != other.about)
			return false;
		if (Double.doubleToLongBits(fromValue) != Double.doubleToLongBits(other.fromValue))
			return false;
		if (Double.doubleToLongBits(meanValue) != Double.doubleToLongBits(other.meanValue))
			return false;
		if (Double.doubleToLongBits(toValue) != Double.doubleToLongBits(other.toValue))
			return false;
		if (unit != other.unit)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SemanticWeight [surfaceForm=" + surfaceForm + ", unit=" + unit + ", meanValue=" + meanValue + ", about="
				+ about + ", fromValue=" + fromValue + ", toValue=" + toValue + "]";
	}

	public TemperatureInterpreter normalize() {
		return convertTo(ETemperatureUnits.getDefault());
	}

	@Override
	public double getMeanValue() {
		return meanValue;
	}

	@Override
	public String asFormattedString() {
		return (about ? "about " : "") + DECIMAL_FORMAT.format(meanValue) + " " + unit;
	}

	final public static ETemperatureUnits defaultUnit = ETemperatureUnits.undef;
	final public static float defaultFromValue = 0;
	final public static float defaultToValue = 0;
	final public static float defaultMeanValue = 0;
	final public static float defaultVarianceValue = 0;
	final public static boolean defaultAbout = false;

	public TemperatureInterpreter(String surfaceForm) {
		super(surfaceForm);

		ETemperatureUnits unit = defaultUnit;
		double meanValue = defaultMeanValue;
		double fromValue = defaultFromValue;
		double varianceValue = defaultVarianceValue;
		double toValue = defaultToValue;
		boolean about = defaultAbout;

		Matcher matcher = TemperatureInterpreter.PATTERN.matcher(surfaceForm);

		if (matcher.find()) {

			if (matcher.group(0).equals(surfaceForm)) {
				interpretable = true;

				about = matcher.group(TemperatureInterpreter.aboutGroupName) != null
						&& !matcher.group(TemperatureInterpreter.aboutGroupName).trim().isEmpty();

				if (matcher.group(TemperatureInterpreter.pattern1GrouName) != null) {
					double fromValue_ = defaultFromValue;
					if (matcher.group(TemperatureInterpreter.p1Unit1) != null)
						unit = ETemperatureUnits
								.valueOf(mapVariation(matcher.group(TemperatureInterpreter.p1Unit1).toLowerCase()));
					if (matcher.group(TemperatureInterpreter.p1Numbers1) != null)
						fromValue_ = Double.valueOf(matcher.group(TemperatureInterpreter.p1Numbers1));
					if (matcher.group(TemperatureInterpreter.p1Numbers2) != null) {
						ETemperatureUnits unit_;
						if (matcher.group(TemperatureInterpreter.p1Unit2) != null) {
							unit_ = ETemperatureUnits
									.valueOf(mapVariation(matcher.group(TemperatureInterpreter.p1Unit2).toLowerCase()));
						} else {
							unit_ = unit;
						}
						double toValue_ = TemperatureInterpreter.convertValue(
								Double.valueOf(matcher.group(TemperatureInterpreter.p1Numbers2)), unit_, unit);

						/*
						 * Since there are only positive weights we either set the variance and mean or
						 * from to values.
						 */
						if (toValue_ > fromValue_) {
							toValue = toValue_;
							fromValue = fromValue_;
						} else {
							varianceValue = toValue_;
							meanValue = fromValue_;
						}
					}
				} else if (matcher.group(TemperatureInterpreter.pattern2GrouName) != null) {
					double fromValue_ = defaultFromValue;
					if (matcher.group(TemperatureInterpreter.p2Unit1) != null)
						unit = ETemperatureUnits
								.valueOf(mapVariation(matcher.group(TemperatureInterpreter.p2Unit1).toLowerCase()));
					if (matcher.group(TemperatureInterpreter.p2Numbers1) != null)
						fromValue_ = Double.valueOf(matcher.group(TemperatureInterpreter.p2Numbers1));
					if (matcher.group(TemperatureInterpreter.p2Numbers2) != null) {
						double toValue_ = Double.valueOf(matcher.group(TemperatureInterpreter.p2Numbers2));
						/*
						 * Since there are only positive weights we either set the variance and mean or
						 * from to values.
						 */
						if (toValue_ > fromValue_) {
							toValue = toValue_;
							fromValue = fromValue_;
						} else {
							varianceValue = toValue_;
							meanValue = fromValue_;
						}
					}
				} else if (matcher.group(TemperatureInterpreter.pattern3GrouName) != null) {
					if (matcher.group(TemperatureInterpreter.p3Unit1) != null)
						unit = ETemperatureUnits
								.valueOf(mapVariation(matcher.group(TemperatureInterpreter.p3Unit1).toLowerCase()));
					if (matcher.group(TemperatureInterpreter.p3Numbers1) != null)
						meanValue = Double.valueOf(matcher.group(TemperatureInterpreter.p3Numbers1));
				}
				meanValue = meanValue == defaultMeanValue ? (fromValue + toValue) / 2 : meanValue;
				fromValue = fromValue == defaultFromValue ? (meanValue - varianceValue) : fromValue;
				toValue = toValue == defaultToValue ? (meanValue + varianceValue) : toValue;

			}
		}
		this.unit = unit;
		this.meanValue = meanValue;
		this.about = about;
		this.fromValue = fromValue;
		this.toValue = toValue;
	}

	public ETemperatureUnits getUnit() {
		return unit;
	}

	@Override
	public String asSimpleString() {
		return DECIMAL_FORMAT.format(meanValue) + " " + unit;

	}
}
