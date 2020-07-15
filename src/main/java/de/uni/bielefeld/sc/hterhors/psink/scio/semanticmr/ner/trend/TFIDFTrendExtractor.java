package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.trend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

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
import de.hterhors.semanticmr.crf.structure.annotations.EntityTypeAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.eval.NerlaEvaluator;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation.ESection;
import de.hterhors.semanticmr.tools.KeyTermExtractor;
import de.hterhors.semanticmr.tools.TFIDF;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.fasttext.FastTextSentenceClassification;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.fasttext.FastTextSentenceClassification.FastTextPrediction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.trend.TrendChunker.TermIndexPair;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Result;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Trend;

public class TFIDFTrendExtractor {
//	Ohne Fast Text
//			Score [macroF1=0.204, macroPrecision=0.120, macroRecall=0.700, macroAddCounter=1]
//			Score [macroF1=0.182, macroPrecision=0.105, macroRecall=0.685, macroAddCounter=1]
//			Score [macroF1=0.215, macroPrecision=0.125, macroRecall=0.754, macroAddCounter=1]
//			Score [macroF1=0.173, macroPrecision=0.098, macroRecall=0.703, macroAddCounter=1]
//			Score [macroF1=0.186, macroPrecision=0.107, macroRecall=0.707, macroAddCounter=1]
//			Score [macroF1=0.171, macroPrecision=0.097, macroRecall=0.719, macroAddCounter=1]
//			Score [macroF1=0.219, macroPrecision=0.130, macroRecall=0.705, macroAddCounter=1]
//			Score [macroF1=0.143, macroPrecision=0.080, macroRecall=0.670, macroAddCounter=1]
//			Score [macroF1=0.226, macroPrecision=0.134, macroRecall=0.712, macroAddCounter=1]
//			Score [macroF1=0.190, macroPrecision=0.110, macroRecall=0.693, macroAddCounter=1]
//			90/10 one out: Score [macroF1=0.191, macroPrecision=0.111, macroRecall=0.705, macroAddCounter=10]

//	Mit Fast Text
//	Score [macroF1=0.284, macroPrecision=0.177, macroRecall=0.706, macroAddCounter=1]
//	Score [macroF1=0.273, macroPrecision=0.170, macroRecall=0.694, macroAddCounter=1]
//	Score [macroF1=0.327, macroPrecision=0.208, macroRecall=0.761, macroAddCounter=1]
//	Score [macroF1=0.266, macroPrecision=0.164, macroRecall=0.703, macroAddCounter=1]
//	Score [macroF1=0.259, macroPrecision=0.158, macroRecall=0.707, macroAddCounter=1]
//	Score [macroF1=0.263, macroPrecision=0.161, macroRecall=0.721, macroAddCounter=1]
//	Score [macroF1=0.291, macroPrecision=0.183, macroRecall=0.706, macroAddCounter=1]
//	Score [macroF1=0.210, macroPrecision=0.124, macroRecall=0.669, macroAddCounter=1]
//	Score [macroF1=0.297, macroPrecision=0.186, macroRecall=0.733, macroAddCounter=1]
//	Score [macroF1=0.266, macroPrecision=0.164, macroRecall=0.691, macroAddCounter=1]
//	90/10 one out: Score [macroF1=0.274, macroPrecision=0.170, macroRecall=0.709, macroAddCounter=10]

	public static void main(String[] args) throws IOException {

		SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(DataStructureLoader.loadNERDataStructureReader("Trend"))
				/**
				 * Finally, we build the systems scope.
				 */
				.build();
		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
				.setSeed(1000L).setDevelopmentProportion(20).setCorpusSizeFraction(1F).build();
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.maxNumberOfAnnotations = 300;
		InstanceProvider.removeInstancesWithToManyAnnotations = false;

		InstanceProvider instanceProvider = new InstanceProvider(
				NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.trend), corpusDistributor);

//		without fast text as sentence prediciton
//		Score [getF1()=0.282, getPrecision()=0.174, getRecall()=0.736, tp=331, fp=1566, fn=119, tn=0]
//		Without fast text and without keyterms
//		Score [getF1()=0.233, getPrecision()=0.138, getRecall()=0.739, tp=346, fp=2153, fn=122, tn=0]

//		With fast text as sentence prediciton
//		Score [getF1()=0.325, getPrecision()=0.208, getRecall()=0.742, tp=310, fp=1179, fn=108, tn=0]

