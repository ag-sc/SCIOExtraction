package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.InjuryRestrictionProvider.EInjuryModificationRules;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.specs.InjurySpecs;

/**
 * Playground to Classify sentences into important or not important. Maybe multi
 * class classification.
 * 
 * @author hterhors
 *
 */
public class SentenceClassification {

	public static void main(String[] args) throws IOException {

		new SentenceClassification(
				SystemScope.Builder.getScopeHandler().addScopeSpecification(InjurySpecs.systemsScope).build());

	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private final File instanceDirectory = new File("src/main/resources/slotfilling/injury/corpus/instances/");

	public static EInjuryModificationRules rule;

	public final String header = "Mode\tF1\tPrecision\tRecall";

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	public SentenceClassification(SystemScope scope) throws IOException {

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(1000L)
				.setTrainingProportion(80).setTestProportion(20).setCorpusSizeFraction(1F).build();

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		for (Instance instance : instanceProvider.getRedistributedTrainingInstances()) {
			Set<Integer> sentencesWithInfo = new HashSet<>();

			for (AbstractAnnotation goldAnnotation : instance.getGoldAnnotations().getAnnotations()) {

				AbstractAnnotation rootAnnotation = goldAnnotation.asInstanceOfEntityTemplate().getRootAnnotation();

				if (!rootAnnotation.isInstanceOfDocumentLinkedAnnotation())
					continue;

				DocumentLinkedAnnotation linkedAnn = rootAnnotation.asInstanceOfDocumentLinkedAnnotation();

				int sentenceIndex = linkedAnn.relatedTokens.get(0).getSentenceIndex();
				sentencesWithInfo.add(sentenceIndex);
			}
			
			for (int i = 0; i < instance.getDocument().getNumberOfSentences(); i++) {
			
				instance.getDocument().getSentenceByIndex(i);
				if(sentencesWithInfo.contains(i)) {
					
				}else {
					
				}
				
			
			}

		}

	}

}
