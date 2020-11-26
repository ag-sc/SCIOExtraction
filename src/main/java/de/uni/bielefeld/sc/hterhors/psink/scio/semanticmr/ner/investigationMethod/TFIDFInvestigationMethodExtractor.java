package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.investigationMethod;

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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.fasttext.FastTextSentenceClassification;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.fasttext.FastTextSentenceClassification.FastTextPrediction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Result;






public class TFIDFInvestigationMethodExtractor {
//NUR FAST TEXT (andere Klasse)	
//	Score [macroF1=0.207, macroPrecision=0.167, macroRecall=0.272, macroAddCounter=10]

//	Ohen fasttext ohne key terms
//	90/10 score = Score [macroF1=0.049, macroPrecision=0.026, macroRecall=0.523, macroAddCounter=10]

	// Mit key Terms
//	90/10 score = Score [macroF1=0.072, macroPrecision=0.039, macroRecall=0.520, macroAddCounter=10]

//	Mit Keyterms + fast text () without pre trained)
//	90/10 score = Score [macroF1=0.088, macroPrecision=0.048, macroRecall=0.538, macroAddCounter=10]
//	90/10 score = Score [getF1()=0.088, getPrecision()=0.048, getRecall()=0.525, tp=475, fp=9471, fn=430, tn=0]
//	90/10 score = Score [getF1()=0.088, getPrecision()=0.048, getRecall()=0.531, tp=476, fp=9421, fn=420, tn=0]

//	Ohne Keyterms + fast text () without pre trained)
//	90/10 score=Score[macroF1=0.074,macroPrecision=0.040,macroRecall=0.535, macroAddCounter=10]

	public static void main(String[] args) throws IOException {

		SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(DataStructureLoader.loadNERDataStructureReader("InvestigationMethod"))
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
				NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.investigationMethod),
				corpusDistributor);

//		TFIDFInvestigationMethodExtractor t = new TFIDFInvestigationMethodExtractor(
//				instanceProvider.getRedistributedTrainingInstances());

//		with fast text as sentence classification
//		Score [macroF1=0.250, macroPrecision=0.150, macroRecall=0.750]
//		with out fast text as sentence classification
//		Score [macroF1=0.206, macroPrecision=0.119, macroRecall=0.778]
//		with out fast text as sentence classification and no key terms
//		Score [macroF1=0.177, macroPrecision=0.100, macroRecall=0.778]

//		Score s = t.evaluate(instanceProvider.getRedistributedDevelopmentInstances());
//		t.evaluateMultiSectionAppearance(instanceProvider.getRedistributedDevelopmentInstances());
//		System.out.println(s.toMacro());

		boolean binary = false;
		// Score score =
		// TFIDFInvestigationMethodExtractor.leaveOneOutEval(instanceProvider.getInstances());
