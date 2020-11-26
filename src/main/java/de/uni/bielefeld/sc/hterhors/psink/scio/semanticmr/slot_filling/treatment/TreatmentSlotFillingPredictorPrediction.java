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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method.DeliveryMethodPredictorPrediction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method.DeliveryMethodRestrictionProvider.EDeliveryMethodModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.hardconstraints.DistinctEntityTemplateConstraint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.initializer.GenericMultiCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.TreatmentRestrictionProvider.ETreatmentModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.templates.TreatmentPriorTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.DocumentPartTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.EntityTypeContextTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.wrapper.OrganismModelWrapper;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.wrapper.SCIOWrapper;

public class TreatmentSlotFillingPredictorPrediction extends TreatmentSlotFillingPredictor {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	public TreatmentSlotFillingPredictorPrediction(String modelName, List<String> trainingInstanceNames,
			ETreatmentModifications rule, ENERModus modus) {
		super(modelName, trainingInstanceNames, new ArrayList<>(), new ArrayList<>(), rule, modus);
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

	DeliveryMethodPredictorPrediction deliveryMethodPrediction = null;

	private void addPredictions(Map<Instance, Collection<AbstractAnnotation>> map, List<Instance> instances) {
		Map<SlotType, Boolean> z = SlotType.storeExcludance();

		String deliveryMethodModelName = "DeliveryMethod_PREDICT";

		if (deliveryMethodPrediction == null) {

			deliveryMethodPrediction = new DeliveryMethodPredictorPrediction(deliveryMethodModelName,
					trainingInstanceNames, developInstanceNames, testInstanceNames,
					EDeliveryMethodModifications.ROOT_LOCATION_DURATION, modus);

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
		return new File("prediction/nerla");
	}

	@Override
	protected File getInstanceDirectory() {
		return new File("prediction/instances");
	}
}
