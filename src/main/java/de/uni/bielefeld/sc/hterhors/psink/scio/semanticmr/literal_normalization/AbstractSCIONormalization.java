package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.normalization.AbstractNormalizationFunction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ILiteralInterpreter;

abstract public class AbstractSCIONormalization extends AbstractNormalizationFunction {

	public AbstractSCIONormalization(EntityType entityType) {
		super(entityType);
	}

	@Override
	public String normalize(String annotation) {

		final ILiteralInterpreter wi = getInterpreter(annotation);

		if (wi.isInterpretable())
			return wi.normalize().asSimpleString();
		else
			return annotation;

	}

	abstract public ILiteralInterpreter getInterpreter(String surfaceForm);

}
