package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.treatment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.deliverymethod.DeliveryMethodPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.deliverymethod.DeliveryMethodRestrictionProvider.EDeliveryMethodModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.treatment.TreatmentRestrictionProvider.ETreatmentModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.DocumentPartTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.EntityTypeContextTemplate;

/**
 * Slot filling for organism models.
 *
 * 
 * Mean Score: Score [getF1()=0.396, getPrecision()=0.587, getRecall()=0.299,
 * tp=148, fp=104, fn=347, tn=0]
 *
 * CRFStatistics [context=Train, getTotalDuration()=2534645]
 * 
 * CRFStatistics [context=Test, getTotalDuration()=133857]
 * 
 * Coverage Training: Score [getF1()=0.844, getPrecision()=0.985,
 * getRecall()=0.739, tp=1207, fp=19, fn=426, tn=0]
 * 
 * Coverage Development: Score [getF1()=0.836, getPrecision()=0.994,
 * getRecall()=0.721, tp=357, fp=2, fn=138, tn=0]
 * 
 * 
 * @author hterhors
 *
 */
public class TreatmentSlotFillingPredictor extends AbstractSlotFillingPredictor {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	public TreatmentSlotFillingPredictor(String modelName, SystemScope scope, List<String> trainingInstanceNames,
			List<String> developmentInstanceNames, List<String> testInstanceNames, ETreatmentModifications rule) {
		super(modelName, scope, trainingInstanceNames, developmentInstanceNames, testInstanceNames, rule);
	}

	@Override
	protected Map<Instance, Collection<AbstractAnnotation>> getAdditionalCandidateProvider(IModificationRule rule) {

		Map<Instance, Collection<AbstractAnnotation>> annotations = new HashMap<>();

		DeliveryMethodPredictor deliveryMethodPrediction = null;
		if (rule != ETreatmentModifications.ROOT) {

			String deliveryMethodModelName = "DeliveryMethod" + modelName;

			deliveryMethodPrediction = new DeliveryMethodPredictor(deliveryMethodModelName, scope,
					trainingInstanceNames, developInstanceNames, testInstanceNames, EDeliveryMethodModifications.ROOT);

			deliveryMethodPrediction.trainOrLoadModel();
			deliveryMethodPrediction.predictAllInstances(2);

		}
		for (Instance instance : instanceProvider.getInstances()) {
			annotations.put(instance, new ArrayList<>());
			if (rule != ETreatmentModifications.ROOT) {
				annotations.get(instance)
						.addAll(deliveryMethodPrediction.predictHighRecallInstanceByName(instance.getName(), 2));
			}
			annotations.get(instance).add(AnnotationBuilder.toAnnotation(SCIOEntityTypes.compoundTreatment));

		}
		return annotations;
	}

	@Override
	protected File getExternalNerlaFile() {
		return SlotFillingCorpusBuilderBib.getDefaultRegExNerlaDir(SCIOEntityTypes.treatment);
	}

	@Override
	protected File getInstanceDirectory() {
		return SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.treatment);
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
		featureTemplates.add(new NGramTokenContextTemplate());
		featureTemplates.add(new SingleTokenContextTemplate());
		featureTemplates.add(new ContextBetweenSlotFillerTemplate());
		featureTemplates.add(new ClusterTemplate());
		featureTemplates.add(new EntityTypeContextTemplate());
		featureTemplates.add(new DocumentPartTemplate());
		featureTemplates.add(new LocalityTemplate());
		featureTemplates.add(new SlotIsFilledTemplate());
		return featureTemplates;
	}

	@Override
	protected int getNumberOfEpochs() {
		return 10;
	}

	@Override
	protected IStateInitializer getStateInitializer() {
		return (instance -> {

			List<AbstractAnnotation> as = new ArrayList<>();

			for (int i = 0; i < instance.getGoldAnnotations().getAnnotations().size(); i++) {
				as.add(new EntityTemplate(AnnotationBuilder.toAnnotation("Treatment")));
			}
			return new State(instance, new Annotations(as));
			//
		});
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
		return new File("models/slotfilling/treatment/");
	}

	@Override
	protected Collection<GoldModificationRule> getGoldModificationRules(IModificationRule rule) {
		return TreatmentRestrictionProvider.getByRule((ETreatmentModifications) rule);
	}

}
