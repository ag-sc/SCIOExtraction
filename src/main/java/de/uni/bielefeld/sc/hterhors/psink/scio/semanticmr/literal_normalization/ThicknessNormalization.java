package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.ThicknessInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ILiteralInterpreter;

public class ThicknessNormalization extends AbstractSCIONormalization {

	public ThicknessNormalization() {
		super(EntityType.get("Thickness"));
	}

	@Override
	public ILiteralInterpreter getInterpreter(String surfaceForm) {
		return new ThicknessInterpreter(surfaceForm);
	}

}
