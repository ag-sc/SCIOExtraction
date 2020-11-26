package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group;

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

import de.hterhors.semanticmr.candidateretrieval.helper.DictionaryFromInstanceHelper;
import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
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
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTypeAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.templates.et.SlotIsFilledTemplate;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.hterhors.semanticmr.json.nerla.JsonNerlaIO;
import de.hterhors.semanticmr.json.nerla.wrapper.JsonEntityAnnotationWrapper;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.rdf.ConvertToRDF;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.hardconstraints.DistinctEntityTemplateConstraint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.hardconstraints.DistinctExpGroupComponentsConstraint;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.InjurySlotFillingPredictorPrediction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrgModelSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrgModelSlotFillingPredictorPrediction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.TreatmentRestrictionProvider.ETreatmentModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.TreatmentSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.TreatmentSlotFillingPredictorPrediction;

public class ExperimentalGroupSlotFillingPrediction extends AbstractSemReadProject {

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
			modusIndex = 17; // GOLD
//			modusIndex = 18; // PREDICTED
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

		List<Instance> trainingInstances;
		InstanceProvider instanceProvider;

		File instanceDirectory = new File("prediction/instances");

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(100)
				.setCorpusSizeFraction(1F).build();

		InstanceProvider.removeEmptyInstances = false;
		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		trainingInstances = instanceProvider.getTrainingInstances();

		ExperimentalGroupSlotFillingPrediction a = new ExperimentalGroupSlotFillingPrediction(trainingInstances);

//		List<EntityTemplate> annotations = new ArrayList<>();

//		for (Entry<Instance, Set<EntityTemplate>> instance : a.extraction.entrySet()) {
//
//			for (AbstractAnnotation entityTemplate : instance.getValue()) {
//				System.out.println(instance.getKey() + "\t" + entityTemplate.toPrettyString());
//				annotations.add(entityTemplate.asInstanceOfEntityTemplate());
//			}
//		}
//
//		new ConvertToRDF(new File("experimentalGroup.n-triples"), annotations);
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private final IObjectiveFunction trainingObjectiveFunction;

	private final IObjectiveFunction predictionObjectiveFunction;

	private List<Instance> instances;

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

