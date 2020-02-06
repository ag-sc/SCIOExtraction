package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
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
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.eval.NerlaEvaluator;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.deliverymethod.DeliveryMethodPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.evaluation.ExperimentalGroupEvaluation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.evaluation.InjuryEvaluation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.evaluation.OrganismModelEvaluation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.evaluation.TreatmentEvaluation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.groupnames.helper.GroupNameExtraction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.hardconstraints.DistinctExperimentalGroupConstraint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.initializer.GoldCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.initializer.MultiCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.initializer.PredictCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.initializer.SampleCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.investigation.CollectExpGroupNames;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EAssignmentMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.ECardinalityMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EComplexityMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EDistinctGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EExtractGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EGroupNamesPreProcessingMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EMainClassMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.specifications.ExperimentalGroupSpecifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.BOWCardinalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ContextBetweenSlotFillerTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrAllUsedTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrBOWTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrInnerNameBOWTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrNameBOWTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrNameOverlapTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrOuterNameBOWTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExpGroupTreatmentLocalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.Word2VecClusterTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.type_based.TB_BOWCardinalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.type_based.TB_ContextBetweenSlotFillerTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.type_based.TB_ContextCardinalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.type_based.TB_ExGrBOWTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.type_based.TB_ExpGroupTreatmentLocalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.type_based.TB_IntraTokenCardinalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.type_based.TB_RemainingTypesTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.type_based.TB_SameOrganismModelTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.type_based.TB_SlotIsFilledTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.type_based.TB_TreatmentCardinalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.type_based.TB_TreatmentPriorTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.type_based.TB_Word2VecClusterTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.InjuryRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.InjuryRestrictionProvider.EInjuryModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.InjurySlotFilling;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.InjurySlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.orgmodel.OrgModelSlotFilling;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.orgmodel.OrgModelSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.orgmodel.OrganismModelRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.treatment.TreatmentRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.treatment.TreatmentRestrictionProvider.ETreatmentModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea.VertebralAreaPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.AgeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.WeightNormalization;

public class ExperimentalGroupSlotFilling extends AbstractSemReadProject {

	public static void main(String[] args) throws IOException {

		new ExperimentalGroupSlotFilling();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private final IObjectiveFunction trainingObjectiveFunction;

	private final IObjectiveFunction predictionObjectiveFunction;
//
//	
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

	private final File instanceDirectory = new File(
			"src/main/resources/slotfilling/experimental_group/corpus/instances/");

	private InstanceProvider instanceProvider;

	private List<Instance> trainingInstances;
	private List<Instance> devInstances;
	private List<Instance> testInstances;

	public EMainClassMode mainClassProviderMode;
	public EExtractGroupNamesMode groupNameProviderMode;
	public EGroupNamesPreProcessingMode groupNameProcessingMode;
	public ECardinalityMode cardinalityMode;
	public EAssignmentMode assignmentMode;
	public EComplexityMode complexityMode;

	private ETreatmentModifications treatmentModificationsRule;
	private EOrgModelModifications orgModelModificationsRule;
	private EInjuryModifications injuryModificationRule;

	private EDistinctGroupNamesMode distinctGroupNamesMode;

	private EExplorationMode explorationMode;

	private String modelName;

	public ExperimentalGroupSlotFilling() throws IOException {
		super(SystemScope.Builder.getScopeHandler().addScopeSpecification(ExperimentalGroupSpecifications.systemsScope)
				.apply().registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization()).build());

		Annotations.removeDuplicates = true;

		CartesianEvaluator.MAXIMUM_PERMUTATION_SIZE = 4;

		SlotFillingExplorer.MAX_NUMBER_OF_ANNOTATIONS = 8;

		String rand = String.valueOf(new Random().nextInt(100000));
//		modelName = "ExperimentalGroup" + 50700;
		modelName = "ExperimentalGroup" + rand;
		log.info("Model name = " + modelName);

		groupNameProviderMode = EExtractGroupNamesMode.PATTERN_NP_CHUNKS;
		groupNameProcessingMode = EGroupNamesPreProcessingMode.WEKA_CLUSTERING;
		distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;

		mainClassProviderMode = EMainClassMode.PRE_PREDICTED;

		cardinalityMode = ECardinalityMode.PARALLEL_CARDINALITIES;

		assignmentMode = EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY;
		complexityMode = EComplexityMode.ROOT;
		explorationMode = EExplorationMode.TYPE_BASED;

		trainingObjectiveFunction = new SlotFillingObjectiveFunction(
				new CartesianEvaluator(explorationMode == EExplorationMode.TYPE_BASED ? EEvaluationDetail.ENTITY_TYPE
						: EEvaluationDetail.DOCUMENT_LINKED));

		predictionObjectiveFunction = new SlotFillingObjectiveFunction(
				new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE));
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

		List<IExplorationStrategy> explorerList = buildExplorer();

		getData();

		addGroupNameCandidates();

