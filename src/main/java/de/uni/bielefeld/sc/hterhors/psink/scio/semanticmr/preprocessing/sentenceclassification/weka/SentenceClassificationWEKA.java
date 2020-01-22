package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.sentenceclassification.weka;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.specifications.ExperimentalGroupSpecifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.sentenceclassification.Classification;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.sentenceclassification.InstanceCollection;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.sentenceclassification.InstanceCollection.FeatureDataPoint;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SparseInstance;

/**
 * Playground to Classify sentences into important or not important. Maybe multi
 * class classification.
 * 
 * @author hterhors
 *
 */
public class SentenceClassificationWEKA {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

//	private static final File instanceDirectory = new File(
//			"src/main/resources/slotfilling/experimental_group/corpus/instances/");
	private static final File instanceDirectory = new File(
			"src/main/resources/slotfilling/organism_model/corpus/instances/");

	private static final String CLASSIFICATION_LABEL_RELEVANT = "Y";

	private static final String CLASSIFICATION_LABEL_NOT_RELEVANT = "N";
//	private static  final File instanceDirectory = new File("src/main/resources/slotfilling/injury/corpus/instances/");

	public static void main(String[] args) throws Exception {
//		
//				SystemScope.Builder.getScopeHandler().addScopeSpecification(OrgModelSpecs.systemsScopeReader).build();
//				SystemScope.Builder.getScopeHandler().addScopeSpecification(InjurySpecs.systemsScope).build();

		SystemScope.Builder.getScopeHandler().addScopeSpecification(ExperimentalGroupSpecifications.systemsScope)
				.build();
		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(100L)
				.setTrainingProportion(80).setTestProportion(20).setCorpusSizeFraction(1F).build();

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		SentenceClassificationWEKA sentenceClassification = new SentenceClassificationWEKA(
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
		System.out.println(score.getAccuracy());
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

	private final InstanceCollection trainingData = new InstanceCollection();

	private final double threshold;

	private final RandomForest rf;

	public SentenceClassificationWEKA(List<Instance> trainingInstances) {

		threshold = 0.5;
		rf = new RandomForest();
		log.info("Train sentence classifier...");

		List<FeatureDataPoint> trainingDataPoints = convertInstancesToDataPoints(trainingInstances, true);

		trainingDataPoints.forEach(fdp -> trainingData.addFeatureDataPoint(fdp));

		Instances wekaTRAINInstance = convertToWekaInstances("TRAIN", trainingData.getDataPoints());

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

		List<FeatureDataPoint> fdps = convertInstanceToDataPoints(testInstance, false);

		for (FeatureDataPoint fdp : fdps) {

			weka.core.Instance instance = convertToWekaInstances("TEST", fdp);

			classifications.put(fdp.sentenceIndex, testForInstance(fdp, rf, score, instance));
		}

		return classifications;
	}

	public Map<Integer, Classification> classifyDocument(Document document) {
		log.info("Classify document: " + document.documentID);
		Map<Integer, Classification> classifications = new HashMap<>();

		final List<FeatureDataPoint> fdps = new ArrayList<>();

		for (int i = 0; i < document.getNumberOfSentences(); i++) {

			String sentence = document.getContentOfSentence(i);

			Map<String, Double> features = getFeaturesForSentence(document, i);

			fdps.add(new FeatureDataPoint(document.documentID, i, sentence, trainingData, features, 0, true));
		}

		for (FeatureDataPoint fdp : fdps) {

			weka.core.Instance instance = convertToWekaInstances("TEST", fdp);

			classifications.put(fdp.sentenceIndex, classifyForInstance(fdp, rf, instance));
		}

		return classifications;
	}

	private Classification testForInstance(FeatureDataPoint featureDataPoint, RandomForest rf, Score score,
			weka.core.Instance instance) {

		try {
			double[] probs;
			probs = rf.distributionForInstance(instance);

			double pred = probs[1] > threshold ? 1 : 0;

			String groundTruth = classAttribute.value((int) instance.classValue());

			String prediction = classAttribute.value((int) pred);

			boolean relevant = prediction.equals(CLASSIFICATION_LABEL_RELEVANT);

			Classification classification = new Classification(featureDataPoint.sentenceIndex, relevant, probs[1]);

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

	private Classification classifyForInstance(FeatureDataPoint featureDataPoint, RandomForest rf,
			weka.core.Instance instance) {

		try {
			double[] probs;
			probs = rf.distributionForInstance(instance);

			double pred = probs[1] > threshold ? 1 : 0;

			Classification classification = new Classification(featureDataPoint.sentenceIndex,
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

	private List<FeatureDataPoint> convertInstanceToDataPoints(Instance instance, boolean training) {
		return convertInstancesToDataPoints(Arrays.asList(instance), training);
	}

	private List<FeatureDataPoint> convertInstancesToDataPoints(List<Instance> instances, boolean training) {

		final List<FeatureDataPoint> dataPoints = new ArrayList<>();

		for (Instance instance : instances) {
			Set<Integer> sentencesWithInfo = new HashSet<>();
			for (AbstractAnnotation goldAnnotation : instance.getGoldAnnotations().getAnnotations()) {
				sentencesWithInfo.addAll(extractRelevantSentences(goldAnnotation));
			}

			int count = 0;
			List<P> negExamples = new ArrayList<>();
			for (int i = 0; i < instance.getDocument().getNumberOfSentences(); i++) {

				String sentence = instance.getDocument().getContentOfSentence(i);

				Map<String, Double> features = getFeaturesForSentence(instance.getDocument(), i);

				if (sentencesWithInfo.contains(i)) {
					count++;
					dataPoints.add(new FeatureDataPoint(instance.getDocument().documentID, i, sentence, trainingData,
							features, 1, training));
				} else {
					if (training)
						negExamples.add(new P(instance.getDocument().documentID, i, features, sentence));
					else
						dataPoints.add(new FeatureDataPoint(instance.getDocument().documentID, i, sentence,
								trainingData, features, 0, training));
				}

			}
			if (training) {

				Collections.shuffle(negExamples);
				for (int i = 0; i < count; i++) {
					dataPoints.add(new FeatureDataPoint(negExamples.get(i).docID, negExamples.get(i).sentenceIndex,
							negExamples.get(i).sentence, trainingData, negExamples.get(i).features, 0, training));
				}
			}

		}
		return dataPoints;
	}

	private Set<Integer> extractRelevantSentences(AbstractAnnotation goldAnnotation) {
		Set<Integer> sentencesWithInfo = new HashSet<>();

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

	private weka.core.Instance convertToWekaInstances(final String dataSetName, final FeatureDataPoint dataPoint) {
		return convertToWekaInstances(dataSetName, Arrays.asList(dataPoint)).get(0);
	}

	private Instances convertToWekaInstances(final String dataSetName, final List<FeatureDataPoint> dataPoints) {

		Attribute[] attributes = new Attribute[trainingData.sparseIndexMapping.size()];

		for (Entry<String, Integer> attribute : trainingData.sparseIndexMapping.entrySet()) {
			attributes[attribute.getValue()] = new Attribute(attribute.getKey(), binaryValues);
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

}
