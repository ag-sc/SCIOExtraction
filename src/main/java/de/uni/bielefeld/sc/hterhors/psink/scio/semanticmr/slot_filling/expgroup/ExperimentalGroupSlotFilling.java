package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.Instance.DuplicationRule;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.eval.NerlaEvaluator;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.hterhors.semanticmr.json.nerla.JsonNerlaIO;
import de.hterhors.semanticmr.json.nerla.wrapper.JsonEntityAnnotationWrapper;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.helper.GroupNameExtraction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.AgeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.groupname.GroupNameNERLPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.deliverymethod.DeliveryMethodPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.deliverymethod.DeliveryMethodRestrictionProvider.EDeliveryMethodModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.evaluation.ExperimentalGroupEvaluation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.evaluation.InjuryEvaluation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.evaluation.OrganismModelEvaluation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.evaluation.TreatmentEvaluation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.hardconstraints.DistinctEntityTemplateConstraint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.hardconstraints.DistinctExpGroupComponentsConstraint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.initializer.GoldCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.initializer.MultiCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.initializer.PredictKMultiCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.initializer.SampleCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.investigation.CollectExpGroupNames;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.modes.Modes.EAssignmentMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.modes.Modes.ECardinalityMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.modes.Modes.EComplexityMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.modes.Modes.EDistinctGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.modes.Modes.EExtractGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.modes.Modes.EGroupNamesClusteringMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.modes.Modes.EMainClassMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.ExGrAllUsedTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.ExGrInnerNameBOWTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.ExGrNameBOWTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.ExGrNameOverlapTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.IntraTokenCardinalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.Word2VecClusterTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based.TB_ExGrBOWTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based.TB_ExpGroupTreatmentLocalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based.TB_GroupBOWTreatmentCardinalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based.TB_RemainingTypesTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based.TB_TreatmentCardinalityPriorTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based.TB_TreatmentInGroupCardinalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based.TB_TreatmentPriorInverseTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based.TB_TreatmentPriorTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based.TB_Word2VecClusterTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.wrapper.DefinedExperimentalGroup;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.InjuryRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.InjuryRestrictionProvider.EInjuryModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.InjurySlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrgModelSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.TreatmentRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.TreatmentRestrictionProvider.ETreatmentModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.VertebralAreaPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.VertebralAreaRestrictionProvider.EVertebralAreaModifications;

public class ExperimentalGroupSlotFilling extends AbstractSemReadProject {

	public static void main(String[] args) throws Exception {

		if (args.length == 0) {
			ExperimentalGroupSlotFilling a = new ExperimentalGroupSlotFilling(7, 105);
			a.maxCacheSize = 800_000;
			a.minCacheSize = 400_000;
		} else
			new ExperimentalGroupSlotFilling(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private final IObjectiveFunction trainingObjectiveFunction;

	private final IObjectiveFunction predictionObjectiveFunction;

//	private final IObjectiveFunction trainingObjectiveFunction = new SlotFillingObjectiveFunction(
//			new BeamSearchEvaluator(EEvaluationDetail.DOCUMENT_LINKED,3));
//	
//	private final IObjectiveFunction predictionObjectiveFunction = new SlotFillingObjectiveFunction(
//			new BeamSearchEvaluator(EEvaluationDetail.ENTITY_TYPE, 3));

//	private final IObjectiveFunction trainingObjectiveFunction = new SlotFillingObjectiveFunction(
//			new GreedySearchEvaluator(EEvaluationDetail.DOCUMENT_LINKED));
//
//	private final IObjectiveFunction predictionObjectiveFunction = new SlotFillingObjectiveFunction(
//			new GreedySearchEvaluator(EEvaluationDetail.ENTITY_TYPE));

	private final File instanceDirectory;

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
	private final String dataRandomSeed;

	private File cacheDir = new File("data/cache/");
	private int maxCacheSize = 80_000_000;
	private int minCacheSize = 40_000_000;

	public ExperimentalGroupSlotFilling(int parameterID, int dataRandomSeed) throws Exception {
		super(SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(
						DataStructureLoader.loadSlotFillingDataStructureReader("DefinedExperimentalGroup"))
				.apply().registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization()).build());

		this.instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.definedExperimentalGroup);

		EScoreType scoreType = EScoreType.MACRO;

		CartesianEvaluator.MAXIMUM_PERMUTATION_SIZE = 8;

		SlotFillingExplorer.MAX_NUMBER_OF_ANNOTATIONS = 8;

		SCIOSlotTypes.hasGroupName.slotMaxCapacity = 20;

//		rand = "61289";
		rand = String.valueOf(new Random().nextInt(100000));

		this.dataRandomSeed = "" + dataRandomSeed;
		modelName = "ExperimentalGroup" + rand;
		log.info("Model name = " + modelName);

		setParameterByID(parameterID);

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

		getData(dataRandomSeed);

		double i = 0;
		for (Instance instance : instanceProvider.getInstances()) {
			i += instance.getDocument().getNumberOfSentences();
		}
		System.out.println(i / instanceProvider.getInstances().size());

		double a = 0;
		for (Instance instance : instanceProvider.getInstances()) {
			a += instance.getGoldAnnotations().getAnnotations().size();
		}
		System.out.println(a / instanceProvider.getInstances().size());

		double b = 0;
		for (Instance instance : instanceProvider.getInstances()) {
			b += instance.getGoldAnnotations().getAnnotations().size();
		}
		System.out.println(b);

		double c = 0;
		int min = 100;
		int max = 0;

		for (Instance instance : instanceProvider.getInstances()) {
			int x = instance.getGoldAnnotations().getAnnotations().size();
			c += x;

			min = Math.min(min, x);
			max = Math.max(max, x);

		}
		System.out.println(min);
		System.out.println(max);
		System.out.println(c);

		final double mean = c / instanceProvider.getInstances().size();

		double variance = 0;
		for (Instance instance : instanceProvider.getInstances()) {
			variance += Math.pow(mean - instance.getGoldAnnotations().getAnnotations().size(), 2);

		}
		System.out.println(computeMean(instanceProvider.getInstances()));
		System.out.println(
				computeStdDeviation(instanceProvider.getInstances(), computeMean(instanceProvider.getInstances())));

		System.out.println(Math.sqrt(variance));

//		System.exit(1);
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

		run(explorerList, sampler, initializer, sampleStoppingCrits, maxStepCrit, featureTemplates, parameter);

	}

