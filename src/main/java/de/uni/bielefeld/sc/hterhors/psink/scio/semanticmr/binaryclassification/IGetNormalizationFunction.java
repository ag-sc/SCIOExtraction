package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.binaryclassification;

import java.util.List;

import de.hterhors.semanticmr.crf.structure.annotations.normalization.AbstractNormalizationFunction;

public interface IGetNormalizationFunction {

	public List<AbstractNormalizationFunction> get();
	
}
