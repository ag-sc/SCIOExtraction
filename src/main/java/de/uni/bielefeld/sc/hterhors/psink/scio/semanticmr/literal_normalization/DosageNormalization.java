package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.DosageInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.interpreter.struct.ILiteralInterpreter;

public class DosageNormalization extends AbstractSCIONormalization {

	public DosageNormalization() {
		super(EntityType.get("DosageExtracorporal"));
	}

	@Override
	public ILiteralInterpreter getInterpreter(String surfaceForm) {
		return new DosageInterpreter(surfaceForm);
	}

}
