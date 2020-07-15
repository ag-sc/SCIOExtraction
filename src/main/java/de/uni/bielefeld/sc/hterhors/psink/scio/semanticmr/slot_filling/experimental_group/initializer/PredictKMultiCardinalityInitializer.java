package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.initializer;

import java.util.ArrayList;
import java.util.Collections;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.ECardinalityMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EExtractGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EGroupNamesClusteringMode;

public class PredictKMultiCardinalityInitializer implements IStateInitializer {
	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The maximum number of annotations
	 */
	public final int max;

	/**
	 * The least number of annotation
	 */
	public final int min;

	private final EExtractGroupNamesMode groupNameMode;
	private final EGroupNamesClusteringMode groupNamesPreProcessingMode;
	private final ECardinalityMode cardinalityMode;

	/**
	 * k, cluster per instance
	 */
	private Map<Instance, List<List<DocumentLinkedAnnotation>>> preComputedClusters = new HashMap<>();
	private final String modelName;

	public PredictKMultiCardinalityInitializer(String modelName, ECardinalityMode cardinalityMode,
			EExtractGroupNamesMode groupNameMode, EGroupNamesClusteringMode groupNamesPreProcessingMode,
			List<Instance> instances, int min, int max, List<Instance> trainingInstances) {
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

		this.cardinalityMode = cardinalityMode;
		this.groupNameMode = groupNameMode;
		this.groupNamesPreProcessingMode = groupNamesPreProcessingMode;

//		if (!(groupNameMode == EExtractGroupNamesMode.GOLD
//				&& groupNamesPreProcessingMode == EGroupNamesClusteringMode.GOLD_CLUSTERING)) {

		if (groupNameMode != EExtractGroupNamesMode.EMPTY
				&& groupNamesPreProcessingMode == EGroupNamesClusteringMode.KMEANS_CLUSTERING) {
			wordBasedKMeans(instances);
		} else if (groupNameMode != EExtractGroupNamesMode.EMPTY
				&& groupNamesPreProcessingMode == EGroupNamesClusteringMode.WEKA_CLUSTERING) {
			wekaBasedKmeans(instances, trainingInstances);
		}
//		}
	}

	public PredictKMultiCardinalityInitializer(String modelName, ECardinalityMode cardinalityMode,
			EExtractGroupNamesMode groupNameProviderMode, EGroupNamesClusteringMode groupNameProcessingMode,
			List<Instance> instances, List<Instance> trainingInstances) {
		this(modelName, cardinalityMode, groupNameProviderMode, groupNameProcessingMode, instances, -1, -1,
				trainingInstances);
	}

	private void wekaBasedKmeans(List<Instance> instances, List<Instance> trainingInstances) {

		try {
			WEKAClustering gnc = new WEKAClustering(modelName);

			gnc.trainOrLoad(trainingInstances);

			for (Instance instance : instances) {

				log.info("Cluster instance " + instance.getName());
				List<DocumentLinkedAnnotation> datapoints = new ArrayList<>();

				for (AbstractAnnotation ec : instance.getSlotTypeCandidates(EExplorationMode.ANNOTATION_BASED,
						SCIOSlotTypes.hasGroupName)) {
					if (ec.isInstanceOfDocumentLinkedAnnotation())
						datapoints.add(ec.asInstanceOfDocumentLinkedAnnotation());
				}
				log.info("Number of elements to cluster: " + datapoints.size());

				List<List<DocumentLinkedAnnotation>> clusters = new ArrayList<>();
				if (cardinalityMode == ECardinalityMode.GOLD) {
					clusters = gnc.cluster(datapoints, instance.getGoldAnnotations().getAnnotations().size());
				} else if (cardinalityMode == ECardinalityMode.RSS_PREDICTED
						|| cardinalityMode == ECardinalityMode.RSS_PREDICTED_SAMPLE) {
					try {
						if (!datapoints.isEmpty())
							clusters = gnc.clusterRSS(datapoints, min, max);
					} catch (Exception e2) {
						log.info("catched Exception: ");
						e2.printStackTrace();
					}
				}
				preComputedClusters.put(instance, clusters);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void wordBasedKMeans(List<Instance> instances) {
		for (Instance instance : instances) {
			List<DocumentLinkedAnnotation> datapoints = new ArrayList<>();

			for (AbstractAnnotation ec : instance.getSlotTypeCandidates(EExplorationMode.ANNOTATION_BASED,
					SCIOSlotTypes.hasGroupName)) {
				if (ec.isInstanceOfDocumentLinkedAnnotation())
					datapoints.add(ec.asInstanceOfDocumentLinkedAnnotation());
			}
			List<List<DocumentLinkedAnnotation>> clusters = new ArrayList<>();
			if (cardinalityMode == ECardinalityMode.GOLD) {
				clusters = new WordBasedKMeans<DocumentLinkedAnnotation>().cluster(datapoints,
						instance.getGoldAnnotations().getAnnotations().size());
			} else if (cardinalityMode == ECardinalityMode.RSS_PREDICTED
					|| cardinalityMode == ECardinalityMode.RSS_PREDICTED_SAMPLE) {

				clusters = new WordBasedKMeans<DocumentLinkedAnnotation>().clusterRSS(datapoints, min, max);
			}

			preComputedClusters.put(instance, clusters);
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
		if (groupNameMode != EExtractGroupNamesMode.EMPTY
				&& (groupNamesPreProcessingMode == EGroupNamesClusteringMode.KMEANS_CLUSTERING
						|| groupNamesPreProcessingMode == EGroupNamesClusteringMode.WEKA_CLUSTERING)) {
			return clusteringBased(instance);
		} else if (groupNameMode == EExtractGroupNamesMode.EMPTY) {
		}
		return null;
	}

	@Override
	public List<State> getInitMultiStates(Instance instance) {
		return Collections.emptyList();
	}

	public State clusteringBased(Instance instance) {

		List<List<DocumentLinkedAnnotation>> clusters = preComputedClusters.get(instance);

		List<AbstractAnnotation> experimentalGroups = new ArrayList<>();
		final int maxGroupNames = SCIOSlotTypes.hasGroupName.slotMaxCapacity;
		SCIOSlotTypes.hasGroupName.slotMaxCapacity = 1000;
		if (clusters != null) {
			for (int i = 0; i < clusters.size(); i++) {

				EntityTemplate experimentalGroup = new EntityTemplate(
						AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));

				List<DocumentLinkedAnnotation> cluster = clusters.get(i);

				for (DocumentLinkedAnnotation groupName : cluster) {
					experimentalGroup.addMultiSlotFiller(SCIOSlotTypes.hasGroupName, groupName);
				}

				experimentalGroups.add(experimentalGroup);

			}
		}
		SCIOSlotTypes.hasGroupName.slotMaxCapacity = maxGroupNames;
		return new State(instance, new Annotations(experimentalGroups));
	}

}
