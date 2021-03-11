package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.candidateretrieval.helper.DictionaryFromInstanceHelper;
import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.ISemanticParsingCRF;
import de.hterhors.semanticmr.crf.SemanticParsingCRF;
import de.hterhors.semanticmr.crf.SemanticParsingCRFMultiState;
import de.hterhors.semanticmr.crf.exploration.IExplorationStrategy;
import de.hterhors.semanticmr.crf.exploration.RootTemplateCardinalityExplorer;
import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer;
import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer.EExplorationMode;
import de.hterhors.semanticmr.crf.exploration.constraints.AbstractHardConstraint;
import de.hterhors.semanticmr.crf.exploration.constraints.HardConstraintsProvider;
import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.learner.optimizer.SGD;
import de.hterhors.semanticmr.crf.learner.regularizer.L2;
import de.hterhors.semanticmr.crf.model.FactorPoolCache;
import de.hterhors.semanticmr.crf.model.Model;
import de.hterhors.semanticmr.crf.of.IObjectiveFunction;
import de.hterhors.semanticmr.crf.of.SlotFillingObjectiveFunction;
import de.hterhors.semanticmr.crf.sampling.AbstractBeamSampler;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.ISampler;
import de.hterhors.semanticmr.crf.sampling.impl.EpochSwitchSampler;
import de.hterhors.semanticmr.crf.sampling.impl.beam.EpochSwitchBeamSampler;
import de.hterhors.semanticmr.crf.sampling.stopcrit.ISamplingStoppingCriterion;
import de.hterhors.semanticmr.crf.sampling.stopcrit.impl.ConverganceCrit;
import de.hterhors.semanticmr.crf.sampling.stopcrit.impl.MaxChainLengthCrit;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTypeAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.templates.et.SlotIsFilledTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.Instance.DeduplicationRule;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.hterhors.semanticmr.json.nerla.JsonNerlaIO;
import de.hterhors.semanticmr.json.nerla.wrapper.JsonEntityAnnotationWrapper;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AnalyzeComplexity;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.groupname.GroupNameNERLPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.evaluation.CoarseGrainedExpGroupEvaluation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.evaluation.ExperimentalGroupEvaluation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.evaluation.InjuryEvaluation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.evaluation.OrganismModelEvaluation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.evaluation.TreatmentEvaluation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.hardconstraints.DistinctEntityTemplateConstraint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.hardconstraints.DistinctExpGroupComponentsConstraint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.initializer.GoldCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.initializer.MultiCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.initializer.PredictKMultiCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.initializer.SampleCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.investigation.CollectExpGroupNames;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EAssignmentMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.ECardinalityMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EComplexityMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EDistinctGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EExtractGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EGroupNamesClusteringMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EMainClassMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.ExGrAllUsedTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.ExGrInnerNameBOWTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.ExGrNameBOWTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.ExGrNameOverlapTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.IntraTokenCardinalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.Word2VecClusterTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.type_based.TB_ExGrBOWTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.type_based.TB_ExpGroupTreatmentLocalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.type_based.TB_GroupBOWTreatmentCardinalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.type_based.TB_RemainingTypesTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.type_based.TB_TreatmentCardinalityPriorTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.type_based.TB_TreatmentInGroupCardinalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.type_based.TB_TreatmentPriorInverseTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.type_based.TB_TreatmentPriorTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.type_based.TB_Word2VecClusterTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.wrapper.DefinedExperimentalGroup;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.InjuryRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.InjuryRestrictionProvider.EInjuryModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.InjurySlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrgModelSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.TreatmentRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.TreatmentRestrictionProvider.ETreatmentModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.TreatmentSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.Stats;

public class ExperimentalGroupSlotFillingPredictorFinalEvaluation extends AbstractSemReadProject {

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
		int fold;
		if (args.length == 0) {
//			modusIndex = 17; // GOLD
//			modusIndex = 18; // PREDICTED
//			modusIndex = 19; // COVERGAE GOLD
			modusIndex = 20; // COVERGAE PREDICT
			dataRandomSeed = 1004L;
			fold = 0;
		} else {
			modusIndex = Integer.parseInt(args[0]);
			dataRandomSeed = Integer.parseInt(args[1]);
			fold = Integer.parseInt(args[2]);
		}

