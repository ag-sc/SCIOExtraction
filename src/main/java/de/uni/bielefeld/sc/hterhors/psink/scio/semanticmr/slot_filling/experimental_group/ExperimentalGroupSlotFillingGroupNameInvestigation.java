package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer;
import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer.EExplorationMode;
import de.hterhors.semanticmr.crf.of.IObjectiveFunction;
import de.hterhors.semanticmr.crf.of.SlotFillingObjectiveFunction;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.Instance.DeduplicationRule;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.helper.GroupNameExtraction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.AgeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DistanceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DosageNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DurationNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ForceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.LengthNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.PressureNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ThicknessNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.VolumeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EAssignmentMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.ECardinalityMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EComplexityMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EDistinctGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EExtractGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EGroupNamesClusteringMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EMainClassMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.InjuryRestrictionProvider.EInjuryModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.TreatmentRestrictionProvider.ETreatmentModifications;

public class ExperimentalGroupSlotFillingGroupNameInvestigation extends AbstractSemReadProject {

	private Map<Instance, State> resultsTest;
	private Map<Instance, State> coverageTest;

	public static void main(String[] args) throws Exception {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(
						DataStructureLoader.loadSlotFillingDataStructureReader("DefinedExperimentalGroup"))
				.apply().registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization())
				.registerNormalizationFunction(new WeightNormalization())
				//
				.registerNormalizationFunction(new DosageNormalization())
				.registerNormalizationFunction(new DurationNormalization())
				.registerNormalizationFunction(new VolumeNormalization())
				.registerNormalizationFunction(new ForceNormalization())
				.registerNormalizationFunction(new ThicknessNormalization())
				.registerNormalizationFunction(new PressureNormalization())
				.registerNormalizationFunction(new LengthNormalization())
				.registerNormalizationFunction(new DistanceNormalization())

				.build();

		int modusIndex;
		long dataRandomSeed;

		if (args.length == 0) {
//			modusIndex = 17; // GOLD
			modusIndex = 18; // PREDICTED
//			modusIndex = 19; // COVERGAE GOLD
//			modusIndex = 20; // COVERGAE PREDICT
			dataRandomSeed = 1004L;
		} else {
			modusIndex = Integer.parseInt(args[0]);
			dataRandomSeed = Integer.parseInt(args[1]);
		}

