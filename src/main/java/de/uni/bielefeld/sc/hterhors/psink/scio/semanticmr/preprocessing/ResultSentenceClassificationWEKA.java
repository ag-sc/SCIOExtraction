package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.DataPointCollector.DataPoint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.sentenceclassification.Classification;
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
public class ResultSentenceClassificationWEKA {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private static final File instanceDirectory = new File("src/main/resources/slotfilling/result/corpus/instances/");

	private static final String CLASSIFICATION_LABEL_RELEVANT = "Y";

	private static final String CLASSIFICATION_LABEL_NOT_RELEVANT = "N";

	private static final String CLASSIFICATION_LABEL_UNKOWN = "?";

	public static void main(String[] args) throws Exception {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadDataStructureReader("Result")).build();
		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(100L)
				.setTrainingProportion(80).setTestProportion(20).setCorpusSizeFraction(1F).build();

		InstanceProvider.maxNumberOfAnnotations = 1000;

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		ResultSentenceClassificationWEKA sentenceClassification = new ResultSentenceClassificationWEKA(
				instanceProvider.getRedistributedTrainingInstances());

		Score score = new Score();
		for (Instance testInstance : instanceProvider.getRedistributedTestInstances()) {
			System.out.println(testInstance.getName());
			Map<Integer, Classification> x = sentenceClassification.test(testInstance, score);

			for (int sentenceIndex = 0; sentenceIndex < testInstance.getDocument()
					.getNumberOfSentences(); sentenceIndex++) {
				Classification classification = x.get(sentenceIndex);
				System.out.println(classification.sentenceIndex + "\t" + classification.isRelevant + "\t"
						+ classification.probability);
			}

		}

