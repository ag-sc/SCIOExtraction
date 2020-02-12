package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.methods.weka;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.helper.GroupNameDataSetHelper;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.helper.GroupNamePair;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.kmeans.BinaryClusterBasedKMeans;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.DataPointCollector;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.DataPointCollector.DataPoint;
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
public class WEKAClustering {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private static final String CLASSIFICATION_LABEL_YES = "Y";

	private static final String CLASSIFICATION_LABEL_NO = "N";

	private static final String CLASSIFICATION_LABEL_UNKOWN = "?";

	private final Attribute classAttribute = new Attribute("classLabel",
			Arrays.asList(CLASSIFICATION_LABEL_NO, CLASSIFICATION_LABEL_YES, CLASSIFICATION_LABEL_UNKOWN));

	private DataPointCollector trainingDataCollector = new DataPointCollector();
	private DataPointCollector testDataCollector = new DataPointCollector();

	private Classifier rf = null;

	public WEKAClustering() {

	}

	public void train(List<Instance> train) throws IOException {

		collectInstances(train, trainingDataCollector, true);
		trainingDataCollector = trainingDataCollector.removeRareFeatures(25);

		try {
			log.info("Load classifier...");
			rf = (RandomForest) SerializationHelper.read(new FileInputStream("wekamodels/rfsmall.model"));
			log.info("Done...");

		} catch (Exception e1) {
			log.info("Could not load classifiery: " + e1.getMessage());
			rf = null;
		}
		if (rf != null)
			return;

		Instances wekaTRAINInstance = convertToWekaInstances("TRAIN", trainingDataCollector.getDataPoints());
		saveArff(new File("groupNameClustering_train.arff"), wekaTRAINInstance);
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

	public Score test(List<Instance> test) throws Exception {
		collectInstances(test, testDataCollector, false);

		Instances wekaTESTInstance = convertToWekaInstances("TEST", testDataCollector.getDataPoints());

		saveArff(new File("groupNameClustering_test.arff"), wekaTESTInstance);
		Score score = new Score();

		for (weka.core.Instance instance : wekaTESTInstance) {
			try {

				double[] pred = rf.distributionForInstance(instance);

				int index = pred[0] > pred[1] ? 0 : 1;

				String groundTruth = classAttribute.value((int) instance.classValue());

				String prediction = classAttribute.value((int) index);
				int tp = groundTruth.equals(CLASSIFICATION_LABEL_YES) && groundTruth.equals(prediction) ? 1 : 0;
				int tn = groundTruth.equals(CLASSIFICATION_LABEL_NO) && groundTruth.equals(prediction) ? 1 : 0;
				int fp = groundTruth.equals(CLASSIFICATION_LABEL_NO) && prediction.equals(CLASSIFICATION_LABEL_YES) ? 1
						: 0;
				int fn = groundTruth.equals(CLASSIFICATION_LABEL_YES) && prediction.equals(CLASSIFICATION_LABEL_NO) ? 1
						: 0;

				score.add(new Score(tp, fp, fn, tn));

			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return score;
	}

	public List<List<DocumentLinkedAnnotation>> cluster(List<DocumentLinkedAnnotation> anns, int k) throws Exception {
		log.info("Number of annotations to cluster: " + anns.size());
		/**
		 * TODO: parameterize if distinction should happen or not .
		 */
		Map<String, Set<DocumentLinkedAnnotation>> distinctMap = new HashMap<>();
		makeDistinct: {

			for (DocumentLinkedAnnotation documentLinkedAnnotation : anns) {
				distinctMap.putIfAbsent(documentLinkedAnnotation.getSurfaceForm(), new HashSet<>());

				distinctMap.get(documentLinkedAnnotation.getSurfaceForm()).add(documentLinkedAnnotation);

			}

			List<DocumentLinkedAnnotation> distinctAnnotations = new ArrayList<>();
			for (Set<DocumentLinkedAnnotation> dlas : distinctMap.values()) {
				for (DocumentLinkedAnnotation documentLinkedAnnotation : dlas) {
					distinctAnnotations.add(documentLinkedAnnotation);
					break;
				}
			}

			anns = distinctAnnotations;
		}
		log.info("Number of DISTINCT annotations to cluster: " + anns.size());

		DataPointCollector predictions = new DataPointCollector();
		List<GroupNamePair> pairs = new ArrayList<>();

		for (int i = 0; i < anns.size(); i++) {
			for (int j = i + 1; j < anns.size(); j++) {
				pairs.add(new GroupNamePair(anns.get(i), anns.get(j), false, 3));
			}
		}

		List<DataPoint> datapoints = convertToDataPoints(pairs, false);

		for (DataPoint dataPoint : datapoints) {
			predictions.addFeatureDataPoint(dataPoint);
		}

		Instances predictionInstances = convertToWekaInstances("CLUSTER", predictions.getDataPoints());
		int i = 0;
		List<GroupNamePair> gnps = new ArrayList<>();
		log.info("Num of pairwise instances to classify = " + predictionInstances.size());

		for (weka.core.Instance instance : predictionInstances) {
			if (i % (predictionInstances.size() / 10) == 0)
				log.info("Num of instances classified = " + i);
			GroupNamePair gnp = (GroupNamePair) predictions.getDataPoints().get(i).parameter.get("groupNamePair");

			double[] pred = rf.distributionForInstance(instance);

			int index = pred[0] > pred[1] ? 0 : 1;

			String prediction = classAttribute.value(index);

			if (prediction.equals(CLASSIFICATION_LABEL_YES)) {
				gnps.add(new GroupNamePair(gnp, true, pred[1]));
			} else {
				gnps.add(new GroupNamePair(gnp, false, pred[1]));
			}

			i++;
		}
		log.info("Binary classification done...");

		BinaryClusterBasedKMeans<DocumentLinkedAnnotation> kmeans = new BinaryClusterBasedKMeans<>(gnps);

		List<List<DocumentLinkedAnnotation>> distinctClusters = kmeans.cluster(anns, k);
		List<List<DocumentLinkedAnnotation>> clusters = new ArrayList<>();
		resolveDistinction: {

			for (List<DocumentLinkedAnnotation> list : distinctClusters) {

				List<DocumentLinkedAnnotation> dlas = new ArrayList<>(list);

				for (DocumentLinkedAnnotation dla : list) {
					dlas.addAll(distinctMap.get(dla.getSurfaceForm()));
				}
				clusters.add(dlas);
			}

		}

		return clusters;

	}

	private void collectInstances(List<Instance> instances, DataPointCollector collector, boolean training) {

		Map<Boolean, Set<GroupNamePair>> dataSet = GroupNameDataSetHelper.getGroupNameClusterDataSet(instances);

		for (DataPoint fdp : convertToDataPoints(dataSet.get(false), training)) {
			collector.addFeatureDataPoint(fdp);
		}
		for (DataPoint fdp : convertToDataPoints(dataSet.get(true), training)) {
			collector.addFeatureDataPoint(fdp);
		}

	}

	public double classifyDocument(GroupNamePair groupNamePair) {
		Map<String, Object> parameter = new HashMap<>();

		parameter.put("docID", groupNamePair.groupName1.asInstanceOfDocumentLinkedAnnotation().document.documentID);

		DataPoint fdp = new DataPoint(parameter, trainingDataCollector, getFeatures(groupNamePair), 0, false);

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

	private List<DataPoint> convertToDataPoints(Collection<GroupNamePair> pairs, boolean training) {

		List<DataPoint> dataPoints = new ArrayList<>();

		for (GroupNamePair groupNamePair : pairs) {
			Map<String, Object> parameter = new HashMap<>();

			parameter.put("docID", groupNamePair.groupName1.asInstanceOfDocumentLinkedAnnotation().document.documentID);

			DataPoint fdp = new DataPoint(parameter, trainingDataCollector, getFeatures(groupNamePair),
					groupNamePair.sameCluster ? 1D : 0D, training);
			fdp.parameter.put("groupNamePair", groupNamePair);
			dataPoints.add(fdp);
		}
		return dataPoints;

	}

	private weka.core.Instance convertToWekaInstances(final String dataSetName, final DataPoint dataPoint) {
		return convertToWekaInstances(dataSetName, Arrays.asList(dataPoint)).get(0);
	}

	private Instances convertToWekaInstances(final String dataSetName, final List<DataPoint> dataPoints) {

		Attribute[] attributes = new Attribute[trainingDataCollector.sparseIndexMapping.size()];

		for (Entry<String, Integer> attribute : trainingDataCollector.sparseIndexMapping.entrySet()) {
			attributes[attribute.getValue()] = new Attribute(attribute.getKey());
		}

		ArrayList<Attribute> attributeList = new ArrayList<>();
		attributeList.addAll(Arrays.asList(attributes));
		Instances instances = new Instances(dataSetName, attributeList, dataPoints.size());

		attributeList.add(classAttribute);

		instances.setClassIndex(attributeList.size() - 1);

		for (DataPoint fdp : dataPoints) {
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
