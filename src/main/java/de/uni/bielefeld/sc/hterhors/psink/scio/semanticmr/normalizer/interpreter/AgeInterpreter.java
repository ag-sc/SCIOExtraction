package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.struct.AbstractNumericInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.struct.ISingleUnit;

/*
 * Test on http://regexr.com/
 * 
 * @formatter:on
 * 
 * \b((((one|two|three|four|five|six|seven|eight|nine|ten)|(\d\.?\d{0,2}))(\W?(
 * day|week|month|year)s?)\W?(\Wto\W|\W?-\W?))((one|two|three|four|five|six|
 * seven|eight|nine|ten)|(\d\.?\d{0,2}))\W?(\7)s?)|((((one|two|three|four|five|
 * six|seven|eight|nine|ten)|(\d\.?\d{0,2}))\W?(\Wto\W|\W?-\W?))?((one|two|three
 * |four|five|six|seven|eight|nine|ten)|(\d\.?\d{0,2}))(\W?(day|week|month|year)
 * s?))\b
 * 
 * @formatter:on
 * 
 */
public class AgeInterpreter extends AbstractNumericInterpreter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// final private static String digits = "\\d\\.?\\d{0,2}";
	// final private static String writtenNumbers = PRE_BOUNDS +
	// "(one|two|three|four|five|six|seven|eight|nine|ten)";
	final private static String connectionPart = "\\W?(\\Wto\\W|\\W?-\\W?)";
	final private static String units = "d(ays?)?|w(eeks?)?|months?|y(ears?)?";

	final private static String toGroupName2 = "toGroup2";
	final private static String fromGroupName2 = "fromGroup2";
	final private static String writtenFromGroup2 = "writtenFrom2";
	final private static String digitsFromGroup2 = "digitsFrom2";
	final private static String writtenToGroup2 = "writtenTo2";
	final private static String digitsToGroup2 = "digitsTo2";
	final private static String unitsGroup2 = "unitsGroup2";

	final private static String agePattern2 = "((?<" + fromGroupName2 + ">(?<" + writtenFromGroup2 + ">"
			+ writtenNumbers + ")|(?<" + digitsFromGroup2 + ">" + digits + "))(\\W?(?<" + unitsGroup2 + ">" + units
			+ ")(\\W?old|\\Wof\\Wage)?)" + connectionPart + ")(?<" + toGroupName2 + ">(?<" + writtenToGroup2 + ">"
			+ writtenNumbers + ")|(?<" + digitsToGroup2 + ">" + digits + "))\\W?(\\k<" + unitsGroup2 + ">)s?";

	final private static String fromGroupName = "fromGroup";
	final private static String toGroupName = "toGroup";
	final private static String writtenFromGroup = "writtenFrom";
	final private static String digitsFromGroup = "digitsFrom";
	final private static String writtenToGroup = "writtenTo";
	final private static String digitsToGroup = "digitsTo";
	final private static String unitsGroup = "unitsGroup";

	final private static String pattern1Full = "pattern1Full";
	final private static String agePattern1GroupName = "pattern1GN";
	final private static String agePattern2GroupName = "pattern2GN";

	final private static String agePattern1 = "(?<" + pattern1Full + ">(?<" + fromGroupName + ">(?<" + writtenFromGroup
			+ ">" + writtenNumbers + ")|(?<" + digitsFromGroup + ">" + digits + "))" + connectionPart + ")?(?<"
			+ toGroupName + ">(?<" + writtenToGroup + ">" + writtenNumbers + ")|(?<" + digitsToGroup + ">" + digits
			+ "))(\\W?(?<" + unitsGroup + ">" + units + ")(\\W?old|\\Wof\\Wage)?)";

	public static final Pattern PATTERN = Pattern.compile(PRE_BOUNDS + "(?<" + agePattern2GroupName + ">" + agePattern2
			+ ")|(?<" + agePattern1GroupName + ">" + agePattern1 + ")" + POST_BOUNDS, PATTERN_BITMASK);

	public static enum EAgeUnits implements ISingleUnit {

		undef(0), hour(1d / 24d), day(1d), week(7d), month(365d / 12d), year(365d);

		final public double factor;

		private EAgeUnits(double factor) {
			this.factor = factor;
		}

		public static EAgeUnits getDefault() {
			return day;
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

	public final EAgeUnits unit;
	public final double meanValue;
	public final double fromValue;
	public final double toValue;

	public AgeInterpreter(String surfaceForm) {
		super(surfaceForm);

		EAgeUnits unit = defaultUnit;
		double meanValue = defaultMeanValue;
		double fromValue = defaultFromValue;
		double varianceValue = defaultVarianceValue;
		double toValue = defaultToValue;

		Matcher matcher = AgeInterpreter.PATTERN.matcher(surfaceForm);

		if (matcher.find()) {

			if (matcher.group(AgeInterpreter.agePattern1GroupName) != null) {
				if (matcher.group(AgeInterpreter.unitsGroup) != null)
					unit = EAgeUnits.valueOf(mapVariation(matcher.group(AgeInterpreter.unitsGroup).toLowerCase()));
				if (matcher.group(AgeInterpreter.pattern1Full) != null) {
					if (matcher.group(AgeInterpreter.digitsFromGroup) != null)
						fromValue = Double.valueOf(matcher.group(AgeInterpreter.digitsFromGroup));
					if (matcher.group(AgeInterpreter.digitsToGroup) != null)
						toValue = Double.valueOf(matcher.group(AgeInterpreter.digitsToGroup));
					if (matcher.group(AgeInterpreter.writtenFromGroup) != null) {
						fromValue = mapWrittenNumbertoInt(matcher.group(AgeInterpreter.writtenFromGroup));
					}
					if (matcher.group(AgeInterpreter.writtenToGroup) != null)
						toValue = mapWrittenNumbertoInt(matcher.group(AgeInterpreter.writtenToGroup));
				} else {
					if (matcher.group(AgeInterpreter.digitsToGroup) != null)
						meanValue = Double.valueOf(matcher.group(AgeInterpreter.digitsToGroup));
					if (matcher.group(AgeInterpreter.writtenToGroup) != null)
						meanValue = mapWrittenNumbertoInt(matcher.group(AgeInterpreter.writtenToGroup));
				}
			} else if (matcher.group(AgeInterpreter.agePattern2GroupName) != null) {
				if (matcher.group(AgeInterpreter.unitsGroup2) != null)
					unit = EAgeUnits.valueOf(mapVariation(matcher.group(AgeInterpreter.unitsGroup2).toLowerCase()));
				if (matcher.group(AgeInterpreter.digitsFromGroup2) != null)
					fromValue = Double.valueOf(matcher.group(AgeInterpreter.digitsFromGroup2));
				if (matcher.group(AgeInterpreter.digitsToGroup2) != null)
					toValue = Double.valueOf(matcher.group(AgeInterpreter.digitsToGroup2));
				if (matcher.group(AgeInterpreter.writtenFromGroup2) != null)
					fromValue = mapWrittenNumbertoInt(matcher.group(AgeInterpreter.writtenFromGroup2));
				if (matcher.group(AgeInterpreter.writtenToGroup2) != null)
					toValue = mapWrittenNumbertoInt(matcher.group(AgeInterpreter.writtenToGroup2));
			}
			double _meanValue = meanValue == defaultMeanValue ? (fromValue + toValue) / 2 : meanValue;
			double _fromValue = fromValue == defaultFromValue ? (meanValue - varianceValue) : fromValue;
			double _toValue = toValue == defaultToValue ? (meanValue + varianceValue) : toValue;

			meanValue = _meanValue;
			fromValue = _fromValue;
			toValue = _toValue;

		}
		this.unit = unit;
		this.meanValue = meanValue;
		this.fromValue = fromValue;
		this.toValue = toValue;
	}

	private AgeInterpreter(String surfaceForm, EAgeUnits unit, double meanValue, double fromValue, double toValue) {
		super(surfaceForm);
		this.unit = unit;
		this.meanValue = meanValue;
		this.fromValue = fromValue;
		this.toValue = toValue;
	}

	public AgeInterpreter convertTo(EAgeUnits toWeightUnit) {
		return new AgeInterpreter(surfaceForm, toWeightUnit, convertValue(meanValue, unit, toWeightUnit),
				convertValue(fromValue, unit, toWeightUnit), convertValue(toValue, unit, toWeightUnit));
	}

	@Override
	public AgeInterpreter normalize() {
		return convertTo(EAgeUnits.getDefault());
	}

	@Override
	public double getMeanValue() {
		return meanValue;
	}

	@Override
	public String toString() {
		return "SemanticAge [surfaceForm=" + surfaceForm + ", unit=" + unit + ", meanValue=" + meanValue
				+ ", fromValue=" + fromValue + ", toValue=" + toValue + "]";
	}

	@Override
	public String asFormattedString() {
		return DECIMAL_FORMAT.format(meanValue) + " " + unit;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		AgeInterpreter other = (AgeInterpreter) obj;
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
	public EAgeUnits getUnit() {
		return unit;
	}

	final public static EAgeUnits defaultUnit = EAgeUnits.undef;
	final public static float defaultFromValue = 0;
	final public static float defaultToValue = 0;
	final public static float defaultMeanValue = 0;
	final public static float defaultVarianceValue = 0;

}
