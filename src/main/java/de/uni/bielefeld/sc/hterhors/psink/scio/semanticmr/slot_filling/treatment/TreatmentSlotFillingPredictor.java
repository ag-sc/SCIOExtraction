package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.candidateretrieval.sf.SlotFillingCandidateRetrieval.IFilter;
import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.crf.exploration.IExplorationStrategy;
import de.hterhors.semanticmr.crf.exploration.constraints.AbstractHardConstraint;
import de.hterhors.semanticmr.crf.exploration.constraints.HardConstraintsProvider;
import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.learner.optimizer.SGD;
import de.hterhors.semanticmr.crf.learner.regularizer.L2;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.impl.EpochSwitchSampler;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method.DeliveryMethodPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method.DeliveryMethodRestrictionProvider.EDeliveryMethodModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.hardconstraints.DistinctEntityTemplateConstraint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.initializer.GenericMultiCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.TreatmentRestrictionProvider.ETreatmentModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.templates.TreatmentPriorTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.DocumentPartTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.EntityTypeContextTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.wrapper.OrganismModelWrapper;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.wrapper.SCIOWrapper;

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

	public TreatmentSlotFillingPredictor(String modelName, List<String> trainingInstanceNames,
			List<String> developmentInstanceNames, List<String> testInstanceNames, ETreatmentModifications rule,
			ENERModus modus) {
		super(modelName, trainingInstanceNames, developmentInstanceNames, testInstanceNames, rule, modus);
	}

	private Map<String, Set<AbstractAnnotation>> organismModel;

	public void setOrganismModel(Map<String, Set<AbstractAnnotation>> organismModel) {
		this.organismModel = organismModel;
	}

	final public boolean useGoldLocationsForTraining = true;
	final public boolean useGoldLocationsForPrediction = false;

	@Override
	protected Map<Instance, Collection<AbstractAnnotation>> getAdditionalCandidateProvider(IModificationRule _rule) {

		ETreatmentModifications rule = (ETreatmentModifications) _rule;

		Map<Instance, Collection<AbstractAnnotation>> map = new HashMap<>();

		if (rule == ETreatmentModifications.ROOT)
			return map;

		if (useGoldLocationsForTraining) {
			addGold(map, instanceProvider.getTrainingInstances());
		} else {
			addPredictions(map, instanceProvider.getTrainingInstances());
		}
		if (useGoldLocationsForPrediction || modus == ENERModus.GOLD) {
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

		for (Instance instance : instanceProvider.getInstances()) {
			map.putIfAbsent(instance, new ArrayList<>());
			map.get(instance).add(AnnotationBuilder.toAnnotation(SCIOEntityTypes.compoundTreatment));
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

	DeliveryMethodPredictor deliveryMethodPrediction = null;

	private void addPredictions(Map<Instance, Collection<AbstractAnnotation>> map, List<Instance> instances) {
		Map<SlotType, Boolean> z = SlotType.storeExcludance();

		String deliveryMethodModelName = "DeliveryMethod_" + modelName;

		if (deliveryMethodPrediction == null) {

			InstanceProvider.removeEmptyInstances = false;
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
	protected File getExternalNerlaFile() {
		if (modus == ENERModus.GOLD)
			return new File(AnnotationsCorpusBuilderBib.ANNOTATIONS_DIR,
					AnnotationsCorpusBuilderBib.toDirName(SCIOEntityTypes.treatment));
		else
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

		featureTemplates.add(new TreatmentPriorTemplate());

		return featureTemplates;
	}

	@Override
	public int getNumberOfEpochs() {
		return 10;
	}

	@Override
	protected IStateInitializer getStateInitializer() {
//		return new GenericMultiCardinalityInitializer(SCIOEntityTypes.compoundTreatment, 2, 6);

				return new GenericMultiCardinalityInitializer(SCIOEntityTypes.compoundTreatment,
				instanceProvider.getTrainingInstances());
		
//		return (instance -> {
//		return new State(instance, new Annotations(new EntityTemplate(SCIOEntityTypes.compoundTreatment)));
//	});
//		return (instance -> {
//
//			List<AbstractAnnotation> as = new ArrayList<>();
//
//			for (int i = 0; i < 4; i++) {
//				as.add(new EntityTemplate(AnnotationBuilder.toAnnotation(SCIOEntityTypes.compoundTreatment)));
//			}
//			return new State(instance, new Annotations(as));
//			//
//		});

//		return (instance -> {
//			
//			List<AbstractAnnotation> as = new ArrayList<>();
//			
//			for (int i = 0; i <instance.getGoldAnnotations().getAnnotations().size(); i++) {
//				as.add(new EntityTemplate(AnnotationBuilder.toAnnotation(SCIOEntityTypes.compoundTreatment)));
//			}
//			return new State(instance, new Annotations(as));
//			//
//		});
	}

	@Override
	public List<IExplorationStrategy> getAdditionalExplorer() {
		return Collections.emptyList();
//		return Arrays.asList(new RootTemplateCardinalityExplorer(trainingObjectiveFunction.getEvaluator(),
//				EExplorationMode.ANNOTATION_BASED, AnnotationBuilder.toAnnotation(SCIOEntityTypes.compoundTreatment)));
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

	@Override
	public HardConstraintsProvider getHardConstraints() {
		HardConstraintsProvider p = new HardConstraintsProvider();
		p.addHardConstraints(new AbstractHardConstraint() {

			@Override
			public boolean violatesConstraint(State currentState, EntityTemplate entityTemplate, int annotationIndex) {
				return !entityTemplate.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
						.containsSlotFiller();

			}

		});
		p.addHardConstraints(new AbstractHardConstraint() {

			@Override
			public boolean violatesConstraint(State currentState, EntityTemplate entityTemplate, int annotationIndex) {
				int c = 0;
				int index = 0;
				for (AbstractAnnotation currentPrediction : currentState.getCurrentPredictions().getAnnotations()) {

					if (index != annotationIndex && currentPrediction
							.evaluateEquals(predictionObjectiveFunction.getEvaluator(), entityTemplate)) {
						c++;
					}
					index++;
				}

				return c >= 1;

			}

			@Override
			public List<EntityTemplate> violatesConstraint(State currentState,
					List<EntityTemplate> candidateListToFilter, int annotationIndex) {

				if (currentState.getCurrentPredictions().getAbstractAnnotations().size() < 2)
					return candidateListToFilter;

				List<EntityTemplate> filteredList = new ArrayList<>(candidateListToFilter.size());
				Map<SlotType, Boolean> x = SlotType.storeExcludance();
				SlotType.excludeAll();
				SCIOSlotTypes.hasCompound.include();
				filteredList = candidateListToFilter.parallelStream()
						.filter(candidate -> !violatesConstraint(currentState, candidate, annotationIndex))
						.collect(Collectors.toList());

				SlotType.restoreExcludance(x);

				return filteredList;
			}

		});

		p.addHardConstraints(new DistinctEntityTemplateConstraint(predictionObjectiveFunction.getEvaluator()));

		p.addHardConstraints(new AbstractHardConstraint() {

			@Override
			public boolean violatesConstraint(State currentState, EntityTemplate entityTemplate, int annotationIndex) {

				if (organismModel == null)
					return false;

				Set<AbstractAnnotation> orgModels = organismModel.get(currentState.getInstance().getName());
				Set<Integer> sentences = new HashSet<>();

				for (AbstractAnnotation orgModel : orgModels) {

					OrganismModelWrapper w = new OrganismModelWrapper(orgModel.asInstanceOfEntityTemplate());

					sentences.addAll(
							w.getAnnotations().stream().map(a -> a.getSentenceIndex()).collect(Collectors.toSet()));

				}
				int max = 0;

				for (Integer integer : sentences) {
					max = Math.max(max, integer);
				}

				List<DocumentLinkedAnnotation> as = new ArrayList<>();
				Set<Integer> sentences2 = new HashSet<>();

				SCIOWrapper.collectDLA(as, entityTemplate.asInstanceOfEntityTemplate());

//				for (AbstractAnnotation ab : currentState.getGoldAnnotations().getAnnotations()) {

//				SCIOWrapper.collectDLA(as, ab.asInstanceOfEntityTemplate());
				sentences2.addAll(as.stream().map(a -> a.getSentenceIndex()).collect(Collectors.toSet()));
//				}

				for (Integer integer : sentences2) {

					if (max < integer && Math.abs(max - integer) > 50)
						return true;
				}
//				System.out.println(Math.abs(max - min));

				return false;
			}
		});

		return p;

	}
}
