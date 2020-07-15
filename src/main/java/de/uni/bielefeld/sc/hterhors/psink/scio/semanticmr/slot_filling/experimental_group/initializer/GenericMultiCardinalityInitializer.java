package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.initializer;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;

public class GenericMultiCardinalityInitializer implements IStateInitializer {
	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The maximum number of annotations
	 */
	private final int max;

	/**
	 * The least number of annotation
	 */
	private final int min;

	private EntityType rootType;

	public GenericMultiCardinalityInitializer(EntityType rootType, int min, int max) {
		this.rootType = rootType;
		this.max = max;
		this.min = min;

	}

	public GenericMultiCardinalityInitializer(EntityType rootType, List<Instance> trainingInstances) {
		this.rootType = rootType;
		double e = computeMean(trainingInstances);
		double stdDev = computeStdDeviation(trainingInstances, e);
		this.min = (int) Math.round((e - stdDev));
		this.max = (int) Math.round((e + stdDev));
	}

	private double computeStdDeviation(List<Instance> trainingInstances, double e) {
		double stdDev = 0;
		for (Instance instance : trainingInstances) {
			stdDev += Math.pow(e - instance.getGoldAnnotations().getAbstractAnnotations().size(), 2)
					/ trainingInstances.size();
		}
		stdDev = Math.sqrt(stdDev);
		return stdDev;
	}

	private double computeMean(List<Instance> trainingInstances) {
		double e = 0;
		for (Instance instance : trainingInstances) {
			e += instance.getGoldAnnotations().getAbstractAnnotations().size();
		}
		e /= trainingInstances.size();
		return e;
	}

	@Override
	public State getInitState(Instance instance) {
		return getInitMultiStates(instance).get(0);
	}

	@Override
	public List<State> getInitMultiStates(Instance instance) {
		List<State> list = new ArrayList<>();

		for (int current = min; current <= max; current++) {
			List<AbstractAnnotation> experimentalGroups = new ArrayList<>();

			if (experimentalGroups.size() < current)
				for (int i = experimentalGroups.size(); i < current; i++) {
					EntityTemplate init = new EntityTemplate(AnnotationBuilder.toAnnotation(rootType));
					experimentalGroups.add(init);
				}

			list.add(new State(instance, new Annotations(experimentalGroups)));

		}

		return list;
	}

}
