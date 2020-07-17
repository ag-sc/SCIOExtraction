package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.fasttext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.jfasttext.JFastText;
import com.github.jfasttext.JFastText.ProbLabel;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.nerla.JsonNerlaIO;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation.ESection;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Result;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Trend;

public class FastTextSentenceClassification {

	public static final String NO_LABEL = "__label__UNLABELED";

	public EntityType type;

	public static int numberOfEpochs;

	public static int numberOfDimensions;
	public static boolean binaryClassification;
	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	public static void main(String[] args) throws IOException {
		SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(DataStructureLoader.loadNERDataStructureReader("Trend"))
//				.addScopeSpecification(DataStructureLoader.loadNERDataStructureReader("InvestigationMethod"))
				/**
				 * Finally, we build the systems scope.
				 */
				.build();

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(90)
				.setTestProportion(10).setSeed(1000L).setCorpusSizeFraction(1F).build();

//		List<String> docs = Files.readAllLines(new File("src/main/resources/corpus_docs.csv").toPath());
//		Collections.sort(docs);
//
//		Collections.shuffle(docs, new Random(100));
//
//		final int x = (int) (((double) docs.size() / 100D) * 80D);
//		List<String> trainingInstanceNames = docs.subList(0, x);
//		List<String> testInstanceNames = docs.subList(x, docs.size());

//		AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
//				.setTrainingInstanceNames(trainingInstanceNames).setTestInstanceNames(testInstanceNames).build();
		InstanceProvider.maxNumberOfAnnotations = 300;
		InstanceProvider.removeInstancesWithToManyAnnotations = false;

//		EntityType type = SCIOEntityTypes.investigationMethod;
		EntityType type = SCIOEntityTypes.trend;

		InstanceProvider instanceProvider = new InstanceProvider(
				NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(type), corpusDistributor);

		boolean binary = false;
		// new FastTextSentenceClassification(type,
		// instanceProvider.getRedistributedTrainingInstances(),
//				instanceProvider.getRedistributedTestInstances());
//		
//		leaveOneOutEval(type, instanceProvider.getInstances());
		Score mScore = tenRandom9010Split(binary, type, new ArrayList<>(instanceProvider.getInstances()), 1000L);

		log.info(mScore);
	}

	private static Score tenRandom9010Split(boolean binary, EntityType type, List<Instance> instances, long randomSeed)
			throws IOException {

		Score mScore = new Score(EScoreType.MACRO);

		Random rand = new Random(randomSeed);

		for (int i = 0; i < 10; i++) {
			log.info("PROGRESS: " + i);

			Collections.shuffle(instances, rand);

			final int x = (int) (((double) instances.size() / 100D) * 90D);

			List<Instance> trainingInstances = instances.subList(0, x);
			List<Instance> testInstances = instances.subList(x, instances.size());

			FastTextSentenceClassification t = new FastTextSentenceClassification("tenRandom9010Split_TEST", binary,
					type, trainingInstances);

			Score s = t.score(testInstances).toMacro();
			log.info(s);
			mScore.add(s);
		}

		return mScore;
	}

	private static Score leaveOneOutEval(boolean binary, EntityType type, List<Instance> instances) throws IOException {

		Score mScore = new Score(EScoreType.MACRO);

		for (int i = 0; i < instances.size(); i++) {
			log.info("PROGRESS: " + i);
			List<Instance> trainingInstances = new ArrayList<>();
			List<Instance> testInstances = new ArrayList<>();

			for (int j = 0; j < instances.size(); j++) {

				if (i == j)
					testInstances.add(instances.get(j));
				else
					trainingInstances.add(instances.get(j));
			}
			FastTextSentenceClassification t = new FastTextSentenceClassification("leaveOneOutEval_TEST", binary, type,
					trainingInstances);

			Score s = t.score(testInstances).toMacro();
			log.info(s);
			mScore.add(s);
		}
		return mScore;
	}

	final public int numOfTrainingDuplicates = 1;
	private JFastText jft;

