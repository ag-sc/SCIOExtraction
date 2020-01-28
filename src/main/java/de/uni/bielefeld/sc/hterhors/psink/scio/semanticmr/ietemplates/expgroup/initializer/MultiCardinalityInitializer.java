package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.initializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer.ESamplingMode;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering.kmeans.KMeansWords;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EExtractGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EGroupNamesPreProcessingMode;

public class MultiCardinalityInitializer implements IStateInitializer {

	/**
	 * The maximum number of annotations
	 */
	private final int max;

	/**
	 * The current number of annotations;
	 */
	private int current;

	private final EExtractGroupNamesMode groupNameMode;
	private final EGroupNamesPreProcessingMode groupNamesPreProcessingMode;

	private Map<Instance, List<List<DocumentLinkedAnnotation>>> allClusters = new HashMap<>();

	public MultiCardinalityInitializer(EExtractGroupNamesMode groupNameMode,
			EGroupNamesPreProcessingMode groupNamesPreProcessingMode, List<Instance> instances, int max) {
		this.max = max;
		this.current = 1;
		this.groupNameMode = groupNameMode;
		this.groupNamesPreProcessingMode = groupNamesPreProcessingMode;

		for (Instance instance : instances) {
			List<DocumentLinkedAnnotation> datapoints = new ArrayList<>();

			for (AbstractAnnotation ec : instance.getSlotTypeCandidates(ESamplingMode.ANNOTATION_BASED,
					SCIOSlotTypes.hasGroupName)) {
				if (ec.isInstanceOfDocumentLinkedAnnotation())
					datapoints.add(ec.asInstanceOfDocumentLinkedAnnotation());
			}

			List<List<DocumentLinkedAnnotation>> clusters = KMeansWords.cluster(datapoints, max);

			allClusters.put(instance, clusters);
		}
	}

	@Override
	public State getInitState(Instance instance) {
		return null;
	}

	@Override
	public List<State> getInitMultiStates(Instance instance) {
		List<State> list = new ArrayList<>();

		if (groupNameMode == EExtractGroupNamesMode.GOLD
				&& groupNamesPreProcessingMode == EGroupNamesPreProcessingMode.GOLD_CLUSTERING) {
			current = 1;

			do {
				List<AbstractAnnotation> as = new ArrayList<>();

				int count = 0;
				for (EntityTemplate goldAnnotation : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {

					if (current == count)
						break;

					count++;

					EntityTemplate init = new EntityTemplate(
							AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));

					if (goldAnnotation.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
						init.addMultiSlotFiller(SCIOSlotTypes.hasGroupName,
								AnnotationBuilder.toAnnotation(instance.getDocument(), SCIOEntityTypes.groupName,
										goldAnnotation.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation()
												.getSurfaceForm(),
										goldAnnotation.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation()
												.getStartDocCharOffset()));

					for (AbstractAnnotation groupName : goldAnnotation.asInstanceOfEntityTemplate()
							.getMultiFillerSlot(SCIOSlotTypes.hasGroupName).getSlotFiller()) {
						init.addMultiSlotFiller(SCIOSlotTypes.hasGroupName, groupName);
					}
					as.add(init);

				}

				if (count < current)
					for (int i = count; i < current; i++) {
						EntityTemplate init = new EntityTemplate(
								AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));
						as.add(init);
					}

				list.add(new State(instance, new Annotations(as)));
			} while (increase());

		} else {
			List<List<DocumentLinkedAnnotation>> clusters = allClusters.get(instance);

			for (int numOfClusters = 1; numOfClusters <= clusters.size(); numOfClusters++) {

				List<AbstractAnnotation> experimentalGroups = new ArrayList<>(clusters.size());

				for (int clusterIndex = 0; clusterIndex < numOfClusters; clusterIndex++) {

					EntityTemplate experimentalGroup = new EntityTemplate(
							AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));

					List<DocumentLinkedAnnotation> cluster = clusters.get(clusterIndex);

					for (DocumentLinkedAnnotation groupName : cluster) {
						experimentalGroup.addMultiSlotFiller(SCIOSlotTypes.hasGroupName, groupName);
					}

					experimentalGroups.add(experimentalGroup);

				}

				list.add(new State(instance, new Annotations(experimentalGroups)));
			}

		}

		return list;
	}

	/**
	 * Increases the current number of annotations.
	 * 
	 * @return true if current number is smaller or equal to max.
	 */
	private boolean increase() {
		current++;
		if (current <= max)
			return true;
		return false;
	}

}
