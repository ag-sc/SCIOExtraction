package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EMainClassMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrgModelSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.VertebralAreaRestrictionProvider.EVertebralAreaModifications;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 *
 */
public class VertebralAreaSlotFilling {
	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new VertebralAreaSlotFilling();
	}

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("data/slot_filling/vertebral_area/instances/");
	String dataRandomSeed;
	List<Instance> trainingInstances;
	List<Instance> devInstances;
	List<Instance> testInstances;

	public VertebralAreaSlotFilling() throws IOException {
		InstanceProvider.removeEmptyInstances = false;

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
				 * Finally, we build the systems scope.
				 */
				.build();

//		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
//				.build();
		SlotType.excludeAll();

		EVertebralAreaModifications rule = EVertebralAreaModifications.NO_MODIFICATION;
		SCIOSlotTypes.hasUpperVertebrae.include();
		SCIOSlotTypes.hasLowerVertebrae.include();
		SCIOSlotTypes.hasOrganismSpecies.include();
		SCIOSlotTypes.hasGender.include();
		SCIOSlotTypes.hasWeight.include();
		SCIOSlotTypes.hasAgeCategory.include();
		SCIOSlotTypes.hasAge.include();
		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(1000L)
				.setTrainingProportion(80).setDevelopmentProportion(20).setCorpusSizeFraction(1F).build();

		PrintStream resultsOut = new PrintStream(new File("results/vertebralAreaResults.csv"));

//		InstanceProvider.removeEmptyInstances = false;

		/**
		 * The instance provider reads all json files in the given directory. We can set
		 * the distributor in the constructor. If not all instances should be read from
		 * the file system, we can add an additional parameter that specifies how many
		 * instances should be read. NOTE: in contrast to the corpusSizeFraction in the
		 * ShuffleCorpusDistributor, we initially set a limit to the number of files
		 * that should be read.
		 */
		InstanceProvider instanceProvider;

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
				VertebralAreaRestrictionProvider.getByRule(rule));

		trainingInstances = instanceProvider.getRedistributedTrainingInstances();
		devInstances = instanceProvider.getRedistributedDevelopmentInstances();
		testInstances = instanceProvider.getRedistributedTestInstances();

		dataRandomSeed = "" + new Random().nextInt();

		String modelName = "VertebralArea" + dataRandomSeed;
		VertebralAreaPredictor predictor = new VertebralAreaPredictor(modelName,
				instanceProvider.getRedistributedTrainingInstances().stream().map(t -> t.getName())
						.collect(Collectors.toList()),
				instanceProvider.getRedistributedDevelopmentInstances().stream().map(t -> t.getName())
						.collect(Collectors.toList()),
				instanceProvider.getRedistributedTestInstances().stream().map(t -> t.getName())
						.collect(Collectors.toList()),
				rule, ENERModus.PREDICT);

//		predictor.setOrganismModel(predictOrganismModel(instanceProvider.getInstances()));

		predictor.trainOrLoadModel();

		Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

//		Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);

		Map<String, Score> scoreMap = new HashMap<>();

		Set<SlotType> slotTypesToConsider = new HashSet<>();
		slotTypesToConsider.add(SCIOSlotTypes.hasUpperVertebrae);
		slotTypesToConsider.add(SCIOSlotTypes.hasLowerVertebrae);

		AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE, EEvaluationDetail.LITERAL);

		Map<Instance, State> coverageStates = predictor.coverageOnDevelopmentInstances(SCIOEntityTypes.vertebralArea,
				false);

		System.out.println("---------------------------------------");

//		PerSlotEvaluator.evalRoot(EScoreType.MICRO, finalStates, coverageStates, evaluator);
//
//		PerSlotEvaluator.evalProperties(EScoreType.MICRO, finalStates, coverageStates, slotTypesToConsider,
//				evaluator);
//
//		PerSlotEvaluator.evalCardinality(EScoreType.MICRO, finalStates, coverageStates);

		PerSlotEvaluator.evalRoot(EScoreType.MACRO, finalStates, coverageStates, evaluator, scoreMap);

		PerSlotEvaluator.evalProperties(EScoreType.MACRO, finalStates, coverageStates, slotTypesToConsider, evaluator,
				scoreMap);

		PerSlotEvaluator.evalCardinality(EScoreType.MACRO, finalStates, coverageStates, scoreMap);

		PerSlotEvaluator.evalOverall(EScoreType.MACRO, finalStates, coverageStates, evaluator, scoreMap);

//		predictor.trainOrLoadModel();
//
//		Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();
//
//		Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);
//
//		resultsOut.println(toResults(rule, score));
//		/**
//		 * Finally, we evaluate the produced states and print some statistics.
//		 */
//
//		final Score trainCoverage = predictor.computeCoverageOnTrainingInstances(false);
//		log.info("Coverage Training: " + trainCoverage);
//
//		final Score devCoverage = predictor.computeCoverageOnDevelopmentInstances(false);
//		log.info("Coverage Development: " + devCoverage);

		/**
		 * Computes the coverage of the given instances. The coverage is defined by the
		 * objective mean score that can be reached relying on greedy objective function
		 * sampling strategy. The coverage can be seen as the upper bound of the system.
		 * The upper bound depends only on the exploration strategy, e.g. the provided
		 * NER-annotations during slot-filling.
		 */
//		log.info("results: " + toResults(rule, score));
		log.info("modelName: " + predictor.modelName);
		/**
		 * TODO: Compare results with results when changing some parameter. Implement
		 * more sophisticated feature-templates.
		 */

		resultsOut.flush();
		resultsOut.close();
	}

	private String toResults(EVertebralAreaModifications rule, Score score) {
		return rule.name() + "\t" + resultFormatter.format(score.getF1()) + "\t"
				+ resultFormatter.format(score.getPrecision()) + "\t" + resultFormatter.format(score.getRecall());
	}

	private Map<String, Set<AbstractAnnotation>> predictOrganismModel(List<Instance> instances) {

		/**
		 * TODO: FULL MODEL EXTRACTION CAUSE THIS IS BEST TO PREDICT SPECIES
		 */
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
		OrgModelSlotFillingPredictor predictor = new OrgModelSlotFillingPredictor(
				"OrganismModel_VertebralArea_" + dataRandomSeed, trainingInstanceNames, developInstanceNames,
				testInstanceNames, rule, ENERModus.GOLD);
		predictor.trainOrLoadModel();

		Map<String, Set<AbstractAnnotation>> organismModelAnnotations = predictor.predictInstances(instances, 1)
				.entrySet().stream().collect(Collectors.toMap(a -> a.getKey().getName(), a -> a.getValue()));

		SlotType.restoreExcludance(x);
		return organismModelAnnotations;
	}
}
