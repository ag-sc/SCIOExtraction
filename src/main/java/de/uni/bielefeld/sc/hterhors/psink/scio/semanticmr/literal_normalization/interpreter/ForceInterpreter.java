package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.ForceInterpreter.EForceUnits.EType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.WeightInterpreter.EWeightUnits;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.AbstractNumericInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.IDoubleUnit;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.IDoubleUnitType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ISingleUnit;

public class ForceInterpreter extends AbstractNumericInterpreter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {

		System.out.println(ForceInterpreter.PATTERN);
		ForceInterpreter wi = new ForceInterpreter("100 dyn");
		ForceInterpreter wi2 = new ForceInterpreter("1 kdyn");

		System.out.println(wi.normalize().asSimpleString());
		System.out.println(wi2.normalize().asSimpleString());

	}

	static public enum EForceUnits implements IDoubleUnit {

		undef(0, 0, EType.ELSE),
		//
		g_mm(1d, 10d, EType.WEIGHT_LENGTH), g_cm(1d, 1d, EType.WEIGHT_LENGTH),
		//
		dyn(1E5d, 1d, EType.NEWTON), kdyn(1E2d, 1d, EType.NEWTON), n(1d, 1d, EType.NEWTON);

		final public double determinatorFactor;
		final public double numeratorFactor;
		final EType type;

		private EForceUnits(double numeratorFactor, double determinatorFactor, EType type) {
			this.numeratorFactor = numeratorFactor;
			this.determinatorFactor = determinatorFactor;
			this.type = type;
		}

		static enum EType implements IDoubleUnitType {

			ELSE("undef"), WEIGHT_LENGTH("g_cm"), NEWTON("n");

			final private String defaultUnit;

			private EType(String defaultUnit) {
				this.defaultUnit = defaultUnit;
			}

			public EForceUnits getDefaultUnit() {
				return EForceUnits.valueOf(defaultUnit);
			}

		}

		@Override
		public IDoubleUnitType getType() {
			return type;
		}

		@Override
		public double getNumeratorFactor() {
			return numeratorFactor;
		}

		@Override
		public double getDeterminatorFactor() {
			return determinatorFactor;
		}

		@Override
		public String getName() {
			return this.name();
		}
	}

	private static String digitsName1 = "digitsName1";
	private static String digitsName2 = "digitsName2";
	private static String unitName1 = "unitName1";
	private static String unitName2 = "unitName2";
	private static String unitName3 = "unitName3";
	private static String digitsName3 = "digitsName3";
	private static String unitPattern1 = "(" + BAD_CHAR + "|µ)?(g\\.?)";
	private static String unitPattern2 = "(" + BAD_CHAR + "m|mm|cm|µm)";
	private static String unitPattern3 = "(N|k?dyne?s?)";
	private static String forcePatternName1 = "forcePatternName1";
	private static String forcePatternName2 = "forcePatternName2";

	final static String pattern3GrouName = "pattern3GrouName";
	final static String pattern4GrouName = "pattern4GrouName";
	final static String pattern5GrouName = "pattern5GrouName";

	final private static String p1Numbers1 = "p1Numbers1";
	final private static String p1Numbers2 = "p1Numbers2";
	final private static String p1Unit1 = "p1Unit1";
	final private static String p1Unit2 = "p1Unit2";

	final private static String p2Numbers1 = "p2Numbers1";
	final private static String p2Numbers2 = "p2Numbers2";
	final private static String p2Unit1 = "p2Unit1";

	final private static String p3Numbers1 = "p3Numbers1";
	final private static String p3Unit1 = "p3Unit1";

	private static final String PATTERN_1 = "(?<" + forcePatternName1 + ">(?<" + digitsName1 + ">" + digits
			+ ")(\\s?(?<" + unitName1 + ">" + unitPattern1 + ")" + relationLessConnection + connection + "((?<"
			+ digitsName2 + ">" + digits + ")\\s?)?(?<" + unitName2 + ">" + unitPattern2 + ")))";

	private static final String PATTERN_2 = "(?<" + forcePatternName2 + ">(?<" + digitsName3 + ">" + digits
			+ ")(\\W?(?<" + unitName3 + ">" + unitPattern3 + ")))";

	/**
	 * 100g - 300 g
	 */
	private final static String PATTERN_3 = "(?<" + pattern3GrouName + ">(?<" + p1Numbers1 + ">" + digits + ")"
			+ freeSpaceQuestionMark_ + "(?<" + p1Unit1 + ">" + unitPattern3 + ")" + connection_ + "(?<" + p1Numbers2
			+ ">" + digits + ")" + freeSpaceQuestionMark_ + "(?<" + p1Unit2 + ">" + unitPattern3 + ")?)";

	/**
	 * 100 - 300g
	 */
	private final static String PATTERN_4 = "(?<" + pattern4GrouName + ">(?<" + p2Numbers1 + ">" + digits + ")"
			+ connection_ + "(?<" + p2Numbers2 + ">" + digits + ")" + freeSpaceQuestionMark_ + "(?<" + p2Unit1 + ">"
			+ unitPattern3 + "))";

	/**
	 * 100g +- 300
	 */
	private final static String PATTERN_5 = "(?<" + pattern5GrouName + ">(?<" + p3Numbers1 + ">" + digits + ")"
			+ freeSpaceQuestionMark_ + "(?<" + p3Unit1 + ">" + p1Unit2 + "))";

	public static final Pattern PATTERN = Pattern.compile(PRE_BOUNDS + "(" + PATTERN_1 + "|" + PATTERN_3 + "|"
			+ PATTERN_4 + "|" + PATTERN_5 + "|" + PATTERN_2 + ")" + POST_BOUNDS, PATTERN_BITMASK);

	public ForceInterpreter(String surfaceForm, EForceUnits unit, double meanValue, boolean unsure, double fromValue,
			double toValue) {
		super(surfaceForm);
		this.unit = unit;
		this.meanValue = meanValue;
		this.unsure = unsure;
		this.fromValue = fromValue;
		this.toValue = toValue;
	}

	public ForceInterpreter convertTo(EForceUnits toUnit) {
		if (unit.type == toUnit.type) {
			return new ForceInterpreter(surfaceForm, toUnit, convertValue(meanValue, toUnit), unsure,
					convertValue(fromValue, toUnit), convertValue(toValue, toUnit));
		}
		System.err.println("Can not convert force from: " + unit + " to " + toUnit);
		return this;
	}

	private double convertValue(double value, EForceUnits toUnit) {
		return convertValue(value, unit, toUnit);
	}

	public static double convertValue(double value, EForceUnits fromUnit, EForceUnits toUnit) {
		if (fromUnit.type == toUnit.type)
			return value * (toUnit.numeratorFactor / fromUnit.numeratorFactor)
					* (fromUnit.determinatorFactor / toUnit.determinatorFactor);
		else
			throw new IllegalArgumentException("Can not convert " + fromUnit.type + " to " + toUnit.type);
	}

	public ForceInterpreter normalize() {

		if (unit.type != EType.ELSE) {
			return convertTo(unit.type.getDefaultUnit());
		}
		return this;
	}

	@Override
	public double getMeanValue() {
		return meanValue;
	}

	@Override
	public String toString() {
		return "SemanticDosage [surfaceForm=" + surfaceForm + ", unit=" + unit + ", meanValue=" + meanValue
				+ ", unsure=" + unsure + "]";
	}

	@Override
	public String asFormattedString() {
		return DECIMAL_FORMAT.format(meanValue) + " " + unit.name().replaceFirst("_", "/");
	}

	@Override
	public EForceUnits getUnit() {
		return unit;
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
		result = prime * result + (unsure ? 1231 : 1237);
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
		ForceInterpreter other = (ForceInterpreter) obj;
		if (Double.doubleToLongBits(fromValue) != Double.doubleToLongBits(other.fromValue))
			return false;
		if (Double.doubleToLongBits(meanValue) != Double.doubleToLongBits(other.meanValue))
			return false;
		if (Double.doubleToLongBits(toValue) != Double.doubleToLongBits(other.toValue))
			return false;
		if (unit != other.unit)
			return false;
		if (unsure != other.unsure)
			return false;
		return true;
	}

	final public static EForceUnits defaultUnit = EForceUnits.undef;
	final public static float defaultMeanValue = 0;
	final public static float defaultVarianceValue = 0;
	final public static float defaultFromValue = 0;
	final public static float defaultFractionValue = 1;
	final public static boolean defaultUnsureValue = false;
	final public static float defaultToValue = 0;

	public final EForceUnits unit;
	public final double meanValue;
	public final boolean unsure;
	final public double fromValue;
	final public double toValue;

	public ForceInterpreter(String surfaceForm) {
		super(surfaceForm);

		Matcher matcher = ForceInterpreter.PATTERN.matcher(surfaceForm);

		double fractionValue = defaultFractionValue;
		double varianceValue = defaultVarianceValue;
		EForceUnits unit = defaultUnit;
		double meanValue = defaultMeanValue;
		boolean unsure = defaultUnsureValue;
		double fromValue = defaultFromValue;
		double toValue = defaultToValue;

		if (matcher.find()) {

			if (matcher.group(0).equals(surfaceForm)) {
				interpretable = true;

				if (surfaceForm.matches(".*" + BAD_CHAR + ".*"))
					unsure = true;
				if (matcher.group(ForceInterpreter.forcePatternName1) != null) {
					if (matcher.group(ForceInterpreter.digitsName1) != null)
						meanValue = Double.valueOf(toValue(matcher.group(ForceInterpreter.digitsName1)));
					if (matcher.group(ForceInterpreter.digitsName2) != null)
						fractionValue = Double.valueOf(toValue(matcher.group(ForceInterpreter.digitsName2)));
					if (matcher.group(ForceInterpreter.unitName1) != null
							&& matcher.group(ForceInterpreter.unitName2) != null)
						unit = EForceUnits.valueOf(clean(mapVariation(matcher.group(ForceInterpreter.unitName1) + "_"
								+ matcher.group(ForceInterpreter.unitName2))));
				} else if (matcher.group(ForceInterpreter.forcePatternName2) != null) {
					if (matcher.group(ForceInterpreter.unitName3) != null)
						unit = EForceUnits.valueOf(clean(mapVariation(matcher.group(ForceInterpreter.unitName3))));
					if (matcher.group(ForceInterpreter.digitsName3) != null)
						meanValue = Double.valueOf(toValue(matcher.group(ForceInterpreter.digitsName3)));
				} else if (matcher.group(ForceInterpreter.pattern3GrouName) != null) {
					double fromValue_ = defaultFromValue;
					if (matcher.group(ForceInterpreter.p1Unit1) != null)
						unit = EForceUnits.valueOf(mapVariation(matcher.group(ForceInterpreter.p1Unit1).toLowerCase()));
					if (matcher.group(ForceInterpreter.p1Numbers1) != null)
						fromValue_ = Double.valueOf(matcher.group(ForceInterpreter.p1Numbers1));
					if (matcher.group(ForceInterpreter.p1Numbers2) != null) {
						EForceUnits unit_;
						if (matcher.group(ForceInterpreter.p1Unit2) != null) {
							unit_ = EForceUnits
									.valueOf(mapVariation(matcher.group(ForceInterpreter.p1Unit2).toLowerCase()));
						} else {
							unit_ = unit;
						}
						double toValue_ = ForceInterpreter
								.convertValue(Double.valueOf(matcher.group(ForceInterpreter.p1Numbers2)), unit_, unit);

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
				} else if (matcher.group(ForceInterpreter.pattern4GrouName) != null) {
					double fromValue_ = defaultFromValue;
					if (matcher.group(ForceInterpreter.p2Unit1) != null)
						unit = EForceUnits.valueOf(mapVariation(matcher.group(ForceInterpreter.p2Unit1).toLowerCase()));
					if (matcher.group(ForceInterpreter.p2Numbers1) != null)
						fromValue_ = Double.valueOf(matcher.group(ForceInterpreter.p2Numbers1));
					if (matcher.group(ForceInterpreter.p2Numbers2) != null) {
						double toValue_ = Double.valueOf(matcher.group(ForceInterpreter.p2Numbers2));
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
				} else if (matcher.group(ForceInterpreter.pattern5GrouName) != null) {
					if (matcher.group(ForceInterpreter.p3Unit1) != null)
						unit = EForceUnits.valueOf(mapVariation(matcher.group(ForceInterpreter.p3Unit1).toLowerCase()));
					if (matcher.group(ForceInterpreter.p3Numbers1) != null)
						meanValue = Double.valueOf(matcher.group(ForceInterpreter.p3Numbers1));
				}
			}
		}

		if (meanValue != defaultMeanValue) {
			meanValue = meanValue * (1 / fractionValue);
		} else {
			meanValue = meanValue == defaultMeanValue ? (fromValue + toValue) / 2 : meanValue;
			fromValue = fromValue == defaultFromValue ? (meanValue - varianceValue) : fromValue;
			toValue = toValue == defaultToValue ? (meanValue + varianceValue) : toValue;
		}

		this.unit = unit;
		this.meanValue = meanValue;
		this.unsure = unsure;
		this.fromValue = fromValue;
		this.toValue = toValue;
	}

	@Override
	public String asSimpleString() {
		return DECIMAL_FORMAT.format(meanValue) + " " + unit;
	}

}
