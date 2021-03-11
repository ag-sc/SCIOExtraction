package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.result;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.learner.optimizer.SGD;
import de.hterhors.semanticmr.crf.learner.regularizer.L2;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.impl.EpochSwitchSampler;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.templates.shared.IntraTokenTemplate;
import de.hterhors.semanticmr.crf.templates.shared.NGramTokenContextTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractNERLPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.templates.BigramTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.templates.HeadTailTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.templates.PairwiseMentionLabelTemplate;

/**
 * Example of how to perform named entity recognition and linking.
 * 
 * @author hterhors
 *
 */
public class ResultNERLPredictor extends AbstractNERLPredictor {
	private static Logger log = LogManager.getFormatterLogger("NERL");

//	/**
//	 * A dictionary file that is used for the in-memory dictionary based candidate
//	 * retrieval component. It is basically a list of terms and synonyms for
//	 * specific entities.
//	 * 
//	 * In a real world scenario dictionary lookups for candidate retrieval is mostly
//	 * not sufficient! Consider implementing your own candidate retrieval e.g. fuzzy
//	 * lookup, Lucene-based etc...
//	 */
//	private final File dictionaryFile = new File("src/main/resources/examples/nerla/dicts/organismModel.dict");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */

	public ResultNERLPredictor(String modelName, List<String> trainingInstanceNames, List<String> developInstanceNames,
			List<String> testInstanceNames) {
		super(modelName, trainingInstanceNames, developInstanceNames, testInstanceNames);
	}

	@Override
	protected File getInstanceDirectory() {
		return NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.result);
	}

	@Override
	protected AdvancedLearner getLearner() {
		return new AdvancedLearner(new SGD(0.0001, 0), new L2(0.000));
	}

	// Ohne HeadTail auf 10% der Daten
//Final Score: Score [getF1()=0.962, getPrecision()=1.000, getRecall()=0.926, tp=277, fp=0, fn=22, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=2333771]
//CRFStatistics [context=Test, getTotalDuration()=387088]
//modelName: OM_-49534045

	// MitHeadTail auf 10% der Daten
//Final Score: Score [getF1()=0.962, getPrecision()=1.000, getRecall()=0.926, tp=277, fp=0, fn=22, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=2755527]
//CRFStatistics [context=Test, getTotalDuration()=314096]
//modelName: OM_-426874068

	@Override
	protected List<AbstractFeatureTemplate<?>> getFeatureTemplates() {

		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();
//		featureTemplates.add(new ContextBetweenAnnotationsTemplate());
//		featureTemplates.add(new LevenshteinTemplate());

//		featureTemplates.add(new MorphologicalNerlaTemplate());

//		featureTemplates.add(new PosInDocTemplate());
//		featureTemplates.add(new PosInSentenceTemplate());

//Final Score: Score [getF1()=0.937, getPrecision()=1.000, getRecall()=0.882, tp=3026, fp=0, fn=404, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=6 221 558]
//CRFStatistics [context= Test, getTotalDuration()=166690]
//modelName: OM_956224153 BIGRAM
		featureTemplates.add(new HeadTailTemplate());
		featureTemplates.add(new BigramTemplate());
		featureTemplates.add(new NGramTokenContextTemplate());
//		CRFStatistics [context=Train, getTotalDuration()=15 523 652]
//				CRFStatistics [context=Test, getTotalDuration()=111975]
//				modelName: OM_-859532365
		featureTemplates.add(new IntraTokenTemplate());
		featureTemplates.add(new PairwiseMentionLabelTemplate());

		/**
		 * 
		 * 
		 */
//		featureTemplates.add(new GroupNamesInSameSentenceTemplate_FAST());
//		featureTemplates.add(new WBFGroupNamesTemplate_FAST());

		// featureTemplates.add(new WBFGroupNamesTemplate_SLOW());
//		featureTemplates.add(new WBFGroupNamesTemplate());
//		featureTemplates.add(new WBGroupNamesTemplate());
//		featureTemplates.add(new WBLGroupNamesTemplate());
//		featureTemplates.add(new WordsInBetweenGroupNamesTemplate());

		return featureTemplates;
	}

	@Override
	protected int getNumberOfEpochs() {
		return 10;
	}

	@Override
	protected IStateInitializer getStateInitializer() {
		return ((instance) -> new State(instance, new Annotations()));

	}

	@Override
	protected AbstractSampler getSampler() {
//		AbstractSampler sampler = SamplerCollection.greedyModelStrategy();
//		AbstractSampler sampler = SamplerCollection.greedyObjectiveStrategy();
		AbstractSampler sampler = new EpochSwitchSampler(epoch -> epoch % 2 == 0);
//		AbstractSampler sampler = new EpochSwitchSampler(new RandomSwitchSamplingStrategy());
//		AbstractSampler sampler = new EpochSwitchSampler(e -> new Random(e).nextBoolean());
		return sampler;
	}

	@Override
	protected File getModelBaseDir() {
		return new File("models/ner/result/");
	}

//	@Override
//	protected File getDictionaryFile() {
//		return dictionaryFile;
//	}

//	Final Score: Score [getF1()=0.058, getPrecision()=0.034, getRecall()=0.184, tp=32, fp=901, fn=142, tn=0]

	@Override
	protected Set<EntityType> getAdditionalCandidates() {
		Set<EntityType> set = new HashSet<>();

		for (EntityType entityType : SCIOEntityTypes.result.getRelatedEntityTypes()) {
//			if (!entityType.isLiteral)
			set.add(entityType);
		}

		return set;
	}

}
