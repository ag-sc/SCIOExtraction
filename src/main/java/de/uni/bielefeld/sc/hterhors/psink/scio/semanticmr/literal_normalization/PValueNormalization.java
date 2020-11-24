package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.DosageInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.PValueInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ILiteralInterpreter;

public class PValueNormalization extends AbstractSCIONormalization {

	public PValueNormalization() {
		super(EntityType.get("PValue"));
	}

	@Override
	public ILiteralInterpreter getInterpreter(String surfaceForm) {
		return new PValueInterpreter(surfaceForm);
	}

}
