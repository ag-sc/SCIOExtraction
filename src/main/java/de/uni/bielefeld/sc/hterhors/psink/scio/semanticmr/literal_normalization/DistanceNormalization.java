package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.DistanceInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ILiteralInterpreter;

public class DistanceNormalization extends AbstractSCIONormalization {

	public DistanceNormalization() {
		super(EntityType.get("Distance"));
	}

	@Override
	public ILiteralInterpreter getInterpreter(String surfaceForm) {
		return new DistanceInterpreter(surfaceForm);
	}

}