		run(modusIndex, dataRandomSeed);

	}

	private static void run(int modusIndex, long dataRandomSeed) throws Exception {
		List<String> docs = Files.readAllLines(new File("src/main/resources/corpus_docs.csv").toPath());
//		List<String> trainingInstanceNames = docs;
//		List<String> testInstanceNames = new ArrayList<>();

		/**
		 * TODO: FULL MODEL
		 */

		Collections.sort(docs);

		Collections.shuffle(docs, new Random(dataRandomSeed));

		final int x = (int) (((double) docs.size() / 100D) * 80D);
		List<String> trainingInstanceNames = docs.subList(0, x);
		List<String> testInstanceNames = docs.subList(x, docs.size());

		List<Instance> trainingInstances;
		List<Instance> devInstances;
		List<Instance> testInstances;
		InstanceProvider instanceProvider;

		File instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.definedExperimentalGroup);

		AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
				.setTrainingInstanceNames(trainingInstanceNames).setTestInstanceNames(testInstanceNames).build();

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		trainingInstances = instanceProvider.getTrainingInstances();
		devInstances = instanceProvider.getDevelopmentInstances();
		testInstances = instanceProvider.getTestInstances();

		ExperimentalGroupSlotFillingGroupNameInvestigation a = new ExperimentalGroupSlotFillingGroupNameInvestigation(
				modusIndex, dataRandomSeed,
				trainingInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
				devInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
				testInstances.stream().map(i -> i.getName()).collect(Collectors.toList()));

	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private final IObjectiveFunction trainingObjectiveFunction;

	private final IObjectiveFunction predictionObjectiveFunction;

	private InstanceProvider instanceProvider;

	private List<Instance> trainingInstances;
	private List<Instance> devInstances;
	private List<Instance> testInstances;

	public EMainClassMode mainClassProviderMode;
	public EExtractGroupNamesMode groupNameProviderMode;
	public EGroupNamesClusteringMode groupNameClusteringMode;
	public ECardinalityMode cardinalityMode;
	public EAssignmentMode assignmentMode;
	public EComplexityMode complexityMode;

	private ETreatmentModifications treatmentModificationsRule;
	private EOrgModelModifications orgModelModificationsRule;
	private EInjuryModifications injuryModificationRule;

	private EDistinctGroupNamesMode distinctGroupNamesMode;

	private EExplorationMode explorationMode;

	private String modelName;
	private File outputFile;
	private File instanceDirectory;

	public void setParameterByID(int id) {

		if (id == -1) {
			/**
			 * Test in eclipse
			 */
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.GROUP_NAME;
			complexityMode = EComplexityMode.ROOT;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.GOLD;
			groupNameClusteringMode = EGroupNamesClusteringMode.NONE;
			cardinalityMode = ECardinalityMode.SAMPLE;

		} else if (id == 0) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
			complexityMode = EComplexityMode.ROOT;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.GOLD;
			groupNameClusteringMode = EGroupNamesClusteringMode.GOLD_CLUSTERING;
			cardinalityMode = ECardinalityMode.GOLD;
		} else if (id == 1) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
			complexityMode = EComplexityMode.ROOT;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.GOLD;
			groupNameClusteringMode = EGroupNamesClusteringMode.WEKA_CLUSTERING;
			cardinalityMode = ECardinalityMode.PARALLEL;
		} else if (id == 2) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
			complexityMode = EComplexityMode.ROOT;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.GOLD;
			groupNameClusteringMode = EGroupNamesClusteringMode.WEKA_CLUSTERING;
			cardinalityMode = ECardinalityMode.PARALLEL_MODEL_UPDATE;

		} else if (id == 3) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
			complexityMode = EComplexityMode.ROOT;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.GOLD;
			groupNameClusteringMode = EGroupNamesClusteringMode.WEKA_CLUSTERING;
			cardinalityMode = ECardinalityMode.RSS_PREDICTED;

		} else if (id == 4) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
			complexityMode = EComplexityMode.ROOT;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.GOLD;
			groupNameClusteringMode = EGroupNamesClusteringMode.WEKA_CLUSTERING;
			cardinalityMode = ECardinalityMode.RSS_PREDICTED_SAMPLE;
		} else if (id == 5) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
			complexityMode = EComplexityMode.ROOT;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.GOLD;
			groupNameClusteringMode = EGroupNamesClusteringMode.NONE;
			cardinalityMode = ECardinalityMode.SAMPLE;

		} else if (id == 6) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
			complexityMode = EComplexityMode.ROOT;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.PREDICTED;
			groupNameClusteringMode = EGroupNamesClusteringMode.WEKA_CLUSTERING;
			cardinalityMode = ECardinalityMode.PARALLEL;

		} else if (id == 7) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
			complexityMode = EComplexityMode.ROOT;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.PREDICTED;
			groupNameClusteringMode = EGroupNamesClusteringMode.WEKA_CLUSTERING;
			cardinalityMode = ECardinalityMode.PARALLEL_MODEL_UPDATE;

		} else if (id == 8) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
			complexityMode = EComplexityMode.ROOT;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.PREDICTED;
			groupNameClusteringMode = EGroupNamesClusteringMode.WEKA_CLUSTERING;
			cardinalityMode = ECardinalityMode.RSS_PREDICTED;

		} else if (id == 9) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
			complexityMode = EComplexityMode.ROOT;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.PREDICTED;
			groupNameClusteringMode = EGroupNamesClusteringMode.WEKA_CLUSTERING;
			cardinalityMode = ECardinalityMode.RSS_PREDICTED_SAMPLE;

		} else if (id == 10) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
			complexityMode = EComplexityMode.ROOT;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.PREDICTED;
			groupNameClusteringMode = EGroupNamesClusteringMode.NONE;
			cardinalityMode = ECardinalityMode.SAMPLE;
		} else if (id == 11) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
			complexityMode = EComplexityMode.ROOT;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.EMPTY;
			groupNameClusteringMode = EGroupNamesClusteringMode.NONE;
			cardinalityMode = ECardinalityMode.SAMPLE;
		} else if (id == 12) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
			complexityMode = EComplexityMode.ROOT;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.EMPTY;
			groupNameClusteringMode = EGroupNamesClusteringMode.NONE;
			cardinalityMode = ECardinalityMode.PARALLEL;
		} else if (id == 13) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
			complexityMode = EComplexityMode.ROOT;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.EMPTY;
			groupNameClusteringMode = EGroupNamesClusteringMode.NONE;
			cardinalityMode = ECardinalityMode.PARALLEL_MODEL_UPDATE;
		} else if (id == 14) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.GROUP_NAME;
			complexityMode = EComplexityMode.ROOT;
			explorationMode = EExplorationMode.ANNOTATION_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.GOLD;
			groupNameClusteringMode = EGroupNamesClusteringMode.NONE;
			cardinalityMode = ECardinalityMode.SAMPLE;
		} else if (id == 15) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.GROUP_NAME;
			complexityMode = EComplexityMode.ROOT;
			explorationMode = EExplorationMode.ANNOTATION_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.PREDICTED;
			groupNameClusteringMode = EGroupNamesClusteringMode.NONE;
			cardinalityMode = ECardinalityMode.SAMPLE;
		} else if (id == 16) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
			complexityMode = EComplexityMode.FULL;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.PREDICTED;
			groupNameClusteringMode = EGroupNamesClusteringMode.WEKA_CLUSTERING;
			cardinalityMode = ECardinalityMode.PARALLEL_MODEL_UPDATE;
		} else if (id == 17) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT;
			complexityMode = EComplexityMode.FULL;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.GOLD;

			groupNameProviderMode = EExtractGroupNamesMode.GOLD;
			groupNameClusteringMode = EGroupNamesClusteringMode.WEKA_CLUSTERING;
			cardinalityMode = ECardinalityMode.PARALLEL_MODEL_UPDATE;
		} else if (id == 18) {
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT;
			complexityMode = EComplexityMode.FULL;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.MANUAL_PATTERN_NP_CHUNKS;
			groupNameClusteringMode = EGroupNamesClusteringMode.WEKA_CLUSTERING;
			cardinalityMode = ECardinalityMode.PARALLEL_MODEL_UPDATE;
		} else if (id == 19) {
			/*
			 * COVERAGE MODE GOLD !!!
			 */
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
			complexityMode = EComplexityMode.FULL;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.GOLD;

			groupNameProviderMode = EExtractGroupNamesMode.GOLD;
			groupNameClusteringMode = EGroupNamesClusteringMode.WEKA_CLUSTERING;
			cardinalityMode = ECardinalityMode.PARALLEL_MODEL_UPDATE;
		} else if (id == 20) {
			/*
			 * COVERAGE MODE PREDICT!!!
			 */
			distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
			assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
			complexityMode = EComplexityMode.FULL;
			explorationMode = EExplorationMode.TYPE_BASED;

			mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

			groupNameProviderMode = EExtractGroupNamesMode.MANUAL_PATTERN_NP_CHUNKS;
			groupNameClusteringMode = EGroupNamesClusteringMode.WEKA_CLUSTERING;
			cardinalityMode = ECardinalityMode.PARALLEL_MODEL_UPDATE;
		}

		log.info("explorationMode: " + explorationMode);
		log.info("assignmentMode: " + assignmentMode);
		log.info("cardinalityMode: " + cardinalityMode);
		log.info("groupNameClusteringMode: " + groupNameClusteringMode);
		log.info("groupNameProviderMode: " + groupNameProviderMode);
		log.info("mainClassProviderMode: " + mainClassProviderMode);
		log.info("distinctGroupNamesMode: " + distinctGroupNamesMode);
		log.info("complexityMode: " + complexityMode);
	}

	private final String rand;

	private File cacheDir = new File("data/cache/");
	public static int maxCacheSize = 80_000_000;
	public static int minCacheSize = 40_000_000;

	private ENERModus modus;
	public Map<Instance, Set<EntityTemplate>> extraction = new HashMap<>();

	public ExperimentalGroupSlotFillingGroupNameInvestigation(int parameterID, long dataRandomSeed,
			List<String> trainingInstanceNames, List<String> devInstanceNames, List<String> testInstanceNames)
			throws Exception {
		instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.definedExperimentalGroup);

		EScoreType scoreType = EScoreType.MACRO;

