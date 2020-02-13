package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.AgeInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ILiteralInterpreter;

public class AgeNormalization extends AbstractSCIONormalization {

	public AgeNormalization() {
		super(EntityType.get("Age"));
	}

	@Override
	public ILiteralInterpreter getInterpreter(String surfaceForm) {
		return new AgeInterpreter(surfaceForm);
	}

}
