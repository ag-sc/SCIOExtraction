package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthetic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.AnnotationsCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthetic.AnaestheticRestrictionProvider.EAnaestheticModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method.DeliveryMethodPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method.DeliveryMethodRestrictionProvider.EDeliveryMethodModifications;
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
			List<String> testInstanceNames, IModificationRule rule, ENERModus modus) {
		super(modelName, trainingInstanceNames, developInstanceNames, testInstanceNames, rule, modus);
	}

	private Map<String, Set<AbstractAnnotation>> organismModel;

	public void setOrganismModel(Map<String, Set<AbstractAnnotation>> organismModel) {
		this.organismModel = organismModel;
	}

	final public boolean useGoldLocationsForTraining = true;
	final public boolean useGoldLocationsForPrediction = false;

	private DeliveryMethodPredictor deliveryMethodPrediction = null;

//	GOLD
//	MACRO	Root = 0.615	0.750	0.521	0.615	0.750	0.521	1.000	1.000	1.000
//			MACRO	hasDeliveryMethod = 0.507	0.600	0.439	0.507	0.600	0.439	1.000	1.000	1.000
//			MACRO	hasDosage = 0.854	1.000	0.745	0.854	1.000	0.745	1.000	1.000	1.000
//			MACRO	Cardinality = 0.850	1.000	0.740	0.850	1.000	0.740	1.000	1.000	1.000
//			MACRO	Overall = 0.713	0.828	0.626	0.713	0.828	0.626	1.000	1.000	1.000
//			modelName: Anaesthetic298091341
//			CRFStatistics [context=Train, getTotalDuration()=22813]
//			CRFStatistics [context=Test, getTotalDuration()=123]

//	PREDICT mit GOLD

//	MACRO	Root = 0.615	0.750	0.521	0.615	0.750	0.521	1.000	1.000	1.000
//			MACRO	hasDeliveryMethod = 0.430	0.517	0.368	0.479	0.577	0.410	0.897	0.897	0.897
//			MACRO	hasDosage = 0.854	1.000	0.745	0.854	1.000	0.745	1.000	1.000	1.000
//			MACRO	Cardinality = 0.850	1.000	0.740	0.850	1.000	0.740	1.000	1.000	1.000
//			MACRO	Overall = 0.683	0.792	0.600	0.705	0.817	0.619	0.969	0.969	0.969
//			modelName: Anaesthetic525840951
//			CRFStatistics [context=Train, getTotalDuration()=17172]
//			CRFStatistics [context=Test, getTotalDuration()=126]
	
