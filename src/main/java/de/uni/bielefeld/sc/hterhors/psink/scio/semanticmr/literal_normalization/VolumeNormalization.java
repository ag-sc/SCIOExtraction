package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.VolumeInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ILiteralInterpreter;

public class VolumeNormalization extends AbstractSCIONormalization {

	public VolumeNormalization() {
		super(EntityType.get("Volume"));
	}

	@Override
	public ILiteralInterpreter getInterpreter(String surfaceForm) {
		return new VolumeInterpreter(surfaceForm);
	}

}