	private String modelName;

	public FastTextSentenceClassification(String modelName, boolean binary, EntityType type,
			List<Instance> trainingInstances) throws IOException {

		log.info("Number of Json training instances: " + trainingInstances.size());

		this.type = type;
		numberOfEpochs = 50;
		numberOfDimensions = 200;
		binaryClassification = binary;
		this.modelName = modelName;
		final String trainingDataFileName = "fasttext/resources/data/" + modelName + "_" + type.name
				+ "_train_labeled_data.txt";

		List<FastTextInstance> trainData = buildTrainingData(trainingInstances, trainingDataFileName, false);
		
		
//		Score [macroF1=0.104, macroPrecision=0.075, macroRecall=0.168, macroAddCounter=10] NOT same size invest NOT project annotations
//		Score [macroF1=0.255, macroPrecision=0.246, macroRecall=0.264, macroAddCounter=10] NOT same size invest project annotations
//	 	Score [macroF1=0.086, macroPrecision=0.057, macroRecall=0.171, macroAddCounter=10] same size invest NOT project annotations
//		Score [macroF1=0.213, macroPrecision=0.229, macroRecall=0.199, macroAddCounter=10] same size invest project annotations
//		Score [macroF1=0.323, macroPrecision=0.308, macroRecall=0.341, macroAddCounter=10] NOT same size invest project annotations +pretrained


		
//		Score [macroF1=0.279, macroPrecision=0.238, macroRecall=0.337, macroAddCounter=10] NOT same size trend NOT project annotations		
//		Score [macroF1=0.455, macroPrecision=0.543, macroRecall=0.391, macroAddCounter=10] NOT same size trend project annotations + +pretrained
//		Score [macroF1=0.442, macroPrecision=0.514, macroRecall=0.388, macroAddCounter=10] NOT same size trend project annotations
//		Score [macroF1=0.270, macroPrecision=0.227, macroRecall=0.333, macroAddCounter=10] same size trend NOT project annotations
//		Score [macroF1=0.411, macroPrecision=0.568, macroRecall=0.322, macroAddCounter=10]  same size trend  project annotations

		log.info("Training Negative samples: " + trainData.stream().filter(a -> a.goldLabel == NO_LABEL).count());
		log.info("Training Positive Samples: "
				+ (trainData.size() - trainData.stream().filter(a -> a.goldLabel == NO_LABEL).count()));
		log.info("Training Total: " + trainData.size());

		this.keyTerms = new HashSet<>();
//		this.keyTerms = KeyTermExtractor.getKeyTerms(trainingInstances);

		jft = new JFastText();
		String ftModelName = modelName +
//				
//				"pretrained_" +
//				
				type.name + "_" + binaryClassification + "_" + numberOfDimensions + "_" + numberOfEpochs
				+ "_supervised.model";
		String preTrainedvec = "wordvector/w2v.vec";
		jft.runCmd(new String[] { "supervised", "-input", trainingDataFileName, "-output",
				"fasttext/resources/models/" + ftModelName, "-epoch", numberOfEpochs + "",

//				"-pretrainedVectors", preTrainedvec,
//				"-wordNgrams" ,"1",

				"-dim", numberOfDimensions + "" });

		jft.loadModel("fasttext/resources/models/" + ftModelName + ".bin");

//		Score scoreTrain = evaluate(jft, getLabledDocuments(trainignInstances, 1));
//		log.info("train: " + scoreTrain);

//		evaluate(testInstances);
		log.info("Fast Text ModelName: " + ftModelName);

	}

