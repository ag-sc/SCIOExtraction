package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct;

public interface IStringInterpreter extends ILiteralInterpreter {


	default public boolean isNumeric() {
		return false;
	}
}