//		CartesianEvaluator.MAXIMUM_PERMUTATION_SIZE = 8;

		SlotFillingExplorer.MAX_NUMBER_OF_ANNOTATIONS = 8;

		SCIOSlotTypes.hasGroupName.slotMaxCapacity = 20;

		rand = String.valueOf(new Random(dataRandomSeed).nextInt() + new Random(parameterID).nextInt());

		setParameterByID(parameterID);

		modus = mainClassProviderMode == EMainClassMode.GOLD ? ENERModus.GOLD : ENERModus.PREDICT;

//		modelName = modus + "_ExperimentalGroup_PREDICT";
		modelName = modus + "_ExperimentalGroup_DissFinal_" + rand;
		log.info("Model name = " + modelName);

		outputFile = new File(parameterID + "_ExperimentalGroupExtractionResults_" + rand + "_" + dataRandomSeed);

		trainingObjectiveFunction = new SlotFillingObjectiveFunction(scoreType,
				new CartesianEvaluator(explorationMode == EExplorationMode.TYPE_BASED ? EEvaluationDetail.ENTITY_TYPE
						: EEvaluationDetail.DOCUMENT_LINKED, EEvaluationDetail.DOCUMENT_LINKED));

		predictionObjectiveFunction = new SlotFillingObjectiveFunction(scoreType,
				new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE, EEvaluationDetail.DOCUMENT_LINKED));

		/**
		 * Exclude some slots that are not needed for now
		 */
		SCIOSlotTypes.hasNNumber.exclude();
		SCIOSlotTypes.hasTotalPopulationSize.exclude();
		SCIOSlotTypes.hasGroupNumber.exclude();

		/**
		 * Apply different modes that were previously set.
		 */
		applyModesAndRestrictions();

		getData(trainingInstanceNames, devInstanceNames, testInstanceNames);
		Score s = new Score();
		Score sString = new Score();
		for (Instance instance : instanceProvider.getInstances()) {

			Set<DocumentLinkedAnnotation> goldGroupNames = new HashSet<>();
			Set<DocumentLinkedAnnotation> predictedGroupNames = new HashSet<>();

			goldGroupNames.addAll(GroupNameExtraction.extractGroupNamesFromGold(instance));
			predictedGroupNames
					.addAll(GroupNameExtraction.extractGroupNamesWithNPCHunks(distinctGroupNamesMode, instance));
			predictedGroupNames
					.addAll(GroupNameExtraction.extractGroupNamesWithPattern(distinctGroupNamesMode, instance));

			s.add(evalPositionOverlap(goldGroupNames, predictedGroupNames));
			sString.add(evalTokenOverlap(goldGroupNames, predictedGroupNames));

		}
		System.out.println(s);
		System.out.println(sString);
	}