		run(modusIndex, dataRandomSeed, fold);

	}

	private static void run(int modusIndex, long dataRandomSeed, int fold) throws Exception {
		List<String> docs = Files.readAllLines(new File("src/main/resources/corpus_docs.csv").toPath());
//		List<String> trainingInstanceNames = docs;
//		List<String> testInstanceNames = new ArrayList<>();

		Collections.sort(docs);

		Collections.shuffle(docs, new Random(dataRandomSeed));
		final int x = (int) (((double) docs.size() / 100D) * 90D);

		/*
		 * Ten fold cross implementation
		 */

		int sum = (docs.size() - x);

		List<String> testI = docs.subList(fold * sum, Math.min((fold + 1) * sum, docs.size()));
		List<String> trainI = new ArrayList<>(docs);
		trainI.removeAll(testI);
		List<String> itr = new ArrayList<>();
		itr.addAll(trainI);
		itr.addAll(testI);

		/**
		 * TODO: FULL MODEL
		 */

		List<String> trainingInstanceNames = itr.subList(0, x);
		List<String> testInstanceNames = itr.subList(x, docs.size());

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

		ExperimentalGroupSlotFillingPredictorFinalEvaluation a = new ExperimentalGroupSlotFillingPredictorFinalEvaluation(
				modusIndex, dataRandomSeed, fold,
				trainingInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
				devInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
				testInstances.stream().map(i -> i.getName()).collect(Collectors.toList()));

		if (modusIndex == 19 || modusIndex == 20)
			a.eval(a.coverageTest);
		else
			a.eval(a.resultsTest);

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

	private File cacheDir = new File("data/cache/");
	public static int maxCacheSize = 80_000_000;
	public static int minCacheSize = 40_000_000;

	private ENERModus modus;
	public Map<Instance, Set<EntityTemplate>> extraction = new HashMap<>();

	public ExperimentalGroupSlotFillingPredictorFinalEvaluation(int parameterID, long dataRandomSeed, int fold,
			List<String> trainingInstanceNames, List<String> devInstanceNames, List<String> testInstanceNames)
			throws Exception {
		instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.definedExperimentalGroup);

		EScoreType scoreType = EScoreType.MACRO;

//		CartesianEvaluator.MAXIMUM_PERMUTATION_SIZE = 8;

		SlotFillingExplorer.MAX_NUMBER_OF_ANNOTATIONS = 8;

		SCIOSlotTypes.hasGroupName.slotMaxCapacity = 20;

		final String rand;
		rand = String.valueOf(new Random(dataRandomSeed).nextInt() + new Random(parameterID).nextInt());

		setParameterByID(parameterID);

		modus = mainClassProviderMode == EMainClassMode.GOLD ? ENERModus.GOLD : ENERModus.PREDICT;

//		modelName = modus + "_ExperimentalGroup_PREDICT";
		modelName = modus + "_ExperimentalGroup_DissFinal_" + rand + "_fold_" + fold;

		log.info("Model name = " + modelName);

		outputFile = new File(
				parameterID + "_ExperimentalGroupExtractionResults_" + rand + "_" + dataRandomSeed + "_" + fold);

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
		Stats.countVariables(0,instanceProvider.getInstances());
		System.exit(1);
		Set<SlotType> slotTypesToConsider = new HashSet<>();
		slotTypesToConsider.add(SCIOSlotTypes.hasTreatmentType);
//		
		AnalyzeComplexity.analyze(SCIOEntityTypes.definedExperimentalGroup
				,slotTypesToConsider, instanceProvider.getInstances(),
				predictionObjectiveFunction.getEvaluator());

		SCIOSlotTypes.hasTreatmentType.slotMaxCapacity = 3;

		addGroupNameCandidates();

		addTreatmentCandidates();

		addOrganismModelCandidates();

		addInjuryCandidates();

		List<IExplorationStrategy> explorerList = buildExplorer();

		IStateInitializer initializer = buildStateInitializer();

		List<AbstractFeatureTemplate<?>> featureTemplates = getFeatureTemplates();

		Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> parameter = getParameter();

		ISampler sampler = getSampler();

		MaxChainLengthCrit maxStepCrit = new MaxChainLengthCrit(200);

		ConverganceCrit noModelChangeCrit = new ConverganceCrit(10 * explorerList.size(), s -> s.getModelScore());

		ISamplingStoppingCriterion[] sampleStoppingCrits = new ISamplingStoppingCriterion[] { maxStepCrit,
				noModelChangeCrit };

		if (parameterID == 19 || parameterID == 20)
			coverageTest = coverageInstances(SCIOEntityTypes.definedExperimentalGroup, true, explorerList);
		else
			run(explorerList, sampler, initializer, sampleStoppingCrits, maxStepCrit, featureTemplates, parameter);

	}

	private void run(List<IExplorationStrategy> explorerList, ISampler sampler, IStateInitializer initializer,
			ISamplingStoppingCriterion[] sampleStoppingCrits, ISamplingStoppingCriterion maxStepCrit,
			List<AbstractFeatureTemplate<?>> featureTemplates,
			Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> parameter) throws Exception {
		/**
		 * Finally, we chose a model base directory and a name for the model.
		 * 
		 * NOTE: Make sure that the base model directory exists!
		 */
		final File modelBaseDir = getModelBaseDir();

		Model model;

		if (Model.exists(modelBaseDir, modelName)) {
			/**
			 * If the model exists load from the file system.
			 */
			model = Model.load(modelBaseDir, modelName);
		} else {
			/**
			 * If the model does not exists, create a new model.
			 */
			model = new Model(featureTemplates, modelBaseDir, modelName);
		}

		model.setFeatureTemplateParameter(parameter);

		model.setCache(new FactorPoolCache(model, maxCacheSize, minCacheSize));
		/**
		 * Create a new semantic parsing CRF and initialize with needed parameter.
		 */

		ISemanticParsingCRF crf;
		if (cardinalityMode == ECardinalityMode.PARALLEL || cardinalityMode == ECardinalityMode.PARALLEL_MODEL_UPDATE) {
			crf = new SemanticParsingCRFMultiState(model, explorerList, (AbstractBeamSampler) sampler,
					trainingObjectiveFunction);
			((SemanticParsingCRFMultiState) crf).parallelModelUpdate = cardinalityMode == ECardinalityMode.PARALLEL_MODEL_UPDATE;

			crf.setInitializer(initializer);
		} else {
			crf = new SemanticParsingCRF(model, explorerList, (AbstractSampler) sampler, trainingObjectiveFunction);

			crf.setInitializer(initializer);

		}

//		log.info("Training instances coverage: " + ((SemanticParsingCRF) crf).computeCoverage(true,
//				predictionObjectiveFunction, trainingInstances));
//
//		log.info("Test instances coverage: " + ((SemanticParsingCRF) crf).computeCoverage(true,
//				predictionObjectiveFunction, testInstances));
////

		/**
		 * If the model was loaded from the file system, we do not need to train it.
		 */
		if (!model.isTrained()) {
			/**
			 * Train the CRF.
			 */
			crf.train(newLearner(), trainingInstances, getNumberOfEpochs(), sampleStoppingCrits);

			/**
			 * Save the model as binary. Do not override, in case a file already exists for
			 * that name.
			 */
			model.save();

			/**
			 * Print the model in a readable format.
			 */
			model.printReadable();
		}
		log.info("Model name = " + modelName);
		log.info("States generated during training: " + SlotFillingExplorer.statesgenerated);

		log.info(crf.getTrainingStatistics());
		log.info(crf.getTestStatistics());
		log.info("modelName: " + modelName);

//		Map<Instance, State> resultsTrain = crf.predict(trainingInstances, maxStepCrit);
//		Map<Instance, State> resultsDev = crf.predict(devInstances, maxStepCrit);
		resultsTest = crf.predict(testInstances, maxStepCrit);

		if (assignmentMode == EAssignmentMode.TREATMENT) {
			postPredictOrganismModel(resultsTest);
			postPredictInjuryModel(resultsTest);
		}
//
		SlotType.includeAll();

		for (Entry<Instance, State> e : resultsTest.entrySet()) {
			extraction.put(e.getKey(), new HashSet<>(e.getValue().getCurrentPredictions().getAnnotations()));
		}
//		for (Entry<Instance, State> e : resultsTrain.entrySet()) {
//			extraction.put(e.getKey(), new HashSet<>(e.getValue().getCurrentPredictions().getAnnotations()));
//		}
//		for (Entry<Instance, State> e : resultsDev.entrySet()) {
//			extraction.put(e.getKey(), new HashSet<>(e.getValue().getCurrentPredictions().getAnnotations()));
//		}

		log.info(crf.getTrainingStatistics());
		log.info(crf.getTestStatistics());
		log.info("modelName: " + modelName);
		log.info("States generated in total: " + SlotFillingExplorer.statesgenerated);
	}

	public Map<Instance, State> coverageInstances(EntityType type, boolean detailedLog,
			List<IExplorationStrategy> explorerList) {
		List<IExplorationStrategy> explorer = new ArrayList<>(explorerList);
		explorer.addAll(getCoverageAdditionalExplorer(type));

		Map<Instance, State> coverage = new SemanticParsingCRF(
				new Model(getFeatureTemplates(), getModelBaseDir(), modelName), explorer,
				new EpochSwitchSampler(epoch -> epoch % 2 == 0), getCoverageStateInitializer(type),
				predictionObjectiveFunction).computeCoverage(detailedLog, predictionObjectiveFunction, testInstances);

		return coverage;
	}

	private IStateInitializer getCoverageStateInitializer(EntityType type) {
		return (instance -> {
			return new State(instance, new Annotations(new EntityTemplate(type)));
		});
	}

	private Collection<? extends IExplorationStrategy> getCoverageAdditionalExplorer(EntityType type) {
		return Arrays.asList(new RootTemplateCardinalityExplorer(predictionObjectiveFunction.getEvaluator(),
				EExplorationMode.ANNOTATION_BASED, AnnotationBuilder.toAnnotation(type)));
	}

	public void eval(Map<Instance, State> results) throws Exception {
		/*
		 * Exclude from evaluation.
		 */
		SCIOSlotTypes.hasGroupName.exclude();
		/**
		 * Evaluate with objective function
		 */

		for (Entry<Instance, State> res : results.entrySet()) {

			for (Iterator<AbstractAnnotation> iterator = res.getValue().getCurrentPredictions().getAnnotations()
					.iterator(); iterator.hasNext();) {
				AbstractAnnotation a = iterator.next();
				if (new DefinedExperimentalGroup(a.asInstanceOfEntityTemplate()).isEmpty())
					iterator.remove();
			}
		}
		PrintStream ps = new PrintStream(outputFile);

		/**
		 * Evaluate assuming TT,OM,IM are one unit. Evaluate assuming TT,OM,IM are one
		 * unit, distinguish vehicle-treatments and main-treatments.
		 */
		evaluateDetailed(ps, results);

		Map<SlotType, Boolean> stored = SlotType.storeExcludance();
		SlotType.excludeAll();
		if (!stored.get(SCIOSlotTypes.hasTreatmentType)) {
			SCIOSlotTypes.hasTreatmentType.include();
			SCIOSlotTypes.hasCompound.include();
			SCIOSlotTypes.hasDosage.include();
			SCIOSlotTypes.hasDirection.include();
			SCIOSlotTypes.hasDeliveryMethod.include();
			SCIOSlotTypes.hasLocations.include();
		}
		if (!stored.get(SCIOSlotTypes.hasInjuryModel)) {
			SCIOSlotTypes.hasInjuryModel.include();
			SCIOSlotTypes.hasInjuryDevice.includeRec();
			SCIOSlotTypes.hasInjuryLocation.includeRec();
			SCIOSlotTypes.hasInjuryAnaesthesia.includeRec();
		}
		if (!stored.get(SCIOSlotTypes.hasOrganismModel)) {
			SCIOSlotTypes.hasOrganismModel.includeRec();
		}
//		NerlaEvaluator nerlaEvaluator = new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE);

		ExperimentalGroupEvaluation expGroupEval = new ExperimentalGroupEvaluation(predictionObjectiveFunction,
				predictionObjectiveFunction.getEvaluator());

		evaluate(log, results, predictionObjectiveFunction);

		Score score = CoarseGrainedExpGroupEvaluation.evaluateCoarsGrained(ps, predictionObjectiveFunction, results);

		log.info("CoarseGrainedEvaluaiton: " + score);

		expGroupEval.evaluate(ps, results, EScoreType.MICRO);
		expGroupEval.evaluate(ps, results, EScoreType.MACRO);

		SlotType.restoreExcludance(stored);

	}

	public void postPredictOrganismModel(Map<Instance, State> results) {
		if (assignmentMode == EAssignmentMode.TREATMENT) {
			SCIOSlotTypes.hasOrganismModel.includeRec();

			if (mainClassProviderMode == EMainClassMode.PRE_PREDICTED) {
				int k = 1;
				for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictOrganismModel(devInstances, k)
						.entrySet()) {
					for (AbstractAnnotation predictedExpGroup : results.get(prediction.getKey()).getCurrentPredictions()
							.getAnnotations()) {

						if (prediction.getValue().isEmpty())
							continue;
						predictedExpGroup.asInstanceOfEntityTemplate().setSingleSlotFiller(
								SCIOSlotTypes.hasOrganismModel, prediction.getValue().iterator().next());

					}
				}
				for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictOrganismModel(testInstances, k)
						.entrySet()) {
					for (AbstractAnnotation predictedExpGroup : results.get(prediction.getKey()).getCurrentPredictions()
							.getAnnotations()) {

						if (prediction.getValue().isEmpty())
							continue;
						predictedExpGroup.asInstanceOfEntityTemplate().setSingleSlotFiller(
								SCIOSlotTypes.hasOrganismModel, prediction.getValue().iterator().next());

					}
				}
			}

			if (mainClassProviderMode == EMainClassMode.GOLD) {
				for (Instance instance : devInstances) {
					for (AbstractAnnotation predictedExpGroup : results.get(instance).getCurrentPredictions()
							.getAnnotations()) {
						for (AbstractAnnotation pred : extractGoldOrganismModels(instance)) {
							predictedExpGroup.asInstanceOfEntityTemplate()
									.setSingleSlotFiller(SCIOSlotTypes.hasOrganismModel, pred);
						}
					}
				}
				for (Instance instance : testInstances) {
					for (AbstractAnnotation predictedExpGroup : results.get(instance).getCurrentPredictions()
							.getAnnotations()) {
						for (AbstractAnnotation pred : extractGoldOrganismModels(instance)) {
							predictedExpGroup.asInstanceOfEntityTemplate()
									.setSingleSlotFiller(SCIOSlotTypes.hasOrganismModel, pred);
						}
					}
				}
			}

		}

	}

	public void postPredictInjuryModel(Map<Instance, State> results) {
		if (assignmentMode == EAssignmentMode.TREATMENT) {
			SCIOSlotTypes.hasInjuryModel.includeRec();

			if (mainClassProviderMode == EMainClassMode.PRE_PREDICTED) {
				int k = 1;

				for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictInjuryModel(devInstances, k)
						.entrySet()) {
					for (AbstractAnnotation predictedExpGroup : results.get(prediction.getKey()).getCurrentPredictions()
							.getAnnotations()) {

						if (prediction.getValue().isEmpty())
							continue;
						predictedExpGroup.asInstanceOfEntityTemplate().setSingleSlotFiller(SCIOSlotTypes.hasInjuryModel,
								prediction.getValue().iterator().next());

					}
				}
				for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictInjuryModel(testInstances, k)
						.entrySet()) {
					for (AbstractAnnotation predictedExpGroup : results.get(prediction.getKey()).getCurrentPredictions()
							.getAnnotations()) {

						if (prediction.getValue().isEmpty())
							continue;
						predictedExpGroup.asInstanceOfEntityTemplate().setSingleSlotFiller(SCIOSlotTypes.hasInjuryModel,
								prediction.getValue().iterator().next());

					}
				}
			}

			if (mainClassProviderMode == EMainClassMode.GOLD) {
				for (Instance instance : devInstances) {
					for (AbstractAnnotation predictedExpGroup : results.get(instance).getCurrentPredictions()
							.getAnnotations()) {
						for (AbstractAnnotation pred : extractGoldInjuryModels(instance)) {
							predictedExpGroup.asInstanceOfEntityTemplate()
									.setSingleSlotFiller(SCIOSlotTypes.hasInjuryModel, pred);
						}
					}
				}
				for (Instance instance : testInstances) {
					for (AbstractAnnotation predictedExpGroup : results.get(instance).getCurrentPredictions()
							.getAnnotations()) {
						for (AbstractAnnotation pred : extractGoldInjuryModels(instance)) {
							predictedExpGroup.asInstanceOfEntityTemplate()
									.setSingleSlotFiller(SCIOSlotTypes.hasInjuryModel, pred);
						}
					}
				}
			}
		}

	}

	private void evaluateDetailed(PrintStream ps, Map<Instance, State> results) throws Exception {

		log.info("explorationMode: " + explorationMode);
		log.info("assignmentMode: " + assignmentMode);
		log.info("cardinalityMode: " + cardinalityMode);
		log.info("groupNameClusteringMode: " + groupNameClusteringMode);
		log.info("groupNameProviderMode: " + groupNameProviderMode);
		log.info("mainClassProviderMode: " + mainClassProviderMode);
		log.info("distinctGroupNamesMode: " + distinctGroupNamesMode);
		log.info("complexityMode: " + complexityMode);

		ps.println("explorationMode: " + explorationMode);
		ps.println("assignmentMode: " + assignmentMode);
		ps.println("cardinalityMode: " + cardinalityMode);
		ps.println("groupNameClusteringMode: " + groupNameClusteringMode);
		ps.println("groupNameProviderMode: " + groupNameProviderMode);
		ps.println("mainClassProviderMode: " + mainClassProviderMode);
		ps.println("distinctGroupNamesMode: " + distinctGroupNamesMode);
		ps.println("complexityMode: " + complexityMode);

//		NerlaEvaluator nerlaEvaluator = new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE);

		/**
		 * TODO: If not only the root stuff should be evaluated we need to add here the
		 * concrete slot types!
		 */

		if (SCIOSlotTypes.hasTreatmentType.isIncluded()) {
			Map<SlotType, Boolean> x = SlotType.storeExcludance();
			SlotType.excludeAll();
			SCIOSlotTypes.hasCompound.include();
			SCIOSlotTypes.hasTreatmentType.include();
			TreatmentEvaluation treatmentEvaluation = new TreatmentEvaluation(
					predictionObjectiveFunction.getEvaluator());
			treatmentEvaluation.evaluate(ps, results, EScoreType.MICRO);
			treatmentEvaluation.evaluate(ps, results, EScoreType.MACRO);
			SlotType.restoreExcludance(x);
		}
		if (SCIOSlotTypes.hasOrganismModel.isIncluded()) {
			Map<SlotType, Boolean> x = SlotType.storeExcludance();
			SlotType.excludeAll();
			SCIOSlotTypes.hasOrganismModel.includeRec();
			OrganismModelEvaluation organismModelEvaluation = new OrganismModelEvaluation(
					predictionObjectiveFunction.getEvaluator());
			organismModelEvaluation.evaluate(ps, results, EScoreType.MICRO);
			organismModelEvaluation.evaluate(ps, results, EScoreType.MACRO);
			SlotType.restoreExcludance(x);
		}

		if (SCIOSlotTypes.hasInjuryModel.isIncluded()) {
			Map<SlotType, Boolean> x = SlotType.storeExcludance();
			SlotType.excludeAll();
			SCIOSlotTypes.hasInjuryModel.includeRec();
			InjuryEvaluation injuryModelEvaluation = new InjuryEvaluation(predictionObjectiveFunction.getEvaluator());
			injuryModelEvaluation.evaluate(ps, results, EScoreType.MICRO);
			injuryModelEvaluation.evaluate(ps, results, EScoreType.MACRO);
			SlotType.restoreExcludance(x);
		}

	}

	private ISampler getSampler() {
		if (cardinalityMode == ECardinalityMode.PARALLEL || cardinalityMode == ECardinalityMode.PARALLEL_MODEL_UPDATE)
			return new EpochSwitchBeamSampler(epoch -> epoch % 2 == 0);
		else
//			AbstractSampler sampler = SamplerCollection.topKModelDistributionSamplingStrategy(2);
//			AbstractSampler sampler = SamplerCollection.greedyModelStrategy();
//			AbstractSampler sampler = SamplerCollection.greedyObjectiveStrategy();
			return new EpochSwitchSampler(epoch -> epoch % 2 == 0);
//			AbstractSampler sampler = new EpochSwitchSampler(new RandomSwitchSamplingStrategy());
//			AbstractSampler sampler = new EpochSwitchSampler(e -> new Random(e).nextBoolean());
	}

	private void addOrganismModelCandidates() {
		if (assignmentMode == EAssignmentMode.ORGANISM_MODEL || assignmentMode == EAssignmentMode.INJURY_ORGANISM_MODEL
				|| assignmentMode == EAssignmentMode.TREATMENT_ORGANISM_MODEL
				|| assignmentMode == EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY) {

			if (mainClassProviderMode == EMainClassMode.SAMPLE) {
				addOrganismModelCandidatesFromNERLA();

				Map<Instance, Set<AbstractAnnotation>> annotations = getDictionaryBasedCandidates();

				for (Instance instance : annotations.keySet()) {

					for (AbstractAnnotation nerla : annotations.get(instance)) {

						if (!SCIOEntityTypes.organismModel.getRelatedEntityTypes().contains(nerla.getEntityType()))
							continue;

						if (nerla.getEntityType() == SCIOEntityTypes.species
								|| nerla.getEntityType().isSubEntityOf(SCIOEntityTypes.species)) {

							EntityTemplate organismModel = new EntityTemplate(
									AnnotationBuilder.toAnnotation(SCIOEntityTypes.organismModel))
											.setSingleSlotFiller(SCIOSlotTypes.hasOrganismSpecies, nerla.deepCopy());
							instance.addCandidateAnnotation(organismModel);
						}

						instance.addCandidateAnnotation(nerla);

					}
				}

			}

			if (mainClassProviderMode == EMainClassMode.PRE_PREDICTED) {
				int k = 1;

//				for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictOrganismModel(trainingInstances,k)
//						.entrySet()) {
//					prediction.getKey().addCandidateAnnotations(prediction.getValue());
//				}
				for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictOrganismModel(devInstances, k)
						.entrySet()) {
					prediction.getKey().addCandidateAnnotations(prediction.getValue());
				}
				for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictOrganismModel(testInstances, k)
						.entrySet()) {
					prediction.getKey().addCandidateAnnotations(prediction.getValue());
				}
				for (Instance instance : trainingInstances) {
					instance.addCandidateAnnotations(extractGoldOrganismModels(instance));
				}
			}

			if (mainClassProviderMode == EMainClassMode.GOLD) {
				for (Instance instance : trainingInstances) {
					instance.addCandidateAnnotations(extractGoldOrganismModels(instance));
				}
				for (Instance instance : devInstances) {
					instance.addCandidateAnnotations(extractGoldOrganismModels(instance));
				}
				for (Instance instance : testInstances) {
					instance.addCandidateAnnotations(extractGoldOrganismModels(instance));
				}
			}
		}

	}

	private void addInjuryCandidates() {
		if (assignmentMode == EAssignmentMode.INJURY || assignmentMode == EAssignmentMode.INJURY_ORGANISM_MODEL
				|| assignmentMode == EAssignmentMode.INJURY_TREATMENT
				|| assignmentMode == EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY) {

			if (mainClassProviderMode == EMainClassMode.SAMPLE) {
				addInjuryCandidatesFromNERLA();

				Map<Instance, Set<AbstractAnnotation>> annotations = getDictionaryBasedCandidates();

				for (Instance instance : annotations.keySet()) {

					for (AbstractAnnotation nerla : annotations.get(instance)) {

						if (!SCIOEntityTypes.injury.getRelatedEntityTypes().contains(nerla.getEntityType()))
							continue;
						instance.addCandidateAnnotation(nerla);

					}
				}

			}

			if (mainClassProviderMode == EMainClassMode.PRE_PREDICTED) {
				int k = 1;

//				for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictInjuryModel(trainingInstances,k)
//						.entrySet()) {
//					prediction.getKey().addCandidateAnnotations(prediction.getValue());
//				}
				for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictInjuryModel(devInstances, k)
						.entrySet()) {
					prediction.getKey().addCandidateAnnotations(prediction.getValue());
				}
				for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictInjuryModel(testInstances, k)
						.entrySet()) {
					prediction.getKey().addCandidateAnnotations(prediction.getValue());
				}
				for (Instance instance : trainingInstances) {
					instance.addCandidateAnnotations(extractGoldInjuryModels(instance));
				}
			}

			if (mainClassProviderMode == EMainClassMode.GOLD) {
				for (Instance instance : trainingInstances) {
					instance.addCandidateAnnotations(extractGoldInjuryModels(instance));
				}
				for (Instance instance : devInstances) {
					instance.addCandidateAnnotations(extractGoldInjuryModels(instance));
				}
				for (Instance instance : testInstances) {
					instance.addCandidateAnnotations(extractGoldInjuryModels(instance));
				}
			}
		}

