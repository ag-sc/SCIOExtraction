package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.templates.et.ContextBetweenSlotFillerTemplate;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthesia.AnaestheticPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthesia.AnaestheticRestrictionProvider.EAnaestheticModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.InjuryRestrictionProvider.EInjuryModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury_device.InjuryDevicePredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury_device.InjuryDeviceRestrictionProvider.EInjuryDeviceModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.VertebralAreaPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.VertebralAreaRestrictionProvider.EVertebralAreaModifications;

/**
 * Slot filling for injuries.
 * 
 * 
 * @author hterhors
 *
 */
public class InjurySlotFillingPredictor extends AbstractSlotFillingPredictor {

	private static Logger log = LogManager.getFormatterLogger(InjurySlotFillingPredictor.class);

	public InjurySlotFillingPredictor(String modelName, List<String> trainingInstanceNames,
			List<String> developInstanceNames, List<String> testInstanceNames, IModificationRule rule) {

		super(modelName, trainingInstanceNames, developInstanceNames, testInstanceNames, rule);
	}

	final public boolean useGoldLocationsForTraining = true;
	final public boolean useGoldLocationsForPrediction = true;

	@Override
	protected Map<Instance, Collection<AbstractAnnotation>> getAdditionalCandidateProvider(IModificationRule _rule) {

		EInjuryModifications rule = (EInjuryModifications) _rule;

		Map<Instance, Collection<AbstractAnnotation>> map = new HashMap<>();

		if (rule == EInjuryModifications.ROOT || rule == EInjuryModifications.ROOT_DEVICE)
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
		return map;
	}

	private void addGold(Map<Instance, Collection<AbstractAnnotation>> map, List<Instance> instances) {
		for (Instance instance : instances) {
			map.putIfAbsent(instance, new ArrayList<>());

			map.get(instance)
					.addAll(instance.getGoldAnnotations().getAnnotations().stream()
							.map(a -> a.asInstanceOfEntityTemplate()
									.getSingleFillerSlot(SCIOSlotTypes.hasInjuryLocation).getSlotFiller())
							.filter(a -> a != null).collect(Collectors.toSet()));
			map.get(instance)
					.addAll(instance.getGoldAnnotations().getAnnotations().stream()
							.flatMap(a -> a.asInstanceOfEntityTemplate()
									.getMultiFillerSlot(SCIOSlotTypes.hasAnaesthesia).getSlotFiller().stream())
							.filter(a -> a != null).collect(Collectors.toSet()));
			map.get(instance)
					.addAll(instance
							.getGoldAnnotations().getAnnotations().stream().map(a -> a.asInstanceOfEntityTemplate()
									.getSingleFillerSlot(SCIOSlotTypes.hasInjuryDevice).getSlotFiller())
							.filter(a -> a != null).collect(Collectors.toSet()));

		}
	}

	private void addPredictions(Map<Instance, Collection<AbstractAnnotation>> map, List<Instance> instances) {
		Map<SlotType, Boolean> z = SlotType.storeExcludance();
		SlotType.includeAll();
		String injuryDeviceName = "InjuryDevice_" + modelName;
		InjuryDevicePredictor injuryDevicePrediction = new InjuryDevicePredictor(injuryDeviceName,
				trainingInstanceNames, developInstanceNames, testInstanceNames,
				EInjuryDeviceModifications.NO_MODIFICATION);

		injuryDevicePrediction.trainOrLoadModel();

		injuryDevicePrediction.predictAllInstances(1);

		for (Instance instance : instances) {
			map.putIfAbsent(instance, new ArrayList<>());
			map.get(instance).addAll(injuryDevicePrediction.predictHighRecallInstanceByName(instance.getName(), 1));
		}
		SlotType.restoreExcludance(z);

		Map<SlotType, Boolean> x = SlotType.storeExcludance();
		SlotType.includeAll();

		String anaestheticName = "Anaesthetic_" + modelName;
		AnaestheticPredictor anaestheticPrediction = new AnaestheticPredictor(anaestheticName, trainingInstanceNames,
				developInstanceNames, testInstanceNames, EAnaestheticModifications.NO_MODIFICATION);

		anaestheticPrediction.trainOrLoadModel();
		anaestheticPrediction.predictAllInstances(1);

		for (Instance instance : instances) {
			map.putIfAbsent(instance, new ArrayList<>());
			map.get(instance).addAll(anaestheticPrediction.predictHighRecallInstanceByName(instance.getName(), 1));
		}

		SlotType.restoreExcludance(x);

		Map<SlotType, Boolean> y = SlotType.storeExcludance();
		SlotType.includeAll();
//		String vertebralAreaModelName = "VertebralArea_STD";
		String vertebralAreaModelName = "VertebralArea_" + modelName;
		VertebralAreaPredictor vertebralAreaPrediction = new VertebralAreaPredictor(vertebralAreaModelName,
				trainingInstanceNames, developInstanceNames, testInstanceNames,
				EVertebralAreaModifications.NO_MODIFICATION);

		vertebralAreaPrediction.trainOrLoadModel();

		vertebralAreaPrediction.predictAllInstances(1);

		for (Instance instance : instances) {
			map.putIfAbsent(instance, new ArrayList<>());
			map.get(instance).addAll(vertebralAreaPrediction.predictHighRecallInstanceByName(instance.getName(), 1));
		}
		SlotType.restoreExcludance(y);

	}

