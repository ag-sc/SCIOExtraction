package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct;

public interface IDoubleUnit extends IUnit {

	final static double E6 = 1000000D;
	final static double E3 = 1000D;

	public IDoubleUnitType getType();

	public double getNumeratorFactor();

	public double getDeterminatorFactor();

}
