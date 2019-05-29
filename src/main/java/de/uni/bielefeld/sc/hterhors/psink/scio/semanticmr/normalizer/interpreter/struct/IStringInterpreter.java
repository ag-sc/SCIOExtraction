package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.struct;

public interface IStringInterpreter extends ILiteralInterpreter {


	default public boolean isNumeric() {
		return false;
	}
}