	private double computeStdDeviation(List<Instance> trainingInstances, double e) {
		double stdDev = 0;
		for (Instance instance : trainingInstances) {
			stdDev += Math.pow(e - instance.getGoldAnnotations().getAbstractAnnotations().size(), 2)
					/ trainingInstances.size();
		}
		stdDev = Math.sqrt(stdDev);
		return stdDev;
	}

	private double computeMean(List<Instance> trainingInstances) {
		double e = 0;
		for (Instance instance : trainingInstances) {
			e += instance.getGoldAnnotations().getAbstractAnnotations().size();
		}
		e /= trainingInstances.size();
		return e;
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

		model.setfeatureTemplateParameter(parameter);

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

//			log.info("Training instances coverage: "
//					+ ((SemanticParsingCRF) crf).computeCoverage(true, predictionObjectiveFunction, trainingInstances));
//
//			log.info("Test instances coverage: "
//					+ ((SemanticParsingCRF) crf).computeCoverage(true, predictionObjectiveFunction, testInstances));
		}
//
//		System.exit(1);	

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

		/**
		 * At this position the model was either successfully loaded or trained. Now we
		 * want to apply the model to unseen data. We select the redistributed test data
		 * in this case. This method returns for each instances a final state (best
		 * state based on the trained model) that contains annotations.
		 */

		Map<Instance, State> results = crf.predict(testInstances, maxStepCrit);

		log.info(crf.getTrainingStatistics());
		log.info(crf.getTestStatistics());
		log.info("modelName: " + modelName);

		if (assignmentMode == EAssignmentMode.TREATMENT) {
			postPredictOrganismModel(results);
			postPredictInjuryModel(results);
		}

		eval(results);

