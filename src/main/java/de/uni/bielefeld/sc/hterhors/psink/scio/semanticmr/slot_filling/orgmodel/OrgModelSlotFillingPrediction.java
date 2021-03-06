package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.rdf.ConvertToRDF;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.AgeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 * 
 *         Score: SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE 0.94 0.97 0.91
 *         modelName: OrganismModel-522582779 CRFStatistics [context=Train,
 *         getTotalDuration()=16064] CRFStatistics [context=Test,
 *         getTotalDuration()=617]
 *
 * 
 */
public class OrgModelSlotFillingPrediction {

	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new OrgModelSlotFillingPrediction();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory;

	public final String header = "Mode\tF1\tPrecision\tRecall";

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");
	Map<Instance, State> coverageStates;

	public OrgModelSlotFillingPrediction() throws IOException {

		/**
		 * Initialize the system.
		 * 
		 * The scope represents the specifications of the 4 defined specification files.
		 * The scope mainly affects the exploration.
		 */
		SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result"))
				/**
				 * We apply the scope, so that we can add normalization functions for various
				 * literal entity types, if necessary.
				 */
				.apply()
				/**
				 * Now normalization functions can be added. A normalization function is
				 * especially used for literal-based annotations. In case a normalization
				 * function is provided for a specific entity type, the normalized value is
				 * compared during evaluation instead of the actual surface form. A
				 * normalization function normalizes different surface forms so that e.g. the
				 * weights "500 g", "0.5kg", "500g" are all equal. Each normalization function
				 * is bound to exactly one entity type.
				 */
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization())
				/**
				 * Finally, we build the systems scope.
				 */
				.build();

		instanceDirectory = new File("prediction/instances/");

		InstanceProvider.removeEmptyInstances = false;

		EOrgModelModifications rule = EOrgModelModifications.SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE;
		OrganismModelRestrictionProvider.applySlotTypeRestrictions(rule);
	
		List<String> instanceNames = Arrays.asList(new File("prediction/instances").list());
		Collections.sort(instanceNames);
		int batchCount =0;
		int batachSize =100;
		instanceNames = instanceNames.subList(batchCount * batachSize, (1 + batchCount) * batachSize);

		
		AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
				.setTrainingInstanceNames(instanceNames).build();
//		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(1)
//				.setTrainingProportion(100).setDevelopmentProportion(0).setCorpusSizeFraction(1F).build();

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
				OrganismModelRestrictionProvider.getByRule(rule));

		List<String> trainingInstanceNames = instanceProvider.getTrainingInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> developInstanceNames = instanceProvider.getDevelopmentInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> testInstanceNames = instanceProvider.getTestInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		String modelName = "OrganismModel_PREDICT";

		OrgModelSlotFillingPredictorPrediction predictor = new OrgModelSlotFillingPredictorPrediction(modelName,
				trainingInstanceNames, rule, ENERModus.PREDICT);

		predictor.trainOrLoadModel();

		List<EntityTemplate> annotations = new ArrayList<>();

//		for (Entry<Instance, State> instance : predictor.predictInstance(predictor.getTrainingInstances()).entrySet()) {
//
//			System.out.println(instance.getKey().getName() + "\t"
//					+ instance.getValue().getCurrentPredictions().getAnnotations().get(0).toPrettyString());
//			annotations.addAll(instance.getValue().getCurrentPredictions().getAnnotations());
//		}

//		for (Entry<String, Set<AbstractAnnotation>> instance : predictor.predictAllInstances().entrySet()) {
//
//			for (AbstractAnnotation entityTemplate : instance.getValue()) {
//				System.out.println(instance.getKey() + "\t" + entityTemplate.toPrettyString());
//				annotations.add(entityTemplate.asInstanceOfEntityTemplate());
//			}
//		}
//
//		new ConvertToRDF(new File("organismModel.n-triples"), annotations);

		/**
		 * Computes the coverage of the given instances. The coverage is defined by the
		 * objective mean score that can be reached relying on greedy objective function
		 * sampling strategy. The coverage can be seen as the upper bound of the system.
		 * The upper bound depends only on the exploration strategy, e.g. the provided
		 * NER-annotations during slot-filling.
		 */
		log.info("modelName: " + predictor.modelName);

		log.info(predictor.crf.getTrainingStatistics());
		log.info(predictor.crf.getTestStatistics());
		/**
		 * TODO: Compare results with results when changing some parameter. Implement
		 * more sophisticated feature-templates.
		 */

	}

}
