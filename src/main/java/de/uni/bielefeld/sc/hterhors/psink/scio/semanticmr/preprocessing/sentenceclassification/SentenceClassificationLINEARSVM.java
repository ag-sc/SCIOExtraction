package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.sentenceclassification;

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

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.InjuryRestrictionProvider.EInjuryModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.specs.InjurySpecs;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.sentenceclassification.InstanceCollection.FeatureDataPoint;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.Prediction;
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
public class SentenceClassificationLINEARSVM {

//	public static void main(String[] args) throws Exception {
//
////		new SentenceClassificationLINEARSVM(
////				SystemScope.Builder.getScopeHandler().addScopeSpecification(OrgModelSpecs.systemsScopeReader).build());
//		new SentenceClassificationLINEARSVM(
//				SystemScope.Builder.getScopeHandler().addScopeSpecification(InjurySpecs.systemsScope).build());
//
//	}
//
//	private static Logger log = LogManager.getFormatterLogger("SlotFilling");
//
////	private final File instanceDirectory = new File("src/main/resources/slotfilling/organism_model/corpus/instances/");
//	private final File instanceDirectory = new File("src/main/resources/slotfilling/injury/corpus/instances/");
//
//	public static EInjuryModificationRules rule;
//
//	public final String header = "Mode\tF1\tPrecision\tRecall";
//
//	Parameter svmParameter = null;
//	private Model model = null;
//
//	public SentenceClassificationLINEARSVM(SystemScope scope) throws Exception {
//
////		InstanceCollection data = new InstanceCollection();
//		InstanceCollection trainingData = new InstanceCollection();
//		InstanceCollection testData = new InstanceCollection();
//
//		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(1000L)
//				.setTrainingProportion(80).setTestProportion(20).setCorpusSizeFraction(1F).build();
//
//		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);
//
////		buildData(data, instanceProvider.getInstances(), true);
//
//		buildData(trainingData, trainingData, instanceProvider.getRedistributedTrainingInstances(), true);
//////		
//		buildData(testData, trainingData, instanceProvider.getRedistributedTestInstances(), false);
//
//		libsvm(trainingData, testData);
//
//		System.exit(1);
//		
////		Instances wekaInstance = convertToARFF("TRAIN", data);
//		Instances wekaTRAINInstance = convertToARFF("TRAIN", trainingData, trainingData);
//		Instances wekaTESTInstance = convertToARFF("TEST", testData, trainingData);
////		System.out.println("WEKA Instance: " + wekaInstance.toSummaryString());
//
////		System.out.println("Save ARFF...");
////		saveArff(new File("organismModelTRAIN.arff"), wekaTRAINInstance);
////		saveArff(new File("organismModelTEST.arff"), wekaTESTInstance);
////		saveArff(new File("organismModel.arff"), wekaInstance);
////		saveArff(new File("injury.arff"), wekaInstance);
//
////		System.out.println("Create Random Forest...");
//		RandomForest rf = new RandomForest();
//		rf.setNumIterations(200);
////
//////		Classifier cls = (Classifier) weka.core.SerializationHelper.read("/some/where/j48.model");
////
//		System.out.println("Train RandomForest...");
//		rf.buildClassifier(wekaTRAINInstance);
////
////		System.out.println("RandomForest: " + rf.toString());
//////		SerializationHelper.write(parameter.rootDirectory + modelBaseDirectory + modelName + ".model", rf);
////
//		System.out.println("Start evaluation...");
////
//
//		for (int i = 0; i < wekaTESTInstance.numInstances(); i++) {
//			   double pred = rf.classifyInstance(wekaTESTInstance.instance(i));
//			   System.out.print("ID: " + wekaTESTInstance.instance(i).value(0));
//			   System.out.print(", actual: " + wekaTESTInstance.classAttribute().value((int) wekaTESTInstance.instance(i).classValue()));
//			   System.out.println(", predicted: " + wekaTESTInstance.classAttribute().value((int) pred));
//			 }
//		
////		Evaluation eval = new Evaluation(wekaTRAINInstance);
////		eval.evaluateModel(rf, wekaTESTInstance);
////		System.out.println(eval.toSummaryString("\nResults\n======\n", false));
////		System.out.println(eval.toClassDetailsString());
////		double[][] conf = eval.confusionMatrix();
////		for (double[] ds : conf) {
////			for (double ds2 : ds) {
////				System.out.print(ds2);
////				System.out.print("\t");
////			}
////			System.out.println();
////		}
//
////		Evaluation evaluation = new Evaluation(wekaInstance);
////		evaluation.crossValidateModel(rf, wekaInstance, 10, new Random(1));
////
////		System.out.println(evaluation.toSummaryString("\nResults\n======\n", true));
////		System.out.println(evaluation.toClassDetailsString());
////		System.out.println("Results For Class -1- ");
////		System.out.println("Precision=  " + evaluation.precision(0));
////		System.out.println("Recall=  " + evaluation.recall(0));
////		System.out.println("F-measure=  " + evaluation.fMeasure(0));
////		System.out.println("Results For Class -2- ");
////		System.out.println("Precision=  " + evaluation.precision(1));
////		System.out.println("Recall=  " + evaluation.recall(1));
////		System.out.println("F-measure=  " + evaluation.fMeasure(1));
//
//	}
//
//	public void libsvm(InstanceCollection trainingData, InstanceCollection testData) {
//		SolverType solver = SolverType.L2R_L2LOSS_SVC; // -s 0
////		double C = 5.12E-5; // cost of constraints violation
//		double C = 0.1; // cost of constraints violation
////		double C = 0.001; // cost of constraints violation
////		double C = 0.0001; // cost of constraints violation
////		 double eps = 0.01; // stopping criteria
//		// double C = 0.0001; // cost of constraints violation
//		double eps = 0.001; // stopping criteria
//
//		svmParameter = new Parameter(solver, C, eps);
//
//		train(trainingData);
//
//		for (FeatureDataPoint fdp : testData.getDataPoints()) {
//
//			double p = predict(fdp);
//
//			if (fdp.score < 1.0D)
//				continue;
//			
//			if (fdp.score == p)
//				System.out.println("YEAH");
//			System.out.println(fdp.score);
//			System.out.println(p);
//		}
//	}
//
//	private Map<String, Double> getFeaturesForSentence(Document doc, Integer sentenceIndex) {
//
//		Map<String, Double> features = new HashMap<>();
//
//		int numOfSentence = doc.getNumberOfSentences();
//
//		final int quarter = (int) ((double) numOfSentence / 4) + 1;
//
//		for (int i = 0; i < 4; i++) {
//			features.put(i + "_Q", (sentenceIndex > quarter * i && sentenceIndex < quarter * (i + 1) ? 1D : 0D));
//		}
//
//		for (DocumentToken documentToken : doc.getSentenceByIndex(sentenceIndex)) {
//			if (documentToken.getText().matches("[\\w\\w\\.\\s-_/]+"))
//				features.put(documentToken.getText(), 1D);
//		}
//		return features;
//	}
//
//	public void buildData(InstanceCollection data, InstanceCollection trainingData, List<Instance> instances,
//			boolean training) {
//		for (Instance instance : instances) {
//
//			Set<Integer> sentencesWithInfo = new HashSet<>();
//
//			for (AbstractAnnotation goldAnnotation : instance.getGoldAnnotations().getAnnotations()) {
//
//				AbstractAnnotation rootAnnotation = goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation();
//
//				if (rootAnnotation.isInstanceOfDocumentLinkedAnnotation()) {
//
//					DocumentLinkedAnnotation linkedAnn = rootAnnotation.asInstanceOfDocumentLinkedAnnotation();
//
//					int sentenceIndex = linkedAnn.getSentenceIndex();
//					sentencesWithInfo.add(sentenceIndex);
//				}
//
//				goldAnnotation.asInstanceOfEntityTemplate().filter().docLinkedAnnoation().multiSlots().singleSlots()
//						.merge().nonEmpty().build().getMergedAnnotations().values().stream().flatMap(a -> a.stream())
//						.forEach(a -> {
//							if (a.isInstanceOfDocumentLinkedAnnotation()) {
//
//								DocumentLinkedAnnotation linkedAnn = a.asInstanceOfDocumentLinkedAnnotation();
//
//								int sentenceIndex = linkedAnn.getSentenceIndex();
//								sentencesWithInfo.add(sentenceIndex);
//							}
//
//						});
//
//			}
//
//			int count = 0;
//			List<Map<String, Double>> negExamples = new ArrayList<>();
//			for (int i = 0; i < instance.getDocument().getNumberOfSentences(); i++) {
//
//				Map<String, Double> features = getFeaturesForSentence(instance.getDocument(), i);
//
//				if (sentencesWithInfo.contains(i)) {
//					count++;
//					data.addFeatureDataPoint(new FeatureDataPoint(trainingData, features, 1, training));
//				} else {
//					if (training)
//						negExamples.add(features);
//					else
//						data.addFeatureDataPoint(new FeatureDataPoint(trainingData, features, 0, training));
//				}
//
//			}
//			if (training) {
//
//				Collections.shuffle(negExamples);
//				for (int i = 0; i < count; i++) {
//					data.addFeatureDataPoint(new FeatureDataPoint(trainingData, negExamples.get(i), 0, training));
//				}
//			}
//
//		}
//	}
//
//	public void train(InstanceCollection trainingData) {
//
//		final int dataCount = trainingData.getDataPoints().size();
//		final int totalFeatureCount = trainingData.numberOfTotalFeatures();
//
//		log.info("Number of training instances " + dataCount);
//		log.info("Number of features = " + totalFeatureCount);
//
//		Problem problem = new Problem();
//		problem.l = dataCount; // number of training examples
//		problem.n = totalFeatureCount; // number of features
//		problem.x = new Feature[dataCount][]; // feature nodes
//		problem.y = new double[dataCount]; // target values
//
//		int dataPointIndex = 0;
//		for (FeatureDataPoint tdp : trainingData.getDataPoints()) {
//			problem.x[dataPointIndex] = toLibLinearNodeArray(tdp);
//			problem.y[dataPointIndex] = tdp.score;
//			dataPointIndex++;
//		}
//		for (Feature[] nodes : problem.x) {
//			int indexBefore = 0;
//			for (Feature n : nodes) {
//				if (n.getIndex() <= indexBefore) {
//					System.out.println(n.getIndex());
//					System.out.println(n);
//					System.out.println(Arrays.toString(nodes));
//					trainingData.sparseIndexMapping.entrySet().forEach(System.out::println);
//					throw new IllegalArgumentException("feature nodes must be sorted by index in ascending order");
//				}
//				indexBefore = n.getIndex();
//			}
//		}
//
//		log.info("Start training SVC...");
//		this.model = Linear.train(problem, svmParameter);
////		ParameterSearchResult r = Linear.findParameters(problem, svmParameter, 2, 0.0000001, 100000);
////		//
////		System.out.println(r.getBestC());
////		System.out.println(r.getBestP());
////		log.info("done");
//	}
//
//	private double predict(FeatureDataPoint dp) {
//
//		Feature[] nodes = toLibLinearNodeArray(dp);
//
//		// double v1 = Linear.predict(model, nodes);
//		// double[] probabilities = new double[model.getLabels().length];
//		//
//		// double v1 = Linear.predictProbability(model, nodes, probabilities);
//
//		double[] functionValues = new double[model.getNrClass()];
//
//		Linear.predictValues(model, nodes, functionValues);
//		// System.out.println("(Actual:" + dp.score + " Prediction:" +
//		// functionValues[0] + ")");
//		// System.out.println("(Actual:" + dp.score + " Prediction:" +
//		// Arrays.toString(functionValues) + ")");
//
//		return functionValues[0];
//	}
//
//	private Feature[] toLibLinearNodeArray(FeatureDataPoint tdp) {
//		Feature[] nodes = new Feature[tdp.featuresIndices.size()];
//
//		int nonZeroFeatureIndex = 0;
//		for (Integer featureIndex : tdp.featuresIndices) {
//			Feature node = new FeatureNode(featureIndex, tdp.features.get(featureIndex));
//			nodes[nonZeroFeatureIndex] = node;
//			nonZeroFeatureIndex++;
//		}
//		return nodes;
//	}


}