//		if (injuryModificationRule != EInjuryModifications.ROOT
//				&& injuryModificationRule != EInjuryModifications.ROOT_DEVICE) {
//
//			final int kVertebrea = 2;
//
//			String vertebralAreaModelName = "VertebralArea_" + dataRandomSeed;
//			VertebralAreaPredictor vertebralAreaPrediction = new VertebralAreaPredictor(vertebralAreaModelName,
//					trainingInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
//					devInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
//					testInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
//					EVertebralAreaModifications.NO_MODIFICATION);
//
//			vertebralAreaPrediction.trainOrLoadModel();
//			vertebralAreaPrediction.predictAllInstances(2);
//
//			for (Instance instance : instanceProvider.getInstances()) {
//				instance.addCandidateAnnotations(
//						vertebralAreaPrediction.predictHighRecallInstanceByName(instance.getName(), kVertebrea));
//			} /**
//				 * TODO: add training location
//				 */
//		}
	}

	private void addTreatmentCandidates() {

		if (!(assignmentMode == EAssignmentMode.TREATMENT || assignmentMode == EAssignmentMode.TREATMENT_ORGANISM_MODEL
				|| assignmentMode == EAssignmentMode.INJURY_TREATMENT
				|| assignmentMode == EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY)) {
			return;
		}

		if (mainClassProviderMode == EMainClassMode.SAMPLE) {
			addTreatmentCandidatesFromNERLA();

			Map<Instance, Set<AbstractAnnotation>> annotations = getDictionaryBasedCandidates();

			for (Instance instance : annotations.keySet()) {

				for (AbstractAnnotation nerla : annotations.get(instance)) {

					if (!SCIOEntityTypes.treatment.getRelatedEntityTypes().contains(nerla.getEntityType()))
						continue;

					if (nerla.getEntityType() == SCIOEntityTypes.compound
							|| nerla.getEntityType().isSubEntityOf(SCIOEntityTypes.compound)) {
						EntityTemplate compoundTreatment = new EntityTemplate(
								AnnotationBuilder.toAnnotation(SCIOEntityTypes.compoundTreatment))
										.setSingleSlotFiller(SCIOSlotTypes.hasCompound, nerla.deepCopy());
						instance.addCandidateAnnotation(compoundTreatment);
					}

					instance.addCandidateAnnotation(nerla);

				}
			}
			for (Instance instance : trainingInstances) {
				instance.addCandidateAnnotations(extractGoldTreatments(instance));
			}
		}

		if (mainClassProviderMode == EMainClassMode.PRE_PREDICTED) {
			int k = 1;

			for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictTreatment(devInstances, k).entrySet()) {
				prediction.getKey().addCandidateAnnotations(prediction.getValue());
			}

			for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictTreatment(testInstances, k).entrySet()) {
				prediction.getKey().addCandidateAnnotations(prediction.getValue());
			}

			for (Instance instance : trainingInstances) {
				instance.addCandidateAnnotations(extractGoldTreatments(instance));
			}

		}

		if (mainClassProviderMode == EMainClassMode.GOLD) {
			for (Instance instance : trainingInstances) {
				instance.addCandidateAnnotations(extractGoldTreatments(instance));
			}
			for (Instance instance : devInstances) {
				instance.addCandidateAnnotations(extractGoldTreatments(instance));
			}
			for (Instance instance : testInstances) {
				instance.addCandidateAnnotations(extractGoldTreatments(instance));
			}
		}