		log.info(crf.getTrainingStatistics());
		log.info(crf.getTestStatistics());
		log.info("modelName: " + modelName);
		log.info("States generated in total: " + SlotFillingExplorer.statesgenerated);
	}

	public void eval(Map<Instance, State> results) throws Exception {
		/*
		 * Exclude from evaluation.
		 */
		SCIOSlotTypes.hasGroupName.exclude();
		/**
		 * Evaluate with objective function
		 */
		evaluate(log, results, predictionObjectiveFunction);

		/**
		 * Evaluate assuming TT,OM,IM are one unit. Evaluate assuming TT,OM,IM are one
		 * unit, distinguish vehicle-treatments and main-treatments.
		 */

		evaluateDetailed(results);
	}

	public void postPredictOrganismModel(Map<Instance, State> results) {
		if (assignmentMode == EAssignmentMode.TREATMENT) {

			int k = 1;

			SCIOSlotTypes.hasOrganismModel.include();

			for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictOrganismModel(devInstances, k)
					.entrySet()) {
				for (AbstractAnnotation predictedExpGroup : results.get(prediction.getKey()).getCurrentPredictions()
						.getAnnotations()) {

					if (prediction.getValue().isEmpty())
						continue;
					predictedExpGroup.asInstanceOfEntityTemplate().setSingleSlotFiller(SCIOSlotTypes.hasOrganismModel,
							prediction.getValue().iterator().next());

				}
			}
			for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictOrganismModel(testInstances, k)
					.entrySet()) {
				for (AbstractAnnotation predictedExpGroup : results.get(prediction.getKey()).getCurrentPredictions()
						.getAnnotations()) {

					if (prediction.getValue().isEmpty())
						continue;
					predictedExpGroup.asInstanceOfEntityTemplate().setSingleSlotFiller(SCIOSlotTypes.hasOrganismModel,
							prediction.getValue().iterator().next());

				}
			}
		}
	}

	public void postPredictInjuryModel(Map<Instance, State> results) {
		if (assignmentMode == EAssignmentMode.TREATMENT) {

			int k = 1;

			SCIOSlotTypes.hasInjuryModel.include();
			SCIOSlotTypes.hasInjuryDevice.exclude();
			for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictInjuryModel(devInstances, k).entrySet()) {
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
	}

	private void evaluateDetailed(Map<Instance, State> results) throws Exception {

		log.info("explorationMode: " + explorationMode);
		log.info("assignmentMode: " + assignmentMode);
		log.info("cardinalityMode: " + cardinalityMode);
		log.info("groupNameClusteringMode: " + groupNameClusteringMode);
		log.info("groupNameProviderMode: " + groupNameProviderMode);
		log.info("mainClassProviderMode: " + mainClassProviderMode);
		log.info("distinctGroupNamesMode: " + distinctGroupNamesMode);
		log.info("complexityMode: " + complexityMode);

		PrintStream ps = new PrintStream(outputFile);

		ps.println("explorationMode: " + explorationMode);
		ps.println("assignmentMode: " + assignmentMode);
		ps.println("cardinalityMode: " + cardinalityMode);
		ps.println("groupNameClusteringMode: " + groupNameClusteringMode);
		ps.println("groupNameProviderMode: " + groupNameProviderMode);
		ps.println("mainClassProviderMode: " + mainClassProviderMode);
		ps.println("distinctGroupNamesMode: " + distinctGroupNamesMode);
		ps.println("complexityMode: " + complexityMode);

		NerlaEvaluator nerlaEvaluator = new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE);

		/**
		 * TODO: If not only the root stuff should be evaluated we need to add here the
		 * concrete slot types!
		 */

		if (SCIOSlotTypes.hasTreatmentType.isIncluded()) {
			SlotType.storeExcludance();
			SlotType.excludeAll();
			SCIOSlotTypes.hasCompound.include();
			SCIOSlotTypes.hasTreatmentType.include();
			TreatmentEvaluation treatmentEvaluation = new TreatmentEvaluation(nerlaEvaluator);
			treatmentEvaluation.evaluate(ps, results, EScoreType.MICRO);
			treatmentEvaluation.evaluate(ps, results, EScoreType.MACRO);
			SlotType.restoreExcludance();
		}
		if (SCIOSlotTypes.hasOrganismModel.isIncluded()) {
			SlotType.storeExcludance();
			SlotType.excludeAll();
			SCIOSlotTypes.hasOrganismModel.include();
			SCIOSlotTypes.hasOrganismSpecies.include();
			OrganismModelEvaluation organismModelEvaluation = new OrganismModelEvaluation(nerlaEvaluator);
			organismModelEvaluation.evaluate(ps, results, EScoreType.MICRO);
			organismModelEvaluation.evaluate(ps, results, EScoreType.MACRO);
			SlotType.restoreExcludance();
		}

		if (SCIOSlotTypes.hasInjuryModel.isIncluded()) {
			SlotType.storeExcludance();
			SlotType.excludeAll();
			SCIOSlotTypes.hasInjuryModel.include();
			InjuryEvaluation injuryModelEvaluation = new InjuryEvaluation(nerlaEvaluator);
			injuryModelEvaluation.evaluate(ps, results, EScoreType.MICRO);
			injuryModelEvaluation.evaluate(ps, results, EScoreType.MACRO);
			SlotType.restoreExcludance();
		}

		Map<SlotType, Boolean> stored = SlotType.storeExcludance();
		SlotType.excludeAll();
		if (!stored.get(SCIOSlotTypes.hasTreatmentType)) {
			SCIOSlotTypes.hasTreatmentType.include();
			SCIOSlotTypes.hasCompound.include();
		}
		if (!stored.get(SCIOSlotTypes.hasInjuryModel))
			SCIOSlotTypes.hasInjuryModel.include();
		if (!stored.get(SCIOSlotTypes.hasOrganismModel)) {
			SCIOSlotTypes.hasOrganismModel.include();
			SCIOSlotTypes.hasOrganismSpecies.include();
		}
		ExperimentalGroupEvaluation expGroupEval = new ExperimentalGroupEvaluation(predictionObjectiveFunction,
				nerlaEvaluator);

		expGroupEval.evaluate(ps, results, EScoreType.MICRO);
		expGroupEval.evaluate(ps, results, EScoreType.MACRO);
		SlotType.restoreExcludance();

		ps.flush();
		ps.close();
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

		if (injuryModificationRule != EInjuryModifications.ROOT
				&& injuryModificationRule != EInjuryModifications.ROOT_DEVICE) {

			final int kVertebrea = 2;

			String vertebralAreaModelName = "VertebralArea_" + dataRandomSeed;
			VertebralAreaPredictor vertebralAreaPrediction = new VertebralAreaPredictor(vertebralAreaModelName, scope,
					trainingInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					devInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					testInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					EVertebralAreaModifications.NO_MODIFICATION);

			vertebralAreaPrediction.trainOrLoadModel();
			vertebralAreaPrediction.predictAllInstances(2);

			for (Instance instance : instanceProvider.getInstances()) {
				instance.addCandidateAnnotations(
						vertebralAreaPrediction.predictHighRecallInstanceByName(instance.getName(), kVertebrea));
			} /**
				 * TODO: add training location
				 */
		}
	}

	private void addTreatmentCandidates() {

		if (!(assignmentMode == EAssignmentMode.TREATMENT || assignmentMode == EAssignmentMode.TREATMENT_ORGANISM_MODEL
				|| assignmentMode == EAssignmentMode.INJURY_TREATMENT
				|| assignmentMode == EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY)) {
			return;
		}

		if (mainClassProviderMode == EMainClassMode.SAMPLE || mainClassProviderMode == EMainClassMode.PRE_PREDICTED) {
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

		/**
		 * TODO: implement treatment prediction.
		 */

//			if (mainClassProviderMode == EMainClassMode.PRE_PREDICTED) {
//				int k = 1;
//
////				for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictTreatmentModel(trainingInstances,k)
////						.entrySet()) {
////					prediction.getKey().addCandidateAnnotations(prediction.getValue());
////				}
//				
//				for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictTreatmentModel(devInstances, k)
//						.entrySet()) {
//					prediction.getKey().addCandidateAnnotations(prediction.getValue());
//				}
//				
//				for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictTreatmentModel(testInstances, k)
//						.entrySet()) {
//					prediction.getKey().addCandidateAnnotations(prediction.getValue());
//				}
//
//				for (Instance instance : trainingInstances) {
//					instance.addCandidateAnnotations(extractGoldTreatments(instance));
//				}
//				
//			}

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

		if (treatmentModificationsRule != ETreatmentModifications.ROOT) {
			final int kDelivery = 2;

			String deliveryMethodModelName = "DeliveryMethod" + dataRandomSeed;

			DeliveryMethodPredictor deliveryMethodPrediction = new DeliveryMethodPredictor(deliveryMethodModelName,
					scope, trainingInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					devInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					testInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					EDeliveryMethodModifications.ROOT);

			deliveryMethodPrediction.trainOrLoadModel();
			deliveryMethodPrediction.predictAllInstances(2);

			for (Instance instance : instanceProvider.getInstances()) {
				instance.addCandidateAnnotations(
						deliveryMethodPrediction.predictHighRecallInstanceByName(instance.getName(), kDelivery));
			} /**
				 * TODO: add training delivery method
				 */
		}
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

			DefinedExperimentalGroup wrapper = new DefinedExperimentalGroup(expGroup);

			if (expGroup.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel).containsSlotFiller())
				orgModels.add(expGroup.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel).getSlotFiller());

			AbstractAnnotation species = wrapper.getOrganismSpecies();
			if (species != null)
				orgModels.add(species);
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

	public void getData(int dataRandomSeed) throws IOException {
		List<String> docs = Files.readAllLines(new File("src/main/resources/corpus_docs.csv").toPath());
		Collections.sort(docs);

		Collections.shuffle(docs, new Random(dataRandomSeed));

		final int x = (int) (((double) docs.size() / 100D) * 80D);
		List<String> trainingInstanceNames = docs.subList(0, x);
		List<String> testInstanceNames = docs.subList(x, docs.size());

		AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
				.setTrainingInstanceNames(trainingInstanceNames).setTestInstanceNames(testInstanceNames).build();

//		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
//				.setTestProportion(20).setSeed(1000L).build();

		InstanceProvider.maxNumberOfAnnotations = 8;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		Collection<GoldModificationRule> goldModificationRules = getGoldModificationRules();
		DuplicationRule deduplicationRule = (a, b) -> false;
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

		trainingInstances = instanceProvider.getRedistributedTrainingInstances();
		devInstances = instanceProvider.getRedistributedDevelopmentInstances();
		testInstances = instanceProvider.getRedistributedTestInstances();
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

		SCIOSlotTypes.hasGender.exclude();
		SCIOSlotTypes.hasWeight.exclude();
		SCIOSlotTypes.hasAgeCategory.exclude();
		SCIOSlotTypes.hasAge.exclude();

		SCIOSlotTypes.hasLocation.exclude();

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
			treatmentModificationsRule = ETreatmentModifications.ROOT;
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
			int k = 1;
			for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictGroupName(instanceProvider.getInstances(),
					k).entrySet()) {
				prediction.getKey().addCandidateAnnotations(prediction.getValue());
			}
