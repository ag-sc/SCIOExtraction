package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering.weka;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.cli.GnuParser;
import org.apache.commons.collections.set.SynchronizedSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering.GroupNameHelperExtractionn;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering.GroupNamePair;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.DataPointCollector;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.DataPointCollector.FeatureDataPoint;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SerializationHelper;
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

	private static final String CLASSIFICATION_LABEL_YES = "Y";

	private static final String CLASSIFICATION_LABEL_NO = "N";

	private final Attribute classAttribute = new Attribute("classLabel",
			Arrays.asList(CLASSIFICATION_LABEL_NO, CLASSIFICATION_LABEL_YES));

	private DataPointCollector trainingData = new DataPointCollector();
	private DataPointCollector testData = new DataPointCollector();

	private Classifier rf = null;

	public GroupNameClusteringWEKA() {

	}

	public void train(List<Instance> train) {

		extract(trainingData, train, true);
		trainingData = trainingData.removeRareFeatures(25);

		try {
			log.info("Load classifier...");
			rf = (RandomForest) SerializationHelper.read(new FileInputStream("wekamodels/rfsmall.model"));
		} catch (Exception e1) {
			log.info("Could not load classifiery: " + e1.getMessage());
			rf = null;
		}
		if (rf != null)
			return;

		Instances wekaTRAINInstance = convertToWekaInstances("TRAIN", trainingData.getDataPoints());
//		saveArff(new File("groupNameClustering_train.arff"), wekaTRAINInstance);
//		System.exit(1);

		rf = new RandomForest();
		((RandomForest) rf).setNumIterations(200);

		try {
			log.info("Build classifier...");
			rf.buildClassifier(wekaTRAINInstance);

		} catch (Exception e2) {
			e2.printStackTrace();
		}

		try {
			log.info("Save classifier...");
			SerializationHelper.write(new FileOutputStream("wekamodels/rfsmall.model"), rf);
		} catch (Exception e3) {
			e3.printStackTrace();
		}
	}

	public void test(List<Instance> test) throws Exception {
		List<GroupNamePair> pairs = extract(testData, test, false);
		Instances wekaTESTInstance = convertToWekaInstances("TEST", testData.getDataPoints());
//		saveArff(new File("groupNameClustering_test.arff"), wekaTESTInstance);
//		System.exit(1);

		Score score = new Score();
		int i = 0;
		for (weka.core.Instance instance : wekaTESTInstance) {
			try {
				double[] pred = rf.distributionForInstance(instance);

				int index = pred[0] > pred[1] ? 0 : 1;
//				System.out.println(pairs.get(i));
//				System.out.println(Arrays.toString(pred));

				String groundTruth = classAttribute.value((int) instance.classValue());

				String prediction = classAttribute.value((int) index);
//				System.out.println(groundTruth + ":" + prediction);
				i++;
				int tp = groundTruth.equals(CLASSIFICATION_LABEL_YES) && groundTruth.equals(prediction) ? 1 : 0;
				int tn = groundTruth.equals(CLASSIFICATION_LABEL_NO) && groundTruth.equals(prediction) ? 1 : 0;
				int fp = groundTruth.equals(CLASSIFICATION_LABEL_NO) && prediction.equals(CLASSIFICATION_LABEL_YES) ? 1
						: 0;
				int fn = groundTruth.equals(CLASSIFICATION_LABEL_YES) && prediction.equals(CLASSIFICATION_LABEL_NO) ? 1
						: 0;

				score.add(new Score(tp, fp, fn, tn));

//			if (groundTruth.equals(CLASSIFICATION_LABEL_RELEVANT) && prediction.equals(CLASSIFICATION_LABEL_NOT_RELEVANT)) {
//				System.out.println("########### FASLE NEG #############");
//			}
				//
//			System.out.println(Arrays.toString(probs));
//			System.out.println(featureDataPoint.docID);
//			System.out.println("Sentence: " + featureDataPoint.sentenceIndex);
//			System.out.println(featureDataPoint.sentence);
//			System.out.println("actual: " + groundTruth + ", predicted: " + prediction);
//			System.out.println();

			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		System.out.println(score);
		System.out.println(score.getAccuracy());
		log.info("done!");
	}

	public List<List<DocumentLinkedAnnotation>> cluster(List<DocumentLinkedAnnotation> anns) throws Exception {
		List<GroupNamePair> pairs = new ArrayList<>();

		for (int i = 0; i < anns.size(); i++) {
			for (int j = i + 1; j < anns.size(); j++) {
				pairs.add(new GroupNamePair(anns.get(i), anns.get(j), false));
			}
		}

		List<FeatureDataPoint> datapoints = convertToDataPoints(pairs, false);

		log.info("Number of datapoints: " + pairs.size());
		Instances predictionInstances = convertToWekaInstances("CLUSTER", datapoints);

		List<List<DocumentLinkedAnnotation>> clusters = new ArrayList<>();

		int i = 0;

		List<GroupNamePair> sameCluster = new ArrayList<>();
		List<GroupNamePair> diffCluster = new ArrayList<>();

		for (weka.core.Instance instance : predictionInstances) {

			double[] pred = rf.distributionForInstance(instance);
//			System.out.println(pairs.get(i));
//			System.out.println(Arrays.toString(pred));
			int index = pred[0] > pred[1] ? 0 : 1;

			String prediction = classAttribute.value(index);

			if (prediction.equals(CLASSIFICATION_LABEL_YES)) {
				sameCluster.add(new GroupNamePair(pairs.get(i), true));
			} else {
				diffCluster.add(new GroupNamePair(pairs.get(i), false));
			}

			i++;
		}

		
		log.info("Number of same clusters = " + sameCluster.size());
		log.info("Number of diff clusters = " + diffCluster.size());

		for (GroupNamePair groupNamePair : sameCluster) {
			System.out.println(groupNamePair);
		}
		System.out.println("*******");
		for (GroupNamePair groupNamePair : diffCluster) {
			System.out.println(groupNamePair);
		}

		List<Set<DocumentLinkedAnnotation>> clustersAsSet = new ArrayList<>();

		for (GroupNamePair groupNamePair : sameCluster) {
			System.out.println("####### ADD #########");
			System.out.println(groupNamePair.groupName1.toPrettyString());
			System.out.println(groupNamePair.groupName2.toPrettyString());
			System.out.println("######################");
			addToCluster(clustersAsSet, groupNamePair.groupName1, groupNamePair.groupName2, true);

			for (Set<DocumentLinkedAnnotation> a : clustersAsSet) {
				for (DocumentLinkedAnnotation set2 : a) {
					System.out.println(set2.toPrettyString());
				}
				System.out.println("---------------");
			}
			System.out.println();
		}

		List<Set<DocumentLinkedAnnotation>> _clustersAsSet = null;
		while (true) {
			_clustersAsSet = merge(clustersAsSet);
			if (_clustersAsSet.isEmpty())
				break;
			clustersAsSet = _clustersAsSet;
		}
		System.out.println("+++++++++++++++++++++");
		for (Set<DocumentLinkedAnnotation> a : clustersAsSet) {
			for (DocumentLinkedAnnotation set2 : a) {
				System.out.println(set2.toPrettyString());
			}
			System.out.println("---------------");
		}
		System.out.println();
		for (GroupNamePair groupNamePair : diffCluster) {
			addToCluster(clustersAsSet, groupNamePair.groupName1, groupNamePair.groupName2, false);
		}

		for (Set<DocumentLinkedAnnotation> set : clustersAsSet) {
			clusters.add(new ArrayList<>(set));
		}
		for (List<DocumentLinkedAnnotation> cluster : clusters) {
			Collections.sort(cluster, DocumentLinkedAnnotation.COMPARE_BY_SURFACEFORM);
		}

		return clusters;
	}

	private List<Set<DocumentLinkedAnnotation>> merge(List<Set<DocumentLinkedAnnotation>> clustersAsSet) {
		List<Set<DocumentLinkedAnnotation>> clustersAsSet2 = new ArrayList<>();

		for (int j = 0; j < clustersAsSet.size(); j++) {
			for (int k = j + 1; k < clustersAsSet.size(); k++) {

				outer: {
					for (DocumentLinkedAnnotation gn1 : clustersAsSet.get(j)) {
						for (DocumentLinkedAnnotation gn2 : clustersAsSet.get(k)) {
							if (gn1 == gn2) {
								Set<DocumentLinkedAnnotation> merge = new HashSet<>();
								merge.addAll(clustersAsSet.get(j));
								merge.addAll(clustersAsSet.get(k));
								clustersAsSet2.add(merge);
								break outer;
							}
						}
					}
					clustersAsSet2.add(clustersAsSet.get(j));
					clustersAsSet2.add(clustersAsSet.get(k));
				}
			}
		}
		return clustersAsSet2;
	}

	private void addToCluster(List<Set<DocumentLinkedAnnotation>> clustersAsSet, DocumentLinkedAnnotation groupName1,
			DocumentLinkedAnnotation groupName2, boolean sameCluster) {

		if (sameCluster) {
			addToCluster(groupName1, groupName2, clustersAsSet);
		} else {
			addToCluster(groupName1, clustersAsSet);
			addToCluster(groupName2, clustersAsSet);
		}
	}

	private void addToCluster(DocumentLinkedAnnotation groupName1, DocumentLinkedAnnotation groupName2,
			List<Set<DocumentLinkedAnnotation>> clustersAsSet) {

		for (Set<DocumentLinkedAnnotation> set : clustersAsSet) {
			if (set.contains(groupName1) || set.contains(groupName2)) {
				set.add(groupName1);
				set.add(groupName2);
				return;
			}
		}
		Set<DocumentLinkedAnnotation> newCluster = new HashSet<>();
		clustersAsSet.add(newCluster);
		newCluster.add(groupName1);
		newCluster.add(groupName2);
	}

	private void addToCluster(DocumentLinkedAnnotation groupName, List<Set<DocumentLinkedAnnotation>> clustersAsSet) {
		for (Set<DocumentLinkedAnnotation> set : clustersAsSet) {
			if (set.contains(groupName)) {
				set.add(groupName);
				return;
			}
		}
		Set<DocumentLinkedAnnotation> newCluster = new HashSet<>();
		clustersAsSet.add(newCluster);
		newCluster.add(groupName);
	}

	private List<GroupNamePair> extract(DataPointCollector instanceCollection, List<Instance> train, boolean training) {
		Map<Boolean, Set<GroupNamePair>> trainPairs = GroupNameHelperExtractionn.extractData(train);
		List<FeatureDataPoint> trainingDataPoints = new ArrayList<>();

		List<GroupNamePair> data = new ArrayList<>();

		List<GroupNamePair> sort1 = new ArrayList<>(trainPairs.get(false));
		List<GroupNamePair> sort2 = new ArrayList<>(trainPairs.get(true));

		data.addAll(sort1);
		data.addAll(sort2);

		trainingDataPoints.addAll(convertToDataPoints(sort1, training));
		trainingDataPoints.addAll(convertToDataPoints(sort2, training));

		for (FeatureDataPoint fdp : trainingDataPoints) {
			instanceCollection.addFeatureDataPoint(fdp);
		}

		return data;
	}

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
//		FeaturesFactory.charBasedNGrams();

		return features;
	}

	private List<FeatureDataPoint> convertToDataPoints(Collection<GroupNamePair> pairs, boolean training) {

		List<FeatureDataPoint> dataPoints = new ArrayList<>();

		for (GroupNamePair groupNamePair : pairs) {
			dataPoints.add(new FeatureDataPoint(groupNamePair.groupName1.document.documentID, 0, null, trainingData,
					getFeatures(groupNamePair), groupNamePair.sameCluster ? 1D : 0D, training));
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
