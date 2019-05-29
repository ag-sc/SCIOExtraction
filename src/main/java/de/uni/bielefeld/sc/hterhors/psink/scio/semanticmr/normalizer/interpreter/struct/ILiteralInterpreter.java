package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.struct;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

public interface ILiteralInterpreter extends Serializable {

	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.########");

	public boolean exists();

	public String asFormattedString();

	public ILiteralInterpreter normalize();

	public boolean isNumeric();

}
