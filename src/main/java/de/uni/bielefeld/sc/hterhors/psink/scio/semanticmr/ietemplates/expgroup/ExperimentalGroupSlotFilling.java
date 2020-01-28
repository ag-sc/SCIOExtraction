package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.candidateretrieval.helper.DictionaryFromInstanceHelper;
import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.SemanticParsingCRFMultiState;
import de.hterhors.semanticmr.crf.exploration.IExplorationStrategy;
import de.hterhors.semanticmr.crf.exploration.RootTemplateCardinalityExplorer;
import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer;
import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer.ESamplingMode;
import de.hterhors.semanticmr.crf.exploration.constraints.HardConstraintsProvider;
import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.learner.optimizer.SGD;
import de.hterhors.semanticmr.crf.learner.regularizer.L2;
import de.hterhors.semanticmr.crf.model.Model;
import de.hterhors.semanticmr.crf.of.IObjectiveFunction;
import de.hterhors.semanticmr.crf.of.SlotFillingObjectiveFunction;
import de.hterhors.semanticmr.crf.sampling.AbstractBeamSampler;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.hardconstraints.DistinctExperimentalGroupConstraint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.initializer.GoldCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.initializer.MultiCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.initializer.PredictCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.initializer.SampleCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.investigation.CollectExpGroupNames;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.investigation.CollectExpGroupNames.PatternIndexPair;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ContextCardinalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrAllUsedTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrBOWTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrInnerNameBOWTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrNameBOWTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrNameOverlapTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrOuterNameBOWTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExpGroupTreatmentLocalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.IntraTokenCardinalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.SlotIsFilledTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.TreatmentCardinalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.TreatmentPriorTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.Word2VecClusterTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.InjuryRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.InjuryRestrictionProvider.EInjuryModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.orgmodel.OrganismModelRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.treatment.TreatmentRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.treatment.TreatmentRestrictionProvider.ETreatmentModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea.VertebralAreaPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.AgeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.NPChunker;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.NPChunker.TermIndexPair;

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

	private ESamplingMode samplingMode;

	private String modelName;

	public ExperimentalGroupSlotFilling() throws IOException {
		super(SystemScope.Builder.getScopeHandler().addScopeSpecification(ExperimentalGroupSpecifications.systemsScope)
				.apply().registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization()).build());

		Annotations.removeDuplicates = true;

		CartesianEvaluator.MAXIMUM_PERMUTATION_SIZE = 4;

		SlotFillingExplorer.MAX_NUMBER_OF_ANNOTATIONS = 5;

		String rand = String.valueOf(new Random().nextInt(100000));
//		modelName = "ExperimentalGroup" + 50700;
		modelName = "ExperimentalGroup" + rand;
		log.info("Model name = " + modelName);

		mainClassProviderMode = EMainClassMode.PREDICT;
		groupNameProviderMode = EExtractGroupNamesMode.PATTERN_NP_CHUNKS;
		groupNameProcessingMode = EGroupNamesPreProcessingMode.KMEANS_CLUSTERING;
		cardinalityMode = ECardinalityMode.MULTI_CARDINALITIES;
		assignmentMode = EAssignmentMode.ALL;
		complexityMode = EComplexityMode.ROOT;
		samplingMode = ESamplingMode.TYPE_BASED;

		distinctGroupNamesMode = EDistinctGroupNamesMode.NOT_DISTINCT;

		trainingObjectiveFunction = new SlotFillingObjectiveFunction(
				new CartesianEvaluator(samplingMode == ESamplingMode.TYPE_BASED ? EEvaluationDetail.ENTITY_TYPE
						: EEvaluationDetail.DOCUMENT_LINKED));

		predictionObjectiveFunction = new SlotFillingObjectiveFunction(
				new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE));
		/**
		 * Exclude some slots that are not needed for now
		 */
		SCIOSlotTypes.hasNNumber.exclude();
		SCIOSlotTypes.hasTotalPopulationSize.exclude();
		SCIOSlotTypes.hasGroupNumber.exclude();
		SCIOSlotTypes.hasEventBefore.exclude();
		SCIOSlotTypes.hasEventAfter.exclude();

		/**
		 * Apply different modes that were previously set.
		 */
		applyModes();

		List<IExplorationStrategy> explorerList = buildExplorer();

		getData();

		addGroupNameCandidates();

		addCandidatesByTrainingDictionary();

		addCandidatesForTreatment();

		addCandidatesForOrganismModel();

		addCandidatesForInjury();

		IStateInitializer initializer = buildStateInitializer();

		List<AbstractFeatureTemplate<?>> featureTemplates = getFeatureTemplates();

		Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> parameter = getParameter();

		AbstractBeamSampler sampler = new EpochSwitchBeamSampler(epoch -> epoch % 2 == 0);

