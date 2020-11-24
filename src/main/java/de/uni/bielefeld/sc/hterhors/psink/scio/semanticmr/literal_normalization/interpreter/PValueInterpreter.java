package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.AbstractNumericInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.AbstractStringInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ILiteralInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ISingleUnit;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.IUnit;

public class PValueInterpreter extends AbstractStringInterpreter {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static void main(String[] args) {

		System.out.println(PValueInterpreter.PATTERN);
//		PValueInterpreter wi = new PValueInterpreter("> 0.05");
		PValueInterpreter wi = new PValueInterpreter("p = 0.05");

		System.out.println(wi.normalize().asSimpleString());

	}
	
	static public enum ETemperatureUnits implements ISingleUnit {

		undef(0);

		final public double factor;

		private ETemperatureUnits(double factor) {
			this.factor = factor;
		}

		public static ETemperatureUnits getDefault() {
			return undef;
		}

		@Override
		public double getFactor() {
			return 1;
		}

		@Override
		public String getName() {
			return this.name();
		}
	}

	final static String pattern1GrouName = "pattern1GrouName";

	final static String numbers_ = "((\\d+\\.\\d+)|\\d+)";
	final static String freeSpace_ = "[^\\d\\w\\.,]";
	final static String freeSpaceQuestionMark_ = freeSpace_ + "?";

	final private static String numGroup = "numGroup";

	private static String operatorGroup = "operatorGroup";

	/**
	 * n = 5
	 */
	final static String pattern1_ = "(p" + freeSpace_ + ")?(?<" + operatorGroup + ">" + BAD_CHAR + "|=|>|<)" + freeSpace_
			+ "?(?<" + numGroup + ">" + numbers_ + ")";

	public final static Pattern PATTERN = Pattern
			.compile("(?<" + pattern1GrouName + ">" + pattern1_ + ")" + POST_BOUNDS, PATTERN_BITMASK);

	final public double value;
	final public String comparitiveOperator;
	final public boolean unsure;

	
	
	public PValueInterpreter(String surfaceForm, double value, String comparitiveOperator, boolean unsure) {
		super(surfaceForm);
		this.value = value;
		this.comparitiveOperator = comparitiveOperator;
		this.unsure = unsure;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((comparitiveOperator == null) ? 0 : comparitiveOperator.hashCode());
		result = prime * result + (unsure ? 1231 : 1237);
		long temp;
		temp = Double.doubleToLongBits(value);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		PValueInterpreter other = (PValueInterpreter) obj;
		if (comparitiveOperator == null) {
			if (other.comparitiveOperator != null)
				return false;
		} else if (!comparitiveOperator.equals(other.comparitiveOperator))
			return false;
		if (unsure != other.unsure)
			return false;
		if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PValueInterpreter [value=" + value + ", comparitiveOperator=" + comparitiveOperator + ", unsure="
				+ unsure + "]";
	}

	public String asFormattedString() {
		return (unsure ? "unsure " : "") + comparitiveOperator + " " + value;
	}

	
	
	final public static int defaultValue = 0;
	final public static String defaultComparitiveOperator = null;
	final public static boolean defaultUnsure = false;

	public PValueInterpreter(String surfaceForm) {
		super(surfaceForm);

		
		 String comparitiveOperator = defaultComparitiveOperator;
		 boolean unsure = defaultUnsure;
		 double value = defaultValue;

		 
		Matcher matcher = PValueInterpreter.PATTERN.matcher(surfaceForm);

		if (matcher.find()) {

			if (matcher.group(0).equals(surfaceForm)) {
				interpretable = true;

				surfaceForm = matcher.group();

				if (matcher.group(pattern1GrouName) != null) {
					if (matcher.group(numGroup) != null)
						value = Double.valueOf(matcher.group(numGroup));
					if (matcher.group(operatorGroup) != null)
						comparitiveOperator = matcher.group(operatorGroup);

					if (!(comparitiveOperator.equals("<") || comparitiveOperator.equals(">"))) {
						unsure = true;
					}

				}
			}
		}
		this.comparitiveOperator = comparitiveOperator;
		this.unsure = unsure;
		this.value = value;
	}


	@Override
	public String asSimpleString() {
		return ""+value;

	}

	@Override
	public PValueInterpreter normalize() {
		return this;
	}

}
