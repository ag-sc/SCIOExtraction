package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering.weka;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering.Baseline;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering.GroupNameHelperExtractionn;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering.GroupNamePair;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.specifications.ExperimentalGroupSpecifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.sentenceclassification.InstanceCollection;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.sentenceclassification.InstanceCollection.FeatureDataPoint;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ArffSaver;

/**
 * Playground to Classify sentences into important or not important. Maybe multi
 * class classification.
 * 
 * @author hterhors
 *
 */
public class GroupNameClusteringWEKA {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private static final File instanceDirectory = new File(
			"src/main/resources/slotfilling/experimental_group/corpus/instances/");

	public static void main(String[] args) throws Exception {

		SystemScope.Builder.getScopeHandler().addScopeSpecification(ExperimentalGroupSpecifications.systemsScope)
				.build();
		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(100L)
				.setTrainingProportion(100).setCorpusSizeFraction(1F).build();

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		new Baseline(instanceProvider.getInstances());

//		GroupNameClusteringWEKA groupNameClustering = new GroupNameClusteringWEKA(instanceProvider.getInstances());

//		double mse = 0;
//		for (Instance testInstance : instanceProvider.getRedistributedTestInstances()) {
//			System.out.println(testInstance.getName());
//			double x = groupNameClustering.test(testInstance);
//			mse += Math.pow(x - testInstance.getGoldAnnotations().getAnnotations().size(), 2);
//			System.out.println(x + "\t" + testInstance.getGoldAnnotations().getAnnotations().size());
//
//		}
//		System.out.println("MSE:" + Math.sqrt(mse));

	}

//	private final List<String> binaryValues = Arrays.asList("0", "1");
	private static final String CLASSIFICATION_LABEL_YES = "Y";

	private static final String CLASSIFICATION_LABEL_NO = "N";

	private final Attribute classAttribute = new Attribute("classLabel",
			Arrays.asList(CLASSIFICATION_LABEL_NO, CLASSIFICATION_LABEL_YES));

	private InstanceCollection trainingData = new InstanceCollection();

	private final Classifier rf;

	public GroupNameClusteringWEKA(List<Instance> allInstances) throws IOException {

		rf = new RandomForest();

		Map<Boolean, Set<GroupNamePair>> pairs = GroupNameHelperExtractionn.extractTrainingData(allInstances);

		List<FeatureDataPoint> dataPoints = new ArrayList<>();

		dataPoints.addAll(convertToDataPoints(pairs.get(false)));
		dataPoints.addAll(convertToDataPoints(pairs.get(true)));

		dataPoints.forEach(fdp -> trainingData.addFeatureDataPoint(fdp));

		trainingData = trainingData.removeRareFeatures(25);
		System.out.println(trainingData.numberOfTotalFeatures());
		Instances wekaTRAINInstance = convertToWekaInstances("FULL", trainingData.getDataPoints());

		saveArff(new File("groupNameClustering.arff"), wekaTRAINInstance);

//		try {
//			rf.buildClassifier(wekaTRAINInstance);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		log.info("done!");

	}

//	public double test(Instance testInstance) {
//
//		return classifyForInstance(rf, convertToWekaInstances("TEST", convertInstanceToDataPoints(testInstance)));
//
//	}

	public double classifyDocument(GroupNamePair groupNamePair) {

		FeatureDataPoint fdp = new FeatureDataPoint(groupNamePair.groupName1.document.documentID, 0, null, trainingData,
				getFeatures(groupNamePair), 0, false);

		return classifyForInstance(rf, convertToWekaInstances("TEST", fdp));
	}

	private double classifyForInstance(Classifier rf, weka.core.Instance instance) {

		try {
			double prob = rf.classifyInstance(instance);

			return prob;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;

	}

	private Map<String, Double> getFeatures(GroupNamePair groupNamePair) {

		Map<String, Double> features = new HashMap<>();

		FeaturesFactory.set(features, groupNamePair);
		FeaturesFactory.levenshtein();
		FeaturesFactory.overlap();
		FeaturesFactory.charBasedNGrams();

		return features;
	}

	private List<FeatureDataPoint> convertToDataPoints(Set<GroupNamePair> pairs) {

		List<FeatureDataPoint> dataPoints = new ArrayList<>();

		for (GroupNamePair groupNamePair : pairs) {
			dataPoints.add(new FeatureDataPoint(groupNamePair.groupName1.document.documentID, 0, null, trainingData,
					getFeatures(groupNamePair), groupNamePair.sameCluster ? 1D : 0D, true));
		}
		return dataPoints;

	}

	private weka.core.Instance convertToWekaInstances(final String dataSetName, final FeatureDataPoint dataPoint) {
		return convertToWekaInstances(dataSetName, Arrays.asList(dataPoint)).get(0);
	}

	private Instances convertToWekaInstances(final String dataSetName, final List<FeatureDataPoint> dataPoints) {

		Attribute[] attributes = new Attribute[trainingData.sparseIndexMapping.size()];

		for (Entry<String, Integer> attribute : trainingData.sparseIndexMapping.entrySet()) {
			attributes[attribute.getValue()] = new Attribute(attribute.getKey());
		}

		ArrayList<Attribute> attributeList = new ArrayList<>();
		attributeList.addAll(Arrays.asList(attributes));
		attributeList.add(classAttribute);

		Instances instances = new Instances(dataSetName, attributeList, trainingData.getDataPoints().size());
		instances.setClassIndex(attributeList.size() - 1);

		for (FeatureDataPoint fdp : dataPoints) {
			double[] attValues = new double[attributeList.size()];

			for (Entry<Integer, Double> d : fdp.features.entrySet()) {
				attValues[d.getKey()] = d.getValue();
			}

			attValues[attributeList.size() - 1] = fdp.score;
			instances.add(new SparseInstance(1, attValues));
		}

		return instances;
	}

	private void saveArff(final File arffOutputFile, Instances dataSet) throws IOException {
		ArffSaver saver = new ArffSaver();
		saver.setInstances(dataSet);
		saver.setFile(arffOutputFile);
		saver.writeBatch();
	}
}
