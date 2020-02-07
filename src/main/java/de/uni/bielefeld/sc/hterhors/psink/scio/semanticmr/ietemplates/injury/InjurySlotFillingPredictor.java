package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury;

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

import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.learner.optimizer.SGD;
import de.hterhors.semanticmr.crf.learner.regularizer.L2;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.impl.EpochSwitchSampler;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.InjuryRestrictionProvider.EInjuryModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea.VertebralAreaPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea.VertebralAreaRestrictionProvider.EVertebralAreaModifications;

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
			List<String> developInstanceNames, List<String> testInstanceNames, IModificationRule rule) {

		super(modelName, scope, trainingInstanceNames, developInstanceNames, testInstanceNames, rule);
	}

	final public boolean useGoldLocations = false;

	@Override
	protected Map<Instance, Collection<AbstractAnnotation>> getAdditionalCandidateProvider(IModificationRule _rule) {

		EInjuryModifications rule = (EInjuryModifications) _rule;
		Map<Instance, Collection<AbstractAnnotation>> map = new HashMap<>();
		if (!useGoldLocations) {

			if (rule == EInjuryModifications.ROOT || rule == EInjuryModifications.ROOT_DEVICE)
				return Collections.emptyMap();

//		String vertebralAreaModelName = "VertebralArea_STD";
			String vertebralAreaModelName = "VertebralArea_" + modelName;
			VertebralAreaPredictor vertebralAreaPrediction = new VertebralAreaPredictor(vertebralAreaModelName, scope,
					trainingInstanceNames, developInstanceNames, testInstanceNames,
					EVertebralAreaModifications.NO_MODIFICATION);

			vertebralAreaPrediction.trainOrLoadModel();

			vertebralAreaPrediction.predictAllInstances(2);

			for (Instance instance : instanceProvider.getInstances()) {
				map.put(instance, new ArrayList<>());
				map.get(instance)
						.addAll(vertebralAreaPrediction.predictHighRecallInstanceByName(instance.getName(), 2));
			}
		} else {
			for (Instance instance : instanceProvider.getInstances()) {
				map.put(instance, new ArrayList<>());

				map.get(instance)
						.addAll(instance
								.getGoldAnnotations().getAnnotations().stream().map(a -> a.asInstanceOfEntityTemplate()
										.getSingleFillerSlot(SCIOSlotTypes.hasLocation).getSlotFiller())
								.filter(a -> a != null).collect(Collectors.toSet()));

			}
		}
		return map;
	}

	@Override
	protected File getExternalNerlaFile() {
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
