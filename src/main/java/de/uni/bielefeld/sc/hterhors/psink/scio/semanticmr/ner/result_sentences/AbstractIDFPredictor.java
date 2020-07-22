package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.result_sentences;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.eval.NerlaEvaluator;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation.ESection;
import de.hterhors.semanticmr.tools.TFIDF;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.Stemmer;

public abstract class AbstractIDFPredictor {

	private HashSet<EntityType> removeEntityTypes = new HashSet<>();
	private Set<String> additionalStopWords = new HashSet<>();
	private Map<String, Map<String, Double>> idfs;
	private Set<ESection> sectionsToConsider = new HashSet<>();
	private Set<String> sentenceStopWords = new HashSet<>();

	private boolean enableUniGram = true;
	private boolean enableBiGram = false;
	private boolean localNormalizing = false;
	private boolean enableStemming;
	private boolean enableToLowerCase;

	public void setEnableUniGram(boolean enableUniGram) {
		this.enableUniGram = enableUniGram;
	}

	public void setSentenceStopWords(Collection<String> sentenceStopWords) {
		this.sentenceStopWords = new HashSet<>(sentenceStopWords);
	}

	public void setRestrictToSections(Collection<ESection> sectionsToConsider) {
		this.sectionsToConsider = new HashSet<>(sectionsToConsider);
	}

	public void setEnableLowerCasing(boolean enableToLowerCase) {
		this.enableToLowerCase = enableToLowerCase;
	}

	public void setEnableStemming(boolean enableStemming) {
		this.enableStemming = enableStemming;
	}

	public void setLocalNormalizing(boolean localNormalizing) {
		this.localNormalizing = localNormalizing;
	}

	public void setEnableBiGram(boolean enableBiGram) {
		this.enableBiGram = enableBiGram;
	}

	public void setAnnotationStopwords(Collection<String> additionalStopWords) {
		this.additionalStopWords = new HashSet<>(additionalStopWords);
	}

	public void setRemoveEntityTypes(Collection<EntityType> removeEntityTypes) {
		this.removeEntityTypes = new HashSet<>(removeEntityTypes);
	}

	public void train(List<Instance> trainingInstances) {

		Map<String, List<String>> documents = new HashMap<>();

		for (Instance trainInstance : trainingInstances) {

			for (DocumentLinkedAnnotation data : extractData(trainInstance)) {

				if (removeEntityTypes.contains(data.getEntityType()))
					continue;

				List<String> tokens = data.relatedTokens.stream()
						.filter(t -> !(t.isPunctuation() || t.isStopWord() || t.isNumber()))
						.map(t -> modify(t.getText())).filter(t -> !additionalStopWords.contains(t))
						.collect(Collectors.toList());

				documents.putIfAbsent(data.getEntityType().name, new ArrayList<>());

				if (enableUniGram)
					documents.get(data.getEntityType().name).addAll(tokens);

				if (enableBiGram)
					for (int i = 0; i < tokens.size() - 1; i++) {
						for (int j = i + 1; j < tokens.size(); j++) {
							documents.get(data.getEntityType().name).add(bigram(tokens.get(i), tokens.get(j)));
						}
					}
			}

		}

		idfs = TFIDF.getIDFs(documents, localNormalizing);
	}

	private String bigram(String t1, String t2) {
		return t1 + "\t" + t2;
	}

	private final Stemmer stemmer = new Stemmer();

	private String modify(String text) {
		String ret = text;
		if (enableToLowerCase)
			ret = text.toLowerCase();
		if (enableStemming)
			ret = stemmer.stem(ret);
		return ret;
	}

	public void printIDFs(String fileName) throws IOException {
		PrintStream ps = new PrintStream(fileName);

		for (Entry<String, Map<String, Double>> e : idfs.entrySet()) {

			for (Entry<String, Double> e2 : e.getValue().entrySet()) {

				ps.println(e.getKey() + "\t" + e2.getKey() + "\t" + e2.getValue());
			}
		}
		ps.flush();
		ps.close();
	}

	public Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> predictInstances(
			List<Instance> trendTestInstances) {
		Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> annotations = new HashMap<>();
		for (Instance instance : trendTestInstances) {
			annotations.put(instance, predictInstance(instance));
		}
		return annotations;
	}

	public Map<Integer, Set<DocumentLinkedAnnotation>> predictInstance(Instance instance) {
		Map<Integer, Set<DocumentLinkedAnnotation>> annotations = new HashMap<>();
		si: for (int sentenceIndex = 0; sentenceIndex < instance.getDocument()
				.getNumberOfSentences(); sentenceIndex++) {

			if (!sectionsToConsider.isEmpty() && !sectionsToConsider
					.contains(AutomatedSectionifcation.getInstance(instance).getSection(sentenceIndex)))
				continue;

			String sentence = instance.getDocument().getContentOfSentence(sentenceIndex);
			for (String sentenceStopWord : sentenceStopWords) {
				if (sentence.contains(sentenceStopWord)) {
					continue si;
				}
			}

			annotations.put(sentenceIndex, predictSentence(instance, sentenceIndex));
		}
		return annotations;
	}