//		AbstractSampler sampler = newSampler();

		MaxChainLengthCrit maxStepCrit = new MaxChainLengthCrit(100);

		ConverganceCrit noModelChangeCrit = new ConverganceCrit(3 * explorerList.size(), s -> s.getModelScore());

		ISamplingStoppingCriterion[] sampleStoppingCrits = new ISamplingStoppingCriterion[] { maxStepCrit,
				noModelChangeCrit };

		evaluation(explorerList, sampler, initializer, sampleStoppingCrits, maxStepCrit, featureTemplates, parameter);

	}

	public void evaluateDetailed(Map<Instance, State> mergedResults) {

		TreatmentEvaluation treatmentEvaluation = new TreatmentEvaluation(
				new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE));

		treatmentEvaluation.evaluate(mergedResults);

		OrganismModelEvaluation organismModelEvaluation = new OrganismModelEvaluation(
				new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE));

		organismModelEvaluation.evaluate(mergedResults);

		InjuryEvaluation injuryModelEvaluation = new InjuryEvaluation(
				new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE));

		injuryModelEvaluation.evaluate(mergedResults);

		ExperimentalGroupEvaluation expGroupEval = new ExperimentalGroupEvaluation(predictionObjectiveFunction,
				new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE));

		expGroupEval.evaluate(mergedResults);
	}

	/**
	 * Selects the state with the higher model score.
	 * 
	 * @param result
	 * @param state
	 * @return
	 */
	private State selectBestState(State state1, State state2) {

		if (state2 == null)
			return state1;

		if (state1 == null)
			return state2;

		final String c1;
		final String c2;

		log.info(state1.getInstance().getName());
		log.info("Gold number: " + state1.getGoldAnnotations().getAnnotations().size());
		log.info("Prev number: " + state1.getCurrentPredictions().getAnnotations().size());
		log.info("New number: " + state2.getCurrentPredictions().getAnnotations().size());
		log.info("Compare MODEL: " + state1.getModelScore() + " with " + state2.getModelScore());
		log.info("Select MODEL: " + (c1 = (state1.getModelScore() > state2.getModelScore() ? "prev" : "new")));
		log.info("Compare OBJECTIVE: " + state1.getObjectiveScore() + " with " + state2.getObjectiveScore());
		log.info("Select OBJECTIVE: "
				+ (c2 = (state1.getObjectiveScore() > state2.getObjectiveScore() ? "prev" : "new")));
		log.info("Correct : " + c1.equals(c2));
		log.info("\n");
		if (state1.getModelScore() > state2.getModelScore())
			return state1;

		return state2;

	}

	private void evaluation(List<IExplorationStrategy> explorerList, AbstractBeamSampler sampler,
			IStateInitializer initializer, ISamplingStoppingCriterion[] sampleStoppingCrits,
			ISamplingStoppingCriterion maxStepCrit, List<AbstractFeatureTemplate<?>> featureTemplates,
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
		SemanticParsingCRFMultiState crf = new SemanticParsingCRFMultiState(model, explorerList, sampler,
				trainingObjectiveFunction);
//		SemanticParsingCRF crf = new SemanticParsingCRF(model, explorerList, sampler, trainingObjectiveFunction);

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

	private void addCandidatesForOrganismModel() {
		if (assignmentMode == EAssignmentMode.ORGANISM_MODEL || assignmentMode == EAssignmentMode.INJURY_ORGANISM_MODEL
				|| assignmentMode == EAssignmentMode.TREATMENT_ORGANISM_MODEL
				|| assignmentMode == EAssignmentMode.ALL) {
			if (mainClassProviderMode != EMainClassMode.GOLD) { // predict_gold and predict
				addOrganismModelCandidatesFromNERLA();
			}

			if (mainClassProviderMode != EMainClassMode.PREDICT) {
				for (Instance instance : trainingInstances) {
					instance.addCandidateAnnotations(extractGoldOrganismModels(instance));
				}
			}

			if (mainClassProviderMode == EMainClassMode.GOLD) {
				for (Instance instance : devInstances) {
					instance.addCandidateAnnotations(extractGoldOrganismModels(instance));
				}
				for (Instance instance : testInstances) {
					instance.addCandidateAnnotations(extractGoldOrganismModels(instance));
				}
			}
		}
	}

	private void addCandidatesForTreatment() {

		if (assignmentMode == EAssignmentMode.TREATMENT || assignmentMode == EAssignmentMode.TREATMENT_ORGANISM_MODEL
				|| assignmentMode == EAssignmentMode.INJURY_TREATMENT || assignmentMode == EAssignmentMode.ALL) {

			if (mainClassProviderMode != EMainClassMode.GOLD) { // predict_gold and predict
				addTreatmentCandidatesFromNERLA();
			}

			if (mainClassProviderMode != EMainClassMode.PREDICT) {
				for (Instance instance : trainingInstances) {
					instance.addCandidateAnnotations(extractGoldTreatments(instance));
				}
			}

			if (mainClassProviderMode == EMainClassMode.GOLD) {
				for (Instance instance : devInstances) {
					instance.addCandidateAnnotations(extractGoldTreatments(instance));
				}
				for (Instance instance : testInstances) {
					instance.addCandidateAnnotations(extractGoldTreatments(instance));
				}
			}
		}

		if (treatmentModificationsRule != ETreatmentModifications.ROOT) {

			String deliveryMethodModelName = "DeliveryMethod" + modelName;

			DeliveryMethodPredictor deliveryMethodPrediction = new DeliveryMethodPredictor(deliveryMethodModelName,
					scope, trainingInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					devInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					testInstances.stream().map(i -> i.getName()).collect(Collectors.toList()));

			deliveryMethodPrediction.trainOrLoadModel();
			deliveryMethodPrediction.predictAllInstances(2);

			for (Instance instance : instanceProvider.getInstances()) {
				instance.addCandidateAnnotations(
						deliveryMethodPrediction.predictHighRecallInstanceByName(instance.getName(), 2));
			} /**
				 * TODO: add training delivery method
				 */
		}
	}

	private void addCandidatesForInjury() {
		if (assignmentMode == EAssignmentMode.INJURY || assignmentMode == EAssignmentMode.INJURY_ORGANISM_MODEL
				|| assignmentMode == EAssignmentMode.INJURY_TREATMENT || assignmentMode == EAssignmentMode.ALL) {
			if (mainClassProviderMode != EMainClassMode.GOLD) {
				addInjuryCandidatesFromNERLA();
			}

			if (mainClassProviderMode != EMainClassMode.PREDICT) {
				for (Instance instance : trainingInstances) {
					instance.addCandidateAnnotations(extractGoldInjuryModels(instance));
				}
			}

			if (mainClassProviderMode == EMainClassMode.GOLD) {
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

			String vertebralAreaModelName = "VertebralArea_" + modelName;
			VertebralAreaPredictor vertebralAreaPrediction = new VertebralAreaPredictor(vertebralAreaModelName, scope,
					trainingInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					devInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					testInstances.stream().map(i -> i.getName()).collect(Collectors.toList()));

			vertebralAreaPrediction.trainOrLoadModel();
			vertebralAreaPrediction.predictAllInstances(2);

			for (Instance instance : instanceProvider.getInstances()) {
				instance.addCandidateAnnotations(
						vertebralAreaPrediction.predictHighRecallInstanceByName(instance.getName(), 2));
			} /**
				 * TODO: add training location
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

		SlotFillingExplorer slotFillingExplorer = new SlotFillingExplorer(samplingMode, predictionObjectiveFunction,
				hardConstraintsProvider);
		explorerList.add(slotFillingExplorer);

		if (cardinalityMode == ECardinalityMode.SAMPLE_CARDINALITY) {
			RootTemplateCardinalityExplorer.MAX_NUMBER_OF_ANNOTATIONS = CartesianEvaluator.MAXIMUM_PERMUTATION_SIZE;
			RootTemplateCardinalityExplorer rootTemplateCardinalityExplorer = new RootTemplateCardinalityExplorer(
					samplingMode, AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));
			explorerList.add(rootTemplateCardinalityExplorer);

		}
		return explorerList;
	}

	public void applyModes() {
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
		} else if (complexityMode == EComplexityMode.ROOT) {
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

		for (Instance instance : instanceProvider.getInstances()) {
			List<DocumentLinkedAnnotation> candidates = new ArrayList<>();

			switch (groupNameProviderMode) {
			case EMPTY:
				break;
			case GOLD:
				candidates.addAll(extractGroupNamesFromGold(instance));
				break;
			case NP_CHUNKS:
				candidates.addAll(extractGroupNamesWithNPCHunks(instance));
				break;
			case PATTERN:
				candidates.addAll(extractGroupNamesWithPattern(instance));
				break;
			case PATTERN_NP_CHUNKS:
				candidates.addAll(extractGroupNamesWithNPCHunks(instance));
				candidates.addAll(extractGroupNamesWithPattern(instance));
				break;
			case PATTERN_NP_CHUNKS_GOLD:
				candidates.addAll(extractGroupNamesFromGold(instance));
				candidates.addAll(extractGroupNamesWithNPCHunks(instance));
				candidates.addAll(extractGroupNamesWithPattern(instance));
				break;
			}

			for (DocumentLinkedAnnotation ec : candidates) {
				instance.addCandidateAnnotation(AnnotationBuilder.toAnnotation(instance.getDocument(),
						SCIOEntityTypes.definedExperimentalGroup, ec.getSurfaceForm(), ec.getStartDocCharOffset()));
			}

			instance.addCandidateAnnotations(candidates);

		}
	}

	private List<AbstractFeatureTemplate<?>> getFeatureTemplates() {
		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();

		if (samplingMode == ESamplingMode.ANNOTATION_BASED) {
			featureTemplates.add(new BOWCardinalityTemplate());

			featureTemplates.add(new TreatmentPriorTemplate());

			featureTemplates.add(new TreatmentCardinalityTemplate());

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
			featureTemplates.add(new SlotIsFilledTemplate());

			featureTemplates.add(new ExpGroupTreatmentLocalityTemplate());

			featureTemplates.add(new ContextBetweenSlotFillerTemplate());

			featureTemplates.add(new ContextCardinalityTemplate());

			featureTemplates.add(new IntraTokenCardinalityTemplate());

			featureTemplates.add(new Word2VecClusterTemplate());

		}
		if (samplingMode == ESamplingMode.TYPE_BASED) {
			featureTemplates.add(new BOWCardinalityTemplate());

			featureTemplates.add(new TreatmentPriorTemplate());

			featureTemplates.add(new TreatmentCardinalityTemplate());

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
			featureTemplates.add(new SlotIsFilledTemplate());

			featureTemplates.add(new ExpGroupTreatmentLocalityTemplate());

			featureTemplates.add(new ContextBetweenSlotFillerTemplate());

			featureTemplates.add(new ContextCardinalityTemplate());

			featureTemplates.add(new IntraTokenCardinalityTemplate());

			featureTemplates.add(new Word2VecClusterTemplate());

			/**
			 * NEW
			 */
//			featureTemplates.add(new RemainingTypesTemplate());
		}

		return featureTemplates;
	}

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

	private AbstractSampler newSampler() {
//		AbstractSampler sampler = SamplerCollection.topKModelDistributionSamplingStrategy(2);
//		AbstractSampler sampler = SamplerCollection.greedyModelStrategy();
//		AbstractSampler sampler = SamplerCollection.greedyObjectiveStrategy();
		AbstractSampler sampler = new EpochSwitchSampler(epoch -> epoch % 2 == 0);
//		AbstractSampler sampler = new EpochSwitchSampler(new RandomSwitchSamplingStrategy());
//		AbstractSampler sampler = new EpochSwitchSampler(e -> new Random(e).nextBoolean());
		return sampler;
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
			if (SCIOSlotTypes.hasGroupName.isExcluded())
				return a;

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

		if (samplingMode == ESamplingMode.TYPE_BASED)
			goldModificationRules.add(a -> a.reduceToEntityType());

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
		case MULTI_CARDINALITIES:

			int maxCardinality = computeMaxCardinality();

			return new MultiCardinalityInitializer(groupNameProviderMode, groupNameProcessingMode,
					instanceProvider.getInstances(), maxCardinality);
		default:
			return null;
		}
	}

	private int computeMaxCardinality() {
		double stdDev = 0;

		double e = 0;
		for (Instance instance : trainingInstances) {
			e += instance.getGoldAnnotations().getAbstractAnnotations().size();
		}
		e /= trainingInstances.size();
		for (Instance instance : trainingInstances) {
			stdDev += Math.pow(e - instance.getGoldAnnotations().getAbstractAnnotations().size(), 2)
					/ trainingInstances.size();
		}
		stdDev = Math.sqrt(stdDev);
		int maxCardinality = (int) (e + stdDev);
		return maxCardinality;
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

	private List<DocumentLinkedAnnotation> extractGroupNamesFromGold(Instance instance) {
		return instance.getGoldAnnotations().getAbstractAnnotations().stream()
				.map(e -> e.asInstanceOfEntityTemplate().getMultiFillerSlot(SCIOSlotTypes.hasGroupName))
				.filter(s -> s.containsSlotFiller()).flatMap(s -> s.getSlotFiller().stream())
				.map(e -> e.asInstanceOfDocumentLinkedAnnotation()).collect(Collectors.toList());
	}

	private List<DocumentLinkedAnnotation> extractGroupNamesWithNPCHunks(Instance instance) {
		Set<String> distinct = new HashSet<>();

		List<DocumentLinkedAnnotation> anns = new ArrayList<>();
		try {
			for (TermIndexPair groupName : new NPChunker(instance.getDocument()).getNPs()) {
				if (groupName.term.matches(".+(group|animals|rats|mice|rats|cats|dogs)")) {
					DocumentLinkedAnnotation annotation;
					if (distinctGroupNamesMode == EDistinctGroupNamesMode.DISTINCT) {

						if (distinct.contains(groupName.term))
							continue;

						distinct.add(groupName.term);
					}
					try {
						annotation = AnnotationBuilder.toAnnotation(instance.getDocument(), SCIOEntityTypes.groupName,
								groupName.term, groupName.index);
					} catch (Exception e) {
						annotation = null;
					}
					if (annotation != null)
						anns.add(annotation);

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return anns;
	}

	private List<DocumentLinkedAnnotation> extractGroupNamesWithPattern(Instance instance) {
		List<DocumentLinkedAnnotation> anns = new ArrayList<>();
		Set<String> distinct = new HashSet<>();
		for (PatternIndexPair p : CollectExpGroupNames.pattern) {
			Matcher m = p.pattern.matcher(instance.getDocument().documentContent);
			while (m.find()) {
				for (Integer group : p.groups) {
					DocumentLinkedAnnotation annotation;
					try {
						String term = m.group(group);
						if (term.length() > NPChunker.maxLength)
							continue;

						if (CollectExpGroupNames.STOP_TERM_LIST.contains(term))
							continue;

						if (distinctGroupNamesMode == EDistinctGroupNamesMode.DISTINCT) {

							if (distinct.contains(term))
								continue;

							distinct.add(term);
						}
						annotation = AnnotationBuilder.toAnnotation(instance.getDocument(), SCIOEntityTypes.groupName,
								term, m.start(group));
					} catch (Exception e) {
						annotation = null;
					}
					if (annotation != null)
						anns.add(annotation);
				}
			}
		}
		return anns;
	}

	@Deprecated
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

		/**
		 * Predict OrganismModels
		 */

//		OrgModelSlotFillingPredictor organismModelPredictor = new OrgModelSlotFillingPredictor("OrganismModel" + rand,
//				scope, trainingInstanceNames, developInstanceNames, testInstanceNames);
//
//		organismModelPredictor.trainOrLoadModel();
//
//		Map<String, Set<AbstractAnnotation>> organismModelAnnotations = organismModelPredictor.predictAllInstances();
//
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

	private void addCandidatesByTrainingDictionary() {

		if (mainClassProviderMode == EMainClassMode.GOLD)
			return;

		Map<EntityType, Set<String>> trainDictionary = DictionaryFromInstanceHelper.toDictionary(trainingInstances);

		for (Instance instance : instanceProvider.getInstances()) {

			Set<String> distinctGroupNames = new HashSet<>();

			for (AbstractAnnotation nerla : DictionaryFromInstanceHelper.getAnnotationsForInstance(instance,
					trainDictionary)) {

				if (distinctGroupNamesMode == EDistinctGroupNamesMode.DISTINCT) {

					if (nerla.getEntityType() == SCIOEntityTypes.groupName) {
						if (distinctGroupNames.contains(nerla.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm()))
							continue;
						distinctGroupNames.add(nerla.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm());
					}

				}
				if (nerla.getEntityType() == SCIOEntityTypes.compound
						|| nerla.getEntityType().isSubEntityOf(SCIOEntityTypes.compound)) {
					EntityTemplate compoundTreatment = new EntityTemplate(
							AnnotationBuilder.toAnnotation(SCIOEntityTypes.compoundTreatment))
									.setSingleSlotFiller(SCIOSlotTypes.hasCompound, nerla.deepCopy());
					instance.addCandidateAnnotation(compoundTreatment);
				}
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

}
