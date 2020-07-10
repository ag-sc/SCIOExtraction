package de.uni.bielefeld.sc.hterhors.psink.scio.tools.fasttext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;

import com.github.jfasttext.JFastText;
import com.github.jfasttext.JFastText.ProbLabel;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
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
import de.hterhors.semanticmr.tools.KeyTermExtractor;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.investigationMethod.TFIDFInvestigationMethodExtractor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Result;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Trend;

//Pretrained + trend + 50 epochs 200 dim
//test: Score [getAccuracy()=0.602, getF1()=0.380, getPrecision()=0.392, getRecall()=0.368, tp=173, fp=268, fn=297, tn=682]

//pretrained_InvestigationMethod_false_200_50_supervised.model
//test: Score [getAccuracy()=0.575, getF1()=0.296, getPrecision()=0.244, getRecall()=0.378, tp=94, fp=292, fn=155, tn=511]

//Pretrained + trend + 5000 epochs 200 dim
//test: Score [getAccuracy()=0.620, getF1()=0.339, getPrecision()=0.417, getRecall()=0.286, tp=143, fp=200, fn=357, tn=767]

//Pretrained + trend + 10 epochs 200 dim
//test: Score [getAccuracy()=0.567, getF1()=0.343, getPrecision()=0.355, getRecall()=0.332, tp=166, fp=301, fn=334, tn=666]

//trend + 1000 epochs 200 dim
//test: Score [getAccuracy()=0.510, getF1()=0.318, getPrecision()=0.303, getRecall()=0.336, tp=168, fp=387, fn=332, tn=580]

//trend + 100 epochs 200 dim
//test: Score [getAccuracy()=0.501, getF1()=0.311, getPrecision()=0.294, getRecall()=0.330, tp=165, fp=397, fn=335, tn=570]

// trend + 10 epochs 200 dim
//test: Score [getAccuracy()=0.550, getF1()=0.279, getPrecision()=0.308, getRecall()=0.256, tp=128, fp=288, fn=372, tn=679]

//trend + 10 epochs 300 dim
//test: Score [getAccuracy()=0.545, getF1()=0.282, getPrecision()=0.305, getRecall()=0.262, tp=131, fp=298, fn=369, tn=669]

//trend + 1000 epochs 10 dim
//test: Score [getAccuracy()=0.512, getF1()=0.325, getPrecision()=0.307, getRecall()=0.344, tp=172, fp=388, fn=328, tn=579]

//trend +5000 epochs 10 dim
//test: Score [getAccuracy()=0.517, getF1()=0.330, getPrecision()=0.313, getRecall()=0.348, tp=174, fp=382, fn=326, tn=585]

//trend + 5000 epochs 200 dim
//test: Score [getAccuracy()=0.515, getF1()=0.327, getPrecision()=0.310, getRecall()=0.346, tp=173, fp=385, fn=327, tn=582]

public class FastTextSentenceClassification {

//	test: Score [getAccuracy()=0.500, getF1()=0.312, getPrecision()=0.294, getRecall()=0.332, tp=166, fp=399, fn=334, tn=568]

	public static final String NO_LABEL = "__label__UNLABELED";

	public EntityType type;

	public static int numberOfEpochs;

	public static int numberOfDimensions;
	public static boolean binaryClassification;

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

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(99)
				.setTestProportion(1).setSeed(1000L).setCorpusSizeFraction(1F).build();

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

//		new FastTextSentenceClassification(type, instanceProvider.getRedistributedTrainingInstances(),
//				instanceProvider.getRedistributedTestInstances());
//		
//		leaveOneOutEval(type, instanceProvider.getInstances());
		Score mScore = tenRandom8020Split(type, new ArrayList<>(instanceProvider.getInstances()), 1000L);