	public Set<DocumentLinkedAnnotation> predictSentence(Instance instance, int sentenceIndex) {

		Set<DocumentLinkedAnnotation> predicted = new HashSet<>();
		List<P> ps = new ArrayList<>();

		for (String etn : idfs.keySet()) {

			double value = 0D;

			double maxTokenImpact = 0;
			DocumentToken maxImpactToken = null;

			if (enableUniGram) {
				for (DocumentToken token : instance.getDocument().getSentenceByIndex(sentenceIndex)) {

					double val = idfs.get(etn).getOrDefault(modify(token.getText()), 0D);

					if (val > maxTokenImpact) {
						maxTokenImpact = val;
						maxImpactToken = token;
					}

					value += val;

				}
			}
			if (enableBiGram) {
				List<DocumentToken> tokens = instance.getDocument().getSentenceByIndex(sentenceIndex);
				for (int i = 0; i < tokens.size() - 1; i++) {
					for (int j = i + 1; j < tokens.size(); j++) {
						;
						double val = idfs.get(etn).getOrDefault(
								bigram(modify(tokens.get(i).getText()), modify(tokens.get(j).getText())), 0D);

						if (val > maxTokenImpact) {
							maxTokenImpact = val;
							maxImpactToken = tokens.get(i); // Take only first token of bigram
						}

						value += val;
					}
				}
			}

			if (maxImpactToken != null)
				ps.add(new P(AnnotationBuilder.toAnnotation(instance.getDocument(), etn, maxImpactToken.getText(),
						maxImpactToken.getDocCharOffset()), value));

		}
		Collections.sort(ps);

		int maxNumber = 2;
		for (P p : ps) {
			if (p.val >= 0 && p.val >= ps.get(0).val / 4) {
				predicted.add(p.trend);
			}
			if (predicted.size() >= maxNumber)
				break;
		}
		return predicted;
	}

	protected abstract List<DocumentLinkedAnnotation> extractData(Instance instance);

	public Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> getGroundTruthAnnotations(
			List<Instance> trendTestInstances) {
		Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> annotations = new HashMap<>();
		for (Instance instance : trendTestInstances) {
			annotations.put(instance, getGroundTruthAnnotations(instance));
		}
		return annotations;
	}

	public Map<Integer, Set<DocumentLinkedAnnotation>> getGroundTruthAnnotations(Instance instance) {
		Map<Integer, Set<DocumentLinkedAnnotation>> annotations = new HashMap<>();
		for (int sentenceIndex = 0; sentenceIndex < instance.getDocument().getNumberOfSentences(); sentenceIndex++) {
			annotations.put(sentenceIndex, getGroundTruthAnnotations(instance, sentenceIndex));
		}
		return annotations;
	}

	public Set<DocumentLinkedAnnotation> getGroundTruthAnnotations(Instance instance, final int sentenceIndex) {
		return instance.getGoldAnnotations().getAnnotations().stream()
				.filter(a -> sentenceIndex == a.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex())
				.map(a -> a.asInstanceOfDocumentLinkedAnnotation()).collect(Collectors.toSet());
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

	public Score evaluate(Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> groundTruth,
			Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> predictions) {

		Score overallScore = new Score(EScoreType.MICRO);
		NerlaEvaluator eval = new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE);

		for (Instance testInstance : groundTruth.keySet()) {
			Score instanceScore = new Score();

			AutomatedSectionifcation sec = AutomatedSectionifcation.getInstance(testInstance);

			Set<Integer> sentencesToEvaluate = new HashSet<>();

			sentencesToEvaluate.addAll(groundTruth.getOrDefault(testInstance, Collections.emptyMap()).keySet());
			sentencesToEvaluate.addAll(predictions.getOrDefault(testInstance, Collections.emptyMap()).keySet());

			for (Integer sentenceIndex : sentencesToEvaluate) {

				if (!sectionsToConsider.isEmpty() && !sectionsToConsider.contains(sec.getSection(sentenceIndex)))
					continue;

				Set<? extends AbstractAnnotation> groudTruthAnnotations = groundTruth.get(testInstance)
						.getOrDefault(sentenceIndex, Collections.emptySet());

				Set<? extends AbstractAnnotation> predictedAnnotations = predictions.get(testInstance)
						.getOrDefault(sentenceIndex, Collections.emptySet());

				groudTruthAnnotations = groudTruthAnnotations.stream()
						.map(a -> AnnotationBuilder.toAnnotation(a.getEntityType())).collect(Collectors.toSet());

				predictedAnnotations = predictedAnnotations.stream()
						.map(a -> AnnotationBuilder.toAnnotation(a.getEntityType())).collect(Collectors.toSet());

				Score s = eval.prf1(groudTruthAnnotations, predictedAnnotations);

				instanceScore.add(s);
			}
			overallScore.add(instanceScore);

		}
		return overallScore;

	}

}
//ohne filter
//Score [macroF1=0.088, macroPrecision=0.052, macroRecall=0.279, macroAddCounter=10]
//Score [macroF1=0.056, macroPrecision=0.030, macroRecall=0.462, macroAddCounter=10]
//Score [macroF1=0.104, macroPrecision=0.092, macroRecall=0.119, macroAddCounter=10]
//Score [macroF1=0.105, macroPrecision=0.068, macroRecall=0.229, macroAddCounter=10]

//mit filter

//Score [macroF1=0.138, macroPrecision=0.081, macroRecall=0.470, macroAddCounter=10]
//Score [macroF1=0.053, macroPrecision=0.028, macroRecall=0.467, macroAddCounter=10]
//Score [macroF1=0.168, macroPrecision=0.144, macroRecall=0.202, macroAddCounter=10]
//Score [macroF1=0.101, macroPrecision=0.065, macroRecall=0.233, macroAddCounter=10]
