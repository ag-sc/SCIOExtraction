package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.LengthInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ILiteralInterpreter;

public class LengthNormalization extends AbstractSCIONormalization {

	public LengthNormalization() {
		super(EntityType.get("Length"));
	}

	@Override
	public ILiteralInterpreter getInterpreter(String surfaceForm) {
		return new LengthInterpreter(surfaceForm);
	}

}