//	PREDICt
//	MACRO	Root = 0.582	0.719	0.490	0.592	0.719	0.505	0.984	1.000	0.969
//			MACRO	hasDeliveryMethod = 0.430	0.517	0.368	0.479	0.577	0.410	0.897	0.897	0.897
//			MACRO	hasDosage = 0.854	1.000	0.745	0.868	1.000	0.769	0.984	1.000	0.969
//			MACRO	Cardinality = 0.850	1.000	0.740	0.864	1.000	0.763	0.984	1.000	0.969
//			MACRO	Overall = 0.668	0.771	0.589	0.701	0.796	0.629	0.953	0.969	0.937
//			modelName: Anaesthetic525060698
//			CRFStatistics [context=Train, getTotalDuration()=15552]
//			CRFStatistics [context=Test, getTotalDuration()=118]
	
	@Override
	protected Map<Instance, Collection<AbstractAnnotation>> getAdditionalCandidateProvider(IModificationRule _rule) {

		EAnaestheticModifications rule = (EAnaestheticModifications) _rule;

		Map<Instance, Collection<AbstractAnnotation>> map = new HashMap<>();

		if (rule == EAnaestheticModifications.ROOT || rule == EAnaestheticModifications.ROOT_DOSAGE)
			return map;

		if (useGoldLocationsForTraining) {
			addGold(map, instanceProvider.getTrainingInstances());
		} else {
			addPredictions(map, instanceProvider.getTrainingInstances());
		}
		if (useGoldLocationsForPrediction|| modus == ENERModus.GOLD) {
			addGold(map, instanceProvider.getDevelopmentInstances());
			addGold(map, instanceProvider.getTestInstances());
		} else {
			addPredictions(map, instanceProvider.getDevelopmentInstances());
			addPredictions(map, instanceProvider.getTestInstances());
		}

		for (Instance instance : instanceProvider.getInstances()) {

			if (map.get(instance).isEmpty())
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

//		String deliveryMethodModelName = "DeliveryMethodFIX";
		String deliveryMethodModelName = "DeliveryMethod_PREDICT";// + modelName;

		if (deliveryMethodPrediction == null) {

			deliveryMethodPrediction = new DeliveryMethodPredictor(deliveryMethodModelName, trainingInstanceNames,
					developInstanceNames, testInstanceNames, EDeliveryMethodModifications.ROOT_LOCATION_DURATION,
					modus);

			deliveryMethodPrediction.trainOrLoadModel();
			deliveryMethodPrediction.predictAllInstances(1);
		}

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
		if (modus == ENERModus.GOLD)
			return new File(AnnotationsCorpusBuilderBib.ANNOTATIONS_DIR,
					AnnotationsCorpusBuilderBib.toDirName(SCIOEntityTypes.anaesthetic));
		else
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
//		p.addHardConstraints(new DistinctEntityTypeConstraint(predictionObjectiveFunction.getEvaluator()));
//		p.addHardConstraints(new DistinctEntityTemplateConstraint(predictionObjectiveFunction.getEvaluator()));
//		p.addHardConstraints(new AbstractHardConstraint() {
//
//			@Override
//			public boolean violatesConstraint(State currentState, EntityTemplate entityTemplate, int index) {
//
//				if (organismModel == null)
//					return false;
//
//				Set<AbstractAnnotation> orgModels = organismModel.get(currentState.getInstance().getName());
//				Set<Integer> sentences = new HashSet<>();
//
//				for (AbstractAnnotation orgModel : orgModels) {
//
//					OrganismModelWrapper w = new OrganismModelWrapper(orgModel.asInstanceOfEntityTemplate());
//
//					sentences.addAll(
//							w.getAnnotations().stream().map(a -> a.getSentenceIndex()).collect(Collectors.toSet()));
//
//				}
//				int maxOrganismIndex = 0;
//
//				for (Integer integer : sentences) {
//					maxOrganismIndex = Math.max(maxOrganismIndex, integer);
//				}
//
//				List<DocumentLinkedAnnotation> as = new ArrayList<>();
//				Set<Integer> sentences2 = new HashSet<>();
//
//				SCIOWrapper.collectDLA(as, entityTemplate.asInstanceOfEntityTemplate());
//
////				for (AbstractAnnotation ab : currentState.getGoldAnnotations().getAnnotations()) {
//
////				SCIOWrapper.collectDLA(as, ab.asInstanceOfEntityTemplate());
//				sentences2.addAll(as.stream().map(a -> a.getSentenceIndex()).collect(Collectors.toSet()));
////				}
//
//				for (Integer integer : sentences2) {
//
//					if (maxOrganismIndex < integer && Math.abs(maxOrganismIndex - integer) > 30)
//						return true;
//				}
////				System.out.println(Math.abs(max - min));
//
//				return false;
//			}
//		});
		return p;
	}

//  distinct et +  only2  root 

//	MACRO	Root = 0.511	0.438	0.615	0.519	0.438	0.634	0.984	1.000	0.969
//			MACRO	hasDeliveryMethod = 0.143	0.117	0.182	0.159	0.131	0.203	0.897	0.897	0.897
//			MACRO	hasDosage = 0.778	0.719	0.849	0.791	0.719	0.876	0.984	1.000	0.969
//			MACRO	Cardinality = 0.849	0.750	0.979	0.863	0.750	1.011	0.984	1.000	0.969
//			MACRO	Overall = 0.561	0.558	0.565	0.589	0.576	0.602	0.953	0.969	0.937
//			modelName: Anaesthetic-857770937
//			CRFStatistics [context=Train, getTotalDuration()=30222]
//			CRFStatistics [context=Test, getTotalDuration()=338]

//	all constraints except organismModel

//	MACRO	Root = 0.593	0.719	0.505	0.603	0.719	0.522	0.984	1.000	0.969
//			MACRO	hasDeliveryMethod = 0.398	0.483	0.339	0.444	0.539	0.378	0.897	0.897	0.897
//			MACRO	hasDosage = 0.854	1.000	0.745	0.868	1.000	0.769	0.984	1.000	0.969
//			MACRO	Cardinality = 0.850	1.000	0.740	0.864	1.000	0.763	0.984	1.000	0.969
//			MACRO	Overall = 0.667	0.779	0.583	0.700	0.804	0.622	0.953	0.969	0.937
//			modelName: Anaesthetic-1477856144
//			CRFStatistics [context=Train, getTotalDuration()=16369]
//			CRFStatistics [context=Test, getTotalDuration()=143]

//	WITH ALL CONSTRAINTS
//	MACRO	Root = 0.636	0.781	0.536	0.668	0.806	0.572	0.953	0.969	0.938
//	MACRO	hasDeliveryMethod = 0.325	0.380	0.283	0.511	0.598	0.446	0.635	0.635	0.635
//	MACRO	hasDosage = 0.854	1.000	0.745	0.875	1.000	0.781	0.976	1.000	0.953
//	MACRO	Cardinality = 0.850	1.000	0.740	0.864	1.000	0.763	0.984	1.000	0.969
//	MACRO	Overall = 0.671	0.836	0.561	0.749	0.858	0.675	0.896	0.974	0.830
//	modelName: Anaesthetic165264315
//	CRFStatistics [context=Train, getTotalDuration()=14403]
//	CRFStatistics [context=Test, getTotalDuration()=82]

//	Wihtout constrints  only 1 root 
//	---------------------------------------
	// MACRO Root = 0.582 0.719 0.490 0.592 0.719 0.505 0.984 1.000 0.969
	// MACRO hasDeliveryMethod = 0.430 0.517 0.368 0.479 0.577 0.410 0.897 0.897
	// 0.897
	// MACRO hasDosage = 0.854 1.000 0.745 0.868 1.000 0.769 0.984 1.000 0.969
	// MACRO Cardinality = 0.850 1.000 0.740 0.864 1.000 0.763 0.984 1.000 0.969
	// MACRO Overall = 0.668 0.771 0.589 0.701 0.796 0.629 0.953 0.969 0.937
	// modelName: Anaesthetic1113428331
	// CRFStatistics [context=Train, getTotalDuration()=20215]
	// CRFStatistics [context=Test, getTotalDuration()=109]
	@Override
	public List<IExplorationStrategy> getAdditionalExplorer() {
		return Collections.emptyList();
//		return Arrays.asList(new RootTemplateCardinalityExplorer(trainingObjectiveFunction.getEvaluator(),
//				EExplorationMode.ANNOTATION_BASED, AnnotationBuilder.toAnnotation(EntityType.get("Anaesthetic"))));
	}
}

//MACRO	Root = 0.582	0.719	0.490	0.592	0.719	0.505	0.984	1.000	0.969
//MACRO	hasDeliveryMethod = 0.377	0.466	0.316	0.443	0.548	0.372	0.849	0.849	0.849
//MACRO	hasDosage = 0.854	1.000	0.745	0.868	1.000	0.769	0.984	1.000	0.969
//MACRO	Cardinality = 0.850	1.000	0.740	0.864	1.000	0.763	0.984	1.000	0.969
//MACRO	Overall = 0.644	0.747	0.566	0.688	0.784	0.614	0.937	0.953	0.922
//modelName: Anaesthetic2052882097
//CRFStatistics [context=Train, getTotalDuration()=14878]
//CRFStatistics [context=Test, getTotalDuration()=165]
