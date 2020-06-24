package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.AbstractNumericInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ISingleUnit;

/*
 * Test on http://regexr.com/
 * 
 * @formatter:off
 * ((~|\babout[^\d\w\.,])?)(((((\d+(\.|,)\d+)|\d+))[^\d\w\.,]?(((((m)(milli)?|((
 * k)(ilo)?)))?(g)(ram)?))([^\d\w\.,](to|and)[^\d\w\.,]|[^\d\w\.,]?((\+?-)|±|[^\
 * d\w\.,])[^\d\w\.,]?)(((\d+(\.|,)\d+)|\d+))[^\d\w\.,]?(((((m)(milli)?|((k)(ilo
 * )?)))?(g)(ram)?))?)|((((\d+(\.|,)\d+)|\d+))([^\d\w\.,](to|and)[^\d\w\.,]|[^\d
 * \w\.,]?((\+?-)|±|[^\d\w\.,])[^\d\w\.,]?)(((\d+(\.|,)\d+)|\d+))[^\d\w\.,]?((((
 * (m)(milli)?|((k)(ilo)?)))?(g)(ram)?)))|((((\d+(\.|,)\d+)|\d+))[^\d\w\.,]?((((
 * (m)(milli)?|((k)(ilo)?)))?(g)(ram)?))))\b
 * 
 * @formatter:on
 * 
 */
public class WeightInterpreter extends AbstractNumericInterpreter {

