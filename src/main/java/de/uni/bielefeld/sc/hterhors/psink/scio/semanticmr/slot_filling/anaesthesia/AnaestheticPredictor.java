package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthesia;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.candidateretrieval.sf.SlotFillingCandidateRetrieval.IFilter;
import de.hterhors.semanticmr.crf.exploration.IExplorationStrategy;
import de.hterhors.semanticmr.crf.exploration.RootTemplateCardinalityExplorer;
import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer.EExplorationMode;
import de.hterhors.semanticmr.crf.exploration.constraints.HardConstraintsProvider;
import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.learner.optimizer.SGD;
import de.hterhors.semanticmr.crf.learner.regularizer.L2;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.impl.EpochSwitchSampler;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor.IModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthesia.AnaestheticRestrictionProvider.EAnaestheticModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method.DeliveryMethodPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method.DeliveryMethodRestrictionProvider.EDeliveryMethodModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.hardconstraints.DistinctEntityTemplateConstraint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.InjuryRestrictionProvider.EInjuryModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury_device.InjuryDevicePredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury_device.InjuryDeviceRestrictionProvider.EInjuryDeviceModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.TreatmentRestrictionProvider.ETreatmentModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.VertebralAreaPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.VertebralAreaRestrictionProvider.EVertebralAreaModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.EntityTypeContextTemplate;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 *
 *
 * 
 *         results: ROOT_DELIVERY_METHOD_DOSAGE
 * 
 *         1 entity per doc
 * 
 *         Compute coverage... Coverage Training: Score [getF1()=0.763,
 *         getPrecision()=0.988, getRecall()=0.621, tp=343, fp=4, fn=209, tn=0]
 *         Compute coverage...
 * 
 *         Coverage Development: Score [getF1()=0.778, getPrecision()=1.000,
 *         getRecall()=0.636, tp=84, fp=0, fn=48, tn=0] results:
 *         ROOT_DELIVERY_METHOD_DOSAGE 0.6 0.92 0.45 modelName:
 *         Anaesthetic-541476241 CRFStatistics [context=Train,
 *         getTotalDuration()=26234] CRFStatistics [context=Test,
 *         getTotalDuration()=390]
 * 
 * 
 * 
 *         2 entities per doc
 * 
 *         Compute coverage... Coverage Training: Score [getF1()=0.899,
 *         getPrecision()=0.868, getRecall()=0.931, tp=514, fp=78, fn=38, tn=0]
 *         Compute coverage...
 * 
 *         Coverage Development: Score [getF1()=0.905, getPrecision()=0.873,
 *         getRecall()=0.939, tp=124, fp=18, fn=8, tn=0] results:
 *         ROOT_DELIVERY_METHOD_DOSAGE 0.62 0.66 0.59 modelName:
 *         Anaesthetic-1755775998 CRFStatistics [context=Train,
 *         getTotalDuration()=50917] CRFStatistics [context=Test,
 *         getTotalDuration()=757]
 * 
 */
public class AnaestheticPredictor extends AbstractSlotFillingPredictor {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	public AnaestheticPredictor(String modelName, List<String> trainingInstanceNames, List<String> developInstanceNames,
			List<String> testInstanceNames, IModificationRule rule) {
		super(modelName, trainingInstanceNames, developInstanceNames, testInstanceNames, rule);
	}

	final public boolean useGoldLocationsForTraining = true;
	final public boolean useGoldLocationsForPrediction = true;

	@Override
	protected Map<Instance, Collection<AbstractAnnotation>> getAdditionalCandidateProvider(IModificationRule _rule) {

		EAnaestheticModifications rule = (EAnaestheticModifications) _rule;
//
		Map<Instance, Collection<AbstractAnnotation>> map = new HashMap<>();

		if (rule == EAnaestheticModifications.ROOT)
			return map;

		if (useGoldLocationsForTraining) {
			addGold(map, instanceProvider.getRedistributedTrainingInstances());
		} else {
			addPredictions(map, instanceProvider.getRedistributedTrainingInstances());
		}
		if (useGoldLocationsForPrediction) {
			addGold(map, instanceProvider.getRedistributedDevelopmentInstances());
			addGold(map, instanceProvider.getRedistributedTestInstances());
		} else {
			addPredictions(map, instanceProvider.getRedistributedDevelopmentInstances());
			addPredictions(map, instanceProvider.getRedistributedTestInstances());
		}

		for (Instance instance : instanceProvider.getInstances()) {

			if (!map.get(instance).isEmpty())
				continue;

			instance.removeCandidateAnnotation(new IFilter() {

				@Override
				public boolean remove(AbstractAnnotation candidate) {
					return SCIOSlotTypes.hasDeliveryMethod.getSlotFillerEntityTypes()
							.contains(candidate.getEntityType());
				}

			});

		}

		return map;
	}

