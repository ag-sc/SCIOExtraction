package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.LightIntensityInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ILiteralInterpreter;

public class LightIntensityNormalization extends AbstractSCIONormalization {

	public LightIntensityNormalization() {
		super(EntityType.get("LightIntensity"));
	}

	@Override
	public ILiteralInterpreter getInterpreter(String surfaceForm) {
		return new LightIntensityInterpreter(surfaceForm);
	}

}
