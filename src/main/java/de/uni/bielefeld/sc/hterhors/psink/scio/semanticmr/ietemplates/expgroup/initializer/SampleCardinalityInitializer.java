package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.initializer;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;

public class SampleCardinalityInitializer implements IStateInitializer {

	/**
	 * The number to begin with
	 */
	private final int beginWith;

	public SampleCardinalityInitializer(int beginWith) {
		this.beginWith = beginWith;
	}

	@Override
	public State getInitState(Instance instance) {
		final List<AbstractAnnotation> as = new ArrayList<>();

		for (int i = 0; i < beginWith; i++) {

			EntityTemplate init = new EntityTemplate(
					AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));

			as.add(init);
		}

		return new State(instance, new Annotations(as));

	}

}
