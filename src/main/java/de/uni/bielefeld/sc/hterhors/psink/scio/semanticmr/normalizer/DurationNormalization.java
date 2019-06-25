package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.DurationInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.struct.ILiteralInterpreter;

public class DurationNormalization extends AbstractSCIONormalization {

	public DurationNormalization() {
		super(EntityType.get("Duration"));
	}

	@Override
	public ILiteralInterpreter getInterpreter(String surfaceForm) {
		return new DurationInterpreter(surfaceForm);
	}

}