//	TOKEN
//	BOTH
//	Score [getF1()=0.327, getPrecision()=0.204, getRecall()=0.828, tp=846, fp=3307, fn=176, tn=0]

//	PATTERN
//	Score [getF1()=0.458, getPrecision()=0.327, getRecall()=0.764, tp=781, fp=1609, fn=241, tn=0]

//	VP
//	Score [getF1()=0.345, getPrecision()=0.567, getRecall()=0.248, tp=253, fp=193, fn=769, tn=0]

//	NP
//	Score [getF1()=0.382, getPrecision()=0.265, getRecall()=0.687, tp=702, fp=1948, fn=320, tn=0]
	
//	VP+NP
//	Score [getF1()=0.393, getPrecision()=0.265, getRecall()=0.756, tp=773, fp=2141, fn=249, tn=0]

	
	
//	POSITION
	// Both
//	Score [getF1()=0.111, getPrecision()=0.064, getRecall()=0.444, tp=454, fp=6678, fn=568, tn=0]

	// Pattern only
//	Score [getF1()=0.130, getPrecision()=0.083, getRecall()=0.308, tp=315, fp=3500, fn=707, tn=0]

//	NPChunks+VPCHunks
//	Score [getF1()=0.131, getPrecision()=0.080, getRecall()=0.368, tp=376, fp=4324, fn=646, tn=0]

//	NP Chunks
//	Score [getF1()=0.131, getPrecision()=0.081, getRecall()=0.341, tp=349, fp=3938, fn=673, tn=0]

