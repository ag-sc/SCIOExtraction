package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.initializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer.EExplorationMode;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.groupnames.clustering.kmeans.WordBasedKMeans;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.groupnames.clustering.weka.GroupNameClusteringWEKA;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EExtractGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EGroupNamesPreProcessingMode;

public class MultiCardinalityInitializer implements IStateInitializer {

	/**
	 * The maximum number of annotations
	 */
	private final int max;

	/**
	 * The least number of annotation
	 */
	private final int min;

	private final EExtractGroupNamesMode groupNameMode;
	private final EGroupNamesPreProcessingMode groupNamesPreProcessingMode;

	private Map<Instance, List<List<DocumentLinkedAnnotation>>> preComputedClusters = new HashMap<>();

	public MultiCardinalityInitializer(EExtractGroupNamesMode groupNameMode,
			EGroupNamesPreProcessingMode groupNamesPreProcessingMode, List<Instance> instances, int min, int max,
			List<Instance> trainingInstances) {

		if (max <= 0 || min <= 0) {
			double e = computeMean(trainingInstances);
			double stdDev = computeStdDeviation(trainingInstances, e);
			this.min = (int) Math.round((e - stdDev));
			this.max = (int) Math.round((e + stdDev));
		} else {
			this.max = max;
			this.min = min;
		}

		this.groupNameMode = groupNameMode;
		this.groupNamesPreProcessingMode = groupNamesPreProcessingMode;

		if (!(groupNameMode == EExtractGroupNamesMode.GOLD
				&& groupNamesPreProcessingMode == EGroupNamesPreProcessingMode.GOLD_CLUSTERING)) {
			if (groupNameMode != EExtractGroupNamesMode.EMPTY
					&& groupNamesPreProcessingMode == EGroupNamesPreProcessingMode.KMEANS_CLUSTERING) {
				wordBasedKMeans(instances);
			} else if (groupNameMode != EExtractGroupNamesMode.EMPTY
					&& groupNamesPreProcessingMode == EGroupNamesPreProcessingMode.WEKA_CLUSTERING) {
				wekaBasedKmeans(instances, trainingInstances);
			}
		}
	}

	public void wekaBasedKmeans(List<Instance> instances, List<Instance> trainingInstances) {
		try {

			GroupNameClusteringWEKA gnc = new GroupNameClusteringWEKA();

			gnc.train(trainingInstances);

			for (Instance instance : instances) {
				List<DocumentLinkedAnnotation> datapoints = new ArrayList<>();

				for (AbstractAnnotation ec : instance.getSlotTypeCandidates(EExplorationMode.ANNOTATION_BASED,
						SCIOSlotTypes.hasGroupName)) {
					if (ec.isInstanceOfDocumentLinkedAnnotation())
						datapoints.add(ec.asInstanceOfDocumentLinkedAnnotation());
				}
				List<List<DocumentLinkedAnnotation>> clusters = gnc.cluster(datapoints, max);

				preComputedClusters.put(instance, clusters);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void wordBasedKMeans(List<Instance> instances) {
		for (Instance instance : instances) {
			List<DocumentLinkedAnnotation> datapoints = new ArrayList<>();

			for (AbstractAnnotation ec : instance.getSlotTypeCandidates(EExplorationMode.ANNOTATION_BASED,
					SCIOSlotTypes.hasGroupName)) {
				if (ec.isInstanceOfDocumentLinkedAnnotation())
					datapoints.add(ec.asInstanceOfDocumentLinkedAnnotation());
			}

			List<List<DocumentLinkedAnnotation>> clusters = new WordBasedKMeans<DocumentLinkedAnnotation>()
					.cluster(datapoints, max);

			preComputedClusters.put(instance, clusters);
		}
	}

	public MultiCardinalityInitializer(EExtractGroupNamesMode groupNameProviderMode,
			EGroupNamesPreProcessingMode groupNameProcessingMode, List<Instance> instances,
			List<Instance> trainingInstances) {
		this(groupNameProviderMode, groupNameProcessingMode, instances, -1, -1, trainingInstances);

	}

	public double computeStdDeviation(List<Instance> trainingInstances, double e) {
		double stdDev = 0;
		for (Instance instance : trainingInstances) {
			stdDev += Math.pow(e - instance.getGoldAnnotations().getAbstractAnnotations().size(), 2)
					/ trainingInstances.size();
		}
		stdDev = Math.sqrt(stdDev);
		return stdDev;
	}

	public double computeMean(List<Instance> trainingInstances) {
		double e = 0;
		for (Instance instance : trainingInstances) {
			e += instance.getGoldAnnotations().getAbstractAnnotations().size();
		}
		e /= trainingInstances.size();
		return e;
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
			goldBased(instance, list);
		} else if (groupNameMode != EExtractGroupNamesMode.EMPTY
				&& (groupNamesPreProcessingMode == EGroupNamesPreProcessingMode.KMEANS_CLUSTERING
						|| groupNamesPreProcessingMode == EGroupNamesPreProcessingMode.WEKA_CLUSTERING)) {
			clusteringBased(instance, list);
		} else if (groupNameMode == EExtractGroupNamesMode.EMPTY) {
			empty(instance, list);
		}
		return list;
	}

	public void empty(Instance instance, List<State> list) {
		for (int current = min; current <= max; current++) {
			List<AbstractAnnotation> experimentalGroups = new ArrayList<>();

			addEmpty(current, experimentalGroups);

			list.add(new State(instance, new Annotations(experimentalGroups)));

		}
	}

	public void clusteringBased(Instance instance, List<State> list) {
		List<List<DocumentLinkedAnnotation>> clusters = preComputedClusters.get(instance);

		for (int current = min; current <= clusters.size(); current++) {

			List<AbstractAnnotation> experimentalGroups = new ArrayList<>(clusters.size());

			for (int i = 0; i < current; i++) {

				EntityTemplate experimentalGroup = new EntityTemplate(
						AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));

				List<DocumentLinkedAnnotation> cluster = clusters.get(i);

				for (DocumentLinkedAnnotation groupName : cluster) {
					experimentalGroup.addMultiSlotFiller(SCIOSlotTypes.hasGroupName, groupName);
				}

				experimentalGroups.add(experimentalGroup);

			}
			addEmpty(current, experimentalGroups);

			list.add(new State(instance, new Annotations(experimentalGroups)));
		}
	}

	public void goldBased(Instance instance, List<State> list) {
		for (int current = min; current <= max; current++) {

			List<AbstractAnnotation> experimentalGroups = new ArrayList<>();

			for (int i = 0; i < instance.getGoldAnnotations().<EntityTemplate>getAnnotations().size(); i++) {

				EntityTemplate goldAnnotation = instance.getGoldAnnotations().<EntityTemplate>getAnnotations().get(i);

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
				experimentalGroups.add(init);

			}

			addEmpty(current, experimentalGroups);

			list.add(new State(instance, new Annotations(experimentalGroups)));
		}
	}

	public void addEmpty(int current, List<AbstractAnnotation> experimentalGroups) {
		if (experimentalGroups.size() < current)
			for (int i = experimentalGroups.size(); i < current; i++) {
				EntityTemplate init = new EntityTemplate(
						AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));
				experimentalGroups.add(init);
			}
	}

}
