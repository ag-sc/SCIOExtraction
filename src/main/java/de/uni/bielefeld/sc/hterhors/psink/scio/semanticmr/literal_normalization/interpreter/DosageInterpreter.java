package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.EDosagesUnits.EType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.AbstractNumericInterpreter;

public class DosageInterpreter extends AbstractNumericInterpreter {

	public static void main(String[] args) {

		System.out.println(new DosageInterpreter("mm"));

	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public final EDosagesUnits unit;
	public final double meanValue;
	public final boolean unsure;
	public boolean interpretable = false;
	final private static String connection = "\\s?(\\/|per|in|" + BAD_CHAR + ")\\s?";
	final private static String relationLessConnection = "(\\sof.{2,5})?";

	private static String bodyWeightGN = "ofBW";
	private static String digitsName1 = "digitsName1";
	private static String digitsName2 = "digitsName2";
	private static String unitName1 = "unitName1";
	private static String unitName2 = "unitName2";
	private static String unitName3 = "unitName3";
	private static String digitsName3 = "digitsName3";
	private static String unitPattern1 = "(" + BAD_CHAR + "|µ|m)?(u(nits?)?|g|%|l|cfu|mol|cm3|kg|i?( |\\.)?u\\.?)";
	private static String unitPattern2 = "(" + BAD_CHAR + "|µ|m)?(h(ours?)?|k?g|l|%|cm3|day)";
	private static String unitPattern3 = "(i?u|(µ|" + BAD_CHAR + ")l|ml|mm|l|%|cm3|(µ|" + BAD_CHAR + ")g|mg|g)";
	private static String dosagePatternName1 = "dosagePatternName1";
	private static String dosagePatternName2 = "dosagePatternName2";

	private static final String PATTERN_1 = "(?<" + dosagePatternName1 + ">(?<" + digitsName1 + ">" + digits
			+ ")(\\s?(?<" + unitName1 + ">" + unitPattern1 + ")" + relationLessConnection + connection + "((?<"
			+ digitsName2 + ">" + digits + ")\\s?)?(?<" + unitName2 + ">" + unitPattern2 + ")(?<" + bodyWeightGN
			+ ">(\\W|\\Wof\\W)?((body\\Wweight)|bw))?))";

	private static final String PATTERN_2 = "(?<" + dosagePatternName2 + ">(?<" + digitsName3 + ">" + digits
			+ ")(\\W?(?<" + unitName3 + ">" + unitPattern3 + ")))";

	public static final Pattern PATTERN = Pattern
			.compile(PRE_BOUNDS + "(" + PATTERN_1 + "|" + PATTERN_2 + ")" + POST_BOUNDS, PATTERN_BITMASK);

	final public static EDosagesUnits defaultUnit = EDosagesUnits.undef;
	final public static float defaultMeanValue = 0;
	final public static float defaultFractionValue = 1;
	final public static boolean defaultUnsureValue = false;

	public DosageInterpreter(String surfaceForm) {
		super(surfaceForm);
		EDosagesUnits unit = defaultUnit;
		double meanValue = defaultMeanValue;
		boolean unsure = defaultUnsureValue;
		double fractionValue = defaultFractionValue;

		Matcher matcher = DosageInterpreter.PATTERN.matcher(surfaceForm);

		if (matcher.find()) {

			if (matcher.group(0).equals(surfaceForm)) {
				interpretable = true;

				surfaceForm = matcher.group();
				if (surfaceForm.matches(".*" + BAD_CHAR + ".*"))
					unsure = true;
				if (matcher.group(DosageInterpreter.dosagePatternName1) != null) {
					if (matcher.group(DosageInterpreter.digitsName1) != null)
						meanValue = Double.valueOf(toValue(matcher.group(DosageInterpreter.digitsName1)));
					if (matcher.group(DosageInterpreter.digitsName2) != null)
						fractionValue = Double.valueOf(toValue(matcher.group(DosageInterpreter.digitsName2)));
					if (matcher.group(DosageInterpreter.unitName1) != null
							&& matcher.group(DosageInterpreter.unitName2) != null) {
						if (matcher.group(DosageInterpreter.unitName1)
								.equals(matcher.group(DosageInterpreter.unitName2))) {
							unit = EDosagesUnits
									.valueOf(mapVariation(clean(matcher.group(DosageInterpreter.unitName1))));
						} else {
							unit = EDosagesUnits.valueOf(mapVariation(clean(matcher.group(DosageInterpreter.unitName1)
									+ "_" + matcher.group(DosageInterpreter.unitName2)
									+ (matcher.group(DosageInterpreter.bodyWeightGN) == null ? "" : "_bw"))));
						}
					}
				} else if (matcher.group(DosageInterpreter.dosagePatternName2) != null) {
					if (matcher.group(DosageInterpreter.unitName3) != null)
						unit = EDosagesUnits.valueOf(mapVariation(clean(matcher.group(DosageInterpreter.unitName3))));
					if (matcher.group(DosageInterpreter.digitsName3) != null)
						meanValue = Double.valueOf(toValue(matcher.group(DosageInterpreter.digitsName3)));
				}

			}

		}

		this.unit = unit;
		this.meanValue = meanValue * (1 / fractionValue);
		this.unsure = unsure;

	}

	private DosageInterpreter(String surfaceForm, EDosagesUnits unit, double meanValue, boolean unsure,
			boolean interpretable) {
		super(surfaceForm);

		this.unit = unit;
		this.meanValue = meanValue;
		this.unsure = unsure;
		this.interpretable = interpretable;
	}

	public DosageInterpreter convertTo(EDosagesUnits toUnit) {
		if (unit.type == toUnit.type) {
			return new DosageInterpreter(surfaceForm, toUnit, convertValue(meanValue, toUnit), unsure, interpretable);
		}
		System.err.println("Can not convert dosage from: " + unit + " to " + toUnit);
		return this;
	}

	private double convertValue(double value, EDosagesUnits toUnit) {
		return convertValue(value, unit, toUnit);
	}

	public DosageInterpreter normalize() {

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
		return "DosageInterpreter [surfaceForm=" + surfaceForm + ", interpretable=" + interpretable + ", meanValue="
				+ meanValue + ", unit=" + unit + ", unsure=" + unsure + "]";
	}

	@Override
	public String asFormattedString() {
		return DECIMAL_FORMAT.format(meanValue) + " " + unit.name().replaceFirst("_", "/");
	}

	@Override
	public EDosagesUnits getUnit() {
		return unit;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(meanValue);
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
		DosageInterpreter other = (DosageInterpreter) obj;
		if (Double.doubleToLongBits(meanValue) != Double.doubleToLongBits(other.meanValue))
			return false;
		if (unit != other.unit)
			return false;
		if (unsure != other.unsure)
			return false;
		return true;
	}

	@Override
	public String asSimpleString() {
		return DECIMAL_FORMAT.format(meanValue) + " " + unit.name().replaceFirst("_", "/");
	}

}