//		if (treatmentModificationsRule != ETreatmentModifications.ROOT) {
//			final int kDelivery = 2;
//
//			String deliveryMethodModelName = "DeliveryMethod_" + modelName;
//
//			DeliveryMethodPredictor deliveryMethodPrediction = new DeliveryMethodPredictor(deliveryMethodModelName,
//					trainingInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
//					devInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
//					testInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
//					EDeliveryMethodModifications.ROOT, modus);
//
//			deliveryMethodPrediction.trainOrLoadModel();
//			deliveryMethodPrediction.predictAllInstances(2);
//
//			for (Instance instance : instanceProvider.getInstances()) {
//				instance.addCandidateAnnotations(
//						deliveryMethodPrediction.predictHighRecallInstanceByName(instance.getName(), kDelivery));
//			} /**
//				 * TODO: add training delivery method
//				 */
//		}
	}

	private Set<AbstractAnnotation> extractGoldTreatments(Instance instance) {
		Set<AbstractAnnotation> treatments = new HashSet<>();
		for (EntityTemplate expGroup : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {

			for (AbstractAnnotation abstractAnnotation : expGroup.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType)
					.getSlotFiller().stream().filter(a -> a != null).collect(Collectors.toList())) {

				treatments.add(abstractAnnotation);

				if (abstractAnnotation.getEntityType() == SCIOEntityTypes.compoundTreatment) {

					SingleFillerSlot sfs = abstractAnnotation.asInstanceOfEntityTemplate()
							.getSingleFillerSlot(SCIOSlotTypes.hasCompound);
					if (sfs.containsSlotFiller())
						treatments.add(sfs.getSlotFiller());
				}
			}
		}

		return treatments;
	}

	private Set<AbstractAnnotation> extractGoldOrganismModels(Instance instance) {
		Set<AbstractAnnotation> orgModels = new HashSet<>();
		for (EntityTemplate expGroup : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {

//			DefinedExperimentalGroup wrapper = new DefinedExperimentalGroup(expGroup);

			if (expGroup.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel).containsSlotFiller())
				orgModels.add(expGroup.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel).getSlotFiller());

//			AbstractAnnotation species = wrapper.getOrganismSpecies();
//			if (species != null)
//				orgModels.add(species);
		}
		return orgModels;
	}

	private Set<AbstractAnnotation> extractGoldInjuryModels(Instance instance) {
		Set<AbstractAnnotation> injuries = new HashSet<>();
		for (EntityTemplate expGroup : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {
			if (expGroup.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).containsSlotFiller())
				injuries.add(expGroup.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).getSlotFiller());
		}
		return injuries;
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

		Collection<GoldModificationRule> goldModificationRules = getGoldModificationRules();
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

	public List<IExplorationStrategy> buildExplorer() {

		List<IExplorationStrategy> explorerList = new ArrayList<>();

		HardConstraintsProvider hardConstraintsProvider = new HardConstraintsProvider();

		if (cardinalityMode == ECardinalityMode.SAMPLE || cardinalityMode == ECardinalityMode.RSS_PREDICTED_SAMPLE) {
			hardConstraintsProvider.addHardConstraints(
					new DistinctEntityTemplateConstraint(predictionObjectiveFunction.getEvaluator()));
		} else {
			hardConstraintsProvider.addHardConstraints(
					new DistinctExpGroupComponentsConstraint(predictionObjectiveFunction.getEvaluator()));

		}

		hardConstraintsProvider.addHardConstraints(new AbstractHardConstraint() {

			@Override
			public boolean violatesConstraint(State currentState, EntityTemplate entityTemplate, int annotationIndex) {
				return new DefinedExperimentalGroup(entityTemplate).getRelevantTreatments().isEmpty();
			}
		});

		SlotFillingExplorer slotFillingExplorer = new SlotFillingExplorer(explorationMode, predictionObjectiveFunction,
				hardConstraintsProvider);

		explorerList.add(slotFillingExplorer);

		if (cardinalityMode == ECardinalityMode.SAMPLE || cardinalityMode == ECardinalityMode.RSS_PREDICTED_SAMPLE) {

			RootTemplateCardinalityExplorer rootTemplateCardinalityExplorer = new RootTemplateCardinalityExplorer(
					hardConstraintsProvider, predictionObjectiveFunction.getEvaluator(), explorationMode,
					AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));
			explorerList.add(rootTemplateCardinalityExplorer);
		}

		return explorerList;
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

	private void addGroupNameCandidates() {

		switch (groupNameProviderMode) {
		case EMPTY:
			break;
		case GOLD:
			for (Instance instance : instanceProvider.getInstances()) {
				instance.addCandidateAnnotations(GroupNameExtraction.extractGroupNamesFromGold(instance));
			}
			break;
		case TRAINING_PATTERN_NP_CHUNKS:
			addGroupNameTrainingPattern();
		case NP_CHUNKS:
			for (Instance instance : instanceProvider.getInstances()) {
				instance.addCandidateAnnotations(
						GroupNameExtraction.extractGroupNamesWithNPCHunks(distinctGroupNamesMode, instance));
			}
			break;
		case TRAINING_MANUAL_PATTERN:
			addGroupNameTrainingPattern();
		case MANUAL_PATTERN:
			for (Instance instance : instanceProvider.getInstances()) {
				instance.addCandidateAnnotations(
						GroupNameExtraction.extractGroupNamesWithPattern(distinctGroupNamesMode, instance));
			}
			break;
		case TRAINING_MANUAL_PATTERN_NP_CHUNKS:
			addGroupNameTrainingPattern();
		case MANUAL_PATTERN_NP_CHUNKS:
			for (Instance instance : instanceProvider.getInstances()) {
				instance.addCandidateAnnotations(
						GroupNameExtraction.extractGroupNamesWithNPCHunks(distinctGroupNamesMode, instance));
				instance.addCandidateAnnotations(
						GroupNameExtraction.extractGroupNamesWithPattern(distinctGroupNamesMode, instance));
			}
			break;
		case TRAINING_PATTERN:
			addGroupNameTrainingPattern();
			break;
		case PREDICTED:
			int k = 50;
//			for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictGroupName(instanceProvider.getInstances(),
//					k).entrySet()) {
//				prediction.getKey().addCandidateAnnotations(prediction.getValue());
//			}
			for (Instance instance : trainingInstances) {
				instance.addCandidateAnnotations(GroupNameExtraction.extractGroupNamesFromGold(instance));
			}
			for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictGroupName(devInstances, k).entrySet()) {
				prediction.getKey().addCandidateAnnotations(prediction.getValue());
			}
			for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictGroupName(testInstances, k).entrySet()) {
				prediction.getKey().addCandidateAnnotations(prediction.getValue());
			}
			break;
		}

		if (cardinalityMode == ECardinalityMode.SAMPLE || cardinalityMode == ECardinalityMode.RSS_PREDICTED_SAMPLE)
			for (Instance instance : instanceProvider.getInstances()) {
				for (EntityTypeAnnotation ec : instance.getEntityTypeCandidates(EExplorationMode.ANNOTATION_BASED,
						SCIOEntityTypes.groupName)) {
					instance.addCandidateAnnotation(AnnotationBuilder.toAnnotation(instance.getDocument(),
							SCIOEntityTypes.definedExperimentalGroup,
							ec.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm(),
							ec.asInstanceOfDocumentLinkedAnnotation().getStartDocCharOffset()));
				}
			}

	}

	public void addGroupNameTrainingPattern() {
		Map<Instance, Set<AbstractAnnotation>> annotations = getDictionaryBasedCandidates();

		for (Instance instance : annotations.keySet()) {

			Set<String> distinctGroupNames = new HashSet<>();

			for (AbstractAnnotation nerla : annotations.get(instance)) {

				if (nerla.getEntityType() == SCIOEntityTypes.groupName) {

					if (CollectExpGroupNames.STOP_TERM_LIST
							.contains(nerla.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm()))
						continue;

					if (distinctGroupNamesMode == EDistinctGroupNamesMode.STRING_DISTINCT) {

						if (distinctGroupNames.contains(nerla.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm()))
							continue;

						distinctGroupNames.add(nerla.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm());
					}

					instance.addCandidateAnnotation(nerla);
				}

			}
		}
	}

	/*
	 * Some comment
	 */
	private Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> getParameter() {

		Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> parameter = new HashMap<>();

		if (cardinalityMode == ECardinalityMode.GOLD) {
			Map<Instance, Map<SlotType, Integer>> numberToPredictParameter = new HashMap<>();

			for (Instance instance : instanceProvider.getInstances()) {

				numberToPredictParameter.put(instance, new HashMap<>());

				if (SCIOSlotTypes.hasOrganismModel.isIncluded())
					numberToPredictParameter.get(instance).put(SCIOSlotTypes.hasOrganismModel,
							extractGoldOrganismModels(instance).size());

				if (SCIOSlotTypes.hasInjuryModel.isIncluded())
					numberToPredictParameter.get(instance).put(SCIOSlotTypes.hasInjuryModel,
							extractGoldInjuryModels(instance).size());

				if (SCIOSlotTypes.hasTreatmentType.isIncluded())
					numberToPredictParameter.get(instance).put(SCIOSlotTypes.hasTreatmentType,
							extractGoldTreatments(instance).size());
			}

			parameter.put(ExGrAllUsedTemplate.class, new Object[] { numberToPredictParameter });
		} else if (cardinalityMode == ECardinalityMode.SAMPLE) {
		} else if (cardinalityMode == ECardinalityMode.RSS_PREDICTED) {
//			throw new IllegalArgumentException("Not impl exception");
		}

		parameter.put(TB_Word2VecClusterTemplate.class,
				new Object[] { new File("wordvector/small_kmeans++_1000_ranking_reduce10.vec"),
						new File("wordvector/kmeans_200_distances.vec") });
		parameter.put(Word2VecClusterTemplate.class,
				new Object[] { new File("wordvector/small_kmeans++_1000_ranking_reduce10.vec"),
						new File("wordvector/kmeans_200_distances.vec") });

//		parameter.put(Word2VecClusterTemplate.class,
//				new Object[] { new File("wordvector/kmeans++_1000_ranking_reduce10.vec"),
//						new File("wordvector/kmeans_200_distances.vec") });

//		 parameter.put(Word2VecClusterTemplate.class,
//				new Object[] { new File("wordvector/small_kmeans++_200_ranking.vec"),
//						new File("wordvector/kmeans_200_distances.vec") });

//		 parameter.put(Word2VecClusterTemplate.class,
//				new Object[] { new File("wordvector/kmeans_1000_ranking.vec"),new File("wordvector/kmeans_1000_distances.vec") });

		// small_kmeans++_1000_ranking_reduce10.vec
		// kmeans++_1000_ranking_reduce10.vec
		// small_kmeans++_200_ranking.vec
		return parameter;
	}

	private Collection<GoldModificationRule> getGoldModificationRules() {
		Collection<GoldModificationRule> goldModificationRules = new ArrayList<>();

		/*
		 * get only DefinedExperimentalGroups
		 */
		goldModificationRules.add(a -> {
			if (a.asInstanceOfEntityTemplate()
					.getRootAnnotation().entityType == SCIOEntityTypes.definedExperimentalGroup)
				return a;
			return null;
		});

		Map<SlotType, Boolean> f = SlotType.storeExcludance();
		SCIOSlotTypes.hasOrganismModel.include();
		SCIOSlotTypes.hasInjuryModel.include();
		SCIOSlotTypes.hasLocation.include();

		goldModificationRules.add(a -> {

			if (SCIOSlotTypes.hasGroupName.isIncluded())
				if (a.asInstanceOfEntityTemplate().getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
					a.asInstanceOfEntityTemplate()
							.addMultiSlotFiller(SCIOSlotTypes.hasGroupName,
									AnnotationBuilder.toAnnotation(
											a.asInstanceOfEntityTemplate().getRootAnnotation()
													.asInstanceOfDocumentLinkedAnnotation().document,
											SCIOEntityTypes.groupName,
											a.asInstanceOfEntityTemplate().getRootAnnotation()
													.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm(),
											a.asInstanceOfEntityTemplate().getRootAnnotation()
													.asInstanceOfDocumentLinkedAnnotation().getStartDocCharOffset()));

			a.asInstanceOfEntityTemplate().reduceRootToEntityType();
			return a;
		});

		if (SCIOSlotTypes.hasTreatmentType.isIncluded())
			goldModificationRules.add(a -> {

				/*
				 * Apply treatment modifications...
				 */
				List<AbstractAnnotation> newTreatments = new ArrayList<>();

				MultiFillerSlot treatments = a.asInstanceOfEntityTemplate()
						.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType);

				for (AbstractAnnotation treatment : treatments.getSlotFiller()) {

					AbstractAnnotation treat = treatment;
					for (GoldModificationRule goldModificationRule : TreatmentRestrictionProvider
							.getByRule(treatmentModificationsRule)) {
						treat = goldModificationRule.modify(treat);

						if (treat == null) {
							break;
						}

					}
					if (treat != null)
						newTreatments.add(treat);

				}

				a.asInstanceOfEntityTemplate().clearSlot(SCIOSlotTypes.hasTreatmentType);

				List<AbstractAnnotation> distinctAnnotations = new ArrayList<>();

				l_1: for (AbstractAnnotation abstractAnnotation : newTreatments) {

					for (AbstractAnnotation abstractAnnotation2 : distinctAnnotations) {

						if (abstractAnnotation2.evaluateEquals(predictionObjectiveFunction.getEvaluator(),
								abstractAnnotation)) {
							continue l_1;
						}

					}

					distinctAnnotations.add(abstractAnnotation);
				}

				for (AbstractAnnotation slotFiller : distinctAnnotations) {
					a.asInstanceOfEntityTemplate().addMultiSlotFiller(SCIOSlotTypes.hasTreatmentType, slotFiller);
				}

				return a;
			});

		if (SCIOSlotTypes.hasOrganismModel.isIncluded())
			goldModificationRules.add(a -> {
				Map<SlotType, Boolean> x = SlotType.storeExcludance();
				SCIOSlotTypes.hasOrganismModel.include();

				/*
				 * Apply organism model modifications...
				 */
				List<AbstractAnnotation> newOrganimModels = new ArrayList<>();

				SingleFillerSlot organismModel = a.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel);

				if (organismModel.containsSlotFiller()) {
					AbstractAnnotation modifiedModel = organismModel.getSlotFiller();
					for (GoldModificationRule goldModificationRule : OrganismModelRestrictionProvider
							.getByRule(orgModelModificationsRule)) {
						modifiedModel = goldModificationRule.modify(modifiedModel);

						if (modifiedModel == null) {
							break;
						}

					}
					if (modifiedModel != null)
						newOrganimModels.add(modifiedModel);

					a.asInstanceOfEntityTemplate().clearSlot(SCIOSlotTypes.hasOrganismModel);

					for (AbstractAnnotation slotFiller : newOrganimModels) {
						a.asInstanceOfEntityTemplate().setSingleSlotFiller(SCIOSlotTypes.hasOrganismModel, slotFiller);
					}

				}
				SlotType.restoreExcludance(x);
				return a;
			});

		if (SCIOSlotTypes.hasInjuryModel.isIncluded())
			goldModificationRules.add(a -> {
				Map<SlotType, Boolean> x = SlotType.storeExcludance();
				SCIOSlotTypes.hasInjuryModel.include();
				/*
				 * Apply injury modifications...
				 */
				List<AbstractAnnotation> newInjury = new ArrayList<>();

				SingleFillerSlot injuryModel = a.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel);

				if (injuryModel.containsSlotFiller()) {

					AbstractAnnotation modifiedModel = injuryModel.getSlotFiller();
					for (GoldModificationRule goldModificationRule : InjuryRestrictionProvider
							.getByRule(injuryModificationRule)) {
						modifiedModel = goldModificationRule.modify(modifiedModel);

						if (modifiedModel == null) {
							break;
						}

					}
					if (modifiedModel != null)
						newInjury.add(modifiedModel);

					a.asInstanceOfEntityTemplate().clearSlot(SCIOSlotTypes.hasInjuryModel);

					for (AbstractAnnotation slotFiller : newInjury) {
						a.asInstanceOfEntityTemplate().setSingleSlotFiller(SCIOSlotTypes.hasInjuryModel, slotFiller);
					}
				}

				SlotType.restoreExcludance(x);

				return a;
			});