//		System.out.println("leave one out score = " + score);
		Score score = TFIDFInvestigationMethodExtractor.tenRandom9010Split(binary, instanceProvider.getInstances(),
				1000L);
		System.out.println("90/10 score = " + score);

	}

	private static Score tenRandom9010Split(boolean binary, List<Instance> instances, long randomSeed)
			throws IOException {

		Score mScore = new Score(EScoreType.MICRO);

		Random rand = new Random(randomSeed);

		for (int i = 0; i < 10; i++) {
			System.out.println("PROGRESS: " + i);

			Collections.shuffle(instances, rand);

			final int x = (int) (((double) instances.size() / 100D) * 90D);

			List<Instance> trainingInstances = instances.subList(0, x);
			List<Instance> testInstances = instances.subList(x, instances.size());

			TFIDFInvestigationMethodExtractor t = new TFIDFInvestigationMethodExtractor(binary, trainingInstances);
			Score s = t.evaluate(testInstances);
			System.out.println(s);
			mScore.add(s);
		}

		return mScore;
	}

	private static Score leaveOneOutEval(List<Instance> instances, boolean binary) throws IOException {

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
			TFIDFInvestigationMethodExtractor t = new TFIDFInvestigationMethodExtractor(binary, trainingInstances);
			Score s = t.evaluate(testInstances).toMacro();
			System.out.println(s);
			mScore.add(s);
		}
		return mScore;
	}

	Map<String, Map<String, Double>> tfidfs;
	FastTextSentenceClassification t;

	boolean includeFastText = false;

	public TFIDFInvestigationMethodExtractor(boolean binary, List<Instance> trainingInstances) throws IOException {
		if (includeFastText)
			t = new FastTextSentenceClassification("TFIDFInvExtractor", binary, SCIOEntityTypes.investigationMethod,
					trainingInstances,false);

		Map<String, List<String>> documents = new HashMap<>();
		Set<String> additionalStopWords = new HashSet<>(
				Arrays.asList("either", "number", "group", "groups", "numbers"));

		Map<EntityType, Integer> frequency = new HashMap<>();
		for (Instance trainInstance : trainingInstances) {

			for (DocumentLinkedAnnotation invM : extractInvestigationMethods(trainInstance)) {
				if (!invM.getEntityType().isLeafEntityType())
					continue;
				frequency.put(invM.getEntityType(), frequency.getOrDefault(invM.getEntityType(), 0) + 1);
				documents.putIfAbsent(invM.getEntityType().name, new ArrayList<>());

				documents.get(invM.getEntityType().name)
						//
						.addAll(invM.relatedTokens.stream()
//				.addAll(invM.relatedTokens.stream()
//								.addAll(trainInstance.getDocument().getSentenceByIndex(invM.getSentenceIndex()).stream()
								.filter(t -> !(t.isPunctuation() || t.isStopWord() || t.isNumber()
										|| additionalStopWords.contains(t.getText())))
								.map(t -> t.getText()).collect(Collectors.toList()));

//				documents.get(invM.getEntityType().name)
//				.addAll(invM.relatedTokens.stream()
//						.filter(t -> !(t.isPunctuation() || t.isStopWord() || t.isNumber()
//								|| additionalStopWords.contains(t.getText())))
//						.map(t -> t.getText()).collect(Collectors.toList()));

//				List<String> tokens = invM.relatedTokens.stream()
//						.filter(t -> !(t.isPunctuation() || t.isStopWord() || t.isNumber()
//								|| additionalStopWords.contains(t.getText())))
//						.map(t -> t.getText()).collect(Collectors.toList());
//
//				for (int i = 0; i < tokens.size() - 1; i++) {
//					for (int j = i + 1; j < tokens.size(); j++) {
//						documents.get(invM.getEntityType().name).add(tokens.get(i) + "\t" + tokens.get(j));
//					}
//				}

			}

		}
//		keyTerms = KeyTermExtractor.getKeyTerms(trainingInstances);
		keyTerms = new HashSet<>();
//System.out.println(keyTerms);

		tfidfs = TFIDF.getTFIDFs(documents, false);

//		for (Entry<EntityType, Integer> string : frequency.entrySet()) {
//			System.out.println(string.getKey().name + "\t" + string.getValue());
//		}
//
//		System.out.println("Calc tfidf...");
//		for (Entry<String, Map<String, Double>> e : tfidfs.entrySet()) {
//
//			for (Entry<String, Double> e2 : e.getValue().entrySet()) {
//
//				System.out.println(e.getKey() + "\t" + e2.getKey() + "\t" + e2.getValue());
//			}
//		}
//		System.exit(1);
	}

	Set<String> keyTerms;

	private List<DocumentLinkedAnnotation> extractInvestigationMethods(Instance trainInstance) {

		List<DocumentLinkedAnnotation> ims = new ArrayList<>();
		for (AbstractAnnotation a : trainInstance.getGoldAnnotations().getAbstractAnnotations()) {

			if (a.isInstanceOfDocumentLinkedAnnotation()) {
				ims.add(a.asInstanceOfDocumentLinkedAnnotation());
			} else {

				for (EntityTemplate result : trainInstance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {
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

//	Bild unterschriften

	public Score evaluate(List<Instance> instances) {
//		List<Instance> instances = instanceProvider.getRedistributedTrainingInstances();
//		new HashSet<>();//
		List<FastTextPrediction> testData = new ArrayList<>();
		if (includeFastText)
			testData = t.predict(t.getLabledDocuments(instances, 1));

		Score score = new Score();
		for (Instance testInstance : instances) {
			Set<Integer> skipSentences = new HashSet<>();
			if (includeFastText)
				skipSentences = testData.stream()
						.filter(a -> a.fastTextInstance.instance.getName().equals(testInstance.getName()))
						.filter(a -> a.label.equals(FastTextSentenceClassification.NO_LABEL))
						.map(a -> a.fastTextInstance.sentenceIndex).collect(Collectors.toSet());
//			System.out.println("Name " + testInstance.getName());
//			Set<EntityTypeAnnotation> gold = testInstance.getGoldAnnotations().getAnnotations().stream()
//					.map(a -> AnnotationBuilder.toAnnotation(a.getEntityType())).collect(Collectors.toSet());
//			Set<EntityTypeAnnotation> predicted = new HashSet<>();

			AutomatedSectionifcation sec = AutomatedSectionifcation.getInstance(testInstance);
			for (int sentenceIndex = 0; sentenceIndex < testInstance.getDocument()
					.getNumberOfSentences(); sentenceIndex++) {

				final int sentenceIndexF = sentenceIndex;
				Set<EntityTypeAnnotation> gold = testInstance.getGoldAnnotations().getAnnotations().stream()
						.filter(a -> sentenceIndexF == a.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex())
						.map(a -> AnnotationBuilder.toAnnotation(a.getEntityType())).collect(Collectors.toSet());

				boolean ignore = false;

				if (includeFastText && skipSentences.contains(new Integer(sentenceIndex)))
					ignore = true;

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
//					ignore = true;

				Set<EntityTypeAnnotation> predicted = new HashSet<>();
				if (!ignore)

//				if (includeFastText && skipSentences.contains(new Integer(sentenceIndex)))
//					continue;
////				
//				if (sec.getSection(sentenceIndex) != ESection.RESULTS)
//					continue;
//
//				boolean containsKeyterm = false;
//				String sentence = testInstance.getDocument().getContentOfSentence(sentenceIndex);
//
//				if (!keyTerms.isEmpty()) {
//					for (String keyTerm : keyTerms) {
//
//						if (sentence.contains(keyTerm)) {
//							containsKeyterm = true;
//							break;
//						}
//					}
//
//					if (!containsKeyterm)
//						continue;
//				}
//

//				Set<EntityTypeAnnotation>
					predicted = getInvestigationMethods(testInstance.getDocument(),
							testInstance.getDocument().getSentenceByIndex(sentenceIndex)).stream()
									.map(s -> AnnotationBuilder.toAnnotation(s.getEntityType()))
									.collect(Collectors.toSet());

//				System.out.println("----------------");
//				for (EntityTypeAnnotation eta : gold) {
//					System.out.println(eta.getEntityType().name);
//				}
//				System.out.println("----------------");
//				for (EntityTypeAnnotation eta : predicted) {
//					System.out.println(eta.getEntityType().name);
//				}
//				System.out.println(sentence);

				NerlaEvaluator eval = new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE);
				Score s = eval.prf1(gold, predicted);
//				System.out.println(s);
				score.add(s);

			}
		}
//		System.out.println(score);
//		System.out.println("Calc tfidf...");
//		for (Entry<String, Map<String, Double>> e : TFIDF.getTFIDFs(documents).entrySet()) {
//
//			for (Entry<String, Double> e2 : e.getValue().entrySet()) {
//
//				System.out.println(e.getKey() + "\t" + e2.getKey() + "\t" + e2.getValue());
//			}
//		}
		return score;
	}

	public void evaluateMultiSectionAppearance(List<Instance> instances) {
//		List<Instance> instances = instanceProvider.getRedistributedTrainingInstances();
//		new HashSet<>();//
		Set<String> keyTerms = KeyTermExtractor.getKeyTerms(instances);

		Score score = new Score();
		for (Instance testInstance : instances) {

			Map<Integer, Set<EntityTypeAnnotation>> collection = new HashMap<>();

//			System.out.println("Name " + testInstance.getName());
//			Set<EntityTypeAnnotation> gold = testInstance.getGoldAnnotations().getAnnotations().stream()
//					.map(a -> AnnotationBuilder.toAnnotation(a.getEntityType())).collect(Collectors.toSet());
//			Set<EntityTypeAnnotation> predicted = new HashSet<>();

			AutomatedSectionifcation sec = AutomatedSectionifcation.getInstance(testInstance);
			Set<EntityTypeAnnotation> resultGoldSectionSet = new HashSet<>();

			for (int sentenceIndex = 0; sentenceIndex < testInstance.getDocument()
					.getNumberOfSentences(); sentenceIndex++) {

				if (sec.getSection(sentenceIndex) != ESection.RESULTS)
					continue;

				boolean containsKeyterm = false;
				String sentence = testInstance.getDocument().getContentOfSentence(sentenceIndex);
				if (!keyTerms.isEmpty()) {
					for (String keyTerm : keyTerms) {

						if (sentence.contains(keyTerm)) {
							containsKeyterm = true;
							break;
						}
					}

					if (!containsKeyterm)
						continue;
				}

				final int sentenceIndexG = sentenceIndex;

				Set<DocumentLinkedAnnotation> predicted = testInstance.getGoldAnnotations().getAnnotations().stream()
						.filter(a -> sentenceIndexG == a.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex())
						.map(a -> a.asInstanceOfDocumentLinkedAnnotation()).collect(Collectors.toSet());
				resultGoldSectionSet.addAll(predicted);
			}

//			System.out.println("---------------");
//			System.out.println("ResultSet GOLD");
//			for (EntityTypeAnnotation string : resultGoldSectionSet) {
//				System.out.println(string.toPrettyString());
//			}

			Set<EntityTypeAnnotation> resultSectionSet = new HashSet<>();

			for (int sentenceIndex = 0; sentenceIndex < testInstance.getDocument()
					.getNumberOfSentences(); sentenceIndex++) {

				if (sec.getSection(sentenceIndex) != ESection.RESULTS)
					continue;

				boolean containsKeyterm = false;
				String sentence = testInstance.getDocument().getContentOfSentence(sentenceIndex);
				if (!keyTerms.isEmpty()) {
					for (String keyTerm : keyTerms) {

						if (sentence.contains(keyTerm)) {
							containsKeyterm = true;
							break;
						}
					}

					if (!containsKeyterm)
						continue;
				}

				Set<EntityTypeAnnotation> predicted = getInvestigationMethods(testInstance.getDocument(),
						testInstance.getDocument().getSentenceByIndex(sentenceIndex)).stream()
								.map(s -> AnnotationBuilder.toAnnotation(s.getEntityType()))
								.collect(Collectors.toSet());
				collection.put(sentenceIndex, predicted);
				resultSectionSet.addAll(predicted);
			}
//			System.out.println("---------------");
//			System.out.println("ResultSet");
//			for (EntityTypeAnnotation string : resultSectionSet) {
//				System.out.println(string.toPrettyString());
//			}

			Set<EntityTypeAnnotation> retainSetMethods = new HashSet<>();
			for (int sentenceIndex = 0; sentenceIndex < testInstance.getDocument()
					.getNumberOfSentences(); sentenceIndex++) {

				if (sec.getSection(sentenceIndex) != ESection.METHODS)
					continue;

				boolean containsKeyterm = false;
				String sentence = testInstance.getDocument().getContentOfSentence(sentenceIndex);
				if (!keyTerms.isEmpty()) {
					for (String keyTerm : keyTerms) {

						if (sentence.contains(keyTerm)) {
							containsKeyterm = true;
							break;
						}
					}

					if (!containsKeyterm)
						continue;
				}
				retainSetMethods.addAll(getInvestigationMethods(testInstance.getDocument(),
						testInstance.getDocument().getSentenceByIndex(sentenceIndex)).stream()
								.map(s -> AnnotationBuilder.toAnnotation(s.getEntityType()))
								.collect(Collectors.toSet()));
			}
//			System.out.println("---------------");
//			System.out.println("RetainSet Methods");
//			for (EntityTypeAnnotation string : retainSetMethods) {
//				System.out.println(string.toPrettyString());
//			}
			Set<EntityTypeAnnotation> retainSetAbstract = new HashSet<>();
			for (int sentenceIndex = 0; sentenceIndex < testInstance.getDocument()
					.getNumberOfSentences(); sentenceIndex++) {

				if (sec.getSection(sentenceIndex) != ESection.ABSTRACT)
					continue;

				boolean containsKeyterm = false;
				String sentence = testInstance.getDocument().getContentOfSentence(sentenceIndex);
				if (!keyTerms.isEmpty()) {
					for (String keyTerm : keyTerms) {

						if (sentence.contains(keyTerm)) {
							containsKeyterm = true;
							break;
						}
					}

					if (!containsKeyterm)
						continue;
				}
				retainSetAbstract.addAll(getInvestigationMethods(testInstance.getDocument(),
						testInstance.getDocument().getSentenceByIndex(sentenceIndex)).stream()
								.map(s -> AnnotationBuilder.toAnnotation(s.getEntityType()))
								.collect(Collectors.toSet()));
			}
//			System.out.println("---------------");
//			System.out.println("RetainSet Abstract");
//			for (EntityTypeAnnotation string : retainSetAbstract) {
//				System.out.println(string.toPrettyString());
//			}

			Set<EntityTypeAnnotation> remaining = new HashSet<>();
			for (EntityTypeAnnotation eta : resultSectionSet) {

				if (!retainSetAbstract.contains(eta))
					continue;

				if (!retainSetMethods.contains(eta))
					continue;

				remaining.add(eta);

			}
//			System.out.println("---------------");
//			System.out.println("Remaining Result");
//			for (EntityTypeAnnotation string : remaining) {
//				System.out.println(string.toPrettyString());
//			}

//			Set<EntityTypeAnnotation> toConsider = new HashSet<>(
//					Arrays.asList(AnnotationBuilder.toAnnotation(EntityType.get("AxonalRegenerationTest"))));

//			System.out.println("--------------------------------------------------------------");
			for (Entry<Integer, Set<EntityTypeAnnotation>> string : collection.entrySet()) {
				String x = testInstance.getName() + "\t";
				Set<EntityTypeAnnotation> predicted = string.getValue();
				predicted.retainAll(remaining);
//				predicted.retainAll(toConsider);
				Set<EntityTypeAnnotation> gold = testInstance.getGoldAnnotations().getAnnotations().stream()
						.filter(a -> string.getKey() == a.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex())
						.map(a -> AnnotationBuilder.toAnnotation(a.getEntityType())).collect(Collectors.toSet());
//				gold.retainAll(toConsider);
				x += string.getKey() + "\t";
//				System.out.println(string.getKey());
//				System.out.println("--------GOLD--------");
				for (EntityTypeAnnotation eta : gold) {
//					System.out.print(eta.getEntityType().name+" ");
					x += eta.getEntityType().name + " ";
				}
				if (!gold.isEmpty())
					x = x.trim();
				x += "\t";
//				System.out.println("--------PRED--------");
				for (EntityTypeAnnotation eta : predicted) {
//					System.out.println(eta.getEntityType().name);
					x += eta.getEntityType().name + " ";
				}
				if (!predicted.isEmpty())
					x = x.trim();
				x += "\t";
//				System.out.println("----------------");
				String sentence = testInstance.getDocument().getContentOfSentence(string.getKey());
				x += sentence;
//				System.out.println(sentence);

				NerlaEvaluator eval = new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE);
				Score s = eval.prf1(gold, predicted);
//				System.out.println(s);
				score.add(s);
//				System.out.println(x);
			}
//		System.out.println("Calc tfidf...");
//		for (Entry<String, Map<String, Double>> e : TFIDF.getTFIDFs(documents).entrySet()) {
//
//			for (Entry<String, Double> e2 : e.getValue().entrySet()) {
//
//				System.out.println(e.getKey() + "\t" + e2.getKey() + "\t" + e2.getValue());
//			}
		}
		System.out.println(score);
	}

	private Set<DocumentLinkedAnnotation> getInvestigationMethods(Document doc, List<DocumentToken> sentence) {

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
				predicted.add(p.invM);
			}
//					break;
		}
		return predicted;
	}

	public Set<DocumentLinkedAnnotation> getInvestigationMethodForSentence(Document doc, List<DocumentToken> sentence) {
		return getInvestigationMethods(doc, sentence);
	}

	class P implements Comparable<P> {
		public final DocumentLinkedAnnotation invM;
		public final Double val;

		public P(DocumentLinkedAnnotation invM, Double val) {
			super();
			this.invM = invM;
			this.val = val;
		}

		@Override
		public int compareTo(P o) {
			return Double.compare(o.val, val);
		}

		@Override
		public String toString() {
			return "P [invM=" + invM.toPrettyString() + ", val=" + val + "]";
		}

	}

	protected File getInstanceDirectory() {
		return NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.investigationMethod);
	}

}
