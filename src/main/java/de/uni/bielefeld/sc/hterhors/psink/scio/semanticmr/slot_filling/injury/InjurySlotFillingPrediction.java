package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.examples.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.rdf.ConvertToRDF;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AnalyzeComplexity;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DistanceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DosageNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DurationNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ForceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.LengthNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.PressureNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ThicknessNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.VolumeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.InjuryRestrictionProvider.EInjuryModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.Stats;

public class InjurySlotFillingPrediction {

	public static void main(String[] args) throws IOException {
		new InjurySlotFillingPrediction();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private File instanceDirectory;

	public final String header = "Mode\tF1\tPrecision\tRecall";

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	public InjurySlotFillingPrediction() throws IOException {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Injury")).apply()
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new DosageNormalization())
				.registerNormalizationFunction(new DurationNormalization())
				.registerNormalizationFunction(new VolumeNormalization())
				.registerNormalizationFunction(new ForceNormalization())
				.registerNormalizationFunction(new ThicknessNormalization())
				.registerNormalizationFunction(new PressureNormalization())
				.registerNormalizationFunction(new LengthNormalization())
				.registerNormalizationFunction(new DistanceNormalization())

				.build();

		instanceDirectory = new File("prediction/instances/");

		Set<SlotType> slotTypesToConsider = new HashSet<>();
		slotTypesToConsider.add(SCIOSlotTypes.hasInjuryDevice);
		slotTypesToConsider.add(SCIOSlotTypes.hasInjuryLocation);
		slotTypesToConsider.add(SCIOSlotTypes.hasAnaesthesia);
		slotTypesToConsider.add(SCIOSlotTypes.hasInjuryIntensity);

		EInjuryModifications rule = EInjuryModifications.ROOT_DEVICE_LOCATION_ANAESTHESIA;

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(1000L)
				.setTrainingProportion(100).setCorpusSizeFraction(1F).build();

		InjuryRestrictionProvider.applySlotTypeRestrictions(rule);
		InstanceProvider.removeEmptyInstances = false;
		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
				InjuryRestrictionProvider.getByRule(rule));

		List<String> trainingInstanceNames = instanceProvider.getTrainingInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> developInstanceNames = instanceProvider.getDevelopmentInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> testInstanceNames = instanceProvider.getTestInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		String modelName = "Injury_PREDICTION";

		InjurySlotFillingPredictorPrediction predictor = new InjurySlotFillingPredictorPrediction(modelName,
				trainingInstanceNames, developInstanceNames, testInstanceNames, rule, ENERModus.PREDICT);

		predictor.trainOrLoadModel();
		
		List<EntityTemplate> annotations = new ArrayList<>();

		for (Entry<String, Set<AbstractAnnotation>> instance : predictor.predictAllInstances().entrySet()) {

			for (AbstractAnnotation entityTemplate : instance.getValue()) {
				System.out.println(instance.getKey() + "\t" + entityTemplate.toPrettyString());
				annotations.add(entityTemplate.asInstanceOfEntityTemplate());
			}
		}

		new ConvertToRDF(new File("injury.n-triples"), annotations);

	}

}
