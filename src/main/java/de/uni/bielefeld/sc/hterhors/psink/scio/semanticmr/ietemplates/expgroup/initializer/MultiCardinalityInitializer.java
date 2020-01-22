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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EExtractGroupNamesMode;

public class MultiCardinalityInitializer implements IStateInitializer {

	/**
	 * The maximum number of annotations
	 */
	private final int max;

	/**
	 * The current number of annotations;
	 */
	private int current;

	public MultiCardinalityInitializer(int max) {
		this.max = max;
		this.current = 1;
	}

	@Override
	public State getInitState(Instance instance) {
		List<AbstractAnnotation> as = new ArrayList<>();

		for (int i = 0; i < current; i++) {

			EntityTemplate init = new EntityTemplate(
					AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));

			as.add(init);
		}

		return new State(instance, new Annotations(as));

	}

	/**
	 * Increases the current number of annotations.
	 * 
	 * @return true if current number is smaller or equal to max.
	 */
	public boolean increase() {
		current++;
		if (current <= max)
			return false;
		return true;
	}

}