		System.out.println(mScore);
	}

	private static Score tenRandom8020Split(EntityType type, List<Instance> instances, long randomSeed)
			throws IOException {
//		Score [macroF1=0.645, macroPrecision=0.500, macroRecall=0.909] for investigationmethods 50 200 binary
//		Score [macroF1=0.261, macroPrecision=0.250, macroRecall=0.273]  for investigationmethods 50 200 multi class
//		Score [macroF1=0.817, macroPrecision=0.690, macroRecall=1.000] for trend 50 200 binary 
//		Score [macroF1=0.393, macroPrecision=0.407, macroRecall=0.379] for trend 50 200 multi class

		Score mScore = new Score(EScoreType.MACRO);

		Random rand = new Random(randomSeed);

		for (int i = 0; i < 10; i++) {
			System.out.println("PROGRESS: " + i);

			Collections.shuffle(instances, rand);

			final int x = (int) (((double) instances.size() / 100D) * 90D);

			List<Instance> trainingInstances = instances.subList(0, x);
			List<Instance> testInstances = instances.subList(x, instances.size());

			FastTextSentenceClassification t = new FastTextSentenceClassification(type, trainingInstances);

			Score s = t.score(testInstances).toMacro();
			System.out.println(s);
			mScore.add(s);
		}

		return mScore;
	}

	private static Score leaveOneOutEval(EntityType type, List<Instance> instances) throws IOException {
//		Score [macroF1=0.645, macroPrecision=0.500, macroRecall=0.909] for investigationmethods 50 200 binary
//		Score [macroF1=0.261, macroPrecision=0.250, macroRecall=0.273]  for investigationmethods 50 200 multi class
//		Score [macroF1=0.817, macroPrecision=0.690, macroRecall=1.000] for trend 50 200 binary 
//		Score [macroF1=0.393, macroPrecision=0.407, macroRecall=0.379] for trend 50 200 multi class

		Score mScore = new Score(EScoreType.MACRO);

		for (int i = 0; i < instances.size(); i++) {
			System.out.println("PROGRESS: " + i);
			List<Instance> trainingInstances = new ArrayList<>();
			List<Instance> testInstances = new ArrayList<>();

			for (int j = 0; j < instances.size(); j++) {

				if (i == j)
					testInstances.add(instances.get(j));
				else
					trainingInstances.add(instances.get(j));
			}
			FastTextSentenceClassification t = new FastTextSentenceClassification(type, trainingInstances);

			Score s = t.score(testInstances).toMacro();
			System.out.println(s);
			mScore.add(s);
		}
		return mScore;
	}

	final public int numOfTrainingDuplicates = 1;
	private JFastText jft;

	public FastTextSentenceClassification(EntityType type, List<Instance> trainingInstances) throws IOException {
		this.type = type;
		numberOfEpochs = 50;
		numberOfDimensions = 200;
		binaryClassification = false;
		final String trainingDataFileName = "fasttext/resources/data/" + type.name + "train_labeled_data.txt";

		List<FastTextInstance> trainData = buildTrainingData(trainingInstances, trainingDataFileName);

		for (Iterator<FastTextInstance> iterator = trainData.iterator(); iterator.hasNext();) {
			FastTextInstance fastTextInstance = (FastTextInstance) iterator.next();

			if (fastTextInstance.section != ESection.RESULTS)
				iterator.remove();

		}

		System.out.println("trainData.size(): " + trainData.size());
		this.keyTerms = KeyTermExtractor.getKeyTerms(trainingInstances);

		jft = new JFastText();
		String modelName =
				"pretrained_"+
				type.name + "_" + binaryClassification + "_" + numberOfDimensions + "_" + numberOfEpochs
						+ "_supervised.model";
		String preTrainedvec = "wordvector/w2v.vec";
		jft.runCmd(new String[] { "supervised", "-input", trainingDataFileName, "-output",
				"fasttext/resources/models/" + modelName, "-epoch", numberOfEpochs + "",

				"-pretrainedVectors", preTrainedvec,
//				"-wordNgrams" ,"1",

				"-dim", numberOfDimensions + "" });

		jft.loadModel("fasttext/resources/models/" + modelName + ".bin");

//		Score scoreTrain = evaluate(jft, getLabledDocuments(trainignInstances, 1));
//		System.out.println("train: " + scoreTrain);

//		evaluate(testInstances);
		System.out.println(modelName);

	}

	private Score score(List<Instance> testInstances) {
		List<FastTextInstance> testData = getLabledDocuments(testInstances, 1);
//		List<String> testData = Files.readAllLines(new File("src/test/resources/data/test_labeled_data.txt").toPath());
		System.out.println("num of test data  = " + testData.size());
		List<FastTextPrediction> predictions = predict(testData);

//		writeNerlas(predictions);

//		for (FastTextPrediction fastTextPrediction : predictions) {
//			System.out.println(fastTextPrediction);
//		}

		Score scoreTest = evaluate(predictions);

//		System.out.println("test: " + scoreTest);

		return scoreTest;
		// for (int k = 1; k <= 10; k++) {
//
//			System.out.println("Test evaluate at " + k + "\t" + evaluateAtK(jft, testData, k));
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

	private List<FastTextInstance> buildTrainingData(List<Instance> instances, final String trainingDataFileName)
			throws FileNotFoundException {
		File trainingDataFile = new File(trainingDataFileName);
		PrintStream ps = new PrintStream(trainingDataFile);

		List<FastTextInstance> trainData = getLabledDocuments(instances, numOfTrainingDuplicates);

		trainData.stream().map(i -> i.toSimpleString()).forEach(ps::println);
		ps.flush();
		ps.close();
		return trainData;
	}

	public List<FastTextPrediction> predict(List<FastTextInstance> data) {

		List<FastTextPrediction> predictions = new ArrayList<>();
		for (FastTextInstance fti : data) {

			if (fti.section != ESection.RESULTS)
				continue;

			boolean containsKeyterm = false;
			String sentence = fti.text;

			String text = fti.text;
			ProbLabel probLabel = jft.predictProba(text);

			String firstPredLabel = probLabel != null ? probLabel.label : NO_LABEL;

			for (String keyTerm : keyTerms) {

				if (sentence.contains(keyTerm)) {
					containsKeyterm = true;
					break;
				}
			}

			if (!containsKeyterm) {
				firstPredLabel = NO_LABEL;
			}

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

//			final String predLabel = !fti.label.equals(NO_LABEL) ? "LAB" : NO_LABEL;
//			final String goldLabel = !fti.fastTextInstance.goldLabel.equals(NO_LABEL) ? "LAB" : NO_LABEL;

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
		for (AbstractAnnotation a : instance.getGoldAnnotations().getAbstractAnnotations()) {

			if (a.isInstanceOfDocumentLinkedAnnotation()) {
				ims.add(a.asInstanceOfDocumentLinkedAnnotation());
			} else {

				for (EntityTemplate result : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {
					Result r = new Result(result);
					EntityTemplate invM = r.getInvestigationMethod();
					if (invM != null)
						if (invM.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
							ims.add(invM.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation());

				}
			}
		}
		return ims;

	}

	private List<DocumentLinkedAnnotation> extractTrends(Instance instance) {

		List<DocumentLinkedAnnotation> ts = new ArrayList<>();

		for (AbstractAnnotation a : instance.getGoldAnnotations().getAbstractAnnotations()) {
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
				ts.add(a.asInstanceOfDocumentLinkedAnnotation());
			}

		}
		return ts;
	}

}
