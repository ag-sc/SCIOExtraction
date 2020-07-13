package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.investigationMethod;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.print.Doc;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.fasttext.FastTextSentenceClassification;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.fasttext.FastTextSentenceClassification.FastTextPrediction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Result;

public class DictionaryInvestigationMethodExtractor {

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
				.setDevelopmentProportion(20).setCorpusSizeFraction(1F).build();
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.maxNumberOfAnnotations = 300;
		InstanceProvider.removeInstancesWithToManyAnnotations = false;

		InstanceProvider instanceProvider = new InstanceProvider(
				NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.investigationMethod),
				corpusDistributor);

		DictionaryInvestigationMethodExtractor t = new DictionaryInvestigationMethodExtractor(
				instanceProvider.getRedistributedTrainingInstances());

		t.evaluate(instanceProvider.getRedistributedDevelopmentInstances());
	}

	FastTextSentenceClassification t;
	boolean includeFastText = true;
//	Score [getF1()=0.092, getPrecision()=0.050, getRecall()=0.576, tp=118, fp=2244, fn=87, tn=0]

	public DictionaryInvestigationMethodExtractor(List<Instance> trainingInstances) throws IOException {

		if (includeFastText)
			t = new FastTextSentenceClassification(SCIOEntityTypes.investigationMethod, trainingInstances);

		for (Instance trainInstance : trainingInstances) {

			for (DocumentLinkedAnnotation invM : extractInvestigationMethods(trainInstance)) {

				dictionary.putIfAbsent(invM.getEntityType().name, new ArrayList<>());
				dictionary.get(invM.getEntityType().name).add(invM);

			}

		}

	}

	private Map<String, List<DocumentLinkedAnnotation>> dictionary = new HashMap<>();

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

	public void evaluate(List<Instance> instances) {
//		List<Instance> instances = instanceProvider.getRedistributedTrainingInstances();

		Set<String> keyTerms = KeyTermExtractor.getKeyTerms(instances);
		List<FastTextPrediction> testData = new ArrayList<>();
		if (includeFastText)
			testData = t.predict(t.getLabledDocuments(instances, 1));

		Score score = new Score();
		for (Instance testInstance : instances) {
			System.out.println("Name " + testInstance.getName());
//			Set<EntityTypeAnnotation> gold = testInstance.getGoldAnnotations().getAnnotations().stream()
//					.map(a -> AnnotationBuilder.toAnnotation(a.getEntityType())).collect(Collectors.toSet());
//			Set<EntityTypeAnnotation> predicted = new HashSet<>();

			Set<Integer> skipSentences = new HashSet<>();
			if (includeFastText)
				skipSentences = testData.stream()
						.filter(a -> a.fastTextInstance.instance.getName().equals(testInstance.getName()))
						.filter(a -> a.label.equals(FastTextSentenceClassification.NO_LABEL))
						.map(a -> a.fastTextInstance.sentenceIndex).collect(Collectors.toSet());
			AutomatedSectionifcation sec = AutomatedSectionifcation.getInstance(testInstance);
			for (int sentenceIndex = 0; sentenceIndex < testInstance.getDocument()
					.getNumberOfSentences(); sentenceIndex++) {

				if (includeFastText && skipSentences.contains(new Integer(sentenceIndex)))
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

//				Set<DocumentLinkedAnnotation> gold = testInstance.getGoldAnnotations().getAnnotations().stream()
//						.filter(a -> sentenceIndexF == a.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex())
//						.map(a -> AnnotationBuilder.toAnnotation(a.getEntityType())).collect(Collectors.toSet());
//				Set<DocumentLinkedAnnotation> predicted = labelInvestigationMethods(testInstance.getDocument(),
//						sentenceIndex).stream().map(s -> AnnotationBuilder.toAnnotation(s.getEntityType()))
//						.collect(Collectors.toSet());
				Set<DocumentLinkedAnnotation> gold = testInstance.getGoldAnnotations().getAnnotations().stream()
						.filter(a -> sentenceIndexF == a.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex())
						.map(a -> a.asInstanceOfDocumentLinkedAnnotation()).collect(Collectors.toSet());
				Set<DocumentLinkedAnnotation> predicted = labelInvestigationMethods(testInstance.getDocument(),
						sentenceIndex).stream().map(s -> s.asInstanceOfDocumentLinkedAnnotation())
								.collect(Collectors.toSet());

				System.out.println("------GOLD----------");
				for (DocumentLinkedAnnotation eta : gold) {
					System.out.println(eta.toPrettyString());
				}
				System.out.println("--------PRED --------");
				for (DocumentLinkedAnnotation eta : predicted) {
					System.out.println(eta.toPrettyString());
				}
				System.out.println(sentence);

				NerlaEvaluator eval = new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE);
				Score s = eval.prf1(gold, predicted);
				System.out.println(s);
				score.add(s);

			}
		}
		System.out.println(score);
//		System.out.println("Calc tfidf...");
//		for (Entry<String, Map<String, Double>> e : TFIDF.getTFIDFs(documents).entrySet()) {
//
//			for (Entry<String, Double> e2 : e.getValue().entrySet()) {
//
//				System.out.println(e.getKey() + "\t" + e2.getKey() + "\t" + e2.getValue());
//			}
//		}
	}
	Set<String> additionalStopWords = new HashSet<>(
			Arrays.asList("either", "number", "group", "groups", "numbers","injury","spinal","cord"));
	
	private Set<DocumentLinkedAnnotation> labelInvestigationMethods(Document doc, int sentenceIndex) {

		Set<DocumentLinkedAnnotation> predicted = new HashSet<>();

		x: for (String etn : dictionary.keySet()) {

			for (DocumentLinkedAnnotation pattern : dictionary.get(etn)) {

				if (doc.getContentOfSentence(sentenceIndex).contains( pattern.getSurfaceForm()))

				for (DocumentToken dt : doc.getSentenceByIndex(sentenceIndex)) {

					if (dt.isPunctuation() || dt.isStopWord() || dt.isNumber()
							|| additionalStopWords.contains(dt.getText()))
						continue;

					for (DocumentToken pt : pattern.relatedTokens) {

						if (pt.isPunctuation() || pt.isStopWord() || pt.isNumber()
								|| additionalStopWords.contains(pt.getText()))
							continue;

						if (dt.getText().equals(pt.getText())) {

							predicted.add(AnnotationBuilder.toAnnotation(doc, EntityType.get(etn), dt.getText(),
									dt.getDocCharOffset()));
							continue x;
						}
					}

				}
			}

		}

		return predicted;
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

}
