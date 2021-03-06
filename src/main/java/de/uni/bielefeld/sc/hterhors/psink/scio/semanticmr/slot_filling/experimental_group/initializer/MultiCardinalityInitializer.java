package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.initializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.kmeans.WordBasedKMeans;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.weka.WEKAClustering;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EExtractGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EGroupNamesClusteringMode;

public class MultiCardinalityInitializer implements IStateInitializer {
	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The maximum number of annotations
	 */
	private int max;

	/**
	 * The least number of annotation
	 */
	private int min;

	private final EExtractGroupNamesMode groupNameMode;
	private final EGroupNamesClusteringMode groupNamesPreProcessingMode;

	/**
	 * k, cluster per instance
	 */
	private Map<Integer, Map<Instance, List<List<DocumentLinkedAnnotation>>>> preComputedClusters = new HashMap<>();
	final private String modelName;

	public MultiCardinalityInitializer(String modelName, EExtractGroupNamesMode groupNameMode,
			EGroupNamesClusteringMode groupNamesPreProcessingMode, List<Instance> instances, int min, int max,
			List<Instance> trainingInstances) {
		this.modelName = modelName;
		if (max <= 0 || min <= 0) {
			double e = computeMean(trainingInstances);
			double stdDev = computeStdDeviation(trainingInstances, e);
			this.min = (int) Math.round((e - stdDev));
			this.max = (int) Math.round((e + stdDev));
		} else {
			this.max = max;
			this.min = min;
		}

		this.min = this.min == 0 ? 2 : this.min;
		this.max = this.max == 0 ? 5 : this.max;

		this.groupNameMode = groupNameMode;
		this.groupNamesPreProcessingMode = groupNamesPreProcessingMode;

		if (!(groupNameMode == EExtractGroupNamesMode.GOLD
				&& groupNamesPreProcessingMode == EGroupNamesClusteringMode.GOLD_CLUSTERING)) {

			if (groupNameMode != EExtractGroupNamesMode.EMPTY
					&& groupNamesPreProcessingMode == EGroupNamesClusteringMode.KMEANS_CLUSTERING) {
				wordBasedKMeans(instances);
			} else if (groupNameMode != EExtractGroupNamesMode.EMPTY
					&& groupNamesPreProcessingMode == EGroupNamesClusteringMode.WEKA_CLUSTERING) {
				wekaBasedKmeans(instances, trainingInstances);
			}
		}
	}

	public MultiCardinalityInitializer(String modelName, EExtractGroupNamesMode groupNameProviderMode,
			EGroupNamesClusteringMode groupNameProcessingMode, List<Instance> instances,
			List<Instance> trainingInstances) {
		this(modelName, groupNameProviderMode, groupNameProcessingMode, instances, -1, -1, trainingInstances);

	}

	private void wekaBasedKmeans(List<Instance> instances, List<Instance> trainingInstances) {
		try {

			WEKAClustering gnc = new WEKAClustering(modelName);

			gnc.trainOrLoad(trainingInstances);
			for (int clusterSize = min; clusterSize <= max; clusterSize++) {
				preComputedClusters.putIfAbsent(clusterSize, new HashMap<>());
				for (Instance instance : instances) {

					log.info("Cluster instance " + instance.getName());
					List<DocumentLinkedAnnotation> datapoints = new ArrayList<>();

					for (AbstractAnnotation ec : instance.getSlotTypeCandidates(EExplorationMode.ANNOTATION_BASED,
							SCIOSlotTypes.hasGroupName)) {
						if (ec.isInstanceOfDocumentLinkedAnnotation())
							datapoints.add(ec.asInstanceOfDocumentLinkedAnnotation());
					}
					log.info("Number of elements to cluster: " + datapoints.size());

					List<List<DocumentLinkedAnnotation>> clusters = gnc.cluster(datapoints, clusterSize);

					preComputedClusters.get(clusterSize).put(instance, clusters);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void wordBasedKMeans(List<Instance> instances) {
		for (int clusterSize = min; clusterSize <= max; clusterSize++) {
			preComputedClusters.putIfAbsent(clusterSize, new HashMap<>());
			for (Instance instance : instances) {
				List<DocumentLinkedAnnotation> datapoints = new ArrayList<>();

				for (AbstractAnnotation ec : instance.getSlotTypeCandidates(EExplorationMode.ANNOTATION_BASED,
						SCIOSlotTypes.hasGroupName)) {
					if (ec.isInstanceOfDocumentLinkedAnnotation())
						datapoints.add(ec.asInstanceOfDocumentLinkedAnnotation());
				}

				List<List<DocumentLinkedAnnotation>> clusters = new WordBasedKMeans<DocumentLinkedAnnotation>()
						.cluster(datapoints, clusterSize);

				preComputedClusters.get(clusterSize).put(instance, clusters);
			}
		}
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
		return null;
	}

	@Override
	public List<State> getInitMultiStates(Instance instance) {
		List<State> list = new ArrayList<>();

		if (groupNameMode == EExtractGroupNamesMode.GOLD
				&& groupNamesPreProcessingMode == EGroupNamesClusteringMode.GOLD_CLUSTERING) {
			goldBased(instance, list);
		} else if (groupNameMode != EExtractGroupNamesMode.EMPTY
				&& (groupNamesPreProcessingMode == EGroupNamesClusteringMode.KMEANS_CLUSTERING
						|| groupNamesPreProcessingMode == EGroupNamesClusteringMode.WEKA_CLUSTERING)) {
			clusteringBased(instance, list);
		} else if (groupNameMode == EExtractGroupNamesMode.EMPTY) {
			empty(instance, list);
		} else if (groupNamesPreProcessingMode == EGroupNamesClusteringMode.NONE) {
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

		final int maxGroupNames = SCIOSlotTypes.hasGroupName.slotMaxCapacity;
		SCIOSlotTypes.hasGroupName.slotMaxCapacity = 1000;
		for (int clusterSize = min; clusterSize <= max; clusterSize++) {
			List<List<DocumentLinkedAnnotation>> clusters = preComputedClusters.get(clusterSize).get(instance);

			List<AbstractAnnotation> experimentalGroups = new ArrayList<>();
			if (clusters != null) {
				for (int i = 0; i < clusterSize; i++) {

					EntityTemplate experimentalGroup = new EntityTemplate(
							AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));

					List<DocumentLinkedAnnotation> cluster = clusters.get(i);

					for (DocumentLinkedAnnotation groupName : cluster) {
						experimentalGroup.addMultiSlotFiller(SCIOSlotTypes.hasGroupName, groupName);
					}

					experimentalGroups.add(experimentalGroup);

				}
			}
			addEmpty(clusterSize, experimentalGroups);

			list.add(new State(instance, new Annotations(experimentalGroups)));
		}
		SCIOSlotTypes.hasGroupName.slotMaxCapacity = maxGroupNames;

	}

	public void addEmpty(int current, List<AbstractAnnotation> experimentalGroups) {
		if (experimentalGroups.size() < current)
			for (int i = experimentalGroups.size(); i < current; i++) {
				EntityTemplate init = new EntityTemplate(
						AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));
				experimentalGroups.add(init);
			}
	}

	public void goldBased(Instance instance, List<State> list) {
		for (int current = min; current <= max; current++) {

			List<AbstractAnnotation> experimentalGroups = new ArrayList<>();

			for (int i = 0; i < Math.min(current,
					instance.getGoldAnnotations().<EntityTemplate>getAnnotations().size()); i++) {

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

}
