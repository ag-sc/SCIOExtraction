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

	private Map<String, Map<String, Double>> idfs;

	private HashSet<EntityType> excludeEntityTypes = new HashSet<>();
	private Set<String> additionalStopWords = new HashSet<>();
	private Set<ESection> sectionsToConsider = new HashSet<>();
	private Set<String> sentenceStopWords = new HashSet<>();

	private boolean enableUniGram = true;
	private boolean enableBiGram = false;
	private boolean localNormalizing = false;
	private boolean enableStemming = false;
	private boolean enableToLowerCase = false;
	private boolean includeNameContainsBonus = false;
	private int minTokenLength = 0;
	private double threshold = 0;
	private int maxAnnotationsPerSentence = 10000;
	private int minAnnotationsPerSentence = 0;

	@Override
	public String toString() {
		return "#excludeEntityTypes=" + excludeEntityTypes.stream().map(et -> et.name).collect(Collectors.toList())
				+ "\n#additionalStopWords=" + additionalStopWords + "\n#sectionsToConsider=" + sectionsToConsider
				+ "\n#sentenceStopWords=" + sentenceStopWords + "\n#enableUniGram=" + enableUniGram + "\n#enableBiGram="
				+ enableBiGram + "\n#localNormalizing=" + localNormalizing + "\n#enableStemming=" + enableStemming
				+ "\n#enableToLowerCase=" + enableToLowerCase + "\n#includeNameContainsBonus="
				+ includeNameContainsBonus + "\n#minTokenLength=" + minTokenLength + "\n#threshold=" + threshold
				+ "\n#maxAnnotationsPerSentence=" + maxAnnotationsPerSentence + "\n#minAnnotationsPerSentence="
				+ minAnnotationsPerSentence;
	}

	public void setMinAnnotationsPerSentence(int setMinAnnotationsPerSentence) {
		this.minAnnotationsPerSentence = setMinAnnotationsPerSentence;
	}

	public void setMaxAnnotationsPerSentence(int maxNumberPerSentence) {
		this.maxAnnotationsPerSentence = maxNumberPerSentence;
	}

	public void setTrehsold(double threshold) {
		this.threshold = threshold;
	}

	public void setIncludeNameContains(boolean includeNameContains) {
		this.includeNameContainsBonus = includeNameContains;
	}

	public void setMinTokenLength(int minTokenLength) {
		this.minTokenLength = minTokenLength;
	}

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
		this.excludeEntityTypes = new HashSet<>(removeEntityTypes);
	}

	public void train(List<Instance> trainingInstances) {

		Map<String, List<String>> documents = new HashMap<>();

		for (Instance trainInstance : trainingInstances) {

			for (DocumentLinkedAnnotation data : extractData(trainInstance)) {

				if (excludeEntityTypes.contains(data.getEntityType()))
					continue;

				List<String> tokens = data.relatedTokens.stream()
						.filter(t -> !(t.isPunctuation() || t.isStopWord() || t.isNumber()))
						.map(t -> modify(t.getText())).filter(t -> !additionalStopWords.contains(t))
						.filter(a -> a.length() >= minTokenLength).collect(Collectors.toList());

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

		if (includeNameContainsBonus) {
			for (String etn : idfs.keySet()) {

				for (String term : idfs.get(etn).keySet()) {

					if (enableToLowerCase) {
						if (etn.toLowerCase().contains(term)) {
							idfs.get(etn).put(term, 10D);
						}
					} else {
						if (etn.contains(term)) {
							idfs.get(etn).put(term, 10D);
						}
					}

				}
			}
		}
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

	public String printInfo(String fileName, Score s) throws IOException {
		PrintStream ps = new PrintStream(fileName);

		String info = "";
		info += "info\t" + toString() + "\n";
		info += "modelId\t" + toString().hashCode() + "\n";
		info += "score\t" + s;
		ps.println("info\t" + toString());
		ps.println("modelId\t" + toString().hashCode());
		ps.println("score\t" + s);

		ps.flush();
		ps.close();
		return info;
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

			Set<String> distincts = new HashSet<>();

			if (enableUniGram) {
				for (DocumentToken token : instance.getDocument().getSentenceByIndex(sentenceIndex)) {

					String modToken = modify(token.getText());
					if (distincts.contains(modToken))
						continue;
					distincts.add(modToken);

					double val = idfs.get(etn).getOrDefault(modToken, 0D);

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
						String modToken = bigram(modify(tokens.get(i).getText()), modify(tokens.get(j).getText()));
						if (distincts.contains(modToken))
							continue;
						distincts.add(modToken);

						double val = idfs.get(etn).getOrDefault(modToken, 0D);

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
		for (P p : ps) {
			// if (p.value >= 1 && p.value >= ps.get(0).value / 4) {
			if (p.value >= threshold) {
				predicted.add(p.entity);
			}
			if (predicted.size() == maxAnnotationsPerSentence)
				break;
		}

		if (predicted.size() < minAnnotationsPerSentence) {
			predicted = new HashSet<>();
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
		public final DocumentLinkedAnnotation entity;
		public final Double value;

		public P(DocumentLinkedAnnotation trend, Double val) {
			super();
			this.entity = trend;
			this.value = val;
		}

		@Override
		public int compareTo(P o) {
			return Double.compare(o.value, value);
		}

		@Override
		public String toString() {
			return "P [invM=" + entity.toPrettyString() + ", val=" + value + "]";
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

			int hierarchyLevel = 1;

			for (Integer sentenceIndex : sentencesToEvaluate) {

				if (!sectionsToConsider.isEmpty() && !sectionsToConsider.contains(sec.getSection(sentenceIndex)))
					continue;

				Set<? extends AbstractAnnotation> groudTruthAnnotations = groundTruth.get(testInstance)
						.getOrDefault(sentenceIndex, Collections.emptySet());

				Set<? extends AbstractAnnotation> predictedAnnotations = predictions.get(testInstance)
						.getOrDefault(sentenceIndex, Collections.emptySet());

				groudTruthAnnotations = groudTruthAnnotations.stream()
						.map(a -> AnnotationBuilder.toAnnotation(toEntityType(hierarchyLevel, a.getEntityType())))
						.collect(Collectors.toSet());

				predictedAnnotations = predictedAnnotations.stream()
						.map(a -> AnnotationBuilder.toAnnotation(toEntityType(hierarchyLevel, a.getEntityType())))
						.collect(Collectors.toSet());

				Score s = eval.prf1(groudTruthAnnotations, predictedAnnotations);

				instanceScore.add(s);
			}
			overallScore.add(instanceScore);

		}
		return overallScore;

	}
//	score	Score [getF1()=0.059, getPrecision()=0.031, getRecall()=0.450, tp=77, fp=2368, fn=94, tn=0]0
//	score	Score [getF1()=0.090, getPrecision()=0.049, getRecall()=0.612, tp=104, fp=2032, fn=66, tn=0]1
//	score	Score [getF1()=0.136, getPrecision()=0.075, getRecall()=0.753, tp=128, fp=1582, fn=42, tn=0]3
//	score	Score [getF1()=0.138, getPrecision()=0.076, getRecall()=0.769, tp=130, fp=1579, fn=39, tn=0] 4

//	score	Score [getF1()=0.213, getPrecision()=0.120, getRecall()=0.945, tp=156, fp=1146, fn=9, tn=0] 10

	private EntityType toEntityType(int hierarchyLevel, EntityType entityType) {

		if (hierarchyLevel == 0)
			return entityType;

		int level = 0;
		EntityType et = entityType;
		while (true) {
			List<EntityType> l = new ArrayList<>(et.getDirectSubEntityTypes());
			if (!et.getDirectSubEntityTypes().isEmpty()) {
				et = l.get(0);
				level++;
			} else {
				break;
			}
		}

		for (int i = 0; i < hierarchyLevel - level; i++) {

			if (!entityType.getDirectSuperEntityTypes().isEmpty())
				entityType = entityType.getDirectSuperEntityTypes().iterator().next();

		}
		return entityType;

	}

	public void printErrors(String fileName, Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> groundTruth,
			Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> predictions) throws IOException {

		PrintStream ps = new PrintStream(fileName);

		NerlaEvaluator eval = new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE);

		for (Instance testInstance : groundTruth.keySet()) {

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

				String line = testInstance.getName() + "\t" + sentenceIndex + "\t" + s.getTp() + "\t" + s.getFp() + "\t"
						+ s.getFn() + "\t"
						+ groudTruthAnnotations.stream().map(e -> e.getEntityType().name).collect(Collectors.toList())
						+ "\t"
						+ predictedAnnotations.stream().map(e -> e.getEntityType().name).collect(Collectors.toList())
						+ "\t\"" + getRelevantTerms(testInstance, sentenceIndex, predictedAnnotations) + "\"\t\""
						+ testInstance.getDocument().getContentOfSentence(sentenceIndex) + "\"";

				ps.println(line);

			}
		}

		ps.flush();
		ps.close();

	}

	private String getRelevantTerms(Instance instance, int sentenceIndex,
			Set<? extends AbstractAnnotation> predictedAnnotations) {

		String line = "";
		for (AbstractAnnotation abstractAnnotation : predictedAnnotations) {

			Set<String> relevantTerms = new HashSet<>();
			double value = 0D;

			Set<String> distincts = new HashSet<>();

			if (enableUniGram) {
				for (DocumentToken token : instance.getDocument().getSentenceByIndex(sentenceIndex)) {

					String modToken = modify(token.getText());
					if (distincts.contains(modToken))
						continue;
					distincts.add(modToken);

					double val = idfs.get(abstractAnnotation.getEntityType().name).getOrDefault(modify(token.getText()),
							0D);

					if (val > 0D) {
						relevantTerms.add(modify(token.getText()));
						value += val;
					}

				}
			}
			if (enableBiGram) {
				List<DocumentToken> tokens = instance.getDocument().getSentenceByIndex(sentenceIndex);
				for (int i = 0; i < tokens.size() - 1; i++) {
					for (int j = i + 1; j < tokens.size(); j++) {
						String modToken = bigram(modify(tokens.get(i).getText()), modify(tokens.get(j).getText()));

						if (distincts.contains(modToken))
							continue;
						distincts.add(modToken);

						double val = idfs.get(abstractAnnotation.getEntityType().name).getOrDefault(modToken, 0D);

						if (val > 0D) {
							relevantTerms.add(modToken);
							value += val;
						}

					}
				}
			}

			if (value >= threshold) {
				line += abstractAnnotation.getEntityType().name + "[";
				for (String string : relevantTerms) {
					line += string + ", ";
				}
				line += value + "] ";
			}
		}
		return line.trim();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((additionalStopWords == null) ? 0 : additionalStopWords.hashCode());
		result = prime * result + (enableBiGram ? 1231 : 1237);
		result = prime * result + (enableStemming ? 1231 : 1237);
		result = prime * result + (enableToLowerCase ? 1231 : 1237);
		result = prime * result + (enableUniGram ? 1231 : 1237);
		result = prime * result + ((idfs == null) ? 0 : idfs.hashCode());
		result = prime * result + (includeNameContainsBonus ? 1231 : 1237);
		result = prime * result + (localNormalizing ? 1231 : 1237);
		result = prime * result + minTokenLength;
		result = prime * result + ((excludeEntityTypes == null) ? 0 : excludeEntityTypes.hashCode());
		result = prime * result + ((sectionsToConsider == null) ? 0 : sectionsToConsider.hashCode());
		result = prime * result + ((sentenceStopWords == null) ? 0 : sentenceStopWords.hashCode());
		result = prime * result + ((stemmer == null) ? 0 : stemmer.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractIDFPredictor other = (AbstractIDFPredictor) obj;
		if (additionalStopWords == null) {
			if (other.additionalStopWords != null)
				return false;
		} else if (!additionalStopWords.equals(other.additionalStopWords))
			return false;
		if (enableBiGram != other.enableBiGram)
			return false;
		if (enableStemming != other.enableStemming)
			return false;
		if (enableToLowerCase != other.enableToLowerCase)
			return false;
		if (enableUniGram != other.enableUniGram)
			return false;
		if (idfs == null) {
			if (other.idfs != null)
				return false;
		} else if (!idfs.equals(other.idfs))
			return false;
		if (includeNameContainsBonus != other.includeNameContainsBonus)
			return false;
		if (localNormalizing != other.localNormalizing)
			return false;
		if (minTokenLength != other.minTokenLength)
			return false;
		if (excludeEntityTypes == null) {
			if (other.excludeEntityTypes != null)
				return false;
		} else if (!excludeEntityTypes.equals(other.excludeEntityTypes))
			return false;
		if (sectionsToConsider == null) {
			if (other.sectionsToConsider != null)
				return false;
		} else if (!sectionsToConsider.equals(other.sectionsToConsider))
			return false;
		if (sentenceStopWords == null) {
			if (other.sentenceStopWords != null)
				return false;
		} else if (!sentenceStopWords.equals(other.sentenceStopWords))
			return false;
		if (stemmer == null) {
			if (other.stemmer != null)
				return false;
		} else if (!stemmer.equals(other.stemmer))
			return false;
		return true;
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