		boolean binary = false;
		// TFIDFTrendExtractor t = new
		// TFIDFTrendExtractor(instanceProvider.getRedistributedTrainingInstances());
//		Score sAll = t.evaluate(instanceProvider.getRedistributedDevelopmentInstances());
//		System.out.println(sAll);
//		 Score s =
//		 TFIDFTrendExtractor.leaveOneOutEval(instanceProvider.getInstances());
//		System.out.println("leave one out: " + s);
		Score s = TFIDFTrendExtractor.tenRandom9010Split(binary, instanceProvider.getInstances(), 1000L);
		System.out.println("90/10 one out: " + s);
//		90/10 one out: Score [macroF1=0.199, macroPrecision=0.116, macroRecall=0.680, macroAddCounter=10]

	}

	private static Score tenRandom9010Split(boolean binary, List<Instance> instances, long randomSeed)
			throws IOException {

		Score mScore = new Score(EScoreType.MACRO);

		Random rand = new Random(randomSeed);

		for (int i = 0; i < 10; i++) {
			System.out.println("PROGRESS: " + i);

			Collections.shuffle(instances, rand);

			final int x = (int) (((double) instances.size() / 100D) * 90D);

			List<Instance> trainingInstances = instances.subList(0, x);
			List<Instance> testInstances = instances.subList(x, instances.size());

			TFIDFTrendExtractor t = new TFIDFTrendExtractor(binary, trainingInstances);
			Score s = t.evaluate(testInstances).toMacro();
			System.out.println(s);
			mScore.add(s);
		}

		return mScore;
	}

	private static Score leaveOneOutEval(boolean binary, List<Instance> instances) throws IOException {
//		Ohne fast text as sentence detection
//		leave one out: Score [macroF1=0.228, macroPrecision=0.138, macroRecall=0.675]
//		mitfast text as sentence detection
//		leave one out: Score [macroF1=0.294, macroPrecision=0.188, macroRecall=0.683]

		Score mScore = new Score(EScoreType.MACRO);

		for (int i = 0; i < instances.size(); i++) {

			List<Instance> trainingInstances = new ArrayList<>();
			List<Instance> testInstances = new ArrayList<>();

			for (int j = 0; j < instances.size(); j++) {

				if (i == j)
					testInstances.add(instances.get(j));
				else
					trainingInstances.add(instances.get(j));
			}
			TFIDFTrendExtractor t = new TFIDFTrendExtractor(binary, trainingInstances);
			Score s = t.evaluate(testInstances).toMacro();
			System.out.println(s);
			mScore.add(s);
		}
		return mScore;
	}

	Set<String> additionalStopWords;
	Map<String, Map<String, Double>> tfidfs;
	boolean useFastText = false;

	FastTextSentenceClassification t;

	public TFIDFTrendExtractor(boolean binary, List<Instance> trainingInstances) throws IOException {

		additionalStopWords = new HashSet<>(
				Arrays.asList("rats", "either", "number", "group", "groups", "numbers", "treatment", "respectively"));
		Map<String, List<String>> documents = new HashMap<>();
		Map<String, Integer> count = new HashMap<>();
		if (useFastText)
			t = new FastTextSentenceClassification("TFIDFtrendExtractor",binary, SCIOEntityTypes.trend, trainingInstances);

		for (Instance trainInstance : trainingInstances) {

			for (DocumentLinkedAnnotation invM : extractTrends(trainInstance)) {

//				System.out.println(invM.toPrettyString());
				if (invM.getEntityType() == EntityType.get("AlphaSignificanceNiveau"))
					continue;
				if (invM.getEntityType() == EntityType.get("RepeatedMeasureTrend"))
					continue;

				List<String> tokens = invM.relatedTokens.stream()
						.filter(t -> !(t.isPunctuation() || t.isStopWord() || t.isNumber()
								|| additionalStopWords.contains(t.getText())))
						.map(t -> t.getText()).collect(Collectors.toList());
				documents.putIfAbsent(invM.getEntityType().name, new ArrayList<>());
				documents.get(invM.getEntityType().name).addAll(tokens);
//

				for (String string : tokens) {
					count.put(string, count.getOrDefault(string, 0) + 1);
				}

//				for (int i = 0; i < tokens.size() - 1; i++) {
//					for (int j = i + 1; j < tokens.size(); j++) {
//						documents.get(invM.getEntityType().name).add(tokens.get(i) + "\t" + tokens.get(j));
//					}
//				}

			}

		}

//		Score [getF1()=0.223, getPrecision()=0.178, getRecall()=0.299, tp=132, fp=611, fn=310, tn=0]
//		System.out.println("Calc tfidf...");
//		for (Entry<String, Map<String, Double>> e : TFIDF.getTFIDFs(documents).entrySet()) {
//
//			for (Entry<String, Double> e2 : e.getValue().entrySet()) {
//
//				System.out.println(e.getKey() + "\t" + e2.getKey() + "\t" + e2.getValue());
//			}
//		}
//		System.exit(1);

		tfidfs = TFIDF.getTFIDFs(documents, true);
		keyTerms = new HashSet<>();
//		keyTerms = KeyTermExtractor.getKeyTerms(trainingInstances);
//		List<Instance> instances = instanceProvider.getRedistributedDevelopmentInstances();

	}

	private List<DocumentLinkedAnnotation> extractTrends(Instance trainInstance) {

		List<DocumentLinkedAnnotation> ts = new ArrayList<>();

		for (AbstractAnnotation a : trainInstance.getGoldAnnotations().getAbstractAnnotations()) {
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

	private Set<String> keyTerms;

	public Score evaluate(List<Instance> instances) {
		List<FastTextPrediction> testData = new ArrayList<>();

		if (useFastText)
			testData = t.predict(t.getLabledDocuments(instances, 1));

		Score score = new Score();
		for (Instance testInstance : instances) {
			Set<Integer> skipSentences = new HashSet<>();
			if (useFastText) {
				skipSentences = testData.stream()
						.filter(a -> a.fastTextInstance.instance.getName().equals(testInstance.getName()))
						.filter(a -> a.label.equals(FastTextSentenceClassification.NO_LABEL))
						.map(a -> a.fastTextInstance.sentenceIndex).collect(Collectors.toSet());
			}

//			System.out.println("Name " + testInstance.getName());
//			Set<EntityTypeAnnotation> gold = testInstance.getGoldAnnotations().getAnnotations().stream()
//					.map(a -> AnnotationBuilder.toAnnotation(a.getEntityType())).collect(Collectors.toSet());
//			Set<EntityTypeAnnotation> predicted = new HashSet<>();

			AutomatedSectionifcation sec = AutomatedSectionifcation.getInstance(testInstance);
			for (int sentenceIndex = 0; sentenceIndex < testInstance.getDocument()
					.getNumberOfSentences(); sentenceIndex++) {

				if (useFastText && skipSentences.contains(new Integer(sentenceIndex)))
					continue;

				if (sec.getSection(sentenceIndex) != ESection.RESULTS)
					continue;

				boolean containsKeyterm = false;
				String sentence = testInstance.getDocument().getContentOfSentence(sentenceIndex);

				for (String keyTerm : keyTerms) {

					if (sentence.contains(keyTerm)) {
						containsKeyterm = true;
						break;
					}
				}

//				if (!containsKeyterm)
//					continue;

				final int sentenceIndexF = sentenceIndex;

				Set<EntityTypeAnnotation> gold = testInstance.getGoldAnnotations().getAnnotations().stream()
						.filter(a -> sentenceIndexF == a.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex())
						.map(a -> AnnotationBuilder.toAnnotation(a.getEntityType())).collect(Collectors.toSet());
				Set<EntityTypeAnnotation> predicted = getForSentence(testInstance.getDocument(),
						testInstance.getDocument().getSentenceByIndex(sentenceIndexF)).stream()
								.map(s -> AnnotationBuilder.toAnnotation(s.getEntityType()))
								.collect(Collectors.toSet());

//				System.out.println("--------GOLD--------");
//				for (EntityTypeAnnotation eta : gold) {
//					System.out.println(eta.getEntityType().name);
//				}
//				System.out.println("---------PRED-------");
//				for (EntityTypeAnnotation eta : predicted) {
//					System.out.println(eta.getEntityType().name);
//				}
//				System.out.println(sentence);

				NerlaEvaluator eval = new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE);
				Score s = eval.prf1(gold, predicted);
//				System.out.println("Partial Score = " + s);
				score.add(s);

//				System.out.println("Overall Score = " + score);
			}
		}
//		System.out.println(score);
		return score;
	}

	private Set<DocumentLinkedAnnotation> getForSentence(Document doc, List<DocumentToken> sentence) {

		Set<DocumentLinkedAnnotation> predicted = new HashSet<>();
		List<P> ps = new ArrayList<>();

		for (String etn : tfidfs.keySet()) {

			double value = 0D;

			double maxTokenImpact = 0;
			DocumentToken maxImpactToken = null;

			for (DocumentToken token : sentence) {

				double val = tfidfs.get(etn).getOrDefault(token.getText(), 0D);

				if (val > maxTokenImpact) {
					maxTokenImpact = val;
					maxImpactToken = token;
				}

				value += val;

//				if (tfidfs.get(etn).containsKey(token.getText()))
//					System.out.println(
//							etn + "\t" + token.getText() + "\t" + tfidfs.get(etn).getOrDefault(token.getText(), 0D));
			}
//					List<String> tokens = testInstance.getDocument().getSentenceByIndex(sentenceIndex).stream()
//							.filter(t -> !(t.isPunctuation() || t.isStopWord() || t.isNumber()
//									|| additionalStopWords.contains(t.getText())))
//							.map(t -> t.getText()).collect(Collectors.toList());

//					for (int i = 0; i < tokens.size() - 1; i++) {
//						for (int j = i + 1; j < tokens.size(); j++) {
//							value += tfidfs.get(etn).getOrDefault(tokens.get(i) + "\t" + tokens.get(j), 0D);
//							if (tfidfs.get(etn).containsKey(tokens.get(i) + "\t" + tokens.get(j)))
//								System.out.println(etn + "\t" + tokens.get(i) + "\t" + tokens.get(j) + ""
//										+ tfidfs.get(etn).getOrDefault(tokens.get(i) + "\t" + tokens.get(j), 0D));
//
//						}
//					}

			if (maxImpactToken != null)
				ps.add(new P(AnnotationBuilder.toAnnotation(doc, etn, maxImpactToken.getText(),
						maxImpactToken.getDocCharOffset()), value));

		}
		Collections.sort(ps);

		for (P p : ps) {
//			System.out.println(p);
			if (p.val != 0 && p.val >= ps.get(0).val / 4) {
				predicted.add(p.trend);
			}
//					break;
		}
		return predicted;
	}

	private Set<DocumentLinkedAnnotation> getForChunks(Document document, List<DocumentToken> sentence) {

		Set<DocumentLinkedAnnotation> predicted = new HashSet<>();

		TrendChunker chuncker = new TrendChunker();
		List<TermIndexPair> list = chuncker
				.extractChunks(document.getContentOfSentence(sentence.get(0).getSentenceIndex()));

		List<P> ps = new ArrayList<>();

		for (TermIndexPair tip : list) {

//			System.out.println("Term = " + tip.term);

			double maxTokenImpact = 0;
			DocumentToken maxIMpactToken = null;

			double value = 0D;
			for (String etn : tfidfs.keySet()) {

				for (String token : tip.term.split(" ")) {

					if (additionalStopWords.contains(token))
						continue;

					double val = tfidfs.get(etn).getOrDefault(token, 0D);

					if (val > maxTokenImpact) {
						maxTokenImpact = val;

						for (DocumentToken dt : sentence) {
							if (dt.getText().equals(token))
								maxIMpactToken = dt;
						}

						if (maxIMpactToken == null)
							for (DocumentToken dt : sentence) {
								if (dt.getText().contains(token))
									maxIMpactToken = dt;
							}

					}
					value += val;

//					if (tfidfs.get(etn).containsKey(token))
//						System.out.println(etn + "\t" + token + "\t" + tfidfs.get(etn).getOrDefault(token, 0D));
				}
				if (maxIMpactToken != null)
					ps.add(new P(AnnotationBuilder.toAnnotation(document, etn, maxIMpactToken.getText(),
							maxIMpactToken.getDocCharOffset()), value));
			}

//			System.out.println("++++++++");

//					List<String> tokens = testInstance.getDocument().getSentenceByIndex(sentenceIndex).stream()
//							.filter(t -> !(t.isPunctuation() || t.isStopWord() || t.isNumber()
//									|| additionalStopWords.contains(t.getText())))
//							.map(t -> t.getText()).collect(Collectors.toList());

//					for (int i = 0; i < tokens.size() - 1; i++) {
//						for (int j = i + 1; j < tokens.size(); j++) {
//							value += tfidfs.get(etn).getOrDefault(tokens.get(i) + "\t" + tokens.get(j), 0D);
//							if (tfidfs.get(etn).containsKey(tokens.get(i) + "\t" + tokens.get(j)))
//								System.out.println(etn + "\t" + tokens.get(i) + "\t" + tokens.get(j) + ""
//										+ tfidfs.get(etn).getOrDefault(tokens.get(i) + "\t" + tokens.get(j), 0D));
//
//						}
//					}

		}

//		System.out.println("-----");
		Collections.sort(ps);

		for (P p : ps) {
//			System.out.println(p + "\t" + (p.val != 0 && p.val >= ps.get(0).val / 2));
			if (p.val != 0 && p.val >= ps.get(0).val / 4) {
				predicted.add(p.trend);
			}
		}
		return predicted;
	}

	public Set<DocumentLinkedAnnotation> getTrendsForSentence(Document document, List<DocumentToken> sentence) {
		return getForSentence(document, sentence);
	}

	class P implements Comparable<P> {
		public final DocumentLinkedAnnotation trend;
		public final Double val;

		public P(DocumentLinkedAnnotation trend, Double val) {
			super();
			this.trend = trend;
			this.val = val;
		}

		@Override
		public int compareTo(P o) {
			return Double.compare(o.val, val);
		}

		@Override
		public String toString() {
			return "P [invM=" + trend.toPrettyString() + ", val=" + val + "]";
		}

	}

}