	public static void main(String[] args) {

		System.out.println(WeightInterpreter.PATTERN);
		WeightInterpreter wi = new WeightInterpreter("100-200 kg");
		WeightInterpreter wi2 = new WeightInterpreter("~200 g");

		System.out.println(wi.normalize().asSimpleString());
		System.out.println(wi2.normalize().asSimpleString());

	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	final private static String aboutGroupName = "about";
	final private static String pattern1GrouName = "pattern1GrouName";
	final private static String pattern2GrouName = "pattern2GrouName";
	final private static String pattern3GrouName = "pattern3GrouName";

	final private static String p1Numbers1 = "p1Numbers1";
	final private static String p1Numbers2 = "p1Numbers2";
	final private static String p1Unit1 = "p1Unit1";
	final private static String p1Unit2 = "p1Unit2";

	final private static String p2Numbers1 = "p2Numbers1";
	final private static String p2Numbers2 = "p2Numbers2";
	final private static String p2Unit1 = "p2Unit1";

	final private static String p3Numbers1 = "p3Numbers1";
	final private static String p3Unit1 = "p3Unit1";

	final private static String numbers_ = "((\\d+\\.\\d+)|\\d+)";
	final private static String freeSpace_ = "[^\\d\\w\\.,]";
	final private static String freeSpaceQuestionMark_ = freeSpace_ + "?";
	final private static String unit_ = "(((m(milli)?|(k(ilo)?)))?g((ra)?ms?)?|kilo|lbs)(?!" + freeSpaceQuestionMark_
			+ "/)";
	final private static String about_ = "(~|<|>|" + PRE_BOUNDS + "about" + freeSpace_ + ")?";
	final private static String connection_ = "(" + freeSpace_ + "(to|and)" + freeSpace_ + "|" + freeSpaceQuestionMark_
			+ "((\\+?-)|\\+(/|\\\\)-|±|" + freeSpace_ + ")" + freeSpaceQuestionMark_ + ")";

	/**
	 * 100g - 300g
	 */
	final static String pattern1_ = "(?<" + p1Numbers1 + ">" + numbers_ + ")" + freeSpaceQuestionMark_ + "(?<" + p1Unit1
			+ ">" + unit_ + ")" + connection_ + "(?<" + p1Numbers2 + ">" + numbers_ + ")(" + freeSpaceQuestionMark_
			+ "(?<" + p1Unit2 + ">" + unit_ + "))?";

	/**
	 * 100 - 300g
	 */
	final static String pattern2_ = "(?<" + p2Numbers1 + ">" + numbers_ + ")" + connection_ + "(?<" + p2Numbers2 + ">"
			+ numbers_ + ")" + freeSpaceQuestionMark_ + "(?<" + p2Unit1 + ">" + unit_ + ")";

	/**
	 * 100g
	 */
	final static String pattern3_ = "(?<" + p3Numbers1 + ">" + numbers_ + ")" + freeSpaceQuestionMark_ + "(?<" + p3Unit1
			+ ">" + unit_ + ")";
//	public final static Pattern PATTERN = Pattern.compile("(?<" + aboutGroupName + ">" + about_ + ")((?<"
//			+ pattern1GrouName + ">" + pattern1_ + "))" + POST_BOUNDS, PATTERN_BITMASK);

	public final static Pattern PATTERN = Pattern.compile("(?<" + aboutGroupName + ">" + about_ + ")((?<"
			+ pattern1GrouName + ">" + pattern1_ + ")|(?<" + pattern2GrouName + ">" + pattern2_ + ")|(?<"
			+ pattern3GrouName + ">" + pattern3_ + "))" + POST_BOUNDS, PATTERN_BITMASK);

	static public enum EWeightUnits implements ISingleUnit {

		undef(0), mg(0.001d), g(1d), lbs(453.592d), kg(1000d), t(1000000d);

		final public double factor;

		private EWeightUnits(double factor) {
			this.factor = factor;
		}

		public static EWeightUnits getDefault() {
			return g;
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

	final private static EWeightUnits defaultUnit = EWeightUnits.undef;
	final private static float defaultFromValue = 0;
	final private static float defaultToValue = 0;
	final private static float defaultMeanValue = 0;
	final private static float defaultVarianceValue = 0;
	final private static boolean defaultAbout = false;

	final public EWeightUnits unit;
	final public double meanValue;
	final public boolean about;
	final public double fromValue;
	final public double toValue;

	public WeightInterpreter(String surfaceForm) {
		super(surfaceForm);

		Matcher matcher = WeightInterpreter.PATTERN.matcher(surfaceForm);

		EWeightUnits unit = defaultUnit;
		double meanValue = defaultMeanValue;
		double fromValue = defaultFromValue;
		double varianceValue = defaultVarianceValue;
		double toValue = defaultToValue;
		boolean about = defaultAbout;

		if (matcher.find()) {

			if (matcher.group(0).equals(surfaceForm)) {
				interpretable = true;

				about = matcher.group(WeightInterpreter.aboutGroupName) != null
						&& !matcher.group(WeightInterpreter.aboutGroupName).trim().isEmpty();

				if (matcher.group(WeightInterpreter.pattern1GrouName) != null) {
					double fromValue_ = defaultFromValue;
					if (matcher.group(WeightInterpreter.p1Unit1) != null)
						unit = EWeightUnits
								.valueOf(mapVariation(matcher.group(WeightInterpreter.p1Unit1).toLowerCase()));
					if (matcher.group(WeightInterpreter.p1Numbers1) != null)
						fromValue_ = Double.valueOf(matcher.group(WeightInterpreter.p1Numbers1));
					if (matcher.group(WeightInterpreter.p1Numbers2) != null) {
						EWeightUnits unit_;
						if (matcher.group(WeightInterpreter.p1Unit2) != null) {
							unit_ = EWeightUnits
									.valueOf(mapVariation(matcher.group(WeightInterpreter.p1Unit2).toLowerCase()));
						} else {
							unit_ = unit;
						}
						double toValue_ = WeightInterpreter
								.convertValue(Double.valueOf(matcher.group(WeightInterpreter.p1Numbers2)), unit_, unit);

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
				} else if (matcher.group(WeightInterpreter.pattern2GrouName) != null) {
					double fromValue_ = defaultFromValue;
					if (matcher.group(WeightInterpreter.p2Unit1) != null)
						unit = EWeightUnits
								.valueOf(mapVariation(matcher.group(WeightInterpreter.p2Unit1).toLowerCase()));
					if (matcher.group(WeightInterpreter.p2Numbers1) != null)
						fromValue_ = Double.valueOf(matcher.group(WeightInterpreter.p2Numbers1));
					if (matcher.group(WeightInterpreter.p2Numbers2) != null) {
						double toValue_ = Double.valueOf(matcher.group(WeightInterpreter.p2Numbers2));
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
				} else if (matcher.group(WeightInterpreter.pattern3GrouName) != null) {
					if (matcher.group(WeightInterpreter.p3Unit1) != null)
						unit = EWeightUnits
								.valueOf(mapVariation(matcher.group(WeightInterpreter.p3Unit1).toLowerCase()));
					if (matcher.group(WeightInterpreter.p3Numbers1) != null)
						meanValue = Double.valueOf(matcher.group(WeightInterpreter.p3Numbers1));
				}
				double _meanValue = meanValue == defaultMeanValue ? (fromValue + toValue) / 2 : meanValue;
				double _fromValue = fromValue == defaultFromValue ? (meanValue - varianceValue) : fromValue;
				double _toValue = toValue == defaultToValue ? (meanValue + varianceValue) : toValue;

				meanValue = _meanValue;
				fromValue = _fromValue;
				toValue = _toValue;
			}

		}
		this.unit = unit;
		this.meanValue = meanValue;
		this.about = about;
		this.fromValue = fromValue;
		this.toValue = toValue;
	}

	private WeightInterpreter(String surfaceForm, EWeightUnits unit, double meanValue, boolean about, double fromValue,
			double toValue, boolean interpretable) {
		super(surfaceForm);
		this.unit = unit;
		this.meanValue = meanValue;
		this.about = about;
		this.fromValue = fromValue;
		this.toValue = toValue;
		this.interpretable = interpretable;
	}

	public WeightInterpreter convertTo(EWeightUnits toWeightUnit) {
		return new WeightInterpreter(surfaceForm, toWeightUnit, convertValue(meanValue, toWeightUnit), about,
				convertValue(fromValue, toWeightUnit), convertValue(toValue, toWeightUnit), this.interpretable);
	}

	public static double convertValue(double value, EWeightUnits fromWeightUnit, EWeightUnits toWeightUnit) {
		return (value * fromWeightUnit.factor) / toWeightUnit.factor;
	}

	private double convertValue(double value, EWeightUnits toWeightUnit) {
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
		WeightInterpreter other = (WeightInterpreter) obj;
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
		return "WeightInterpreter [surfaceForm=" + surfaceForm + ", interpretable=" + interpretable + ", about=" + about
				+ ", meanValue=" + meanValue + ", unit=" + unit + ", fromValue=" + fromValue + ", toValue=" + toValue
				+ "]";
	}

	@Override
	public WeightInterpreter normalize() {
		return convertTo(EWeightUnits.getDefault());
	}

	@Override
	public double getMeanValue() {
		return meanValue;
	}

	@Override
	public String asFormattedString() {
		return (about ? "about " : "") + DECIMAL_FORMAT.format(meanValue) + " " + unit;
	}

	@Override
	public String asSimpleString() {
		return DECIMAL_FORMAT.format(meanValue) + " " + unit;
	}

	@Override
	public ISingleUnit getUnit() {
		return unit;
	}

}
