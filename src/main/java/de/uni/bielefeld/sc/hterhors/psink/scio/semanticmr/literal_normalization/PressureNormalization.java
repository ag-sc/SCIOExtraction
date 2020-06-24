package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.PressureInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ILiteralInterpreter;

public class PressureNormalization extends AbstractSCIONormalization {

	public PressureNormalization() {
		super(EntityType.get("Pressure"));
	}

	@Override
	public ILiteralInterpreter getInterpreter(String surfaceForm) {
		return new PressureInterpreter(surfaceForm);
	}

}
