package de.uni.bielefeld.sc.hterhors.psink.scio.playground.preprocessing.cardinalityprediction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.playground.preprocessing.DataPointCollector;
import de.uni.bielefeld.sc.hterhors.psink.scio.playground.preprocessing.DataPointCollector.DataPoint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
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
public class CardinalityPredictionWEKA {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

//	private static final File instanceDirectory = new File(
//			"src/main/resources/slotfilling/experimental_group/corpus/instances/");
//	private static final File instanceDirectory = new File(
//			"src/main/resources/slotfilling/organism_model/corpus/instances/");
	private static File instanceDirectory;
//	private static  final File instanceDirectory = new File("src/main/resources/slotfilling/injury/corpus/instances/");

	public static void main(String[] args) throws Exception {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("ExperimentalGroup")).build();

		instanceDirectory = SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.treatment);

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(100L)
				.setTrainingProportion(80).setTestProportion(20).setCorpusSizeFraction(1F).build();

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		CardinalityPredictionWEKA sentenceClassification = new CardinalityPredictionWEKA(
				instanceProvider.getTrainingInstances()
//				, instanceProvider.getRedistributedTestInstances()
		);

		double mse = 0;
		for (Instance testInstance : instanceProvider.getTestInstances()) {
			System.out.println(testInstance.getName());
			double x = sentenceClassification.test(testInstance);
			mse += Math.pow(x - testInstance.getGoldAnnotations().getAnnotations().size(), 2);
			System.out.println(x + "\t" + testInstance.getGoldAnnotations().getAnnotations().size());

		}
		System.out.println("MSE:" + Math.sqrt(mse));

	}

	private final List<String> binaryValues = Arrays.asList("0", "1");

	private final Attribute classAttribute = new Attribute("classLabel");

	private final DataPointCollector trainingData = new DataPointCollector();

	private final Classifier rf;

	public CardinalityPredictionWEKA(List<Instance> trainingInstances) {

		rf = new RandomForest();
		log.info("Train sentence classifier...");

		List<DataPoint> trainingDataPoints = new ArrayList<>();
		for (Instance instance : trainingInstances) {
			trainingDataPoints.add(convertInstanceToDataPoint(instance, true));
		}

		trainingDataPoints.forEach(fdp -> trainingData.addFeatureDataPoint(fdp));

		Instances wekaTRAINInstance = convertToWekaInstances("TRAIN", trainingData.getDataPoints());

		try {
			rf.buildClassifier(wekaTRAINInstance);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("done!");

	}

	public CardinalityPredictionWEKA(List<Instance> redistributedTrainingInstances,
			List<Instance> redistributedTestInstances) throws IOException {
		rf = null;
		final DataPointCollector trainingData = new DataPointCollector();
		final DataPointCollector testData = new DataPointCollector();

		List<DataPoint> trainingDataPoints = new ArrayList<>();
		for (Instance instance : redistributedTrainingInstances) {
			trainingDataPoints.add(convertInstanceToDataPoint(instance, true));
		}

		trainingDataPoints.forEach(fdp -> trainingData.addFeatureDataPoint(fdp));
		Instances wekaTRAINInstance = convertToWekaInstances("TRAIN", trainingData.getDataPoints());

		saveArff(new File("treatment_train.arff"), wekaTRAINInstance);

		List<DataPoint> testDataPoints = new ArrayList<>();
		for (Instance instance : redistributedTestInstances) {
			testDataPoints.add(convertInstanceToDataPoint(instance, false));
		}

		testDataPoints.forEach(fdp -> testData.addFeatureDataPoint(fdp));
		Instances wekaTESTInstance = convertToWekaInstances("TEST", testData.getDataPoints());

		saveArff(new File("treatment_test.arff"), wekaTESTInstance);
	}

	public double test(Instance testInstance) {

		return classifyForInstance(rf, convertToWekaInstances("TEST", convertInstanceToDataPoint(testInstance, false)));

	}

	public double classifyDocument(Document document) {
		log.info("Classify document: " + document.documentID);
		Map<String, Object> parameter = new HashMap<>();

		parameter.put("docID", document.documentID);

		DataPoint fdp = new DataPoint(parameter, trainingData, getFeatures(document), 0, false);

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

	private Map<String, Double> getFeatures(Document doc) {

		Map<String, Double> features = new HashMap<>();

		for (DocumentToken documentToken : doc.tokenList) {
			if (documentToken.getText().matches("[\\w\\w\\.\\s-_/]+"))
				features.put(documentToken.getText(), 1D);
		}
		return features;
	}

	private DataPoint convertInstanceToDataPoint(Instance instance, boolean training) {

		Map<String, Double> features = getFeatures(instance.getDocument());
		Map<String, Object> parameter = new HashMap<>();

		parameter.put("docID", instance.getDocument().documentID);

		return new DataPoint(parameter, trainingData, features, instance.getGoldAnnotations().getAnnotations().size(),
				training);

	}

	private weka.core.Instance convertToWekaInstances(final String dataSetName, final DataPoint dataPoint) {
		return convertToWekaInstances(dataSetName, Arrays.asList(dataPoint)).get(0);
	}

	private Instances convertToWekaInstances(final String dataSetName, final List<DataPoint> dataPoints) {

		Attribute[] attributes = new Attribute[trainingData.sparseIndexMapping.size()];

		for (Entry<String, Integer> attribute : trainingData.sparseIndexMapping.entrySet()) {
			attributes[attribute.getValue()] = new Attribute(attribute.getKey(), binaryValues);
		}

		ArrayList<Attribute> attributeList = new ArrayList<>();
		attributeList.addAll(Arrays.asList(attributes));
		attributeList.add(classAttribute);

		Instances instances = new Instances(dataSetName, attributeList, trainingData.getDataPoints().size());
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
