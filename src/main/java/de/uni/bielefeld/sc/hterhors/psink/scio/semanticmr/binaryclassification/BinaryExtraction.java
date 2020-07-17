package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.binaryclassification;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.uni.bielefeld.sc.hterhors.psink.scio.playground.preprocessing.DataPointCollector;
import de.uni.bielefeld.sc.hterhors.psink.scio.playground.preprocessing.DataPointCollector.DataPoint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DistanceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DurationNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ForceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.LengthNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.PressureNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ThicknessNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.VolumeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.WeightNormalization;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SparseInstance;

/**
 * Class to predict the organism model in a binary classification style.
 * 
 * 
 * Create pairs of entities that are mentioned in the text and
 * 
 * @author hterhors
 *
 */
public abstract class BinaryExtraction {

	private final File instanceDirectory;

	private static final List<String> BINARY_VALUES = Arrays.asList("0", "1");
	private static final String CLASSIFICATION_LABEL_RELEVANT = "Y";
	private static final String CLASSIFICATION_LABEL_NOT_RELEVANT = "N";
	private static final Attribute classAttribute = new Attribute("classLabel",
			Arrays.asList(CLASSIFICATION_LABEL_NOT_RELEVANT, CLASSIFICATION_LABEL_RELEVANT));

	protected abstract File getExternalNerlaFile();

	protected abstract File getInstanceDirectory();

	protected abstract EntityTemplate convertToEntityTemplate(DLAPredictions dlaPredictions);

	private final DataPointCollector trainingData = new DataPointCollector();

	public BinaryExtraction(String context) throws Exception {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader(context)).apply()
				
				
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new DurationNormalization())
				.registerNormalizationFunction(new VolumeNormalization())
				.registerNormalizationFunction(new ForceNormalization())
				.registerNormalizationFunction(new ThicknessNormalization())
				.registerNormalizationFunction(new PressureNormalization())
				.registerNormalizationFunction(new LengthNormalization())
				.registerNormalizationFunction(new DistanceNormalization())