//	VP Chunks
//	Score [getF1()=0.043, getPrecision()=0.074, getRecall()=0.030, tp=31, fp=386, fn=991, tn=0]

	private Score evalPositionOverlap(Set<DocumentLinkedAnnotation> goldGroupNames,
			Set<DocumentLinkedAnnotation> predictedGroupNames) {

		int tp = 0;
		int fp = 0;
		int fn = 0;

		out: for (DocumentLinkedAnnotation gold : goldGroupNames) {

			int gOnset = gold.getStartDocCharOffset();
			int gOffset = gold.getEndDocCharOffset();

			for (DocumentLinkedAnnotation pred : predictedGroupNames) {

				int pOnset = pred.getStartDocCharOffset();
				int pOffset = pred.getEndDocCharOffset();

				if (pOnset >= gOnset && pOnset <= gOffset || pOffset >= gOnset && pOffset <= gOffset
						|| gOnset >= pOnset && gOffset <= pOffset) {
					tp++;
					continue out;
				}

			}
			fn++;

		}
		out: for (DocumentLinkedAnnotation gold : predictedGroupNames) {

			int gOnset = gold.getStartDocCharOffset();
			int gOffset = gold.getEndDocCharOffset();

			for (DocumentLinkedAnnotation pred : goldGroupNames) {

				int pOnset = pred.getStartDocCharOffset();
				int pOffset = pred.getEndDocCharOffset();

				if (pOnset >= gOnset && pOnset <= gOffset || pOffset >= gOnset && pOffset <= gOffset
						|| gOnset >= pOnset && gOffset <= pOffset) {
					continue out;
				}

			}
			fp++;

		}

		Score s = new Score(tp, fp, fn);
//		System.out.println(s);
		return s;
	}

	private Score evalTokenOverlap(Set<DocumentLinkedAnnotation> goldGroupNames,
			Set<DocumentLinkedAnnotation> predictedGroupNames) {

		int tp = 0;
		int fp = 0;
		int fn = 0;

		out: for (DocumentLinkedAnnotation gold : goldGroupNames) {

			Set<String> gTokens = gold.relatedTokens.stream().filter(t -> notStopWord(t.getText()))
					.filter(t -> !t.isNumber()).filter(t -> !t.isPunctuation()).filter(t -> !t.isStopWord())
					.map(t -> t.getText()).collect(Collectors.toSet());

			for (DocumentLinkedAnnotation pred : predictedGroupNames) {

				Set<String> pTokens = pred.relatedTokens.stream().filter(t -> !t.isNumber())
						.filter(t -> notStopWord(t.getText())).filter(t -> !t.isPunctuation())
						.filter(t -> !t.isStopWord()).map(t -> t.getText()).collect(Collectors.toSet());

				if (new HashSet<>(gTokens).removeAll(pTokens)) {
					tp++;
//					System.out.println(gTokens + "<--->" + pTokens);
					continue out;
				}

			}
//			System.out.println(gTokens + "<--FN->" );
			fn++;

		}
		out: for (DocumentLinkedAnnotation gold : predictedGroupNames) {

			Set<String> gTokens = gold.relatedTokens.stream().filter(t -> notStopWord(t.getText()))
					.filter(t -> !t.isNumber()).filter(t -> !t.isPunctuation()).filter(t -> !t.isStopWord())
					.map(t -> t.getText()).collect(Collectors.toSet());

			for (DocumentLinkedAnnotation pred : goldGroupNames) {

				Set<String> pTokens = pred.relatedTokens.stream().filter(t -> !t.isNumber())
						.filter(t -> notStopWord(t.getText())).filter(t -> !t.isPunctuation())
						.filter(t -> !t.isStopWord()).map(t -> t.getText()).collect(Collectors.toSet());

				if (new HashSet<>(gTokens).removeAll(pTokens)) {
					continue out;
				}

			}
//			System.out.println(gTokens + "<--FP->" );
			fp++;

		}

		Score s = new Score(tp, fp, fn);
//		System.out.println(s);
		return s;
	}

	Set<String> stopWords = new HashSet<>(Arrays.asList("rat", "rats", "group", "groups"));

	private boolean notStopWord(String text) {
		return !stopWords.contains(text);
	}

	public void getData(List<String> trainingInstanceNames, List<String> devInstanceNames,
			List<String> testInstanceNames) throws IOException {

		AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
				.setTrainingInstanceNames(trainingInstanceNames).setDevelopInstanceNames(devInstanceNames)
				.setTestInstanceNames(testInstanceNames).build();

		InstanceProvider.verbose = true;
		InstanceProvider.maxNumberOfAnnotations = 8;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		Collection<GoldModificationRule> goldModificationRules = Collections.emptyList();
		DeduplicationRule deduplicationRule = (a, b) -> false;
		if (assignmentMode != EAssignmentMode.GROUP_NAME) {

			deduplicationRule = (a1, a2) -> {
				boolean inclTmp = SCIOSlotTypes.hasGroupName.isIncluded();
				SCIOSlotTypes.hasGroupName.exclude();
				boolean equals = a1.evaluateEquals(predictionObjectiveFunction.getEvaluator(), a2);
				if (inclTmp)
					SCIOSlotTypes.hasGroupName.include();
				return equals;
			};
		}

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor, goldModificationRules,
				deduplicationRule);

		trainingInstances = instanceProvider.getTrainingInstances();
		devInstances = instanceProvider.getDevelopmentInstances();
		testInstances = instanceProvider.getTestInstances();
	}

	public void applyModesAndRestrictions() {

		if (groupNameClusteringMode == EGroupNamesClusteringMode.GOLD_CLUSTERING
				&& (cardinalityMode == ECardinalityMode.RSS_PREDICTED
						|| cardinalityMode == ECardinalityMode.RSS_PREDICTED_SAMPLE)) {
			throw new IllegalArgumentException("Can not combine modes: groupNameClusteringMode = "
					+ groupNameClusteringMode + " with cardinalityMode = " + cardinalityMode);
		}

		if (groupNameClusteringMode == EGroupNamesClusteringMode.GOLD_CLUSTERING
				&& groupNameProviderMode != EExtractGroupNamesMode.GOLD) {
			throw new IllegalArgumentException("Can not combine modes: groupNameClusteringMode = "
					+ groupNameClusteringMode + " with groupNameProviderMode = " + groupNameProviderMode);
		}

		if (groupNameClusteringMode != EGroupNamesClusteringMode.NONE && (cardinalityMode == ECardinalityMode.SAMPLE)) {
			throw new IllegalArgumentException("Can not combine modes: groupNameClusteringMode = "
					+ groupNameClusteringMode + " with cardinalityMode = " + cardinalityMode);
		}

		if (groupNameProviderMode == EExtractGroupNamesMode.EMPTY) {
			/*
			 * If groupNameMode is EMPTY exclude this slot in general
			 */
			SCIOSlotTypes.hasGroupName.exclude();
		}
		if (cardinalityMode != ECardinalityMode.RSS_PREDICTED_SAMPLE && cardinalityMode != ECardinalityMode.SAMPLE) {
			/*
			 * If groupNameMode is GOLD_CLUSTERED freeze this slot as the clustering is
			 * already given and fixed by the ground truth. No exploration of this slot type
			 * is needed.
			 */
			SCIOSlotTypes.hasGroupName.freeze();

		}

		if (complexityMode == EComplexityMode.ROOT) {
			treatmentModificationsRule = ETreatmentModifications.ROOT;
			orgModelModificationsRule = EOrgModelModifications.SPECIES;
			injuryModificationRule = EInjuryModifications.ROOT;
		} else if (complexityMode == EComplexityMode.FULL) {
			treatmentModificationsRule = ETreatmentModifications.DOSAGE_DELIVERY_METHOD_APPLICATION_INSTRUMENT_DIRECTION;
			orgModelModificationsRule = EOrgModelModifications.SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE;
			injuryModificationRule = EInjuryModifications.ROOT_DEVICE_LOCATION_ANAESTHESIA;
		}

		if (assignmentMode == EAssignmentMode.TREATMENT) {
			SCIOSlotTypes.hasInjuryModel.exclude();
			SCIOSlotTypes.hasOrganismModel.exclude();
		} else if (assignmentMode == EAssignmentMode.INJURY) {
			SCIOSlotTypes.hasTreatmentType.exclude();
			SCIOSlotTypes.hasOrganismModel.exclude();
		} else if (assignmentMode == EAssignmentMode.ORGANISM_MODEL) {
			SCIOSlotTypes.hasInjuryModel.exclude();
			SCIOSlotTypes.hasTreatmentType.exclude();
		} else if (assignmentMode == EAssignmentMode.INJURY_ORGANISM_MODEL) {
			SCIOSlotTypes.hasTreatmentType.exclude();
		} else if (assignmentMode == EAssignmentMode.INJURY_TREATMENT) {
			SCIOSlotTypes.hasOrganismModel.exclude();
		} else if (assignmentMode == EAssignmentMode.TREATMENT_ORGANISM_MODEL) {
			SCIOSlotTypes.hasInjuryModel.exclude();
		} else if (assignmentMode == EAssignmentMode.GROUP_NAME) {
			SCIOSlotTypes.hasInjuryModel.exclude();
			SCIOSlotTypes.hasOrganismModel.exclude();
			SCIOSlotTypes.hasTreatmentType.exclude();
		}
	}

}
