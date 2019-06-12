package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.struct;

import java.io.Serializable;
import java.text.DecimalFormat;

public interface ILiteralInterpreter extends Serializable {

	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.########");

	public boolean isInterpretable();

	public String asFormattedString();
	public String asSimpleString();

	public ILiteralInterpreter normalize();

	public boolean isNumeric();

}
