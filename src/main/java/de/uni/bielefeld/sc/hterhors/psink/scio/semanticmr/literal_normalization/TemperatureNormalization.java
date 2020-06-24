package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.TemperatureInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ILiteralInterpreter;

public class TemperatureNormalization extends AbstractSCIONormalization {

	public TemperatureNormalization() {
		super(EntityType.get("Temperature"));
	}

	@Override
	public ILiteralInterpreter getInterpreter(String surfaceForm) {
		return new TemperatureInterpreter(surfaceForm);
	}

}
