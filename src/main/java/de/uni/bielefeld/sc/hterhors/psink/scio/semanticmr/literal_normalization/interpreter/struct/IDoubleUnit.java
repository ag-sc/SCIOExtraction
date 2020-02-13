package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct;

public interface IDoubleUnit extends IUnit {

	public IDoubleUnitType getType();

	public double getNumeratorFactor();

	public double getDeterminatorFactor();

}
