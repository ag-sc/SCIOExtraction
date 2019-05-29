package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.struct;

public interface INumericInterpreter extends ILiteralInterpreter {

	public double getMeanValue();

	public IUnit getUnit();

	default public boolean isNumeric() {
		return true;
	}
}