//		if (explorationMode == EExplorationMode.TYPE_BASED)
//			goldModificationRules.add(a -> a.reduceToEntityType());

		/*
		 * remove from annotation if it has no injury, treatment, groupName, and
		 * organism.
		 */
		goldModificationRules.add(a -> {
			if (a.asInstanceOfEntityTemplate().getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()
					|| (SCIOSlotTypes.hasGroupName.isIncluded() && !SCIOSlotTypes.hasGroupName.isFrozen()
							&& a.asInstanceOfEntityTemplate().getMultiFillerSlot(SCIOSlotTypes.hasGroupName)
									.containsSlotFiller())
//					|| (SCIOSlotTypes.hasInjuryModel.isIncluded() && a.asInstanceOfEntityTemplate()
//							.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).containsSlotFiller())
//					|| (SCIOSlotTypes.hasOrganismModel.isIncluded() && a.asInstanceOfEntityTemplate()
//							.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel).containsSlotFiller())
					|| (SCIOSlotTypes.hasTreatmentType.isIncluded() && a.asInstanceOfEntityTemplate()
							.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).containsSlotFiller()))
				return a;
			return null;
		});

		SlotType.restoreExcludance(f);
		return goldModificationRules;
	}

	/**
	 * The state initializer.
	 * 
	 * @param groupNamesCandidates
	 * 
	 */
	private IStateInitializer buildStateInitializer() {

		if (cardinalityMode == ECardinalityMode.GOLD
				&& (groupNameClusteringMode == EGroupNamesClusteringMode.GOLD_CLUSTERING
						|| groupNameClusteringMode == EGroupNamesClusteringMode.NONE))
			return new GoldCardinalityInitializer(groupNameProviderMode, groupNameClusteringMode);

		if (cardinalityMode == ECardinalityMode.RSS_PREDICTED_SAMPLE) {
			PredictKMultiCardinalityInitializer init = new PredictKMultiCardinalityInitializer(modelName,
					cardinalityMode, groupNameProviderMode, groupNameClusteringMode, instanceProvider.getInstances(), 1,
					8, trainingInstances);
			RootTemplateCardinalityExplorer.MAX_NUMBER_OF_ANNOTATIONS = init.max;
			RootTemplateCardinalityExplorer.MIN_NUMBER_OF_ANNOTATIONS = init.min;

			return init;

		}
		if (cardinalityMode == ECardinalityMode.GOLD || cardinalityMode == ECardinalityMode.RSS_PREDICTED) {
			return new PredictKMultiCardinalityInitializer(modelName, cardinalityMode, groupNameProviderMode,
					groupNameClusteringMode, instanceProvider.getInstances(), 1, 8, trainingInstances);

		}

		if (cardinalityMode == ECardinalityMode.SAMPLE)
			return new SampleCardinalityInitializer(SCIOEntityTypes.definedExperimentalGroup, 1);

		if (cardinalityMode == ECardinalityMode.PARALLEL || cardinalityMode == ECardinalityMode.PARALLEL_MODEL_UPDATE)
			return new MultiCardinalityInitializer(modelName, groupNameProviderMode, groupNameClusteringMode,
					instanceProvider.getInstances(), trainingInstances);

		return null;
	}

	private File getModelBaseDir() {
		return new File("models/slotfilling/experimental_group/");
	}

	private AdvancedLearner newLearner() {
		return new AdvancedLearner(new SGD(0.0001, 0), new L2(0.0000000));
	}

	private void addTreatmentCandidatesFromNERLA() {

		JSONNerlaReader prov = new JSONNerlaReader(
				SlotFillingCorpusBuilderBib.getDefaultRegExNerlaDir(SCIOEntityTypes.treatment));

		for (Instance instance : instanceProvider.getInstances()) {

			for (DocumentLinkedAnnotation nerla : prov.getForInstance(instance)) {

				if (nerla.getEntityType() == SCIOEntityTypes.compound
						|| nerla.getEntityType().isSubEntityOf(SCIOEntityTypes.compound)) {

					EntityTemplate compoundTreatment = new EntityTemplate(
							AnnotationBuilder.toAnnotation(SCIOEntityTypes.compoundTreatment))
									.setSingleSlotFiller(SCIOSlotTypes.hasCompound, new EntityTemplate(nerla));
					instance.addCandidateAnnotation(compoundTreatment);
				} else if (nerla.getEntityType() != SCIOEntityTypes.compoundTreatment
						&& nerla.getEntityType().isSubEntityOf(SCIOEntityTypes.treatment)) {
					instance.addCandidateAnnotation(new EntityTemplate(nerla));
				}
				instance.addCandidateAnnotation(nerla);
			}
		}
	}

	private void addOrganismModelCandidatesFromNERLA() {

		JSONNerlaReader prov = new JSONNerlaReader(
				SlotFillingCorpusBuilderBib.getDefaultRegExNerlaDir(SCIOEntityTypes.organismModel));

		for (Instance instance : instanceProvider.getInstances()) {

			for (DocumentLinkedAnnotation nerla : prov.getForInstance(instance)) {

				if (nerla.getEntityType() == SCIOEntityTypes.species
						|| nerla.getEntityType().isSubEntityOf(SCIOEntityTypes.species)) {

					EntityTemplate animalModel = new EntityTemplate(
							AnnotationBuilder.toAnnotation(SCIOEntityTypes.organismModel))
									.setSingleSlotFiller(SCIOSlotTypes.hasOrganismSpecies, new EntityTemplate(nerla));
					instance.addCandidateAnnotation(animalModel);
				}

				instance.addCandidateAnnotation(nerla);
			}
		}

	}

	private void addInjuryCandidatesFromNERLA() {

		JSONNerlaReader prov = new JSONNerlaReader(
				SlotFillingCorpusBuilderBib.getDefaultRegExNerlaDir(SCIOEntityTypes.injury));

		for (Instance instance : instanceProvider.getInstances()) {

			for (DocumentLinkedAnnotation nerla : prov.getForInstance(instance)) {

				instance.addCandidateAnnotation(nerla);
			}
		}

	}

	private Map<Instance, Set<AbstractAnnotation>> predictGroupName(List<Instance> instances, int k) {
		Map<Instance, Set<AbstractAnnotation>> groupNameAnnotations;

//		File groupNamesCacheDir = new File("data/NERLA/groupNames/recall_at_50/");
//		File groupNamesCacheDir = new File(cacheDir, "/" + "GroupName_" + modelName + "/");
		File groupNamesCacheDir = new File(
				"data/annotations/groupNames/" + "GroupName_" + modelName + "_recall_at_" + k + "/");

		if (!groupNamesCacheDir.exists() || groupNamesCacheDir.list().length == 0) {
			groupNamesCacheDir.mkdirs();

			Map<SlotType, Boolean> x = SlotType.storeExcludance();
			SlotType.excludeAll();

			List<String> trainingInstanceNames = trainingInstances.stream().map(t -> t.getName())
					.collect(Collectors.toList());

			List<String> developInstanceNames = devInstances.stream().map(t -> t.getName())
					.collect(Collectors.toList());

			List<String> testInstanceNames = testInstances.stream().map(t -> t.getName()).collect(Collectors.toList());

			GroupNameNERLPredictor predictor = new GroupNameNERLPredictor("GroupName_" + modelName,
					trainingInstanceNames, developInstanceNames, testInstanceNames);

			predictor.trainOrLoadModel();

			groupNameAnnotations = predictor.predictBatchHighRecallInstances(instances, k);

			SlotType.restoreExcludance(x);

			/*
			 * Write cache...
			 */
			JsonNerlaIO io = new JsonNerlaIO(true);
			for (Instance instance : groupNameAnnotations.keySet()) {

				try {

					List<JsonEntityAnnotationWrapper> wrappedAnnotation = groupNameAnnotations.get(instance).stream()
							.map(d -> new JsonEntityAnnotationWrapper(d.asInstanceOfDocumentLinkedAnnotation()))
							.collect(Collectors.toList());

					io.writeNerlas(new File(groupNamesCacheDir, instance.getName() + ".nerla.json"), wrappedAnnotation);

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			/*
			 * Read cache...
			 */
			groupNameAnnotations = new HashMap<>();

			JSONNerlaReader nerlaJSONReader = new JSONNerlaReader(groupNamesCacheDir);

			for (Instance instance : instances) {

				groupNameAnnotations.put(instance, new HashSet<>(nerlaJSONReader.getForInstance(instance)));
			}

		}

		return groupNameAnnotations;
	}

	private OrgModelSlotFillingPredictor orgModelPredictor = null;
	private TreatmentSlotFillingPredictor treatmentPredictor = null;
	private InjurySlotFillingPredictor injuryPredictor = null;

	private Map<Instance, Set<AbstractAnnotation>> predictOrganismModel(List<Instance> instances, int k) {

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
		if (orgModelPredictor == null) {
			InstanceProvider.removeEmptyInstances = false;
			orgModelPredictor = new OrgModelSlotFillingPredictor("OrganismModel_" + modelName, trainingInstanceNames,
					developInstanceNames, testInstanceNames, rule, modus);
			orgModelPredictor.trainOrLoadModel();
		}

		Map<Instance, Set<AbstractAnnotation>> organismModelAnnotations = orgModelPredictor.predictInstances(instances,
				k);

		SlotType.restoreExcludance(x);
		return organismModelAnnotations;
	}

	private Map<Instance, Set<AbstractAnnotation>> predictTreatment(List<Instance> instances, int k) {

		/**
		 * Predict Injury Model
		 */
		ETreatmentModifications rule = ETreatmentModifications.DOSAGE_DELIVERY_METHOD_APPLICATION_INSTRUMENT_DIRECTION;
		Map<SlotType, Boolean> x = SlotType.storeExcludance();

		List<String> trainingInstanceNames = trainingInstances.stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> developInstanceNames = devInstances.stream().map(t -> t.getName()).collect(Collectors.toList());

		List<String> testInstanceNames = testInstances.stream().map(t -> t.getName()).collect(Collectors.toList());
		if (treatmentPredictor == null) {
			InstanceProvider.removeEmptyInstances = false;
			treatmentPredictor = new TreatmentSlotFillingPredictor("Treatment_" + modelName, trainingInstanceNames,
					developInstanceNames, testInstanceNames, rule, modus);
			treatmentPredictor.trainOrLoadModel();
		}

		Map<Instance, Set<AbstractAnnotation>> treatmentModelAnnotations = treatmentPredictor
				.predictInstances(instances, k);

		SlotType.restoreExcludance(x);
		return treatmentModelAnnotations;
	}

	private Map<Instance, Set<AbstractAnnotation>> predictInjuryModel(List<Instance> instances, int k) {

		/**
		 * Predict Injury Model
		 */
		EInjuryModifications rule = EInjuryModifications.ROOT_DEVICE_LOCATION_ANAESTHESIA;
		Map<SlotType, Boolean> x = SlotType.storeExcludance();
		InjuryRestrictionProvider.applySlotTypeRestrictions(rule);

		List<String> trainingInstanceNames = trainingInstances.stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> developInstanceNames = devInstances.stream().map(t -> t.getName()).collect(Collectors.toList());

		List<String> testInstanceNames = testInstances.stream().map(t -> t.getName()).collect(Collectors.toList());
		if (injuryPredictor == null) {
			InstanceProvider.removeEmptyInstances = false;
			injuryPredictor = new InjurySlotFillingPredictor("InjuryModel_" + modelName, trainingInstanceNames,
					developInstanceNames, testInstanceNames, rule, modus);
			injuryPredictor.trainOrLoadModel();
		}
		Map<Instance, Set<AbstractAnnotation>> injuryModelAnnotations = injuryPredictor.predictInstances(instances, k);

		SlotType.restoreExcludance(x);
		return injuryModelAnnotations;
	}

	private Map<Instance, Set<AbstractAnnotation>> getDictionaryBasedCandidates() {

		Map<Instance, Set<AbstractAnnotation>> annotations = new HashMap<>();

		Map<EntityType, Set<String>> trainDictionary = DictionaryFromInstanceHelper.toDictionary(trainingInstances);

		for (Instance instance : instanceProvider.getInstances()) {
			annotations.put(instance,
					DictionaryFromInstanceHelper.getAnnotationsForInstance(instance, trainDictionary));
		}
		return annotations;
	}

	private int getNumberOfEpochs() {
		return 10;
//		return 1;
	}

	private List<AbstractFeatureTemplate<?>> getFeatureTemplates() {
		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();

		if (explorationMode == EExplorationMode.ANNOTATION_BASED) {
//			featureTemplates.add(new BOWCardinalityTemplate());
//
//			featureTemplates.add(new TreatmentPriorTemplate());
//
//			featureTemplates.add(new TreatmentInGroupCardinalityTemplate());

//			featureTemplates.add(new ExGrBOWTemplate());
			//

			/**
			 * TODO: activate if cardinality can be predicted without gold data.
			 */
//		if (cardinalityMode != ECardinalityMode.SAMPLE_CARDINALITY)
//			featureTemplates.add(new ExGrAllUsedTemplate());
//
			featureTemplates.add(new IntraTokenCardinalityTemplate());

			featureTemplates.add(new ExGrInnerNameBOWTemplate());
//		
//			featureTemplates.add(new ExGrOuterNameBOWTemplate());
////
			featureTemplates.add(new ExGrNameBOWTemplate());
////
			featureTemplates.add(new ExGrNameOverlapTemplate());

//
			featureTemplates.add(new SlotIsFilledTemplate());
//
//			featureTemplates.add(new ExpGroupTreatmentLocalityTemplate());
//
//			featureTemplates.add(new ContextBetweenSlotFillerTemplate());
//
//			featureTemplates.add(new ContextCardinalityTemplate());
//
//			featureTemplates.add(new IntraTokenCardinalityTemplate());
//
//			featureTemplates.add(new Word2VecClusterTemplate());

		}
		if (explorationMode == EExplorationMode.TYPE_BASED) {

			featureTemplates.add(new IntraTokenCardinalityTemplate());

			featureTemplates.add(new ExGrInnerNameBOWTemplate());
//		
//			featureTemplates.add(new ExGrOuterNameBOWTemplate());
////
			featureTemplates.add(new ExGrNameBOWTemplate());
////
			featureTemplates.add(new ExGrNameOverlapTemplate());

//			featureTemplates.add(new ExGrInnerNameBOWTemplate());
////		
//			featureTemplates.add(new ExGrOuterNameBOWTemplate());
////
//			featureTemplates.add(new ExGrNameBOWTemplate());
////
//			featureTemplates.add(new ExGrNameOverlapTemplate());
////
//
			/*
			 * Textual features
			 */
//			featureTemplates.add(new TB_NGramTokenContextTemplate());
//			featureTemplates.add(new TB_SingleTokenContextTemplate());
//////
//			featureTemplates.add(new TB_Word2VecClusterTemplate());
//			featureTemplates.add(new TB_ContextBetweenSlotFillerTemplate());
			featureTemplates.add(new TB_ExpGroupTreatmentLocalityTemplate());
//
//			featureTemplates.add(new TB_IntraTokenCardinalityTemplate());
//			featureTemplates.add(new TB_ContextCardinalityTemplate());
			featureTemplates.add(new TB_GroupBOWTreatmentCardinalityTemplate());
			featureTemplates.add(new TB_ExGrBOWTemplate());
//
//			featureTemplates.add(new TB_ExGrBOWInverseTemplate());

			/*
			 * Semantic features
			 */
//
//			featureTemplates.add(new TB_SlotIsFilledTemplate());

			/*
			 * TRETAMENTS
			 */

			featureTemplates.add(new TB_RemainingTypesTemplate());
			featureTemplates.add(new TB_TreatmentPriorTemplate());
			featureTemplates.add(new TB_TreatmentPriorInverseTemplate());
			featureTemplates.add(new TB_TreatmentCardinalityPriorTemplate());
			featureTemplates.add(new TB_TreatmentInGroupCardinalityTemplate());

		}

		return featureTemplates;
	}
}

//GOLD
//explorationMode: TYPE_BASED
//assignmentMode: TREATMENT
//cardinalityMode: PARALLEL_MODEL_UPDATE
//groupNameClusteringMode: WEKA_CLUSTERING
//groupNameProviderMode: MANUAL_PATTERN_NP_CHUNKS
//mainClassProviderMode: GOLD
//distinctGroupNamesMode: NOT_DISTINCT
//complexityMode: FULL
//TREATMENTS MICRO: BOTH = Score [getF1()=0.983, getPrecision()=1.000, getRecall()=0.967, tp=58, fp=0, fn=2, tn=0]
//TREATMENTS MICRO: Vehicle = Score [getF1()=0.974, getPrecision()=1.000, getRecall()=0.950, tp=19, fp=0, fn=1, tn=0]
//TREATMENTS MICRO: Non Vehicle = Score [getF1()=0.987, getPrecision()=1.000, getRecall()=0.975, tp=39, fp=0, fn=1, tn=0]
//TREATMENTS MACRO: BOTH = Score [macroF1=0.990, macroPrecision=1.000, macroRecall=0.980, macroAddCounter=20]
//TREATMENTS MACRO: Vehicle = Score [macroF1=0.987, macroPrecision=1.000, macroRecall=0.974, macroAddCounter=19]
//TREATMENTS MACRO: Non Vehicle = Score [macroF1=0.992, macroPrecision=1.000, macroRecall=0.983, macroAddCounter=20]
//ORGANISM MODEL MICRO: SCORE = Score [getF1()=1.000, getPrecision()=1.000, getRecall()=1.000, tp=20, fp=0, fn=0, tn=0]
//ORGANISM MODEL MACRO: SCORE = Score [macroF1=1.000, macroPrecision=1.000, macroRecall=1.000, macroAddCounter=20]
//INJURY MODEL MICRO: SCORE = Score [getF1()=1.000, getPrecision()=1.000, getRecall()=1.000, tp=19, fp=0, fn=0, tn=0]
//INJURY MODEL MACRO: SCORE = Score [macroF1=1.000, macroPrecision=1.000, macroRecall=1.000, macroAddCounter=19]
//EXP GROUP MICRO  CARDINALITY = Score [getF1()=0.859, getPrecision()=0.787, getRecall()=0.946, tp=70, fp=19, fn=4, tn=0]
//EXP GROUP MICRO  INTERVALL CARDINALITY = 0:0.2	1:0.75	2:0.9	3:1.0
//EXP GROUP MICRO  CARDINALITY RMSE = 1.4317821063276353
//EXP GROUP MICRO  OVERALL SCORE = Score [getF1()=0.782, getPrecision()=0.678, getRecall()=0.925, tp=725, fp=345, fn=59, tn=0]
//EXP GROUP MICRO  COMPONENTS SCORE = Score [getF1()=0.760, getPrecision()=0.651, getRecall()=0.914, tp=192, fp=103, fn=18, tn=0]
//EXP GROUP MICRO: TREATMENT BOTH = Score [getF1()=0.821, getPrecision()=0.770, getRecall()=0.879, tp=94, fp=28, fn=13, tn=0]
//EXP GROUP MICRO: TREATMENT Vehicle = Score [getF1()=0.809, getPrecision()=0.760, getRecall()=0.864, tp=19, fp=6, fn=3, tn=0]
//EXP GROUP MICRO: TREATMENT Non Vehicle = Score [getF1()=0.824, getPrecision()=0.773, getRecall()=0.882, tp=75, fp=22, fn=10, tn=0]
//EXP GROUP MICRO: ORG MODEL = Score [getF1()=0.713, getPrecision()=0.573, getRecall()=0.944, tp=51, fp=38, fn=3, tn=0]
//EXP GROUP MICRO: INJURY MODEL = Score [getF1()=0.707, getPrecision()=0.560, getRecall()=0.959, tp=47, fp=37, fn=2, tn=0]
//EXP GROUP MACRO  CARDINALITY = Score [macroF1=0.875, macroPrecision=0.798, macroRecall=0.970, macroAddCounter=20]
//EXP GROUP MACRO  INTERVALL CARDINALITY = 0:0.2	1:0.75	2:0.9	3:1.0
//EXP GROUP MACRO  CARDINALITY RMSE = 1.4317821063276353
//EXP GROUP MACRO  OVERALL SCORE = Score [macroF1=0.672, macroPrecision=0.614, macroRecall=0.743, macroAddCounter=93]
//EXP GROUP MACRO  COMPONENTS SCORE = Score [macroF1=0.620, macroPrecision=0.620, macroRecall=0.620, macroAddCounter=271]
//EXP GROUP MACRO: TREATMENT BOTH = Score [macroF1=0.753, macroPrecision=0.754, macroRecall=0.753, macroAddCounter=93]
//EXP GROUP MACRO: TREATMENT Vehicle = Score [macroF1=0.621, macroPrecision=0.621, macroRecall=0.621, macroAddCounter=29]
//EXP GROUP MACRO: TREATMENT Non Vehicle = Score [macroF1=0.708, macroPrecision=0.711, macroRecall=0.705, macroAddCounter=79]
//EXP GROUP MACRO: ORG MODEL = Score [macroF1=0.554, macroPrecision=0.554, macroRecall=0.554, macroAddCounter=92]
//EXP GROUP MACRO: INJURY MODEL = Score [macroF1=0.547, macroPrecision=0.547, macroRecall=0.547, macroAddCounter=86]
//null
//CRFStatistics [context=Test, getTotalDuration()=88863]
//modelName: ExperimentalGroup-5321357482610627633
//States generated in total: 104908