	private Score score(List<Instance> testInstances) {
		List<FastTextInstance> testData = getLabledDocuments(testInstances, 1);
//		List<String> testData = Files.readAllLines(new File("src/test/resources/data/test_labeled_data.txt").toPath());

		for (Iterator<FastTextInstance> iterator = testData.iterator(); iterator.hasNext();) {
			FastTextInstance fastTextInstance = (FastTextInstance) iterator.next();

			if (fastTextInstance.section != ESection.RESULTS)
				iterator.remove();

		}
		log.info("Test Negative samples: " + testData.stream().filter(a -> a.goldLabel == NO_LABEL).count());
		log.info("Test Positive Samples: "
				+ (testData.size() - testData.stream().filter(a -> a.goldLabel == NO_LABEL).count()));
		log.info("Test Total: " + testData.size());

		List<FastTextPrediction> predictions = predict(testData);

		Score scoreTest = evaluate(predictions);

		return scoreTest;
		// for (int k = 1; k <= 10; k++) {
//
//			log.info("Test evaluate at " + k + "\t" + evaluateAtK(jft, testData, k));
//		}
	}

	private void writeNerlas(List<FastTextPrediction> predictions) {

		Map<Instance, Set<DocumentLinkedAnnotation>> annotationsToWrite = new HashMap<>();
		File root = new File("data/annotations/fasttext/" + type.name + "/");
		root.mkdirs();
		for (FastTextPrediction fastTextPrediction : predictions) {

			if (fastTextPrediction.label.equals(NO_LABEL))
				continue;

			annotationsToWrite.putIfAbsent(fastTextPrediction.fastTextInstance.instance, new HashSet<>());
			annotationsToWrite.get(fastTextPrediction.fastTextInstance.instance)
					.add(toDocLinkedAnnotation(fastTextPrediction));

		}

		for (Entry<Instance, Set<DocumentLinkedAnnotation>> map : annotationsToWrite.entrySet()) {

			try {
				new JsonNerlaIO(true).writeNerlas(new File(root, map.getKey().getName()), map.getValue());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public Map<Instance, Set<DocumentLinkedAnnotation>> predictNerlas(List<Instance> testInstances) {
		List<FastTextInstance> testData = getLabledDocuments(testInstances, 1);

		for (Iterator<FastTextInstance> iterator = testData.iterator(); iterator.hasNext();) {
			FastTextInstance fastTextInstance = (FastTextInstance) iterator.next();

			if (fastTextInstance.section != ESection.RESULTS)
				iterator.remove();

		}

		log.info("Test Negative samples: " + testData.stream().filter(a -> a.goldLabel == NO_LABEL).count());
		log.info("Test Positive Samples: "
				+ (testData.size() - testData.stream().filter(a -> a.goldLabel == NO_LABEL).count()));
		log.info("Test Total: " + testData.size());

		List<FastTextPrediction> predictions = predict(testData);

		Map<Instance, Set<DocumentLinkedAnnotation>> annotationsToWrite = new HashMap<>();
		for (FastTextPrediction fastTextPrediction : predictions) {

			if (fastTextPrediction.label.equals(NO_LABEL))
				continue;

			annotationsToWrite.putIfAbsent(fastTextPrediction.fastTextInstance.instance, new HashSet<>());
			annotationsToWrite.get(fastTextPrediction.fastTextInstance.instance)
					.add(toDocLinkedAnnotation(fastTextPrediction));

		}
		return annotationsToWrite;
	}

	private DocumentLinkedAnnotation toDocLinkedAnnotation(FastTextPrediction fastTextPrediction) {
		// convert a sentence based annotation to doc-linked annotation. This req. a doc
		// link which in this case is always the first token.
		DocumentToken firstToken = fastTextPrediction.fastTextInstance.instance.getDocument()
				.getSentenceByIndex(fastTextPrediction.fastTextInstance.sentenceIndex).get(0);
		String textualContent = firstToken.getText();
		int offset = firstToken.getDocCharOffset();
		return AnnotationBuilder.toAnnotation(fastTextPrediction.fastTextInstance.instance.getDocument(),
				fastTextPrediction.getEntityType(), textualContent, offset);
	}

	private final Set<String> keyTerms;

	private List<FastTextInstance> buildTrainingData(List<Instance> instances, final String trainingDataFileName,
			boolean sameSize) throws FileNotFoundException {
		File trainingDataFile = new File(trainingDataFileName);
		PrintStream ps = new PrintStream(trainingDataFile);

		List<FastTextInstance> trainData = getLabledDocuments(instances, numOfTrainingDuplicates);

		for (Iterator<FastTextInstance> iterator = trainData.iterator(); iterator.hasNext();) {
			FastTextInstance fastTextInstance = (FastTextInstance) iterator.next();

			if (fastTextInstance.section != ESection.RESULTS)
				iterator.remove();

		}
		long numOfNeg = trainData.stream().filter(a -> a.goldLabel == NO_LABEL).count();
		long numOfPos = (trainData.size() - trainData.stream().filter(a -> a.goldLabel == NO_LABEL).count());

		long min = Math.min(numOfNeg, numOfPos);
		int countPos = 0;
		int countNeg = 0;

		if (sameSize)
			for (Iterator<FastTextInstance> iterator = trainData.iterator(); iterator.hasNext();) {
				FastTextInstance fastTextInstance = (FastTextInstance) iterator.next();

				boolean negSample = fastTextInstance.goldLabel == NO_LABEL;

				if (negSample) {
					if (countNeg >= min)
						iterator.remove();
				} else {
					if (countPos >= min)
						iterator.remove();
				}

				if (negSample)
					countNeg++;
				else
					countPos++;
			}

		trainData.stream().map(i -> i.toSimpleString()).forEach(ps::println);

		ps.flush();
		ps.close();
		return trainData;

	}

	public List<FastTextPrediction> predict(List<FastTextInstance> data) {

		List<FastTextPrediction> predictions = new ArrayList<>();
		for (FastTextInstance fti : data) {

//			boolean containsKeyterm = false;
//			String sentence = fti.text;

			String text = fti.text;
			ProbLabel probLabel = jft.predictProba(text);

			String firstPredLabel = probLabel != null ? probLabel.label : NO_LABEL;

//			for (String keyTerm : keyTerms) {
//
//				if (sentence.contains(keyTerm)) {
//					containsKeyterm = true;
//					break;
//				}
//			}
//
//			if (!containsKeyterm) {
//				firstPredLabel = NO_LABEL;
//			}

			predictions.add(new FastTextPrediction(fti, firstPredLabel,
					(probLabel != null ? Math.exp(probLabel.logProb) : 0D)));
		}

		return predictions;
	}

	private Score evaluate(List<FastTextPrediction> predicitons) {
		Score score = new Score();
		for (FastTextPrediction fti : predicitons) {

			final String predLabel = fti.label;
			final String goldLabel = fti.fastTextInstance.goldLabel;

			if (predLabel.equals(goldLabel)) {
				if (predLabel.equals(NO_LABEL)) {
					score.increaseTrueNegative();
				} else {
					score.increaseTruePositive();
				}
			} else {
				if (goldLabel.equals(NO_LABEL)) {
					score.increaseFalsePositive();
				} else {
					score.increaseFalseNegative();
				}
			}
		}

		return score;
	}

	private Score evaluateAtK(JFastText jft, List<FastTextInstance> data, int k) {
		Score score = new Score();

		for (FastTextInstance string : data) {

			String text = string.text;
			String label = string.goldLabel;

			List<ProbLabel> probLabels = jft.predictProba(text, k);

			boolean tp = false;
			boolean tn = false;
			int c = 0;
			for (ProbLabel probLabel : probLabels) {
				c++;
				String predLabel = probLabel != null ? probLabel.label : NO_LABEL;
				if (predLabel.equals(label)) {
					if (predLabel.equals(NO_LABEL)) {
						tn = true;
					} else {
						tp = true;
						tn = false;
						break;
					}
				}
			}
			if (tp || tn) {
				if (tn) {
					score.increaseTrueNegative();
				} else {
					score.increaseTruePositive();
				}
			} else {
				if (label.equals(NO_LABEL)) {
					score.increaseFalsePositive();
				} else {
					score.increaseFalseNegative();
				}
			}
		}
		return score;
	}

	static public class FastTextPrediction {
		final public FastTextInstance fastTextInstance;
		final public String label;
		final public double probability;

		public FastTextPrediction(FastTextInstance fastTextInstance, String label, double probability) {
			this.fastTextInstance = fastTextInstance;
			this.label = label;
			this.probability = probability;
		}

		public EntityType getEntityType() {
			return EntityType.get(label.replaceFirst("__label__", ""));
		}

		@Override
		public String toString() {
			return "FastTextPrediction [fastTextInstance=" + fastTextInstance + ", label=" + label + ", probability="
					+ probability + "]";
		}

	}

	public final static String BAD_CHAR = "[^\\x20-\\x7E]+";

	static public class FastTextInstance {

		final public String text;
		final public String goldLabel;
		final public Instance instance;
		final public int sentenceIndex;
		final public ESection section;

		public FastTextInstance(String text, String goldLabel, Instance instance, int sentenceIndex, ESection section) {

			this.text = text.replaceAll(BAD_CHAR, "#BADCHAR");
			this.goldLabel = goldLabel;
			this.instance = instance;
			this.sentenceIndex = sentenceIndex;
			this.section = section;
		}

		public String toSimpleString() {
			return goldLabel + " " + text;
		}

		@Override
		public String toString() {
			return "FastTextInstance [text=" + text + ", goldLabel=" + goldLabel + ", instance=" + instance
					+ ", sentenceIndex=" + sentenceIndex + ", section=" + section + "]";
		}

	}

	public List<FastTextInstance> getLabledDocuments(List<Instance> instances, int numOfDuplicates) {

		List<FastTextInstance> labeledData = new ArrayList<>();

		Set<Integer> positiveSentences = new HashSet<>();
		for (Instance instance : instances) {

			List<DocumentLinkedAnnotation> annotations = new ArrayList<>();
			if (type == SCIOEntityTypes.investigationMethod)
				annotations = extractInvestigationMethods(instance);
			if (type == SCIOEntityTypes.trend)
				annotations = extractTrends(instance);

			for (DocumentLinkedAnnotation annotation : annotations) {

				positiveSentences.add(annotation.getSentenceIndex());

				String label;
				if (binaryClassification)
					label = "__label__" + type.name;
				else
					label = "__label__" + annotation.getEntityType().name;

				for (int j = 0; j < numOfDuplicates; j++) {
					labeledData.add(new FastTextInstance(
							instance.getDocument().getContentOfSentence(annotation.getSentenceIndex()), label, instance,
							annotation.getSentenceIndex(),
							AutomatedSectionifcation.getInstance(instance).getSection(annotation)));
				}

			}
			for (int i = 0; i < instance.getDocument().getNumberOfSentences(); i++) {

				if (positiveSentences.contains(i))
					continue;

				for (int j = 0; j < numOfDuplicates; j++) {
					labeledData.add(new FastTextInstance(instance.getDocument().getContentOfSentence(i), NO_LABEL,
							instance, i, AutomatedSectionifcation.getInstance(instance).getSection(i)));
				}
			}
		}
		return labeledData;

	}

//	public List<FastTextInstance> getMultiLabledDocuments(List<Instance> instances, int numOfDuplicates) {
//
//		List<FastTextInstance> labeledData = new ArrayList<>();
//
//		Set<Integer> positiveSentences = new HashSet<>();
//		for (Instance instance : instances) {
//
//			List<DocumentLinkedAnnotation> annotations = extractTrends(instance);
//			for (DocumentLinkedAnnotation annotation : annotations) {
//
//				if (positiveSentences.contains(annotation.getSentenceIndex()))
//					continue;
//
//				positiveSentences.add(annotation.getSentenceIndex());
//
//				String label = annotations.stream().filter(a -> annotation.getSentenceIndex() == a.getSentenceIndex())
//						.map(a -> "__label__" + a.getEntityType().name + " ").distinct().reduce("", String::concat)
//						.trim();
//
//				for (int j = 0; j < numOfDuplicates; j++) {
//					labeledData.add(new FastTextInstance(
//							instance.getDocument().getContentOfSentence(annotation.getSentenceIndex()), label, instance,
//							annotation.getSentenceIndex(),
//							AutomatedSectionifcation.getInstance(instance).getSection(annotation)));
//				}
//
//			}
//
//			for (int i = 0; i < instance.getDocument().getNumberOfSentences(); i++) {
//				if (positiveSentences.contains(i))
//					continue;
//				for (int j = 0; j < numOfDuplicates; j++) {
//					labeledData.add(new FastTextInstance(instance.getDocument().getContentOfSentence(i), NO_LABEL,
//							instance, i, AutomatedSectionifcation.getInstance(instance).getSection(i)));
//				}
//			}
//		}
//
//		return labeledData;
//
//	}

	private List<DocumentLinkedAnnotation> extractInvestigationMethods(Instance instance) {

		List<DocumentLinkedAnnotation> ims = new ArrayList<>();
		/**
		 * IF invest data set is loaded:
		 */
		for (AbstractAnnotation a : instance.getGoldAnnotations().getAbstractAnnotations()) {

			if (!a.isInstanceOfDocumentLinkedAnnotation())
				continue;
			ims.add(a.asInstanceOfDocumentLinkedAnnotation());
		}
		/**
		 * IF result data set is loaded:
		 */
		for (AbstractAnnotation result : instance.getGoldAnnotations().getAnnotations()) {
			if (!result.isInstanceOfEntityTemplate())
				continue;
			Result r = new Result(result);
			EntityTemplate invM = r.getInvestigationMethod();
			if (invM != null)
				if (invM.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
					ims.add(invM.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation());

		}
		return ims;

	}

	private List<DocumentLinkedAnnotation> extractTrends(Instance instance) {

		List<DocumentLinkedAnnotation> ts = new ArrayList<>();

		for (AbstractAnnotation a : instance.getGoldAnnotations().getAbstractAnnotations()) {
			/**
			 * IF result data set is loaded:
			 */
			if (a.isInstanceOfEntityTemplate()) {

				Result r = new Result(a.asInstanceOfEntityTemplate().asInstanceOfEntityTemplate());
				EntityTemplate trend = r.getTrend();

				if (trend == null)
					continue;

				Trend t = new Trend(trend);

				DocumentLinkedAnnotation root = t.getRootAnntoationAsDocumentLinkedAnnotation();

				if (root != null)
					ts.add(root);
				DocumentLinkedAnnotation diff = t.getDifferenceAsDocumentLinkedAnnotation();

				if (diff != null)
					ts.add(diff);

				SingleFillerSlot sigSlot = trend.getSingleFillerSlot(SCIOSlotTypes.hasSignificance);

				if (!sigSlot.containsSlotFiller()) {
					continue;
				}

				EntityTemplate significance = sigSlot.getSlotFiller().asInstanceOfEntityTemplate();

				DocumentLinkedAnnotation sig = significance.getRootAnnotation() != null
						&& significance.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()
								? significance.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation()
								: null;

				if (sig != null)
					ts.add(sig);

				SingleFillerSlot alphaSlot = significance.getSingleFillerSlot(SCIOSlotTypes.hasAlphaSignificanceNiveau);

				if (alphaSlot.containsSlotFiller()) {

					ts.add(alphaSlot.getSlotFiller().asInstanceOfDocumentLinkedAnnotation());

				}
				SingleFillerSlot pvalueSlot = significance.getSingleFillerSlot(SCIOSlotTypes.hasPValue);

				if (pvalueSlot.containsSlotFiller()
						&& pvalueSlot.getSlotFiller().isInstanceOfDocumentLinkedAnnotation()) {
					ts.add(pvalueSlot.getSlotFiller().asInstanceOfDocumentLinkedAnnotation());

				}

			} else {
				/**
				 * IF trend data set is loaded:
				 */
				ts.add(a.asInstanceOfDocumentLinkedAnnotation());
			}

		}
		return ts;
	}

}