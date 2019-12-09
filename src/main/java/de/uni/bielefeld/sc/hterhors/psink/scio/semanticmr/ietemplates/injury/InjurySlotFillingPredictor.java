package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.candprov.sf.GeneralCandidateProvider;
import de.hterhors.semanticmr.candprov.sf.ICandidateProvider;
import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.learner.optimizer.SGD;
import de.hterhors.semanticmr.crf.learner.regularizer.L2;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.impl.EpochSwitchSampler;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.templates.et.ClusterTemplate;
import de.hterhors.semanticmr.crf.templates.et.ContextBetweenSlotFillerTemplate;
import de.hterhors.semanticmr.crf.templates.et.LocalityTemplate;
import de.hterhors.semanticmr.crf.templates.et.SlotIsFilledTemplate;
import de.hterhors.semanticmr.crf.templates.shared.IntraTokenTemplate;
import de.hterhors.semanticmr.crf.templates.shared.NGramTokenContextTemplate;
import de.hterhors.semanticmr.crf.templates.shared.SingleTokenContextTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.InjuryRestrictionProvider.EInjuryModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea.VertebralAreaPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.DistinctMultiValueSlotsTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.DocumentPartTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.EntityTypeContextTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.MultiValueSlotSizeTemplate;

/**
 * Slot filling for injuries.
 * 
 * 
 * @author hterhors
 *
 */
public class InjurySlotFillingPredictor extends AbstractSlotFillingPredictor {

	private static Logger log = LogManager.getFormatterLogger(InjurySlotFillingPredictor.class);

	public InjurySlotFillingPredictor(String modelName, SystemScope scope, List<String> trainingInstanceNames,
			List<String> developInstanceNames, List<String> testInstanceNames) {

		super(modelName, scope, trainingInstanceNames, developInstanceNames, testInstanceNames);

	}

	@Override
	protected List<? extends ICandidateProvider> getAdditionalCandidateProvider() {

		if (InjurySlotFilling.rule == EInjuryModifications.ROOT)
			return Collections.emptyList();

		List<GeneralCandidateProvider> provider = new ArrayList<>();
		String vertebralAreaModelName = "VertebralArea_" + modelName;
		VertebralAreaPredictor vertebralAreaPrediction = new VertebralAreaPredictor(vertebralAreaModelName, scope,
				trainingInstanceNames, developInstanceNames, testInstanceNames);

		vertebralAreaPrediction.trainOrLoadModel();

		vertebralAreaPrediction.predictAllInstances(2);

		for (Instance instance : instanceProvider.getInstances()) {
			GeneralCandidateProvider ap = new GeneralCandidateProvider(instance);
			ap.addBatchSlotFiller(vertebralAreaPrediction.predictHighRecallInstanceByName(instance.getName(), 2));
			provider.add(ap);
		}
		return provider;
	}

	@Override
	protected File getExternalNerlaAnnotations() {
		final File externalNerlaAnnotations = new File("src/main/resources/slotfilling/injury/corpus/nerla/");
//		 final File externalNerlaAnnotations = new File("src/main/resources/slotfilling/injury/corpus/Normal/");
//		final File externalNerlaAnnotations = new File("src/main/resources/slotfilling/injury/corpus/HighRecall20/");
		return externalNerlaAnnotations;
	}

	@Override
	protected File getInstanceDirectory() {
		return new File("src/main/resources/slotfilling/injury/corpus/instances/");
	}

	@Override
	protected AdvancedLearner getLearner() {
		return new AdvancedLearner(new SGD(0.00001, 0), new L2(0.00));
	}

	@Override
	protected List<AbstractFeatureTemplate<?>> getFeatureTemplates() {
		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();
//		featureTemplates.add(new LevenshteinTemplate());
		featureTemplates.add(new IntraTokenTemplate());
		featureTemplates.add(new DistinctMultiValueSlotsTemplate());
		featureTemplates.add(new MultiValueSlotSizeTemplate());
		featureTemplates.add(new NGramTokenContextTemplate());
		featureTemplates.add(new SingleTokenContextTemplate());
		featureTemplates.add(new ContextBetweenSlotFillerTemplate());
		featureTemplates.add(new ClusterTemplate());
		featureTemplates.add(new EntityTypeContextTemplate());
//		featureTemplates.add(new OlfactoryContextTemplate());
		featureTemplates.add(new DocumentPartTemplate());
		featureTemplates.add(new LocalityTemplate());
		featureTemplates.add(new SlotIsFilledTemplate());
//		featureTemplates.add(new PriorNumericInterpretationInjuryTemplate(trainingInstances));
//		featureTemplates.add(new NumericInterpretationTemplate());

		return featureTemplates;
	}

	@Override
	protected int getNumberOfEpochs() {
		return 35;
	}

	@Override
	protected IStateInitializer getStateInitializer() {
		return ((instance) -> new State(instance,
				new Annotations(new EntityTemplate(AnnotationBuilder.toAnnotation("Injury")))));

//		return (instance -> {
//			List<AbstractAnnotation> as = new ArrayList<>();
//			for (int i = 0; i < instance.getGoldAnnotations().getAnnotations().size(); i++) {
//				as.add(new EntityTemplate(AnnotationBuilder.toAnnotation("Injury")));
//			}
//			return new State(instance, new Annotations(as));
//		});
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
		return new File("models/slotfilling/injury/");
	}

	@Override
	protected Collection<GoldModificationRule> getGoldModificationRules() {
		return InjuryRestrictionProvider.getByRule(InjurySlotFilling.rule);
	}
}
