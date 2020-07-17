package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.initializer;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;

public class SampleCardinalityInitializer implements IStateInitializer {

	/**
	 * The number to begin with
	 */
	private final int beginWith;
	private final EntityType rootEntityType;

	public SampleCardinalityInitializer(EntityType rootEntityType, int beginWith) {
		this.beginWith = beginWith;
		this.rootEntityType = rootEntityType;
	}

	@Override
	public State getInitState(Instance instance) {
		final List<AbstractAnnotation> as = new ArrayList<>();

		for (int i = 0; i < beginWith; i++) {

			EntityTemplate init = new EntityTemplate(AnnotationBuilder.toAnnotation(rootEntityType));

			as.add(init);
		}

		return new State(instance, new Annotations(as));

	}

}
