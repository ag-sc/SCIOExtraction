package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.rdf.ConvertToRDF;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DosageNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrgModelSlotFillingPredictorPrediction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.TreatmentRestrictionProvider.ETreatmentModifications;

/**
 * 
 * @author hterhors
 */
public class TreatmentSlotFillingPrediction {

	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new TreatmentSlotFillingPrediction();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory;

	public final String header = "Mode\tF1\tPrecision\tRecall";

	List<Instance> trainingInstances;
	List<Instance> devInstances;
	List<Instance> testInstances;

	public TreatmentSlotFillingPrediction() throws IOException {
		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).apply()
				.registerNormalizationFunction(new DosageNormalization()).build();

		instanceDirectory = new File("prediction/instances");

		Set<SlotType> slotTypesToConsider = new HashSet<>();
		slotTypesToConsider.add(SCIOSlotTypes.hasDeliveryMethod);
		slotTypesToConsider.add(SCIOSlotTypes.hasDirection);
		slotTypesToConsider.add(SCIOSlotTypes.hasApplicationInstrument);
		slotTypesToConsider.add(SCIOSlotTypes.hasDosage);
		slotTypesToConsider.add(SCIOSlotTypes.hasCompound);
		slotTypesToConsider.add(SCIOSlotTypes.hasVoltage);
		slotTypesToConsider.add(SCIOSlotTypes.hasRehabMedication);
		slotTypesToConsider.add(SCIOSlotTypes.hasElectricFieldStrength);

		ETreatmentModifications rule = ETreatmentModifications.DOSAGE_DELIVERY_METHOD_APPLICATION_INSTRUMENT_DIRECTION;

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(1000L)
				.setTrainingProportion(100).setDevelopmentProportion(0).setCorpusSizeFraction(1F).build();

		InstanceProvider.removeEmptyInstances = false;

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
				TreatmentRestrictionProvider.getByRule(rule));

		List<String> trainingInstanceNames = instanceProvider.getTrainingInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> developInstanceNames = instanceProvider.getDevelopmentInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> testInstanceNames = instanceProvider.getTestInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		String modelName = "Treatment_PREDICT";

		trainingInstances = instanceProvider.getTrainingInstances();
		devInstances = instanceProvider.getDevelopmentInstances();
		testInstances = instanceProvider.getTestInstances();

		TreatmentSlotFillingPredictorPrediction predictor = new TreatmentSlotFillingPredictorPrediction(modelName,
				trainingInstanceNames, rule, ENERModus.PREDICT);

		SCIOSlotTypes.hasDirection.slotMaxCapacity = 3;

//		predictor.setOrganismModel(predictOrganismModel(instanceProvider.getInstances()));

		predictor.trainOrLoadModel();

//		Map<String, List<EntityTemplate>> ansMap = new HashMap<>();

//		List<EntityTemplate> annotations = new ArrayList<>();
//
//		for (Entry<String, Set<AbstractAnnotation>> instance : predictor.predictAllInstances().entrySet()) {
//
//			for (AbstractAnnotation entityTemplate : instance.getValue()) {
//				System.out.println(instance.getKey() + "\t" + entityTemplate.toPrettyString());
//				annotations.add(entityTemplate.asInstanceOfEntityTemplate());
//			}
//		}

//			List<EntityTemplate> ans = new ArrayList<>();
//			for (AbstractAnnotation entityTemplate : instance.getValue().getCurrentPredictions().getAnnotations()) {
//				System.out.println(instance.getKey() + "\t" + entityTemplate.toPrettyString());
//				ans.add(entityTemplate.asInstanceOfEntityTemplate());
//			}
//			ansMap.put(instance.getKey().getName(), ans);

//		File outPutFile = new File("treatment.n-triples");

//		new ConvertToRDF(outPutFile, ansMap);
	}

	private Map<String, Set<AbstractAnnotation>> predictOrganismModel(List<Instance> instances) {

		EOrgModelModifications rule = EOrgModelModifications.SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE;

		/**
		 * Predict OrganismModels
		 */
		Map<SlotType, Boolean> x = SlotType.storeExcludance();
		OrganismModelRestrictionProvider.applySlotTypeRestrictions(rule);

		List<String> trainingInstanceNames = trainingInstances.stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> developInstanceNames = devInstances.stream().map(t -> t.getName()).collect(Collectors.toList());

		List<String> testInstanceNames = testInstances.stream().map(t -> t.getName()).collect(Collectors.toList());
//	+ modelName
		OrgModelSlotFillingPredictorPrediction predictor = new OrgModelSlotFillingPredictorPrediction(
				"OrganismModel_PREDICTION", trainingInstanceNames, rule, ENERModus.PREDICT);
		predictor.trainOrLoadModel();

		Map<String, Set<AbstractAnnotation>> organismModelAnnotations = predictor.predictInstances(instances, 1)
				.entrySet().stream().collect(Collectors.toMap(a -> a.getKey().getName(), a -> a.getValue()));

		SlotType.restoreExcludance(x);
		return organismModelAnnotations;
	}
}