	@Override
	protected File getExternalNerlaFile() {
//		standard: Score [ getF1()=0.415, getPrecision()=0.560, getRecall()=0.330, tp=61, fp=48, fn=124, tn=0]
//				only root: Score [ getF1()=0.571, getPrecision()=0.600, getRecall()=0.545, tp=24, fp=16, fn=20, tn=0]
//				CRFStatistics [context=Train, getTotalDuration()=91841]
//				CRFStatistics [context=Test, getTotalDuration()=491]
//		return SlotFillingCorpusBuilderBib.getDefaultRegExNerlaDir(SCIOEntityTypes.injury);

//		standard: Score [ getF1()=0.439, getPrecision()=0.618, getRecall()=0.341, tp=63, fp=39, fn=122, tn=0]
//				only root: Score [ getF1()=0.571, getPrecision()=0.600, getRecall()=0.545, tp=24, fp=16, fn=20, tn=0]
//				CRFStatistics [context=Train, getTotalDuration()=79462]
//				CRFStatistics [context=Test, getTotalDuration()=312]

//		return new File("src/main/resources/additional_nerla/injury/LITERAL");

//		standard: Score [ getF1()=0.425, getPrecision()=0.598, getRecall()=0.330, tp=61, fp=41, fn=124, tn=0]
//				only root: Score [ getF1()=0.548, getPrecision()=0.575, getRecall()=0.523, tp=23, fp=17, fn=21, tn=0]
//				CRFStatistics [context=Train, getTotalDuration()=78526]
//				CRFStatistics [context=Test, getTotalDuration()=244]
//				Compute coverage...
		return new File("data/additional_nerla/injury/DOCUMENT_LINKED");
	}

	@Override
	protected File getInstanceDirectory() {
		return SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.injury);
	}

	@Override
	protected AdvancedLearner getLearner() {
		return new AdvancedLearner(new SGD(0.00001, 0), new L2(0.00));
	}

//	@Override
//	protected List<AbstractFeatureTemplate<?>> getFeatureTemplates() {
//		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();
////		featureTemplates.add(new LevenshteinTemplate());
//		featureTemplates.add(new IntraTokenTemplate());
////		featureTemplates.add(new DistinctMultiValueSlotsTemplate());
////		featureTemplates.add(new MultiValueSlotSizeTemplate());
//		featureTemplates.add(new NGramTokenContextTemplate());
//		featureTemplates.add(new SingleTokenContextTemplate());
//		featureTemplates.add(new ContextBetweenSlotFillerTemplate());
////		featureTemplates.add(new ClusterTemplate());
////		featureTemplates.add(new EntityTypeContextTemplate());
////		featureTemplates.add(new OlfactoryContextTemplate());
//		featureTemplates.add(new DocumentPartTemplate());
////		featureTemplates.add(new LocalityTemplate());
////		featureTemplates.add(new SlotIsFilledTemplate());
////		featureTemplates.add(new PriorNumericInterpretationInjuryTemplate(trainingInstances));
////		featureTemplates.add(new NumericInterpretationTemplate());
//
//		return featureTemplates;
//	}
	@Override
	protected List<AbstractFeatureTemplate<?>> getFeatureTemplates() {
		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();
//		featureTemplates.add(new LevenshteinTemplate());
		featureTemplates.add(new IntraTokenTemplate());
//		featureTemplates.add(new DistinctMultiValueSlotsTemplate());
//		featureTemplates.add(new MultiValueSlotSizeTemplate());
		featureTemplates.add(new NGramTokenContextTemplate());
		featureTemplates.add(new SingleTokenContextTemplate());
		featureTemplates.add(new ContextBetweenSlotFillerTemplate());
//		featureTemplates.add(new ClusterTemplate());
//		featureTemplates.add(new EntityTypeContextTemplate());

//		featureTemplates.add(new DocumentPartTemplate());
//		featureTemplates.add(new LocalityTemplate());
//		featureTemplates.add(new SlotIsFilledTemplate());
//
//		featureTemplates.add(new InjuryDeviceTemplate());
//		featureTemplates.add(new DocumentSectionTemplate());
//
//		featureTemplates.add(new AnnotationExistsInAbstractTemplate());

//		featureTemplates.add(new OlfactoryContextTemplate());
//		featureTemplates.add(new PriorNumericInterpretationInjuryTemplate(trainingInstances));
//		featureTemplates.add(new NumericInterpretationTemplate());

		return featureTemplates;
	}

	@Override
	protected int getNumberOfEpochs() {
		return 10;
	}

//	mean score: Score [getF1()=0.333, getPrecision()=0.344, getRecall()=0.324, tp=11, fp=21, fn=23, tn=0]
//			CRFStatistics [context=Train, getTotalDuration()=121168]
//			CRFStatistics [context=Test, getTotalDuration()=192]
//			modelName: Injury7669
//
//	mean score: Score [getF1()=0.303, getPrecision()=0.312, getRecall()=0.294, tp=10, fp=22, fn=24, tn=0]
//			CRFStatistics [context=Train, getTotalDuration()=122682]
//			CRFStatistics [context=Test, getTotalDuration()=220]
//			modelName: Injury1444
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
	protected Collection<GoldModificationRule> getGoldModificationRules(IModificationRule rule) {
		return InjuryRestrictionProvider.getByRule((EInjuryModifications) rule);
	}

//	@Override
//	public HardConstraintsProvider getHardConstraints() {
//
//		HardConstraintsProvider hcp = new HardConstraintsProvider();
//		hcp.addHardConstraints(new AbstractHardConstraint() {
//
//			@Override
//			public boolean violatesConstraint(State state, EntityTemplate entityTemplate) {
//
//				if (entityTemplate.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()) {
//					final ESection section = AutomatedSectionifcation.getInstance(state.getInstance())
//							.getSection(entityTemplate.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation()
//									.getSentenceIndex());
//					if (!(section == ESection.BEGIN || section == ESection.ABSTRACT) || entityTemplate
//							.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation().getSentenceIndex() > 15)
//						return true;
//				}
//
//				return false;
//			}
//
//		});
//		return hcp;
//	}
}
