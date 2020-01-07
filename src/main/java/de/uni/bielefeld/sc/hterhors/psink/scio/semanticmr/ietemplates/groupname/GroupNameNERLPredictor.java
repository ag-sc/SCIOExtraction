package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.groupname;

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
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractNERLPredictor;

/**
 * Example of how to perform named entity recognition and linking.
 * 
 * @author hterhors
 *
 */
public class GroupNameNERLPredictor extends AbstractNERLPredictor {
	private static Logger log = LogManager.getFormatterLogger("NERL");

	/**
	 * A dictionary file that is used for the in-memory dictionary based candidate
	 * retrieval component. It is basically a list of terms and synonyms for
	 * specific entities.
	 * 
	 * In a real world scenario dictionary lookups for candidate retrieval is mostly
	 * not sufficient! Consider implementing your own candidate retrieval e.g. fuzzy
	 * lookup, Lucene-based etc...
	 */
	private final File dictionaryFile = new File("src/main/resources/examples/nerla/dicts/organismModel.dict");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */

	public GroupNameNERLPredictor(String modelName, SystemScope scope, List<String> trainingInstanceNames,
			List<String> developInstanceNames, List<String> testInstanceNames) {
		super(modelName, scope, trainingInstanceNames, developInstanceNames, testInstanceNames);
	}

	@Override
	protected File getInstanceDirectory() {
		return new File("src/main/resources/nerl/group_name/corpus/instances/");
	}

	@Override
	protected AdvancedLearner getLearner() {
		return new AdvancedLearner(new SGD(0.0001, 0), new L2(0.000));
	}

	@Override
	protected List<AbstractFeatureTemplate<?>> getFeatureTemplates() {

		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();

//		featureTemplates.add(new ContextBetweenAnnotationsTemplate());
//		featureTemplates.add(new MorphologicalNerlaTemplate());
//		featureTemplates.add(new NGramTokenContextTemplate());
		featureTemplates.add(new IntraTokenTemplate());
//		featureTemplates.add(new LevenshteinTemplate());

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
		return new File("models/nerla/group_name/");
	}

	@Override
	protected File getDictionaryFile() {
		return dictionaryFile;
	}

	@Override
	protected Set<EntityType> getAdditionalCandidates() {
		Set<EntityType> set = new HashSet<>();
		set.add(EntityType.get("GroupName"));
		return set;
	}

}
