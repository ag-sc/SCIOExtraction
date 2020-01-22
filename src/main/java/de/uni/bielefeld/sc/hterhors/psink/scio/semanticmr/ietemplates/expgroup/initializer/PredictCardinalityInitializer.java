package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.initializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;

public class PredictCardinalityInitializer implements IStateInitializer {

	private final Map<Instance, Integer> cache = new HashMap<>();

	public PredictCardinalityInitializer(List<Instance> instances) {

		for (Instance instance : instances) {
			cache.put(instance, predictNumber());
		}
	}

	private Integer predictNumber() {
		return null;
	}

	@Override
	public State getInitState(Instance instance) {
		List<AbstractAnnotation> as = new ArrayList<>();

		for (int i = 0; i < cache.getOrDefault(instance, 0); i++) {

			EntityTemplate init = new EntityTemplate(
					AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));

			as.add(init);
		}

		return new State(instance, new Annotations(as));
	}

}
