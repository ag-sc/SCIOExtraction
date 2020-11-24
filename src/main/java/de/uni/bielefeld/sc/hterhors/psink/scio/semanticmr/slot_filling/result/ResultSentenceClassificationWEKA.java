package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result;

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
import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation.ESection;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.playground.preprocessing.DataPointCollector;
import de.uni.bielefeld.sc.hterhors.psink.scio.playground.preprocessing.DataPointCollector.DataPoint;
import de.uni.bielefeld.sc.hterhors.psink.scio.playground.preprocessing.sentenceclassification.Classification;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
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

	private static File instanceDirectory;

	private static final String CLASSIFICATION_LABEL_RELEVANT = "Y";

	private static final String CLASSIFICATION_LABEL_NOT_RELEVANT = "N";

	public static void main(String[] args) throws Exception {
		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).build();

		instanceDirectory = SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.result);

//		CartesianEvaluator.MAXIMUM_PERMUTATION_SIZE = 200;

		SlotFillingExplorer.MAX_NUMBER_OF_ANNOTATIONS = 200;

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
				.setTestProportion(20).setSeed(1000L).build();

		InstanceProvider.maxNumberOfAnnotations = 80;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		ResultSentenceClassificationWEKA sentenceClassification = new ResultSentenceClassificationWEKA(
				instanceProvider.getTrainingInstances());

		Score score = new Score();
		for (Instance testInstance : instanceProvider.getTestInstances()) {
			System.out.println(testInstance.getName());
			AutomatedSectionifcation sectionification = AutomatedSectionifcation.getInstance(testInstance);
			Map<Integer, Classification> x = sentenceClassification.test(testInstance, score);

			for (int sentenceIndex = 0; sentenceIndex < testInstance.getDocument()
					.getNumberOfSentences(); sentenceIndex++) {

				if (sectionification.getSection(sentenceIndex) != ESection.RESULTS)
					continue;

				Classification classification = x.get(sentenceIndex);
				System.out.println(classification.sentenceIndex + "\t" + classification.isRelevant + "\t"
						+ classification.probability + "\t"
						+ (classification.s.getTp() == 1 || classification.s.getTn() == 1 ? "TRUE" : "FALSE"));
			}

		}

		System.out.println(score);
		System.out.println(score.getAccuracy());
		System.out.println(score.microToString());
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

	private final Attribute classAttribute = new Attribute("classLabel",
			Arrays.asList(CLASSIFICATION_LABEL_NOT_RELEVANT, CLASSIFICATION_LABEL_RELEVANT));

	private final DataPointCollector trainingData = new DataPointCollector();

	private final double threshold;

	private final RandomForest rf;

	public ResultSentenceClassificationWEKA(List<Instance> trainingInstances) {

		threshold = 0.5;
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

//	public Map<Integer, Classification> classifyDocument(AutomatedSectionifcation sec, Instance inst,
//			Document document) {
//		log.info("Classify document: " + document.documentID);
//		Map<Integer, Classification> classifications = new HashMap<>();
//
//		final List<DataPoint> fdps = new ArrayList<>();
//
//		File groupNamesCacheDir = new File("data/annotations/result/");
//
//		JSONNerlaReader nerlaJSONReader = new JSONNerlaReader(groupNamesCacheDir);
//
//		Map<Integer, List<EntityType>> entitiesPerSentence = new HashMap<>();
//
//		for (DocumentLinkedAnnotation eta : new HashSet<>(nerlaJSONReader.getForInstance(inst))) {
//
//			entitiesPerSentence.putIfAbsent(eta.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex(),
//					new ArrayList<>());
//			entitiesPerSentence.get(eta.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex())
//					.add(eta.getEntityType());
//
//		}
//
//		for (
//
//				int i = 0; i < document.getNumberOfSentences(); i++) {
//
//			String sentence = document.getContentOfSentence(i);
//
//			Map<String, Double> features = getFeaturesForSentence(entitiesPerSentence, inst, i);
//			Map<String, Object> parameter = new HashMap<>();
//			parameter.put("docID", document.documentID);
//			parameter.put("sentenceIndex", i);
//			parameter.put("sentence", sentence);
//			fdps.add(new DataPoint(parameter, trainingData, features, 0, true));
//		}
//
//		for (DataPoint fdp : fdps) {
//
//			weka.core.Instance instance = convertToWekaInstances("TEST", fdp);
//
//			classifications.put((Integer) fdp.parameter.get("sentenceIndex"), classifyForInstance(fdp, rf, instance));
//		}
//
//		return classifications;
//	}

	private Classification testForInstance(DataPoint featureDataPoint, RandomForest rf, Score score,
			weka.core.Instance instance) {

		try {
			double[] probs;
			probs = rf.distributionForInstance(instance);

			double pred = probs[1] > threshold ? 1 : 0;

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

			Classification classification = new Classification(
					(Integer) featureDataPoint.parameter.get("sentenceIndex"), relevant, probs[1], s);
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

//	private Classification classifyForInstance(DataPoint featureDataPoint, RandomForest rf,
//			weka.core.Instance instance) {
//
//		try {
//			double[] probs;
//			probs = rf.distributionForInstance(instance);
//
//			double pred = probs[1] > threshold ? 1 : 0;
//
//			Classification classification = new Classification(
//					(Integer) featureDataPoint.parameter.get("sentenceIndex"),
//					classAttribute.value((int) pred).equals(CLASSIFICATION_LABEL_RELEVANT), probs[1], null);
//
//			return classification;
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return null;
//	}

	/*
	 * Only Annotations:
	 */
//	Score [getAccuracy()=0.955, getF1()=0.844, getPrecision()=0.738, getRecall()=0.986, tp=141, fp=50, fn=2, tn=969]

	/*
	 * Only Words:
	 */
//	Score [getAccuracy()=0.862, getF1()=0.602, getPrecision()=0.467, getRecall()=0.846, tp=121, fp=138, fn=22, tn=881]

	/*
	 * Words and Annotations:
	 */
//	Score [getAccuracy()=0.949, getF1()=0.822, getPrecision()=0.723, getRecall()=0.951, tp=136, fp=52, fn=7, tn=967]

	private Map<String, Double> getFeaturesForSentence(Map<Integer, List<EntityType>> entitiesPerSentence,
			Instance instance, Integer sentenceIndex) {

		Map<String, Double> features = new HashMap<>();

		if (entitiesPerSentence.containsKey(sentenceIndex))
			for (EntityType et : entitiesPerSentence.get(sentenceIndex)) {
				features.put(et.name, 1d);
			}

//		for (DocumentToken documentToken : instance.getDocument().getSentenceByIndex(sentenceIndex)) {
//			if (documentToken.getText().matches("[\\w\\w\\.\\s-_/]+"))
//				features.put(documentToken.getText(), 1D);
//		}
		return features;
	}
	/*
	 * nur anno auf allen sätzen
	 */
//	Score [getAccuracy()=0.966, getF1()=0.867, getPrecision()=0.828, getRecall()=0.909, tp=130, fp=27, fn=13, tn=992]

	/*
	 * beides auf allen sätzen
	 */
//	Score [getAccuracy()=0.929, getF1()=0.602, getPrecision()=0.984, getRecall()=0.434, tp=62, fp=1, fn=81, tn=1018]

	/*
	 * nur words
	 */
//	Score [getAccuracy()=0.878, getF1()=0.014, getPrecision()=1.000, getRecall()=0.007, tp=1, fp=0, fn=142, tn=1019]

	private List<DataPoint> convertInstanceToDataPoints(Instance instance, boolean training) {
		return convertInstancesToDataPoints(Arrays.asList(instance), training);
	}

	private List<DataPoint> convertInstancesToDataPoints(List<Instance> instances, boolean training) {

		final List<DataPoint> dataPoints = new ArrayList<>();

		for (Instance instance : instances) {

			AutomatedSectionifcation sectionification = AutomatedSectionifcation.getInstance(instance);
			
			File groupNamesCacheDir = new File("data/annotations/result/");

			JSONNerlaReader nerlaJSONReader = new JSONNerlaReader(groupNamesCacheDir);

			Map<Integer, List<EntityType>> entitiesPerSentence = new HashMap<>();

			for (DocumentLinkedAnnotation eta : new HashSet<>(nerlaJSONReader.getForInstance(instance))) {

				entitiesPerSentence.putIfAbsent(eta.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex(),
						new ArrayList<>());
				entitiesPerSentence.get(eta.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex())
						.add(eta.getEntityType());

			}
			Set<Integer> sentencesWithInfo = new HashSet<>();
			for (AbstractAnnotation goldAnnotation : instance.getGoldAnnotations().getAnnotations()) {
				sentencesWithInfo.addAll(extractRelevantSentences(goldAnnotation));
			}

			int count = 0;
			List<P> negExamples = new ArrayList<>();
			for (int i = 0; i < instance.getDocument().getNumberOfSentences(); i++) {

				String sentence = instance.getDocument().getContentOfSentence(i);

				if (sectionification.getSection(i) != ESection.RESULTS)
					continue;

				Map<String, Double> features = getFeaturesForSentence(entitiesPerSentence, instance, i);
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

				Collections.shuffle(negExamples);
				for (int i = 0; i < negExamples.size(); i++) {
					Map<String, Object> parameter = new HashMap<>();
					parameter.put("docID", negExamples.get(i).docID);
					parameter.put("sentenceIndex", negExamples.get(i).sentenceIndex);
					parameter.put("sentence", negExamples.get(i).sentence);
					dataPoints.add(new DataPoint(parameter, trainingData, negExamples.get(i).features, 0, training));
				}
			}

		}
		return dataPoints;
	}

	private EntityType containsRelatedClassOf(List<EntityType> annotations, EntityType entityType) {

		for (EntityType et : entityType.getRelatedEntityTypes()) {
			if (annotations.contains(et))
				return et;

		}
		return null;
	}

	private EntityType containsSubClassOf(List<EntityType> annotations, EntityType entityType) {

		for (EntityType et : entityType.getTransitiveClosureSubEntityTypes()) {
			if (annotations.contains(et))
				return et;

		}
		return null;
	}

	private Set<Integer> extractRelevantSentences(AbstractAnnotation goldAnnotation) {
		Set<Integer> sentencesWithInfo = new HashSet<>();

		AbstractAnnotation rootAnnotation = goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation();

//		if (rootAnnotation.isInstanceOfDocumentLinkedAnnotation()) {
//
//			DocumentLinkedAnnotation linkedAnn = rootAnnotation.asInstanceOfDocumentLinkedAnnotation();
//
//			int sentenceIndex = linkedAnn.getSentenceIndex();
//			sentencesWithInfo.add(sentenceIndex);
//		}

		goldAnnotation.asInstanceOfEntityTemplate().filter().docLinkedAnnoation().multiSlots().singleSlots().merge()
				.nonEmpty().build().getMergedAnnotations().values().stream().flatMap(a -> a.stream()).filter(a -> {

					if (containsRelatedClassOf(Arrays.asList(a.getEntityType()), EntityType.get("Trend")) == null) {
						return false;
					}

					EntityType invM = containsSubClassOf(Arrays.asList(a.getEntityType()),
							EntityType.get("InvestigationMethod"));
//					EntityType groupName = null;
//					containsRelatedClassOf(Arrays.asList(a.getEntityType()), EntityType.get("GroupName"));
//					EntityType treatment = null;
//					containsRelatedClassOf(Arrays.asList(a.getEntityType()), EntityType.get("Treatment"));

					if (invM == null) {
//							&& groupName == null && treatment == null) {
						return false;
					}

					return true;
				}).forEach(a -> {
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

	private void saveArff(final File arffOutputFile, Instances dataSet) {
		try {
			ArffSaver saver = new ArffSaver();
			saver.setInstances(dataSet);
			saver.setFile(arffOutputFile);
			saver.writeBatch();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
