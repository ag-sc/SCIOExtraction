package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.ForceInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ILiteralInterpreter;

public class ForceNormalization extends AbstractSCIONormalization {

	public ForceNormalization() {
		super(EntityType.get("Force"));
	}

	@Override
	public ILiteralInterpreter getInterpreter(String surfaceForm) {
		return new ForceInterpreter(surfaceForm);
	}

}