//			for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictGroupName(devInstances, k).entrySet()) {
//				prediction.getKey().addCandidateAnnotations(prediction.getValue());
//			}
//			for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictGroupName(testInstances, k).entrySet()) {
//				prediction.getKey().addCandidateAnnotations(prediction.getValue());
//			}
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
					|| (SCIOSlotTypes.hasGroupName.isIncluded() && a.asInstanceOfEntityTemplate()
							.getMultiFillerSlot(SCIOSlotTypes.hasGroupName).containsSlotFiller())
					|| (SCIOSlotTypes.hasInjuryModel.isIncluded() && a.asInstanceOfEntityTemplate()
							.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).containsSlotFiller())
					|| (SCIOSlotTypes.hasOrganismModel.isIncluded() && a.asInstanceOfEntityTemplate()
							.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel).containsSlotFiller())
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
			PredictKMultiCardinalityInitializer init = new PredictKMultiCardinalityInitializer(cardinalityMode,
					groupNameProviderMode, groupNameClusteringMode, instanceProvider.getInstances(), 1, 8,
					trainingInstances);
			RootTemplateCardinalityExplorer.MAX_NUMBER_OF_ANNOTATIONS = init.max;
			RootTemplateCardinalityExplorer.MIN_NUMBER_OF_ANNOTATIONS = init.min;

			return init;

		}
		if (cardinalityMode == ECardinalityMode.GOLD || cardinalityMode == ECardinalityMode.RSS_PREDICTED) {
			return new PredictKMultiCardinalityInitializer(cardinalityMode, groupNameProviderMode,
					groupNameClusteringMode, instanceProvider.getInstances(), 1, 8, trainingInstances);

		}

		if (cardinalityMode == ECardinalityMode.SAMPLE)
			return new SampleCardinalityInitializer(SCIOEntityTypes.definedExperimentalGroup, 1);

		if (cardinalityMode == ECardinalityMode.PARALLEL || cardinalityMode == ECardinalityMode.PARALLEL_MODEL_UPDATE)
			return new MultiCardinalityInitializer(groupNameProviderMode, groupNameClusteringMode,
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

		File groupNamesCacheDir = new File("data/NERLA/groupNames/recall_at_50/");
//		File groupNamesCacheDir = new File(cacheDir, "/" + "GroupName_EXP_GROUP_" + dataRandomSeed + "/");
		if (!groupNamesCacheDir.exists() || groupNamesCacheDir.list().length == 0) {
			groupNamesCacheDir.mkdirs();

			SlotType.storeExcludance();
			SlotType.excludeAll();

			List<String> trainingInstanceNames = trainingInstances.stream().map(t -> t.getName())
					.collect(Collectors.toList());

			List<String> developInstanceNames = devInstances.stream().map(t -> t.getName())
					.collect(Collectors.toList());

			List<String> testInstanceNames = testInstances.stream().map(t -> t.getName()).collect(Collectors.toList());

			GroupNameNERLPredictor predictor = new GroupNameNERLPredictor("GroupName_EXP_GROUP_" + dataRandomSeed,
					scope, trainingInstanceNames, developInstanceNames, testInstanceNames);

			predictor.trainOrLoadModel();

			groupNameAnnotations = predictor.predictBatchHighRecallInstances(instances, k);

			SlotType.restoreExcludance();

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

	private Map<Instance, Set<AbstractAnnotation>> predictOrganismModel(List<Instance> instances, int k) {

		/**
		 * TODO: FULL MODEL EXTRACTION CAUSE THIS IS BEST TO PREDICT SPECIES
		 */
		EOrgModelModifications rule = EOrgModelModifications.SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE;

		/**
		 * Predict OrganismModels
		 */
		SlotType.storeExcludance();
		OrganismModelRestrictionProvider.applySlotTypeRestrictions(rule);

		List<String> trainingInstanceNames = trainingInstances.stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> developInstanceNames = devInstances.stream().map(t -> t.getName()).collect(Collectors.toList());

		List<String> testInstanceNames = testInstances.stream().map(t -> t.getName()).collect(Collectors.toList());
//		+ modelName
		OrgModelSlotFillingPredictor predictor = new OrgModelSlotFillingPredictor(
				"OrganismModel_EXP_GROUP_" + dataRandomSeed, scope, trainingInstanceNames, developInstanceNames,
				testInstanceNames, rule);
		predictor.trainOrLoadModel();

		Map<Instance, Set<AbstractAnnotation>> organismModelAnnotations = predictor.predictInstances(instances, k);

		SlotType.restoreExcludance();
		return organismModelAnnotations;
	}

	private Map<Instance, Set<AbstractAnnotation>> predictInjuryModel(List<Instance> instances, int k) {

		/**
		 * Predict Injury Model
		 */
		EInjuryModifications rule = EInjuryModifications.ROOT_DEVICE;
		SlotType.storeExcludance();
		InjuryRestrictionProvider.applySlotTypeRestrictions(rule);

		List<String> trainingInstanceNames = trainingInstances.stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> developInstanceNames = devInstances.stream().map(t -> t.getName()).collect(Collectors.toList());

		List<String> testInstanceNames = testInstances.stream().map(t -> t.getName()).collect(Collectors.toList());
//		+ modelName
		InjurySlotFillingPredictor predictor = new InjurySlotFillingPredictor("InjuryModel_EXP_GROUP_" + dataRandomSeed,
				scope, trainingInstanceNames, developInstanceNames, testInstanceNames, rule);
		predictor.trainOrLoadModel();

		Map<Instance, Set<AbstractAnnotation>> injuryModelAnnotations = predictor.predictInstances(instances, k);

		SlotType.restoreExcludance();
		return injuryModelAnnotations;
	}

	private void preprocessingPrediction() {
		/**
		 * Predict GroupNames.
		 */

//		GroupNameNERLPredictor groupNamePredictor = new GroupNameNERLPredictor("GroupName" + rand, scope,
//				trainingInstanceNames, developInstanceNames, testInstanceNames);
//
//		groupNamePredictor.trainOrLoadModel();
//
//		Map<String, Set<AbstractAnnotation>> groupNameAnnotations = groupNamePredictor.predictBatchInstances();

//		/**
//		 * Predict Injuries
//		 */
//
//		InjurySlotFillingPredictor injuryPredictor = new InjurySlotFillingPredictor("InjuryModel" + rand, scope,
//				trainingInstanceNames, developInstanceNames, testInstanceNames);
//
//		injuryPredictor.trainOrLoadModel();
//
//		Map<String, Set<AbstractAnnotation>> injuryAnnotations = injuryPredictor.predictAllInstances();

		/**
		 * Predict Treatments
		 */

//		TreatmentSlotFillingPredictor treatmentPredictor = new TreatmentSlotFillingPredictor("Treatment" + rand, scope,
//				trainingInstanceNames, developInstanceNames, testInstanceNames);
//
//		treatmentPredictor.trainOrLoadModel();
//
//		Map<String, Set<AbstractAnnotation>> treatmentAnnotations = treatmentPredictor.predictAllInstances();
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