//				.registerNormalizationFunction(new DosageNormalization())
				
				
				.build();

		this.instanceDirectory = getInstanceDirectory();

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(1000L)
				.setTrainingProportion(80).setDevelopmentProportion(20).setCorpusSizeFraction(1F).build();

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);
		JSONNerlaReader nerlaJSONReader = new JSONNerlaReader(getExternalNerlaFile());

		System.out.println("Collect training data points... ");
		Set<BinaryDataPoint> trainingDataPoints = toBinaryDataPoints(nerlaJSONReader,
				instanceProvider.getTrainingInstances(), true);
		System.out.println("Number of binary training data points: " + trainingDataPoints.size());
		System.out.println("Convert to weka...");
		List<DataPoint> wekaTrainingDataPoints = toWekaDataPoints(trainingDataPoints, true);
		wekaTrainingDataPoints.forEach(fdp -> trainingData.addFeatureDataPoint(fdp));
		Instances wekaTRAINInstance = convertToWekaInstances("TRAIN", trainingData.getDataPoints());

		System.out.println("Train classifier...");
		RandomForest rf = new RandomForest();
		rf.setNumIterations(200);
		rf.buildClassifier(wekaTRAINInstance);

		Score binaryScore = new Score();

		List<BinaryDataPointPrediction> predictions = predict(instanceProvider, nerlaJSONReader, rf, binaryScore);

		Map<Instance, List<DLAPredictions>> sortedPredictions = sortPredictions(predictions);
		
		CartesianEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE);
		
		
		
		Score macroScore = new Score(EScoreType.MACRO);
		for (Entry<Instance, List<DLAPredictions>> binaryDataPointPrediction : sortedPredictions.entrySet()) {
			System.out.println(binaryDataPointPrediction.getKey().getName());
			System.out.println("----GOLD----");
			List<EntityTemplate> goldETs = binaryDataPointPrediction.getKey().getGoldAnnotations().getAnnotations();

			for (EntityTemplate goldET : goldETs) {
				System.out.println(goldET.toPrettyString());
			}

			Collections.sort(binaryDataPointPrediction.getValue());
			System.out.println(binaryDataPointPrediction.getValue().size());
			List<EntityTemplate> predictedETs = binaryDataPointPrediction.getValue().stream().limit(1)
					.map(bdpp -> convertToEntityTemplate(bdpp)).collect(Collectors.toList());
			System.out.println("----PREDICTED----");
			for (EntityTemplate predictedET : predictedETs) {
				System.out.println(predictedET.toPrettyString());
			}
			Score evalScore = evaluator.scoreMultiValues(goldETs, predictedETs, EScoreType.MICRO);
			System.out.println(evalScore);
			macroScore.add(evalScore.toMacro());
		}

		System.out.println("macroScore = " + macroScore);
		System.out.println("binaryScore = " + binaryScore);

	}

	private List<BinaryDataPointPrediction> predict(InstanceProvider instanceProvider, JSONNerlaReader nerlaJSONReader,
			RandomForest rf, Score binaryScore) {

		List<BinaryDataPointPrediction> predictions = new ArrayList<>();

		for (Instance instance : instanceProvider.getDevelopmentInstances()) {
			System.out.println(instance.getName());
			System.out.println("Collect test data points... ");
			Set<BinaryDataPoint> testDataPoints = toBinaryDataPoints(nerlaJSONReader, Arrays.asList(instance), false);
			System.out.println("Number of binary test data points: " + testDataPoints.size());
			System.out.println("Convert to weka...");
			List<DataPoint> wekaTestDataPoints = toWekaDataPoints(testDataPoints, false);

			System.out.println("Predict...");
			int progress = 0;

			for (DataPoint fdp : wekaTestDataPoints) {
				progress++;

				if (progress % 10000 == 0)
					System.out.println("progress = " + progress + "/" + testDataPoints.size() + "\t" + binaryScore);
				weka.core.Instance wekaInstance = convertToWekaInstances("TEST", fdp);

				BinaryDataPointPrediction e = testForInstance(binaryScore, fdp, rf, wekaInstance);
				if (e.predictedLabel.equals(ELabel.CORRECT)) {
					System.out.println(e);
					predictions.add(e);
				}

			}

		}

		return predictions;
	}

	private Map<Instance, List<DLAPredictions>> sortPredictions(List<BinaryDataPointPrediction> predictions) {
		Map<Instance, List<DLAPredictions>> sortedPredictions = new HashMap<>();

		for (BinaryDataPointPrediction binaryDataPointPrediction : predictions) {

			sortedPredictions.putIfAbsent(binaryDataPointPrediction.binaryDataPoint.instance, new ArrayList<>());

			List<DLAPredictions> collectons = sortedPredictions.get(binaryDataPointPrediction.binaryDataPoint.instance);

			DocumentLinkedAnnotation dla1 = binaryDataPointPrediction.binaryDataPoint.annotation1;
			DocumentLinkedAnnotation dla2 = binaryDataPointPrediction.binaryDataPoint.annotation2;

			boolean createNewCollection = true;
			for (DLAPredictions dlap : collectons) {
				if (dlap.collection.contains(dla1) || dlap.collection.contains(dla2)) {
					createNewCollection = false;
					dlap.collection.add(dla1);
					dlap.collection.add(dla2);
					dlap.probabilities.add(binaryDataPointPrediction.confidence);
					break;
				}
			}
			if (createNewCollection) {
				List<Double> probabilities = new ArrayList<>();
				Set<DocumentLinkedAnnotation> collection = new HashSet<>();
				collection.add(dla1);
				collection.add(dla2);
				probabilities.add(binaryDataPointPrediction.confidence);
				collectons.add(new DLAPredictions(collection, probabilities));
			}

		}
		return sortedPredictions;
	}

	private BinaryDataPointPrediction testForInstance(Score score, DataPoint featureDataPoint, RandomForest rf,
			weka.core.Instance instance) {

		try {
			double[] probs;
			probs = rf.distributionForInstance(instance);

			double pred = probs[1] > 0.5 ? 1 : 0;

			String groundTruth = classAttribute.value((int) instance.classValue());

			String prediction = classAttribute.value((int) pred);

			boolean relevant = prediction.equals(CLASSIFICATION_LABEL_RELEVANT);

			int tp = groundTruth.equals(CLASSIFICATION_LABEL_RELEVANT) && groundTruth.equals(prediction) ? 1 : 0;
			int tn = groundTruth.equals(CLASSIFICATION_LABEL_NOT_RELEVANT) && groundTruth.equals(prediction) ? 1 : 0;
			int fp = groundTruth.equals(CLASSIFICATION_LABEL_NOT_RELEVANT)
					&& prediction.equals(CLASSIFICATION_LABEL_RELEVANT) ? 1 : 0;
			int fn = groundTruth.equals(CLASSIFICATION_LABEL_RELEVANT)
					&& prediction.equals(CLASSIFICATION_LABEL_NOT_RELEVANT) ? 1 : 0;

			Score s = new Score(tp, fp, fn, tn);
			score.add(s);

			return new BinaryDataPointPrediction((BinaryDataPoint) featureDataPoint.parameter.get("binaryDP"),
					relevant ? ELabel.CORRECT : ELabel.NOT_CORRECT, probs[1]);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private Set<BinaryDataPoint> toBinaryDataPoints(JSONNerlaReader nerlaJSONReader, List<Instance> instances,
			boolean trainingData) {

		Set<BinaryDataPoint> dataPoints = new HashSet<>();
		int totalCorrect = 0;
		int totalNotCorrect = 0;
		for (Instance instance : instances) {

			List<DocumentLinkedAnnotation> nerla = nerlaJSONReader.getForInstance(instance);

			int correct = 0;

			if (trainingData)
				for (EntityTemplate et : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {

					List<DocumentLinkedAnnotation> goldAnnotations = getDLAnnotations(et);
					nerla.removeAll(goldAnnotations);
					for (int i = 0; i < goldAnnotations.size(); i++) {
						for (int j = i + 1; j < goldAnnotations.size(); j++) {
							correct++;
							dataPoints.add(new BinaryDataPoint(instance, goldAnnotations.get(i), goldAnnotations.get(j),
									ELabel.CORRECT));

						}
					}
				}

			totalCorrect += correct;
			int notCorrect = 0;

			if (correct > 0 || !trainingData)
				out: for (int i = 0; i < nerla.size(); i++) {
					for (int j = i + 1; j < nerla.size(); j++) {

						if (!trainingData
								&& Math.abs(nerla.get(i).getSentenceIndex() - nerla.get(j).getSentenceIndex()) > 5)
							continue;

						dataPoints.add(new BinaryDataPoint(instance, nerla.get(i), nerla.get(j), ELabel.NOT_CORRECT));
						notCorrect++;
						if (trainingData && notCorrect >= correct)
							break out;

					}
				}
			totalNotCorrect += notCorrect;
		}

		if (trainingData) {
			System.out.println("Number of data points with label " + ELabel.CORRECT + ": " + totalCorrect);
			System.out.println("Number of data points with label " + ELabel.NOT_CORRECT + ": " + totalNotCorrect);
		}

		return dataPoints;
	}

	protected abstract List<DocumentLinkedAnnotation> getDLAnnotations(EntityTemplate organismModelET);

	private List<DataPoint> toWekaDataPoints(Set<BinaryDataPoint> binaryDataPoint, boolean training) {

		final List<DataPoint> dataPoints = new ArrayList<>();

		for (BinaryDataPoint dataPoint : binaryDataPoint) {

			Map<String, Double> features = getFeatures(dataPoint);
			Map<String, Object> parameter = new HashMap<>();

			parameter.put("binaryDP", dataPoint);

			dataPoints.add(new DataPoint(parameter, trainingData, features,
					dataPoint.goldLabel == ELabel.CORRECT ? 1 : 0, training));

		}

		return dataPoints;
	}

	protected abstract Map<String, Double> getFeatures(BinaryDataPoint dataPoint);

	private weka.core.Instance convertToWekaInstances(final String dataSetName, final DataPoint dataPoint) {
		return convertToWekaInstances(dataSetName, Arrays.asList(dataPoint)).get(0);
	}

	private Instances convertToWekaInstances(final String dataSetName, final List<DataPoint> dataPoints) {

		Attribute[] attributes = new Attribute[trainingData.sparseIndexMapping.size()];

		for (Entry<String, Integer> attribute : trainingData.sparseIndexMapping.entrySet()) {
			attributes[attribute.getValue()] = new Attribute(attribute.getKey(), BINARY_VALUES);
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
//		System.out.println("Context: " + dataSetName);
//		System.out.println("Number of attributes: " + attributes.length);
//		System.out.println("Number of Instances: " + instances.size());
		return instances;
	}

}
