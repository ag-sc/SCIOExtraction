package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.AgeInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.struct.ILiteralInterpreter;

public class AgeNormalization extends AbstractSCIONormalization {

	public AgeNormalization() {
		super(EntityType.get("Age"));
	}

	@Override
	public ILiteralInterpreter getInterpreter(String surfaceForm) {
		return new AgeInterpreter(surfaceForm);
	}

}