		addTreatmentCandidates();

		addOrganismModelCandidates();

		addInjuryCandidates();

		IStateInitializer initializer = buildStateInitializer();

		List<AbstractFeatureTemplate<?>> featureTemplates = getFeatureTemplates();

		Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> parameter = getParameter();

		ISampler sampler = getSampler();

		MaxChainLengthCrit maxStepCrit = new MaxChainLengthCrit(100);

		ConverganceCrit noModelChangeCrit = new ConverganceCrit(10 * explorerList.size(), s -> s.getModelScore());

		ISamplingStoppingCriterion[] sampleStoppingCrits = new ISamplingStoppingCriterion[] { maxStepCrit,
				noModelChangeCrit };

		run(explorerList, sampler, initializer, sampleStoppingCrits, maxStepCrit, featureTemplates, parameter);

	}

	private void run(List<IExplorationStrategy> explorerList, ISampler sampler, IStateInitializer initializer,
			ISamplingStoppingCriterion[] sampleStoppingCrits, ISamplingStoppingCriterion maxStepCrit,
			List<AbstractFeatureTemplate<?>> featureTemplates,
			Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> parameter) {
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

		model.setParameter(parameter);

		/**
		 * Create a new semantic parsing CRF and initialize with needed parameter.
		 */
		ISemanticParsingCRF crf;
		if (cardinalityMode == ECardinalityMode.PARALLEL_CARDINALITIES)
			crf = new SemanticParsingCRFMultiState(model, explorerList, (AbstractBeamSampler) sampler,
					trainingObjectiveFunction);
		else
			crf = new SemanticParsingCRF(model, explorerList, (AbstractSampler) sampler, trainingObjectiveFunction);

		crf.setInitializer(initializer);

//		log.info("Training instances coverage: "
//				+ crf.computeCoverage(true, predictionObjectiveFunction, trainingInstances));
//
//		log.info("Test instances coverage: " + crf.computeCoverage(true, predictionObjectiveFunction, testInstances));
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
		/**
		 * Evaluate with objective function
		 */
		evaluate(log, results, predictionObjectiveFunction);

		/**
		 * Evaluate assuming TT,OM,IM are one unit. Evaluate assuming TT,OM,IM are one
		 * unit, distinguish vehicle-treatments and main-treatments.
		 */

		evaluateDetailed(results);

		log.info(crf.getTrainingStatistics());
		log.info(crf.getTestStatistics());
		log.info("modelName: " + modelName);
		log.info("States generated: " + SlotFillingExplorer.statesgenerated);

	}

	private void evaluateDetailed(Map<Instance, State> mergedResults) {

		log.info("samplingMode: " + explorationMode);
		log.info("assignmentMode: " + assignmentMode);
		log.info("cardinalityMode: " + cardinalityMode);
		log.info("groupNameProcessingMode: " + groupNameProcessingMode);
		log.info("groupNameProviderMode: " + groupNameProviderMode);
		log.info("mainClassProviderMode: " + mainClassProviderMode);
		log.info("distinctGroupNamesMode: " + distinctGroupNamesMode);

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
			treatmentEvaluation.evaluate(mergedResults);
			SlotType.restoreExcludance();
		}
		if (SCIOSlotTypes.hasOrganismModel.isIncluded()) {
			SlotType.storeExcludance();
			SlotType.excludeAll();
			SCIOSlotTypes.hasOrganismModel.include();
			SCIOSlotTypes.hasOrganismSpecies.include();
			OrganismModelEvaluation organismModelEvaluation = new OrganismModelEvaluation(nerlaEvaluator);
			organismModelEvaluation.evaluate(mergedResults);
			SlotType.restoreExcludance();
		}

		if (SCIOSlotTypes.hasInjuryModel.isIncluded()) {
			SlotType.storeExcludance();
			SlotType.excludeAll();
			SCIOSlotTypes.hasInjuryModel.include();
			InjuryEvaluation injuryModelEvaluation = new InjuryEvaluation(nerlaEvaluator);
			injuryModelEvaluation.evaluate(mergedResults);
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

		expGroupEval.evaluate(mergedResults);
		SlotType.restoreExcludance();
	}

	private ISampler getSampler() {
		if (cardinalityMode == ECardinalityMode.PARALLEL_CARDINALITIES)
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

			String vertebralAreaModelName = "VertebralArea_" + modelName;
			VertebralAreaPredictor vertebralAreaPrediction = new VertebralAreaPredictor(vertebralAreaModelName, scope,
					trainingInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					devInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					testInstances.stream().map(i -> i.getName()).collect(Collectors.toList()));

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

		if (assignmentMode == EAssignmentMode.TREATMENT || assignmentMode == EAssignmentMode.TREATMENT_ORGANISM_MODEL
				|| assignmentMode == EAssignmentMode.INJURY_TREATMENT
				|| assignmentMode == EAssignmentMode.TREATMENT_ORGANISM_MODEL_INJURY) {

			if (mainClassProviderMode == EMainClassMode.SAMPLE
					|| mainClassProviderMode == EMainClassMode.PRE_PREDICTED) {
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

//			if (mainClassProviderMode == EMainClassMode.PREDICT) {
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
		}

		if (treatmentModificationsRule != ETreatmentModifications.ROOT) {
			final int kDelivery = 2;

			String deliveryMethodModelName = "DeliveryMethod" + modelName;

			DeliveryMethodPredictor deliveryMethodPrediction = new DeliveryMethodPredictor(deliveryMethodModelName,
					scope, trainingInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					devInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					testInstances.stream().map(i -> i.getName()).collect(Collectors.toList()));

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
			treatments.addAll(expGroup.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller().stream()
					.filter(a -> a != null).collect(Collectors.toList()));
		}
		return treatments;
	}

	private Set<AbstractAnnotation> extractGoldOrganismModels(Instance instance) {
		Set<AbstractAnnotation> orgModels = new HashSet<>();
		for (EntityTemplate expGroup : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {
			if (expGroup.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel).containsSlotFiller())
				orgModels.add(expGroup.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel).getSlotFiller());
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

	public void getData() {
		// List<String> docs = Files.readAllLines(new
		// File("src/main/resources/slotfilling/corpus_docs.csv").toPath());
		//
		// Collections.shuffle(docs, new Random(1000L));
		//
		// List<String> trainingInstanceNames = docs.subList(0, 75);
		// List<String> testInstanceNames = docs.subList(75, docs.size());
		//
		// AbstractCorpusDistributor corpusDistributor = new
		// SpecifiedDistributor.Builder()
		// .setTrainingInstanceNames(trainingInstanceNames).setTestInstanceNames(testInstanceNames).build();

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
				.setTestProportion(20).setSeed(1000L).build();

		InstanceProvider.maxNumberOfAnnotations = 8;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		Collection<GoldModificationRule> goldModificationRules = getGoldModificationRules();

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor, goldModificationRules);

		trainingInstances = instanceProvider.getRedistributedTrainingInstances();
		devInstances = instanceProvider.getRedistributedDevelopmentInstances();
		testInstances = instanceProvider.getRedistributedTestInstances();
	}

	public List<IExplorationStrategy> buildExplorer() {

		List<IExplorationStrategy> explorerList = new ArrayList<>();

		HardConstraintsProvider hardConstraintsProvider = new HardConstraintsProvider();

		hardConstraintsProvider.addHardConstraints(
				new DistinctExperimentalGroupConstraint(predictionObjectiveFunction.getEvaluator()));

		SlotFillingExplorer slotFillingExplorer = new SlotFillingExplorer(explorationMode, predictionObjectiveFunction,
				hardConstraintsProvider);
		explorerList.add(slotFillingExplorer);

		if (cardinalityMode == ECardinalityMode.SAMPLE_CARDINALITY) {
			RootTemplateCardinalityExplorer.MAX_NUMBER_OF_ANNOTATIONS = CartesianEvaluator.MAXIMUM_PERMUTATION_SIZE;
			RootTemplateCardinalityExplorer rootTemplateCardinalityExplorer = new RootTemplateCardinalityExplorer(
					explorationMode, AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));
			explorerList.add(rootTemplateCardinalityExplorer);
		}
		return explorerList;
	}

	public void applyModesAndRestrictions() {

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
		} else if (groupNameProcessingMode != EGroupNamesPreProcessingMode.SAMPLE) {
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
		}
	}

	private void addGroupNameCandidates() {

		if (groupNameProviderMode != EExtractGroupNamesMode.GOLD
				&& groupNameProviderMode != EExtractGroupNamesMode.EMPTY) {
			Map<Instance, Set<AbstractAnnotation>> annotations = getDictionaryBasedCandidates();

			for (Instance instance : annotations.keySet()) {

				Set<String> distinctGroupNames = new HashSet<>();

				for (AbstractAnnotation nerla : annotations.get(instance)) {

					if (nerla.getEntityType() == SCIOEntityTypes.groupName) {

						if (CollectExpGroupNames.STOP_TERM_LIST
								.contains(nerla.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm()))
							continue;

						if (distinctGroupNamesMode == EDistinctGroupNamesMode.STRING_DISTINCT) {

							if (distinctGroupNames
									.contains(nerla.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm()))
								continue;

							distinctGroupNames.add(nerla.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm());
						}

						instance.addCandidateAnnotation(nerla);
					}

				}
			}
		}

		for (Instance instance : instanceProvider.getInstances()) {
			/**
			 * Get and add group names
			 */
			List<DocumentLinkedAnnotation> candidates = GroupNameExtraction.extractGroupNames(instance,
					distinctGroupNamesMode, groupNameProviderMode);

			instance.addCandidateAnnotations(candidates);

			/**
			 * Add groupNames as DefinedExperimentalGroup
			 */
			if (mainClassProviderMode == EMainClassMode.SAMPLE)
				for (DocumentLinkedAnnotation ec : candidates) {
					instance.addCandidateAnnotation(AnnotationBuilder.toAnnotation(instance.getDocument(),
							SCIOEntityTypes.definedExperimentalGroup, ec.getSurfaceForm(), ec.getStartDocCharOffset()));
				}

		}
	}

	/*
	 * Some comment
	 */
	private Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> getParameter() {

		Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> parameter = new HashMap<>();
		if (true)
			return parameter;

		if (cardinalityMode == ECardinalityMode.GOLD_CARDINALITY) {
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
		} else if (cardinalityMode == ECardinalityMode.SAMPLE_CARDINALITY) {
		} else if (cardinalityMode == ECardinalityMode.PREDICTED_CARDINALITY) {
			throw new IllegalArgumentException("Not impl exception");
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

	private int getNumberOfEpochs() {
		return 10;
//		return 1;
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

					if (Annotations.removeDuplicates)
						for (AbstractAnnotation abstractAnnotation2 : distinctAnnotations) {

							if (abstractAnnotation2
									.evaluate(predictionObjectiveFunction.getEvaluator(), abstractAnnotation)
									.getF1() == 1.0D) {
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

				/*
				 * Apply organism model modifications...
				 */
				List<AbstractAnnotation> newOrganimModels = new ArrayList<>();

				SingleFillerSlot organimModel = a.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel);

				if (!organimModel.containsSlotFiller())
					return a;

				AbstractAnnotation modifiedModel = organimModel.getSlotFiller();
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

				return a;
			});

		if (SCIOSlotTypes.hasInjuryModel.isIncluded())
			goldModificationRules.add(a -> {

				/*
				 * Apply injury modifications...
				 */
				List<AbstractAnnotation> newInjury = new ArrayList<>();

				SingleFillerSlot organimModel = a.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel);

				if (!organimModel.containsSlotFiller())
					return a;

				AbstractAnnotation modifiedModel = organimModel.getSlotFiller();
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

		return goldModificationRules;
	}

	/**
	 * The state initializer.
	 * 
	 * @param groupNamesCandidates
	 * 
	 */
	private IStateInitializer buildStateInitializer() {

		switch (cardinalityMode) {
		case GOLD_CARDINALITY:
			return new GoldCardinalityInitializer(groupNameProviderMode, groupNameProcessingMode);
		case PREDICTED_CARDINALITY:
			return new PredictCardinalityInitializer(groupNameProviderMode, groupNameProcessingMode,
					instanceProvider.getInstances());
		case SAMPLE_CARDINALITY:
			return new SampleCardinalityInitializer(1);
		case PARALLEL_CARDINALITIES:
			return new MultiCardinalityInitializer(groupNameProviderMode, groupNameProcessingMode,
					instanceProvider.getInstances(), trainingInstances);
		default:
			return null;
		}
	}

	private File getModelBaseDir() {
		return new File("models/slotfilling/experimental_group/");
	}

	private AdvancedLearner newLearner() {
		return new AdvancedLearner(new SGD(0.0001, 0), new L2(0.0000000));
	}

	private void addTreatmentCandidatesFromNERLA() {

		JSONNerlaReader prov = new JSONNerlaReader(new File("src/main/resources/slotfilling/treatment/corpus/nerla/"));

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
				new File("src/main/resources/slotfilling/organism_model/corpus/nerla/"));

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

		JSONNerlaReader prov = new JSONNerlaReader(new File("src/main/resources/slotfilling/injury/corpus/nerla/"));

		for (Instance instance : instanceProvider.getInstances()) {

			for (DocumentLinkedAnnotation nerla : prov.getForInstance(instance)) {

				instance.addCandidateAnnotation(nerla);
			}
		}

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
		OrgModelSlotFillingPredictor predictor = new OrgModelSlotFillingPredictor("OrganismModel_12345", scope,
				trainingInstanceNames, developInstanceNames, testInstanceNames, rule);
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
		InjurySlotFillingPredictor predictor = new InjurySlotFillingPredictor("InjuryModel_12345", scope,
				trainingInstanceNames, developInstanceNames, testInstanceNames, rule);
		predictor.trainOrLoadModel();

		Map<Instance, Set<AbstractAnnotation>> organismModelAnnotations = predictor.predictInstances(instances, k);

		SlotType.restoreExcludance();
		return organismModelAnnotations;
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

	private List<AbstractFeatureTemplate<?>> getFeatureTemplates() {
		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();

		if (explorationMode == EExplorationMode.ANNOTATION_BASED) {
			featureTemplates.add(new BOWCardinalityTemplate());

			featureTemplates.add(new TB_TreatmentPriorTemplate());

			featureTemplates.add(new TB_TreatmentCardinalityTemplate());

			featureTemplates.add(new ExGrBOWTemplate());
			//

			/**
			 * TODO: activate if cardinality can be predicted without gold data.
			 */
//		if (cardinalityMode != ECardinalityMode.SAMPLE_CARDINALITY)
//			featureTemplates.add(new ExGrAllUsedTemplate());
//
			featureTemplates.add(new ExGrInnerNameBOWTemplate());
//		
			featureTemplates.add(new ExGrOuterNameBOWTemplate());
//
			featureTemplates.add(new ExGrNameBOWTemplate());
//
			featureTemplates.add(new ExGrNameOverlapTemplate());
//
			featureTemplates.add(new TB_SlotIsFilledTemplate());

			featureTemplates.add(new ExpGroupTreatmentLocalityTemplate());

			featureTemplates.add(new ContextBetweenSlotFillerTemplate());

			featureTemplates.add(new TB_ContextCardinalityTemplate());

			featureTemplates.add(new TB_IntraTokenCardinalityTemplate());

			featureTemplates.add(new Word2VecClusterTemplate());

		}
		if (explorationMode == EExplorationMode.TYPE_BASED) {

			featureTemplates.add(new TB_Word2VecClusterTemplate());
			featureTemplates.add(new TB_IntraTokenCardinalityTemplate());
			featureTemplates.add(new TB_ContextCardinalityTemplate());
			featureTemplates.add(new TB_ContextBetweenSlotFillerTemplate());
			featureTemplates.add(new TB_ExpGroupTreatmentLocalityTemplate());
			featureTemplates.add(new TB_BOWCardinalityTemplate());
			featureTemplates.add(new TB_ExGrBOWTemplate());
			featureTemplates.add(new TB_SlotIsFilledTemplate());
			featureTemplates.add(new TB_RemainingTypesTemplate());
			featureTemplates.add(new TB_SameOrganismModelTemplate());
			featureTemplates.add(new TB_SlotIsFilledTemplate());
			featureTemplates.add(new TB_TreatmentPriorTemplate());

		}

		return featureTemplates;
	}
}

// ALl REAl

//TREATMENTS MICRO: BOTH = Score [getF1()=0.393, getPrecision()=1.000, getRecall()=0.245, tp=12, fp=0, fn=37, tn=0]
//TREATMENTS MICRO: Vehicle = Score [getF1()=0.000, getPrecision()=0.000, getRecall()=0.000, tp=0, fp=0, fn=14, tn=0]
//TREATMENTS MICRO: Non Vehicle = Score [getF1()=0.511, getPrecision()=1.000, getRecall()=0.343, tp=12, fp=0, fn=23, tn=0]
//ORGANISM MODEL MICRO: SCORE = Score [getF1()=0.500, getPrecision()=0.346, getRecall()=0.900, tp=18, fp=34, fn=2, tn=0]
//INJURY MODEL MICRO: SCORE = Score [getF1()=0.478, getPrecision()=0.423, getRecall()=0.550, tp=11, fp=15, fn=9, tn=0]
//EXP GROUP MICRO  CARDINALITY = Score [getF1()=0.806, getPrecision()=0.722, getRecall()=0.912, tp=52, fp=20, fn=5, tn=0]
//EXP GROUP MICRO  CARDINALITY RMSE = 1.5727950313140984
//EXP GROUP MICRO  SCORE = Score [getF1()=0.455, getPrecision()=0.517, getRecall()=0.407, tp=92, fp=86, fn=134, tn=0]
//EXP GROUP MICRO: TREATMENT BOTH = Score [getF1()=0.519, getPrecision()=0.976, getRecall()=0.354, tp=40, fp=1, fn=73, tn=0]
//EXP GROUP MICRO: TREATMENT Vehicle = Score [getF1()=0.000, getPrecision()=0.000, getRecall()=0.000, tp=0, fp=0, fn=19, tn=0]
//EXP GROUP MICRO: TREATMENT Non Vehicle = Score [getF1()=0.593, getPrecision()=0.976, getRecall()=0.426, tp=40, fp=1, fn=54, tn=0]
//EXP GROUP MICRO: ORG MODEL = Score [getF1()=0.406, getPrecision()=0.366, getRecall()=0.456, tp=26, fp=45, fn=31, tn=0]
//EXP GROUP MICRO: INJURY MODEL = Score [getF1()=0.426, getPrecision()=0.394, getRecall()=0.464, tp=26, fp=40, fn=30, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=76819290]
//CRFStatistics [context=Test, getTotalDuration()=4336707]
//modelName: ExperimentalGroup42009
//States generated: 3500250
//----------------------------------------------------------------

//Additional Template: RemainingTypesTemplate

//ALL REAL

//TREATMENTS MICRO: BOTH = Score [getF1()=0.500, getPrecision()=0.704, getRecall()=0.388, tp=19, fp=8, fn=30, tn=0]
//TREATMENTS MICRO: Vehicle = Score [getF1()=0.000, getPrecision()=0.000, getRecall()=0.000, tp=0, fp=0, fn=14, tn=0]
//TREATMENTS MICRO: Non Vehicle = Score [getF1()=0.613, getPrecision()=0.704, getRecall()=0.543, tp=19, fp=8, fn=16, tn=0]
//ORGANISM MODEL MICRO: SCORE = Score [getF1()=0.633, getPrecision()=0.475, getRecall()=0.950, tp=19, fp=21, fn=1, tn=0]
//INJURY MODEL MICRO: SCORE = Score [getF1()=0.522, getPrecision()=0.462, getRecall()=0.600, tp=12, fp=14, fn=8, tn=0]
//EXP GROUP MICRO  CARDINALITY = Score [getF1()=0.810, getPrecision()=0.739, getRecall()=0.895, tp=51, fp=18, fn=6, tn=0]
//EXP GROUP MICRO  CARDINALITY RMSE = 1.5559732104309982
//EXP GROUP MICRO  SCORE = Score [getF1()=0.473, getPrecision()=0.500, getRecall()=0.450, tp=98, fp=98, fn=120, tn=0]
//EXP GROUP MICRO: TREATMENT BOTH = Score [getF1()=0.491, getPrecision()=0.661, getRecall()=0.390, tp=41, fp=21, fn=64, tn=0]
//EXP GROUP MICRO: TREATMENT Vehicle = Score [getF1()=0.000, getPrecision()=0.000, getRecall()=0.000, tp=0, fp=0, fn=19, tn=0]
//EXP GROUP MICRO: TREATMENT Non Vehicle = Score [getF1()=0.554, getPrecision()=0.661, getRecall()=0.477, tp=41, fp=21, fn=45, tn=0]
//EXP GROUP MICRO: ORG MODEL = Score [getF1()=0.524, getPrecision()=0.478, getRecall()=0.579, tp=33, fp=36, fn=24, tn=0]
//EXP GROUP MICRO: INJURY MODEL = Score [getF1()=0.397, getPrecision()=0.369, getRecall()=0.429, tp=24, fp=41, fn=32, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=75242255]
//CRFStatistics [context=Test, getTotalDuration()=5241078]
//modelName: ExperimentalGroup49767
//States generated: 3562396

//----------------------------------------------------------------

//ALL GOLD

//TREATMENTS MICRO: BOTH = Score [getF1()=0.957, getPrecision()=1.000, getRecall()=0.918, tp=45, fp=0, fn=4, tn=0]
//TREATMENTS MICRO: Vehicle = Score [getF1()=0.833, getPrecision()=1.000, getRecall()=0.714, tp=10, fp=0, fn=4, tn=0]
//TREATMENTS MICRO: Non Vehicle = Score [getF1()=1.000, getPrecision()=1.000, getRecall()=1.000, tp=35, fp=0, fn=0, tn=0]
//ORGANISM MODEL MICRO: SCORE = Score [getF1()=0.974, getPrecision()=1.000, getRecall()=0.950, tp=19, fp=0, fn=1, tn=0]
//INJURY MODEL MICRO: SCORE = Score [getF1()=1.000, getPrecision()=1.000, getRecall()=1.000, tp=20, fp=0, fn=0, tn=0]
//EXP GROUP MICRO  CARDINALITY = Score [getF1()=1.000, getPrecision()=1.000, getRecall()=1.000, tp=57, fp=0, fn=0, tn=0]
//EXP GROUP MICRO  CARDINALITY RMSE = 0.0
//EXP GROUP MICRO  SCORE = Score [getF1()=0.886, getPrecision()=0.871, getRecall()=0.901, tp=182, fp=27, fn=20, tn=0]
//EXP GROUP MICRO: TREATMENT BOTH = Score [getF1()=0.817, getPrecision()=0.765, getRecall()=0.876, tp=78, fp=24, fn=11, tn=0]
//EXP GROUP MICRO: TREATMENT Vehicle = Score [getF1()=0.774, getPrecision()=1.000, getRecall()=0.632, tp=12, fp=0, fn=7, tn=0]
//EXP GROUP MICRO: TREATMENT Non Vehicle = Score [getF1()=0.825, getPrecision()=0.733, getRecall()=0.943, tp=66, fp=24, fn=4, tn=0]
//EXP GROUP MICRO: ORG MODEL = Score [getF1()=0.945, getPrecision()=0.981, getRecall()=0.912, tp=52, fp=1, fn=5, tn=0]
//EXP GROUP MICRO: INJURY MODEL = Score [getF1()=0.945, getPrecision()=0.963, getRecall()=0.929, tp=52, fp=2, fn=4, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=169806]
//CRFStatistics [context=Test, getTotalDuration()=5461]
//modelName: ExperimentalGroup20719
//States generated: 252855

// MAIN CLASS PREDICT REST GOLD
//TREATMENTS MICRO: BOTH = Score [getF1()=0.545, getPrecision()=0.615, getRecall()=0.490, tp=24, fp=15, fn=25, tn=0]
//TREATMENTS MICRO: Vehicle = Score [getF1()=0.250, getPrecision()=1.000, getRecall()=0.143, tp=2, fp=0, fn=12, tn=0]
//TREATMENTS MICRO: Non Vehicle = Score [getF1()=0.611, getPrecision()=0.595, getRecall()=0.629, tp=22, fp=15, fn=13, tn=0]
//ORGANISM MODEL MICRO: SCORE = Score [getF1()=0.642, getPrecision()=0.515, getRecall()=0.850, tp=17, fp=16, fn=3, tn=0]
//INJURY MODEL MICRO: SCORE = Score [getF1()=0.449, getPrecision()=0.379, getRecall()=0.550, tp=11, fp=18, fn=9, tn=0]
//EXP GROUP MICRO  CARDINALITY = Score [getF1()=1.000, getPrecision()=1.000, getRecall()=1.000, tp=57, fp=0, fn=0, tn=0]
//EXP GROUP MICRO  CARDINALITY RMSE = 0.0
//EXP GROUP MICRO  SCORE = Score [getF1()=0.475, getPrecision()=0.470, getRecall()=0.480, tp=95, fp=107, fn=103, tn=0]
//EXP GROUP MICRO: TREATMENT BOTH = Score [getF1()=0.425, getPrecision()=0.416, getRecall()=0.435, tp=37, fp=52, fn=48, tn=0]
//EXP GROUP MICRO: TREATMENT Vehicle = Score [getF1()=0.273, getPrecision()=1.000, getRecall()=0.158, tp=3, fp=0, fn=16, tn=0]
//EXP GROUP MICRO: TREATMENT Non Vehicle = Score [getF1()=0.447, getPrecision()=0.395, getRecall()=0.515, tp=34, fp=52, fn=32, tn=0]
//EXP GROUP MICRO: ORG MODEL = Score [getF1()=0.579, getPrecision()=0.579, getRecall()=0.579, tp=33, fp=24, fn=24, tn=0]
//EXP GROUP MICRO: INJURY MODEL = Score [getF1()=0.446, getPrecision()=0.446, getRecall()=0.446, tp=25, fp=31, fn=31, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=460965]
//CRFStatistics [context=Test, getTotalDuration()=57495]
//modelName: ExperimentalGroup35429
//States generated: 2031996

//ALL GOLD BUT NO GROUPNAMES
//TREATMENTS MICRO: BOTH = Score [getF1()=0.990, getPrecision()=1.000, getRecall()=0.980, tp=48, fp=0, fn=1, tn=0]
//TREATMENTS MICRO: Vehicle = Score [getF1()=1.000, getPrecision()=1.000, getRecall()=1.000, tp=14, fp=0, fn=0, tn=0]
//TREATMENTS MICRO: Non Vehicle = Score [getF1()=0.986, getPrecision()=1.000, getRecall()=0.971, tp=34, fp=0, fn=1, tn=0]
//ORGANISM MODEL MICRO: SCORE = Score [getF1()=0.974, getPrecision()=1.000, getRecall()=0.950, tp=19, fp=0, fn=1, tn=0]
//INJURY MODEL MICRO: SCORE = Score [getF1()=0.974, getPrecision()=1.000, getRecall()=0.950, tp=19, fp=0, fn=1, tn=0]
//EXP GROUP MICRO  CARDINALITY = Score [getF1()=1.000, getPrecision()=1.000, getRecall()=1.000, tp=57, fp=0, fn=0, tn=0]
//EXP GROUP MICRO  CARDINALITY RMSE = 0.0
//EXP GROUP MICRO  SCORE = Score [getF1()=0.812, getPrecision()=0.778, getRecall()=0.848, tp=168, fp=48, fn=30, tn=0]
//EXP GROUP MICRO: TREATMENT BOTH = Score [getF1()=0.722, getPrecision()=0.617, getRecall()=0.871, tp=74, fp=46, fn=11, tn=0]
//EXP GROUP MICRO: TREATMENT Vehicle = Score [getF1()=0.723, getPrecision()=0.607, getRecall()=0.895, tp=17, fp=11, fn=2, tn=0]
//EXP GROUP MICRO: TREATMENT Non Vehicle = Score [getF1()=0.722, getPrecision()=0.620, getRecall()=0.864, tp=57, fp=35, fn=9, tn=0]
//EXP GROUP MICRO: ORG MODEL = Score [getF1()=0.907, getPrecision()=0.961, getRecall()=0.860, tp=49, fp=2, fn=8, tn=0]
//EXP GROUP MICRO: INJURY MODEL = Score [getF1()=0.891, getPrecision()=1.000, getRecall()=0.804, tp=45, fp=0, fn=11, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=25583]
//CRFStatistics [context=Test, getTotalDuration()=2984]
//modelName: ExperimentalGroup39554
//States generated: 260627

// MAIN GOLD, NO GROUPNAMES, MULTI CARD
//TREATMENTS MICRO: BOTH = Score [getF1()=0.860, getPrecision()=1.000, getRecall()=0.755, tp=37, fp=0, fn=12, tn=0]
//TREATMENTS MICRO: Vehicle = Score [getF1()=0.783, getPrecision()=1.000, getRecall()=0.643, tp=9, fp=0, fn=5, tn=0]
//TREATMENTS MICRO: Non Vehicle = Score [getF1()=0.889, getPrecision()=1.000, getRecall()=0.800, tp=28, fp=0, fn=7, tn=0]
//ORGANISM MODEL MICRO: SCORE = Score [getF1()=0.974, getPrecision()=1.000, getRecall()=0.950, tp=19, fp=0, fn=1, tn=0]
//INJURY MODEL MICRO: SCORE = Score [getF1()=0.974, getPrecision()=1.000, getRecall()=0.950, tp=19, fp=0, fn=1, tn=0]
//EXP GROUP MICRO  CARDINALITY = Score [getF1()=0.784, getPrecision()=0.950, getRecall()=0.667, tp=38, fp=2, fn=19, tn=0]
//EXP GROUP MICRO  CARDINALITY RMSE = 1.5727950313140984
//EXP GROUP MICRO  SCORE = Score [getF1()=0.743, getPrecision()=0.934, getRecall()=0.617, tp=127, fp=9, fn=79, tn=0]
//EXP GROUP MICRO: TREATMENT BOTH = Score [getF1()=0.701, getPrecision()=0.885, getRecall()=0.581, tp=54, fp=7, fn=39, tn=0]
//EXP GROUP MICRO: TREATMENT Vehicle = Score [getF1()=0.667, getPrecision()=0.909, getRecall()=0.526, tp=10, fp=1, fn=9, tn=0]
//EXP GROUP MICRO: TREATMENT Non Vehicle = Score [getF1()=0.710, getPrecision()=0.880, getRecall()=0.595, tp=44, fp=6, fn=30, tn=0]
//EXP GROUP MICRO: ORG MODEL = Score [getF1()=0.792, getPrecision()=0.974, getRecall()=0.667, tp=38, fp=1, fn=19, tn=0]
//EXP GROUP MICRO: INJURY MODEL = Score [getF1()=0.761, getPrecision()=0.972, getRecall()=0.625, tp=35, fp=1, fn=21, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=70814]
//CRFStatistics [context=Test, getTotalDuration()=4821]
//modelName: ExperimentalGroup36084
//States generated: 760057

// ALL GOLD BUT CLUSTERING NAD CARDINALITIES
//TREATMENTS MICRO: BOTH = Score [getF1()=0.886, getPrecision()=1.000, getRecall()=0.796, tp=39, fp=0, fn=10, tn=0]
//TREATMENTS MICRO: Vehicle = Score [getF1()=0.444, getPrecision()=1.000, getRecall()=0.286, tp=4, fp=0, fn=10, tn=0]
//TREATMENTS MICRO: Non Vehicle = Score [getF1()=1.000, getPrecision()=1.000, getRecall()=1.000, tp=35, fp=0, fn=0, tn=0]
//ORGANISM MODEL MICRO: SCORE = Score [getF1()=0.974, getPrecision()=1.000, getRecall()=0.950, tp=19, fp=0, fn=1, tn=0]
//INJURY MODEL MICRO: SCORE = Score [getF1()=0.974, getPrecision()=1.000, getRecall()=0.950, tp=19, fp=0, fn=1, tn=0]
//EXP GROUP MICRO  CARDINALITY = Score [getF1()=0.850, getPrecision()=0.771, getRecall()=0.947, tp=54, fp=16, fn=3, tn=0]
//EXP GROUP MICRO  CARDINALITY RMSE = 1.2773327473170102
//EXP GROUP MICRO  SCORE = Score [getF1()=0.792, getPrecision()=0.745, getRecall()=0.845, tp=175, fp=60, fn=32, tn=0]
//EXP GROUP MICRO: TREATMENT BOTH = Score [getF1()=0.738, getPrecision()=0.679, getRecall()=0.809, tp=76, fp=36, fn=18, tn=0]
//EXP GROUP MICRO: TREATMENT Vehicle = Score [getF1()=0.462, getPrecision()=0.857, getRecall()=0.316, tp=6, fp=1, fn=13, tn=0]
//EXP GROUP MICRO: TREATMENT Non Vehicle = Score [getF1()=0.778, getPrecision()=0.667, getRecall()=0.933, tp=70, fp=35, fn=5, tn=0]
//EXP GROUP MICRO: ORG MODEL = Score [getF1()=0.864, getPrecision()=0.836, getRecall()=0.895, tp=51, fp=10, fn=6, tn=0]
//EXP GROUP MICRO: INJURY MODEL = Score [getF1()=0.814, getPrecision()=0.774, getRecall()=0.857, tp=48, fp=14, fn=8, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=1731228]
//CRFStatistics [context=Test, getTotalDuration()=12861]
//modelName: ExperimentalGroup93583
//States generated: 692301
