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

import de.hterhors.semanticmr.candidateretrieval.sf.SlotFillingCandidateRetrieval.IFilter;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthetic.AnaestheticPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthetic.AnaestheticPredictorPrediction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthetic.AnaestheticRestrictionProvider.EAnaestheticModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.InjuryRestrictionProvider.EInjuryModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury_device.InjuryDevicePredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury_device.InjuryDevicePredictorPrediction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury_device.InjuryDeviceRestrictionProvider.EInjuryDeviceModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.VertebralAreaRestrictionProvider.EVertebralAreaModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.VertebralLocationPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.VertebralLocationPredictorPrediction;

/**
 * Slot filling for injuries.
 * 
 * 
 * @author hterhors
 *
 */
public class InjurySlotFillingPredictorPrediction extends InjurySlotFillingPredictor {

	private static Logger log = LogManager.getFormatterLogger(InjurySlotFillingPredictorPrediction.class);

	public InjurySlotFillingPredictorPrediction(String modelName, List<String> trainingInstanceNames,
			List<String> developInstanceNames, List<String> testInstanceNames, IModificationRule rule,
			ENERModus modus) {

		super(modelName, trainingInstanceNames, developInstanceNames, testInstanceNames, rule, modus);
	}

	final public boolean useGoldLocationsForTraining = true;
	final public boolean useGoldLocationsForPrediction = false;

	@Override
	protected Map<Instance, Collection<AbstractAnnotation>> getAdditionalCandidateProvider(IModificationRule _rule) {

		EInjuryModifications rule = (EInjuryModifications) _rule;
//
		Map<Instance, Collection<AbstractAnnotation>> map = new HashMap<>();

		if (rule == EInjuryModifications.ROOT || rule == EInjuryModifications.ROOT_DEVICE)
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
					return SCIOSlotTypes.hasInjuryLocation.getSlotFillerEntityTypes()
							.contains(candidate.getEntityType())
							|| SCIOSlotTypes.hasAnaesthesia.getSlotFillerEntityTypes()
									.contains(candidate.getEntityType())
							|| SCIOSlotTypes.hasInjuryDevice.getSlotFillerEntityTypes()
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

	InjuryDevicePredictorPrediction injuryDevicePrediction = null;
	AnaestheticPredictorPrediction anaestheticPrediction = null;
//	VertebralAreaPredictor vertebralAreaPrediction = null;
	VertebralLocationPredictorPrediction VertebralLocationPrediction = null;

	private void addPredictions(Map<Instance, Collection<AbstractAnnotation>> map, List<Instance> instances) {
		Map<SlotType, Boolean> z = SlotType.storeExcludance();
		SlotType.includeAll();
		String injuryDeviceName = "InjuryDevice_" + modelName;

		if (injuryDevicePrediction == null) {
			injuryDevicePrediction = new InjuryDevicePredictorPrediction(injuryDeviceName, trainingInstanceNames,
					developInstanceNames, testInstanceNames, EInjuryDeviceModifications.NO_MODIFICATION, modus);

			injuryDevicePrediction.trainOrLoadModel();

			injuryDevicePrediction.predictAllInstances(1);
		}

		for (Instance instance : instances) {
			map.putIfAbsent(instance, new ArrayList<>());
			map.get(instance).addAll(injuryDevicePrediction.predictHighRecallInstanceByName(instance.getName(), 1));
		}
		SlotType.restoreExcludance(z);

		Map<SlotType, Boolean> x = SlotType.storeExcludance();
		SlotType.includeAll();

		String anaestheticName = "Anaesthetic_" + modelName;
		if (anaestheticPrediction == null) {

			anaestheticPrediction = new AnaestheticPredictorPrediction(anaestheticName, trainingInstanceNames,
					developInstanceNames, testInstanceNames, EAnaestheticModifications.NO_MODIFICATION, modus);

			anaestheticPrediction.trainOrLoadModel();
			anaestheticPrediction.predictAllInstances(1);

		}
		for (Instance instance : instances) {
			map.putIfAbsent(instance, new ArrayList<>());
			map.get(instance).addAll(anaestheticPrediction.predictHighRecallInstanceByName(instance.getName(), 1));
		}

		SlotType.restoreExcludance(x);
		Map<SlotType, Boolean> y = SlotType.storeExcludance();
		SlotType.includeAll();
		String injuryLocationModelName = "VertebralLocation_" + modelName;

		if (VertebralLocationPrediction == null) {

			VertebralLocationPrediction = new VertebralLocationPredictorPrediction(injuryLocationModelName,
					trainingInstanceNames, developInstanceNames, testInstanceNames,
					EVertebralAreaModifications.NO_MODIFICATION, modus);

			VertebralLocationPrediction.trainOrLoadModel();

			VertebralLocationPrediction.predictAllInstances(1);
		}

		for (Instance instance : instances) {
			map.putIfAbsent(instance, new ArrayList<>());
			map.get(instance)
					.addAll(VertebralLocationPrediction.predictHighRecallInstanceByName(instance.getName(), 1));
		}
		SlotType.restoreExcludance(y);

	}

	@Override
	protected File getExternalNerlaFile() {
		return new File("prediction/nerla/");
	}

	@Override
	protected File getInstanceDirectory() {
		return new File("prediction/instances/");
	}

}
