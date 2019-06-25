package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.DosageInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.struct.ILiteralInterpreter;

public class DosageNormalization extends AbstractSCIONormalization {

	public DosageNormalization() {
		super(EntityType.get("Dosage"));
	}

	@Override
	public ILiteralInterpreter getInterpreter(String surfaceForm) {
		return new DosageInterpreter(surfaceForm);
	}

}
