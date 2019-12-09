package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.WeightInterpreter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.interpreter.struct.ILiteralInterpreter;

public class WeightNormalization extends AbstractSCIONormalization {

	public WeightNormalization() {
		super(EntityType.get("Weight"));
	}

	@Override
	public ILiteralInterpreter getInterpreter(String surfaceForm) {
		return new WeightInterpreter(surfaceForm);
	}

}
