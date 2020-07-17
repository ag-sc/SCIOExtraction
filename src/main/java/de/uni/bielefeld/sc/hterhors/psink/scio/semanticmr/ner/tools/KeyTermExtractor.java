package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation.ESection;
import de.hterhors.semanticmr.tools.TFIDF;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.SectionizedEntityRecLinkExplorer;

public class KeyTermExtractor {

	public static void main(String[] args) {

		SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(DataStructureLoader.loadNERDataStructureReader("InvestigationMethod"))
				/**
				 * Finally, we build the systems scope.
				 */

				.build();

		for (int i = 0; i < 1; i++) {
			new KeyTermExtractor(i);
		}
	}

	private KeyTermExtractor(int i) {
		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
				.setSeed(i).setDevelopmentProportion(20).setCorpusSizeFraction(1F).build();
//			AbstractCorpusDistributor originalCorpusDistributor = new OriginalCorpusDistributor.Builder()
//					.setCorpusSizeFraction(1F).build();
		SectionizedEntityRecLinkExplorer.MAX_WINDOW_SIZE = 4;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.maxNumberOfAnnotations = 300;
		InstanceProvider.removeInstancesWithToManyAnnotations = false;

		InstanceProvider instanceProvider = new InstanceProvider(
				NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.investigationMethod),
				corpusDistributor);

		Map<String, List<String>> documents = new HashMap<>();
		for (Instance trainInstance : instanceProvider.getTrainingInstances()) {

			Set<Integer> invMSentences = new HashSet<>();

			for (DocumentLinkedAnnotation invM : trainInstance.getGoldAnnotations()
					.<DocumentLinkedAnnotation>getAnnotations()) {
				invMSentences.add(invM.getSentenceIndex());
			}
			AutomatedSectionifcation sec = AutomatedSectionifcation.getInstance(trainInstance);

			for (int sentenceIndex = 0; sentenceIndex < trainInstance.getDocument()
					.getNumberOfSentences(); sentenceIndex++) {

				if (sec.getSection(sentenceIndex) != ESection.RESULTS)
					continue;

				final String docName = String.valueOf(invMSentences.contains(sentenceIndex));
				documents.putIfAbsent(docName, new ArrayList<>());
				for (DocumentToken documentToken : trainInstance.getDocument().getSentenceByIndex(sentenceIndex)) {

					if (documentToken.isPunctuation() || documentToken.isStopWord())
						continue;
					if (documentToken.getLength() == 1)
						continue;
					if (documentToken.isNumber())
						continue;

					documents.get(docName).add(documentToken.getText());
				}

			}

		}
		if (!(documents.containsKey("true") && documents.containsKey("false")))
			return;

		Map<String, Double> frequendInvSentenceTerms = TFIDF.getTFs(documents.get("true"), true);
		Map<String, Double> frequendNOTInvSentenceTerms = TFIDF.getTFs(documents.get("false"), true);

		Set<String> keyTerms = new HashSet<>();

		for (String freqInvterm : frequendInvSentenceTerms.keySet()) {

			if (!(frequendInvSentenceTerms.get(freqInvterm) > frequendNOTInvSentenceTerms.getOrDefault(freqInvterm,
					0D)))
				continue;

			if (frequendInvSentenceTerms.get(freqInvterm) < 0.01)
				continue;

			keyTerms.add(freqInvterm);
		}

		for (String string : keyTerms) {
			System.out.println(string);
		}

		System.out.println(keyTerms.size() + "/" + frequendInvSentenceTerms.size());

		evaluate(instanceProvider, keyTerms, instanceProvider.getTrainingInstances());
		evaluate(instanceProvider, keyTerms, instanceProvider.getDevelopmentInstances());

//		for (Entry<String, List<String>> document : documents.entrySet()) {
//			for (Entry<String, Double> e : TFIDF.getTFs(document.getValue(), true).entrySet()) {
//			
//				
//				System.out.println(document.getKey() + "\t" + e.getKey() + "\t" + e.getValue());
//		
//			
//			}
//		}

//		System.out.println("Calc tfidf...");
//		for (Entry<String, Map<String, Double>> e : TFIDF.getTFIDFs(documents).entrySet()) {
//
//			for (Entry<String, Double> e2 : e.getValue().entrySet()) {
//
//				System.out.println(e.getKey() + "\t" + e2.getKey() + "\t" + e2.getValue());
//			}
//		}

	}

//	Score [getF1()=0.244, getPrecision()=0.139, getRecall()=1.000, tp=706, fp=4366, fn=0, tn=0]

	private void evaluate(InstanceProvider instanceProvider, Set<String> keyTerms, List<Instance> instances) {
		Score s = new Score();

		for (Instance instance : instances) {
			AutomatedSectionifcation sec = AutomatedSectionifcation.getInstance(instance);

			Set<Integer> invMSentences = new HashSet<>();

			for (DocumentLinkedAnnotation invM : instance.getGoldAnnotations()
					.<DocumentLinkedAnnotation>getAnnotations()) {
				invMSentences.add(invM.getSentenceIndex());
			}

			for (int sentenceIndex = 0; sentenceIndex < instance.getDocument()
					.getNumberOfSentences(); sentenceIndex++) {

				if (sec.getSection(sentenceIndex) != ESection.RESULTS)
					continue;

				String sentence = instance.getDocument().getContentOfSentence(sentenceIndex);

				boolean containsKeyterm = false;

				for (String keyTerm : keyTerms) {

					if (sentence.contains(keyTerm)) {
						containsKeyterm = true;
						break;
					}
				}
				if (containsKeyterm) {
					if (invMSentences.contains(sentenceIndex)) {
						s.increaseTruePositive();
					} else {
						s.increaseFalsePositive();
					}
				} else {
					if (invMSentences.contains(sentenceIndex)) {
//						System.out.println(instance.getName());
//						System.out.println(sentenceIndex);
//						System.out.println(sentence);
//						System.out.println();
						s.increaseFalseNegative();
					} else {
						s.increaseTrueNegative();
					}
				}

			}

		}

		System.out.println(s);
	}

	protected File getInstanceDirectory() {
		return NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.investigationMethod);
	}

}