		System.out.println(score);
	}

	static class P {
		final String docID;
		final public Map<String, Double> features;
		final public String sentence;
		final public int sentenceIndex;

		public P(String docID, int sentenceIndex, Map<String, Double> features, String sentence) {
			this.features = features;
			this.sentence = sentence;
			this.docID = docID;
			this.sentenceIndex = sentenceIndex;
		}

	}

	private final List<String> binaryValues = Arrays.asList("0", "1");

	private final Attribute classAttribute = new Attribute("classLabel", Arrays
			.asList(CLASSIFICATION_LABEL_NOT_RELEVANT, CLASSIFICATION_LABEL_RELEVANT, CLASSIFICATION_LABEL_UNKOWN));

	private final DataPointCollector trainingData = new DataPointCollector();

	private final double threshold;

	private final RandomForest rf;

	public ResultSentenceClassificationWEKA(List<Instance> trainingInstances) throws IOException {

		threshold = 0.3;
		rf = new RandomForest();
		log.info("Train sentence classifier...");

		List<DataPoint> trainingDataPoints = convertInstancesToDataPoints(trainingInstances, true);

		trainingDataPoints.forEach(fdp -> trainingData.addFeatureDataPoint(fdp));

		Instances wekaTRAINInstance = convertToWekaInstances("TRAIN", trainingData.getDataPoints());
		saveArff(new File("result_train.arff"), wekaTRAINInstance);

		rf.setNumIterations(200);

		try {
			rf.buildClassifier(wekaTRAINInstance);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("done!");

	}

	public Map<Integer, Classification> test(Instance testInstance, Score score) {
		Map<Integer, Classification> classifications = new HashMap<>();

		List<DataPoint> fdps = convertInstanceToDataPoints(testInstance, false);

		for (DataPoint fdp : fdps) {

			weka.core.Instance instance = convertToWekaInstances("TEST", fdp);

			classifications.put((Integer) fdp.parameter.get("sentenceIndex"),
					testForInstance(fdp, rf, score, instance));
		}

		return classifications;
	}

	public Map<Integer, Classification> classifyDocument(Document document) {
		log.info("Classify document: " + document.documentID);
		Map<Integer, Classification> classifications = new HashMap<>();

		final List<DataPoint> fdps = new ArrayList<>();

		for (int i = 0; i < document.getNumberOfSentences(); i++) {

			String sentence = document.getContentOfSentence(i);

			Map<String, Double> features = getFeaturesForSentence(document, i);
			Map<String, Object> parameter = new HashMap<>();
			parameter.put("docID", document.documentID);
			parameter.put("sentenceIndex", i);
			parameter.put("sentence", sentence);
			fdps.add(new DataPoint(parameter, trainingData, features, 0, true));
		}

		for (DataPoint fdp : fdps) {

			weka.core.Instance instance = convertToWekaInstances("TEST", fdp);

			classifications.put((Integer) fdp.parameter.get("sentenceIndex"), classifyForInstance(fdp, rf, instance));
		}

		return classifications;
	}

	private Classification testForInstance(DataPoint featureDataPoint, RandomForest rf, Score score,
			weka.core.Instance instance) {

		try {
			double[] probs;
			probs = rf.distributionForInstance(instance);

			double pred = probs[1] > threshold ? 1 : 0;

			String groundTruth = classAttribute.value((int) instance.classValue());

			String prediction = classAttribute.value((int) pred);

			boolean relevant = prediction.equals(CLASSIFICATION_LABEL_RELEVANT);

			Classification classification = new Classification(
					(Integer) featureDataPoint.parameter.get("sentenceIndex"), relevant, probs[1]);

			int tp = groundTruth.equals(CLASSIFICATION_LABEL_RELEVANT) && groundTruth.equals(prediction) ? 1 : 0;
			int tn = groundTruth.equals(CLASSIFICATION_LABEL_NOT_RELEVANT) && groundTruth.equals(prediction) ? 1 : 0;
			int fp = groundTruth.equals(CLASSIFICATION_LABEL_NOT_RELEVANT)
					&& prediction.equals(CLASSIFICATION_LABEL_RELEVANT) ? 1 : 0;
			int fn = groundTruth.equals(CLASSIFICATION_LABEL_RELEVANT)
					&& prediction.equals(CLASSIFICATION_LABEL_NOT_RELEVANT) ? 1 : 0;

			score.add(new Score(tp, fp, fn, tn));

			if (prediction.equals(CLASSIFICATION_LABEL_NOT_RELEVANT)
					&& groundTruth.equals(CLASSIFICATION_LABEL_NOT_RELEVANT))
				return classification;

//		if (groundTruth.equals(CLASSIFICATION_LABEL_RELEVANT) && prediction.equals(CLASSIFICATION_LABEL_NOT_RELEVANT)) {
//			System.out.println("########### FASLE NEG #############");
//		}
//
//		System.out.println(Arrays.toString(probs));
//		System.out.println(featureDataPoint.docID);
//		System.out.println("Sentence: " + featureDataPoint.sentenceIndex);
//		System.out.println(featureDataPoint.sentence);
//		System.out.println("actual: " + groundTruth + ", predicted: " + prediction);
//		System.out.println();

			return classification;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private Classification classifyForInstance(DataPoint featureDataPoint, RandomForest rf,
			weka.core.Instance instance) {

		try {
			double[] probs;
			probs = rf.distributionForInstance(instance);

			double pred = probs[1] > threshold ? 1 : 0;

			Classification classification = new Classification(
					(Integer) featureDataPoint.parameter.get("sentenceIndex"),
					classAttribute.value((int) pred).equals(CLASSIFICATION_LABEL_RELEVANT), probs[1]);

			return classification;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private Map<String, Double> getFeaturesForSentence(Document doc, Integer sentenceIndex) {

		Map<String, Double> features = new HashMap<>();

		int numOfSentence = doc.getNumberOfSentences();

		final int quarter = (int) ((double) numOfSentence / 4) + 1;

		for (int i = 0; i < 4; i++) {
			features.put(i + "_Q", (sentenceIndex > quarter * i && sentenceIndex < quarter * (i + 1) ? 1D : 0D));
		}

		for (DocumentToken documentToken : doc.getSentenceByIndex(sentenceIndex)) {
			if (documentToken.getText().matches("[\\w\\w\\.\\s-_/]+"))
				features.put(documentToken.getText(), 1D);
		}
		return features;
	}

	private List<DataPoint> convertInstanceToDataPoints(Instance instance, boolean training) {
		return convertInstancesToDataPoints(Arrays.asList(instance), training);
	}

	private List<DataPoint> convertInstancesToDataPoints(List<Instance> instances, boolean training) {

		final List<DataPoint> dataPoints = new ArrayList<>();

		for (Instance instance : instances) {
			Set<Integer> sentencesWithInfo = new HashSet<>();

			for (AbstractAnnotation goldAnnotation : instance.getGoldAnnotations().getAnnotations()) {

				SingleFillerSlot judgement = goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SlotType.get("hasJudgement"));
				if (judgement.containsSlotFiller())
					sentencesWithInfo.addAll(extractRelevantSentences(judgement.getSlotFiller()));
				SingleFillerSlot test = goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SlotType.get("hasStatisticalTest"));
				if (test.containsSlotFiller())
					sentencesWithInfo.addAll(extractRelevantSentences(test.getSlotFiller()));
				SingleFillerSlot invest = goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SlotType.get("hasInvestigationMethod"));
				if (invest.containsSlotFiller())
					sentencesWithInfo.addAll(extractRelevantSentences(invest.getSlotFiller()));
				SingleFillerSlot trend = goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SlotType.get("hasTrend"));
				if (trend.containsSlotFiller())
					sentencesWithInfo.addAll(extractRelevantSentences(trend.getSlotFiller()));
			}

			int count = 0;
			List<P> negExamples = new ArrayList<>();
			for (int i = 0; i < instance.getDocument().getNumberOfSentences(); i++) {

				String sentence = instance.getDocument().getContentOfSentence(i);

				Map<String, Double> features = getFeaturesForSentence(instance.getDocument(), i);
				{
					Map<String, Object> parameter = new HashMap<>();

					parameter.put("docID", instance.getDocument().documentID);
					parameter.put("sentenceIndex", i);
					parameter.put("sentence", sentence);

					if (sentencesWithInfo.contains(i)) {
						count++;
						dataPoints.add(new DataPoint(parameter, trainingData, features, 1, training));
					} else {
						if (training)
							negExamples.add(new P(instance.getDocument().documentID, i, features, sentence));
						else
							dataPoints.add(new DataPoint(parameter, trainingData, features, 0, training));
					}
				}
			}

			if (training) {
				for (int i = 0; i < count * 3; i++) {
					Map<String, Object> parameter = new HashMap<>();
					parameter.put("docID", negExamples.get(i).docID);
					parameter.put("sentenceIndex", negExamples.get(i).sentenceIndex);
					parameter.put("sentence", negExamples.get(i).sentence);
					Collections.shuffle(negExamples);
					dataPoints.add(new DataPoint(parameter, trainingData, negExamples.get(i).features, 0, training));
				}
			}

		}

		return dataPoints;
	}

	private Set<Integer> extractRelevantSentences(AbstractAnnotation goldAnnotation) {
		Set<Integer> sentencesWithInfo = new HashSet<>();

		if (goldAnnotation.isInstanceOfLiteralAnnotation()) {
			int sentenceIndex = goldAnnotation.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex();
			sentencesWithInfo.add(sentenceIndex);
			return sentencesWithInfo;
		}

		if (goldAnnotation.isInstanceOfEntityTypeAnnotation()) {
			return sentencesWithInfo;
		}

		AbstractAnnotation rootAnnotation = goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation();

		if (rootAnnotation.isInstanceOfDocumentLinkedAnnotation()) {

			DocumentLinkedAnnotation linkedAnn = rootAnnotation.asInstanceOfDocumentLinkedAnnotation();

			int sentenceIndex = linkedAnn.getSentenceIndex();
			sentencesWithInfo.add(sentenceIndex);
		}

		goldAnnotation.asInstanceOfEntityTemplate().filter().docLinkedAnnoation().multiSlots().singleSlots().merge()
				.nonEmpty().build().getMergedAnnotations().values().stream().flatMap(a -> a.stream()).forEach(a -> {
					if (a.isInstanceOfDocumentLinkedAnnotation()) {

						DocumentLinkedAnnotation linkedAnn = a.asInstanceOfDocumentLinkedAnnotation();

						int sentenceIndex = linkedAnn.getSentenceIndex();
						sentencesWithInfo.add(sentenceIndex);
					}

				});

		goldAnnotation.asInstanceOfEntityTemplate().filter().entityTemplateAnnoation().multiSlots().singleSlots()
				.merge().nonEmpty().build().getMergedAnnotations().values().stream().flatMap(a -> a.stream())
				.forEach(a -> sentencesWithInfo.addAll(extractRelevantSentences(a.asInstanceOfEntityTemplate())));

		return sentencesWithInfo;
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
