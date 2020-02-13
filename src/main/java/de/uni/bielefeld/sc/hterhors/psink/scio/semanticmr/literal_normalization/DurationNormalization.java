package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.DurationInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ILiteralInterpreter;

public class DurationNormalization extends AbstractSCIONormalization {

	public DurationNormalization() {
		super(EntityType.get("Duration"));
	}

	@Override
	public ILiteralInterpreter getInterpreter(String surfaceForm) {
		return new DurationInterpreter(surfaceForm);
	}

}
