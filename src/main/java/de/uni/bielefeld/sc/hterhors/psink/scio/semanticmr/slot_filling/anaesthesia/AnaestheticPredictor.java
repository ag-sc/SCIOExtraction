package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthesia;

import java.io.File;
import java.util.ArrayList;
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
import de.hterhors.semanticmr.crf.exploration.constraints.HardConstraintsProvider;
import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.learner.optimizer.SGD;
import de.hterhors.semanticmr.crf.learner.regularizer.L2;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.impl.EpochSwitchSampler;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthesia.AnaestheticRestrictionProvider.EAnaestheticModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method.DeliveryMethodPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method.DeliveryMethodRestrictionProvider.EDeliveryMethodModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method.DistinctEntityTypeConstraint;
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
	final public boolean useGoldLocationsForPrediction = false;

	@Override
	protected Map<Instance, Collection<AbstractAnnotation>> getAdditionalCandidateProvider(IModificationRule _rule) {

		EAnaestheticModifications rule = (EAnaestheticModifications) _rule;

		Map<Instance, Collection<AbstractAnnotation>> map = new HashMap<>();

		if (rule == EAnaestheticModifications.ROOT || rule == EAnaestheticModifications.ROOT_DOSAGE)
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
//		return (instance -> {
//			return new State(instance, new Annotations(new EntityTemplate(SCIOEntityTypes.anaesthetic)));
//		});

		return (instance -> {
			return new State(instance, new Annotations(new EntityTemplate(SCIOEntityTypes.anaesthetic),
					new EntityTemplate(SCIOEntityTypes.anaesthetic)));
		});

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
		p.addHardConstraints(new DistinctEntityTypeConstraint(predictionObjectiveFunction.getEvaluator()));
		return p;
	}

//			MICRO	Root = 0.735	0.694	0.782	0.878	0.878	0.878
//			MICRO	hasDosage = 0.500	0.694	0.391	0.878	0.878	0.878
//			MICRO	Cardinality = 0.906	0.855	0.964	1.000	1.000	1.000
//			MACRO	Root = 0.739	0.694	0.790	0.879	0.878	0.880
//			MACRO	hasDosage = 0.503	0.694	0.395	0.879	0.878	0.880
//			MACRO	Cardinality = 0.912	0.855	0.978	1.000	1.000	1.000
//			MACRO	Overall = 0.739	0.694	0.790	0.879	0.878	0.880
//			modelName: Anaesthetic497593622
//			CRFStatistics [context=Train, getTotalDuration()=10390]
//			CRFStatistics [context=Test, getTotalDuration()=42]

//			MICRO	Root = 0.513	0.484	0.545	0.612	0.612	0.612
//			MICRO	hasDosage = 0.419	0.395	0.445	0.483	0.450	0.521
//			MICRO	Cardinality = 0.906	0.855	0.964	1.000	1.000	1.000
//			MACRO	Root = 0.521	0.484	0.565	0.620	0.612	0.629
//			MACRO	hasDosage = 0.433	0.395	0.478	0.497	0.455	0.548
//			MACRO	Cardinality = 0.912	0.855	0.978	1.000	1.000	1.000
//			MACRO	Overall = 0.433	0.395	0.478	0.497	0.455	0.548
//			modelName: Anaesthetic-214726547
//			CRFStatistics [context=Train, getTotalDuration()=70283]
//			CRFStatistics [context=Test, getTotalDuration()=1023]
	@Override
	public List<IExplorationStrategy> getAdditionalExplorer() {
		return Collections.emptyList();
//		return Arrays.asList(new RootTemplateCardinalityExplorer(trainingObjectiveFunction.getEvaluator(),
//				EExplorationMode.ANNOTATION_BASED, AnnotationBuilder.toAnnotation(EntityType.get("Anaesthetic"))));
	}
}

//mit 3
//MICRO	Root = 0.631	0.490	0.887	0.922	0.922	0.922
//MICRO	hasDosage = 0.468	0.490	0.448	0.922	0.922	0.922
//MICRO	Cardinality = 0.711	0.552	1.000	1.000	1.000	1.000
//MACRO	Root = 0.620	0.490	0.844	0.903	0.922	0.871
//MACRO	hasDosage = 0.455	0.490	0.425	0.895	0.922	0.872
//MACRO	Cardinality = 0.711	0.552	1.000	1.000	1.000	1.000
//MACRO	Overall = 0.620	0.490	0.844	0.903	0.922	0.871
//modelName: Anaesthetic-1915420003
//CRFStatistics [context=Train, getTotalDuration()=15417]
//CRFStatistics [context=Test, getTotalDuration()=42]

//mit 1
//MICRO	Root = 0.635	0.844	0.509	0.844	0.844	0.844
//MICRO	hasDosage = 0.394	0.844	0.257	0.844	0.844	0.844
//MICRO	Cardinality = 0.753	1.000	0.604	1.000	1.000	1.000
//MACRO	Root = 0.693	0.844	0.589	0.833	0.844	0.825
//MACRO	hasDosage = 0.438	0.844	0.295	0.830	0.844	0.825
//MACRO	Cardinality = 0.833	1.000	0.714	1.000	1.000	1.000
//MACRO	Overall = 0.693	0.844	0.589	0.833	0.844	0.825
//modelName: Anaesthetic-75682253
//CRFStatistics [context=Train, getTotalDuration()=6865]
//CRFStatistics [context=Test, getTotalDuration()=20]

// mit 2
//MICRO	Root = 0.752	0.688	0.830	0.936	0.936	0.936
//MICRO	hasDosage = 0.521	0.688	0.419	0.936	0.936	0.936
//MICRO	Cardinality = 0.838	0.766	0.925	1.000	1.000	1.000
//MACRO	Root = 0.774	0.688	0.885	0.944	0.936	0.955
//MACRO	hasDosage = 0.540	0.688	0.445	0.948	0.936	0.955
//MACRO	Cardinality = 0.851	0.766	0.958	1.000	1.000	1.000
//MACRO	Overall = 0.774	0.688	0.885	0.944	0.936	0.955
//modelName: Anaesthetic1773861614
//CRFStatistics [context=Train, getTotalDuration()=10734]
//CRFStatistics [context=Test, getTotalDuration()=31]