	public void setParameterByID(int id) {
		distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;
		assignmentMode = EAssignmentMode.TREATMENT;
		complexityMode = EComplexityMode.FULL;
		explorationMode = EExplorationMode.TYPE_BASED;

		mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

		groupNameProviderMode = EExtractGroupNamesMode.MANUAL_PATTERN;
		groupNameClusteringMode = EGroupNamesClusteringMode.WEKA_CLUSTERING;
		cardinalityMode = ECardinalityMode.PARALLEL_MODEL_UPDATE;

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

	final private ENERModus modus = ENERModus.PREDICT;;
	public Map<Instance, Set<EntityTemplate>> extraction = new HashMap<>();

	public ExperimentalGroupSlotFillingPrediction(List<Instance> instances) throws Exception {

		EScoreType scoreType = EScoreType.MACRO;

//		CartesianEvaluator.MAXIMUM_PERMUTATION_SIZE = 8;

		SlotFillingExplorer.MAX_NUMBER_OF_ANNOTATIONS = 8;

		SCIOSlotTypes.hasGroupName.slotMaxCapacity = 20;

		setParameterByID(-1000);

		modelName = modus + "_ExperimentalGroup_PREDICT";
		log.info("Model name = " + modelName);

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

		this.instances = instances;

//		getData(instanceNames);

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
//			model = new Model(featureTemplates, modelBaseDir, modelName);
			throw new IllegalStateException("No model trained");
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

		/**
		 * If the model was loaded from the file system, we do not need to train it.
		 */
		if (!model.isTrained()) {
			throw new IllegalStateException("No model trained");
			// /**
//			 * Train the CRF.
//			 */
//			crf.train(newLearner(), trainingInstances, getNumberOfEpochs(), sampleStoppingCrits);
//
//			/**
//			 * Save the model as binary. Do not override, in case a file already exists for
//			 * that name.
//			 */
//			model.save();
//
//			/**
//			 * Print the model in a readable format.
//			 */
//			model.printReadable();
		}
		log.info("Model name = " + modelName);

		Map<Instance, State> resultsTrain = crf.predict(instances, maxStepCrit);

		if (assignmentMode == EAssignmentMode.TREATMENT) {
			postPredictOrganismModel(resultsTrain);
			postPredictInjuryModel(resultsTrain);
		}
//
		SlotType.includeAll();

		for (Entry<Instance, State> e : resultsTrain.entrySet()) {
			extraction.put(e.getKey(), new HashSet<>(e.getValue().getCurrentPredictions().getAnnotations()));
		}

		log.info(crf.getTestStatistics());
		log.info("modelName: " + modelName);
		log.info("States generated in total: " + SlotFillingExplorer.statesgenerated);
	}

	public void postPredictOrganismModel(Map<Instance, State> results) {
		if (assignmentMode == EAssignmentMode.TREATMENT) {
			SCIOSlotTypes.hasOrganismModel.includeRec();

			if (mainClassProviderMode == EMainClassMode.PRE_PREDICTED) {
				int k = 1;
				for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictOrganismModel(instances, k)
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

		}

	}

	public void postPredictInjuryModel(Map<Instance, State> results) {
		if (assignmentMode == EAssignmentMode.TREATMENT) {
			SCIOSlotTypes.hasInjuryModel.includeRec();

			if (mainClassProviderMode == EMainClassMode.PRE_PREDICTED) {
				int k = 1;

				for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictInjuryModel(instances, k)
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

				for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictOrganismModel(instances, k)
						.entrySet()) {
					prediction.getKey().addCandidateAnnotations(prediction.getValue());
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

				for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictInjuryModel(instances, k)
						.entrySet()) {
					prediction.getKey().addCandidateAnnotations(prediction.getValue());
				}
			}

		}

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
		}

		if (mainClassProviderMode == EMainClassMode.PRE_PREDICTED) {
			int k = 1;

			for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictTreatment(instances, k).entrySet()) {
				prediction.getKey().addCandidateAnnotations(prediction.getValue());
			}
		}

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
		case GOLD:
			break;
		case TRAINING_PATTERN_NP_CHUNKS:
			addGroupNameTrainingPattern();
		case NP_CHUNKS:
			for (Instance instance : instances) {
				instance.addCandidateAnnotations(
						GroupNameExtraction.extractGroupNamesWithNPCHunks(distinctGroupNamesMode, instance));
			}
			break;
		case TRAINING_MANUAL_PATTERN:
			addGroupNameTrainingPattern();
		case MANUAL_PATTERN:
			for (Instance instance : instances) {
				instance.addCandidateAnnotations(
						GroupNameExtraction.extractGroupNamesWithPattern(distinctGroupNamesMode, instance));
			}
			break;
		case TRAINING_MANUAL_PATTERN_NP_CHUNKS:
			addGroupNameTrainingPattern();
		case MANUAL_PATTERN_NP_CHUNKS:
			for (Instance instance : instances) {
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
			for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictGroupName(instances, k).entrySet()) {
				prediction.getKey().addCandidateAnnotations(prediction.getValue());
			}
			break;
		}

		if (cardinalityMode == ECardinalityMode.SAMPLE || cardinalityMode == ECardinalityMode.RSS_PREDICTED_SAMPLE)
			for (Instance instance : instances) {
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

		if (cardinalityMode == ECardinalityMode.SAMPLE) {
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

	/**
	 * The state initializer.
	 * 
	 * @param groupNamesCandidates
	 * 
	 */
	private IStateInitializer buildStateInitializer() {

		if (cardinalityMode == ECardinalityMode.RSS_PREDICTED_SAMPLE) {
			PredictKMultiCardinalityInitializer init = new PredictKMultiCardinalityInitializer(modelName,
					cardinalityMode, groupNameProviderMode, groupNameClusteringMode, instances, 1, 8, instances);
			RootTemplateCardinalityExplorer.MAX_NUMBER_OF_ANNOTATIONS = init.max;
			RootTemplateCardinalityExplorer.MIN_NUMBER_OF_ANNOTATIONS = init.min;

			return init;

		}
		if (cardinalityMode == ECardinalityMode.RSS_PREDICTED) {
			return new PredictKMultiCardinalityInitializer(modelName, cardinalityMode, groupNameProviderMode,
					groupNameClusteringMode, instances, 1, 8, instances);

		}

		if (cardinalityMode == ECardinalityMode.SAMPLE)
			return new SampleCardinalityInitializer(SCIOEntityTypes.definedExperimentalGroup, 1);

		if (cardinalityMode == ECardinalityMode.PARALLEL || cardinalityMode == ECardinalityMode.PARALLEL_MODEL_UPDATE)
			return new MultiCardinalityInitializer(modelName, groupNameProviderMode, groupNameClusteringMode, instances,
					instances);

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

		for (Instance instance : instances) {

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

		for (Instance instance : instances) {

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

		for (Instance instance : instances) {

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
				"prediciton/data/annotations/groupNames/" + "GroupName_" + modelName + "_recall_at_" + k + "/");

		if (!groupNamesCacheDir.exists() || groupNamesCacheDir.list().length == 0) {
			groupNamesCacheDir.mkdirs();

			Map<SlotType, Boolean> x = SlotType.storeExcludance();
			SlotType.excludeAll();

			List<String> trainingInstanceNames = instances.stream().map(t -> t.getName()).collect(Collectors.toList());


			GroupNameNERLPredictor predictor = new GroupNameNERLPredictor("GroupName_" + modelName,
					trainingInstanceNames, new ArrayList<>(),new ArrayList<>());

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

		List<String> trainingInstanceNames = instances.stream().map(t -> t.getName()).collect(Collectors.toList());

		if (orgModelPredictor == null) {
			orgModelPredictor = new OrgModelSlotFillingPredictorPrediction("OrganismModel_PREDICT",
					trainingInstanceNames, rule, modus);
			orgModelPredictor.trainOrLoadModel();
		}

		Map<Instance, Set<AbstractAnnotation>> organismModelAnnotations = orgModelPredictor.predictInstances(instances,
				k);

		SlotType.restoreExcludance(x);
		return organismModelAnnotations;
	}

	private Map<Instance, Set<AbstractAnnotation>> predictTreatment(List<Instance> instances, int k) {

		/**
		 * Predict Treatment Model
		 */
		ETreatmentModifications rule = ETreatmentModifications.DOSAGE_DELIVERY_METHOD_APPLICATION_INSTRUMENT_DIRECTION;
		Map<SlotType, Boolean> x = SlotType.storeExcludance();

		List<String> trainingInstanceNames = instances.stream().map(t -> t.getName()).collect(Collectors.toList());

		if (treatmentPredictor == null) {
			treatmentPredictor = new TreatmentSlotFillingPredictorPrediction("Treatment_PREDICT", trainingInstanceNames,
					rule, modus);
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

		List<String> trainingInstanceNames = instances.stream().map(t -> t.getName()).collect(Collectors.toList());

		if (injuryPredictor == null) {
			injuryPredictor = new InjurySlotFillingPredictorPrediction("InjuryModel_PREDICT", trainingInstanceNames,
					rule, modus);
			injuryPredictor.trainOrLoadModel();
		}
		Map<Instance, Set<AbstractAnnotation>> injuryModelAnnotations = injuryPredictor.predictInstances(instances, k);

		SlotType.restoreExcludance(x);
		return injuryModelAnnotations;
	}

	private Map<Instance, Set<AbstractAnnotation>> getDictionaryBasedCandidates() {

		Map<Instance, Set<AbstractAnnotation>> annotations = new HashMap<>();

		Map<EntityType, Set<String>> trainDictionary = DictionaryFromInstanceHelper.toDictionary(instances);

		for (Instance instance : instances) {
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