	private void addGold(Map<Instance, Collection<AbstractAnnotation>> map, List<Instance> instances) {
		for (Instance instance : instances) {
			map.putIfAbsent(instance, new ArrayList<>());

			map.get(instance)
					.addAll(instance.getGoldAnnotations().getAnnotations().stream()
							.map(a -> a.asInstanceOfEntityTemplate()
									.getSingleFillerSlot(SCIOSlotTypes.hasDeliveryMethod).getSlotFiller())
							.filter(a -> a != null).collect(Collectors.toSet()));

		}
	}

	private void addPredictions(Map<Instance, Collection<AbstractAnnotation>> map, List<Instance> instances) {
		Map<SlotType, Boolean> z = SlotType.storeExcludance();

		DeliveryMethodPredictor deliveryMethodPrediction = null;

		String deliveryMethodModelName = "DeliveryMethod" + modelName;

		deliveryMethodPrediction = new DeliveryMethodPredictor(deliveryMethodModelName, trainingInstanceNames,
				developInstanceNames, testInstanceNames, EDeliveryMethodModifications.ROOT_LOCATION_DURATION);

		deliveryMethodPrediction.trainOrLoadModel();
		deliveryMethodPrediction.predictAllInstances(2);

		for (Instance instance : instances) {
			map.putIfAbsent(instance, new ArrayList<>());
			map.get(instance).addAll(deliveryMethodPrediction.predictHighRecallInstanceByName(instance.getName(), 1));
		}
		SlotType.restoreExcludance(z);

	}

	@Override
	protected File getInstanceDirectory() {
		return SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.anaesthetic);
	}

	@Override
	protected File getModelBaseDir() {
		return new File("models/slotfilling/anaesthetic/");
	}

	@Override
	protected AdvancedLearner getLearner() {
		return new AdvancedLearner(new SGD(0.00001, 0), new L2(0.00));
	}

	@Override
	protected File getExternalNerlaFile() {
		return SlotFillingCorpusBuilderBib.getDefaultRegExNerlaDir(SCIOEntityTypes.anaesthetic);
	}

	@Override
	protected IStateInitializer getStateInitializer() {
		return (instance -> {
			return new State(instance, new Annotations(new EntityTemplate(SCIOEntityTypes.anaesthetic)));
		});

//		return (instance -> {
//			return new State(instance, new Annotations(new EntityTemplate(SCIOEntityTypes.anaesthetic),
//					new EntityTemplate(SCIOEntityTypes.anaesthetic)));
//		});
//		return (instance -> {
//
//			List<AbstractAnnotation> as = new ArrayList<>();
//
//			for (int i = 0; i < instance.getGoldAnnotations().getAnnotations().size(); i++) {
//				as.add(new EntityTemplate(AnnotationBuilder.toAnnotation("DeliveryMethod")));
//			}
//			return new State(instance, new Annotations(as));
//			//
//		});
	}

	@Override
	protected AbstractSampler getSampler() {
//		return SamplerCollection.greedyModelStrategy();
//		return SamplerCollection.greedyObjectiveStrategy();
		return new EpochSwitchSampler(epoch -> epoch % 2 == 0);
//		return new EpochSwitchSampler(new RandomSwitchSamplingStrategy());
//		return new EpochSwitchSampler(e -> new Random(e).nextBoolean());
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
		featureTemplates.add(new LocalityTemplate());
		featureTemplates.add(new SlotIsFilledTemplate());

		return featureTemplates;
	}

	@Override
	protected int getNumberOfEpochs() {
		return 10;
	}

	@Override
	protected Collection<GoldModificationRule> getGoldModificationRules(IModificationRule rule) {
		return AnaestheticRestrictionProvider.getByRule((EAnaestheticModifications) rule);
	}

	@Override
	public HardConstraintsProvider getHardConstraints() {
		HardConstraintsProvider p = new HardConstraintsProvider();
		p.addHardConstraints(new DistinctEntityTemplateConstraint(predictionObjectiveFunction.getEvaluator()));

		return p;
	}

	@Override
	public List<IExplorationStrategy> getAdditionalExplorer() {
		return Collections.emptyList();
//		return Arrays.asList(new RootTemplateCardinalityExplorer(trainingObjectiveFunction.getEvaluator(),
//				EExplorationMode.ANNOTATION_BASED, AnnotationBuilder.toAnnotation(EntityType.get("Anaesthetic"))));
	}

}
