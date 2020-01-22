package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import de.hterhors.semanticmr.crf.SemanticParsingCRF;
import de.hterhors.semanticmr.crf.exploration.IExplorationStrategy;
import de.hterhors.semanticmr.crf.exploration.RootTemplateCardinalityExplorer;
import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer;
import de.hterhors.semanticmr.crf.exploration.constraints.HardConstraintsProvider;
import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.learner.optimizer.SGD;
import de.hterhors.semanticmr.crf.learner.regularizer.L2;
import de.hterhors.semanticmr.crf.model.Model;
import de.hterhors.semanticmr.crf.of.IObjectiveFunction;
import de.hterhors.semanticmr.crf.of.SlotFillingObjectiveFunction;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.impl.EpochSwitchSampler;
import de.hterhors.semanticmr.crf.sampling.stopcrit.ISamplingStoppingCriterion;
import de.hterhors.semanticmr.crf.sampling.stopcrit.impl.ConverganceCrit;
import de.hterhors.semanticmr.crf.sampling.stopcrit.impl.MaxChainLengthCrit;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.eval.NerlaEvaluator;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.deliverymethod.DeliveryMethodPredictor;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EExtractGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EMainClassMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.ESimpleEvaluationMode;
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

	private final IObjectiveFunction trainingObjectiveFunction = new SlotFillingObjectiveFunction(
			new CartesianEvaluator(EEvaluationDetail.DOCUMENT_LINKED));

	private final IObjectiveFunction predictionObjectiveFunction = new SlotFillingObjectiveFunction(
			new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE));
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

	public EMainClassMode mainClassMode;
	public EExtractGroupNamesMode groupNameMode;
	public ECardinalityMode cardinalityMode;
	public EAssignmentMode assignmentMode;
	public EComplexityMode complexityMode;

	private ETreatmentModifications treatmentModificationsRule;
	private EOrgModelModifications orgModelModificationsRule;
	private EInjuryModifications injuryModificationRule;

	private final String modelName;

	public ExperimentalGroupSlotFilling() throws IOException {
		super(SystemScope.Builder.getScopeHandler().addScopeSpecification(ExperimentalGroupSpecifications.systemsScope)
				.apply().registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization()).build());

		String rand = String.valueOf(new Random().nextInt(100000));
		modelName = "ExperimentalGroup" + rand;
		log.info("Model name = " + modelName);

		mainClassMode = EMainClassMode.PREDICT;
		groupNameMode = EExtractGroupNamesMode.GOLD_CLUSTERED;
		cardinalityMode = ECardinalityMode.GOLD_CARDINALITY;
		assignmentMode = EAssignmentMode.TREATMENT;
		complexityMode = EComplexityMode.ROOT;

		/**
		 * Exclude some slots that are not needed for now
		 */
		SCIOSlotTypes.hasNNumber.exclude();
		SCIOSlotTypes.hasTotalPopulationSize.exclude();
		SCIOSlotTypes.hasGroupNumber.exclude();

		/**
		 * Apply different modes that were previously set.
		 */
		applyModes();

		List<IExplorationStrategy> explorerList = buildExplorer();

		IStateInitializer initializer = getStateInitializer();

		getData();

		addGroupNameCandidates();

		addMainClassCandidatesByDictionary();

		addCandidatesForTreatment();

		addCandidatesForOrganismModel();

		addCandidatesForInjury();

		List<AbstractFeatureTemplate<?>> featureTemplates = getFeatureTemplates();

		Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> parameter = getParameter();

		AbstractSampler sampler = newSampler();

		MaxChainLengthCrit maxStepCrit = new MaxChainLengthCrit(100);

		ConverganceCrit noModelChangeCrit = new ConverganceCrit(3 * explorerList.size(), s -> s.getModelScore());

		ISamplingStoppingCriterion[] sampleStoppingCrits = new ISamplingStoppingCriterion[] { maxStepCrit,
				noModelChangeCrit };
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
		SemanticParsingCRF crf = new SemanticParsingCRF(model, explorerList, sampler, initializer,
				trainingObjectiveFunction);

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

		/**
		 * Evaluate with objective function
		 */
		evaluate(log, results, predictionObjectiveFunction);

		/**
		 * Evaluate assuming TT,OM,IM are one unit. Evaluate assuming TT,OM,IM are one
		 * unit, distinguish vehicle-treatments and main-treatments.
		 */
		evaluateExpGroupSimple(results);
		evaluateTreatments(results, new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE));

		log.info(crf.getTrainingStatistics());
		log.info(crf.getTestStatistics());
		log.info("modelName: " + modelName);

	}

	private void addCandidatesForOrganismModel() {
		if (assignmentMode == EAssignmentMode.ORGANISM_MODEL || assignmentMode == EAssignmentMode.INJURY_ORGANISM_MODEL
				|| assignmentMode == EAssignmentMode.TREATMENT_ORGANISM_MODEL
				|| assignmentMode == EAssignmentMode.ALL) {
			if (mainClassMode != EMainClassMode.GOLD) { // predict_gold and predict
				addOrganismModelCandidatesFromNERLA();
			}

			if (mainClassMode != EMainClassMode.PREDICT) {
				for (Instance instance : trainingInstances) {
					instance.addCandidateAnnotations(extractGoldOrganismModels(instance));
				}
			}

			if (mainClassMode == EMainClassMode.GOLD) {
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

			if (mainClassMode != EMainClassMode.GOLD) { // predict_gold and predict
				addTreatmentCandidatesFromNERLA();
			}

			if (mainClassMode != EMainClassMode.PREDICT) {
				for (Instance instance : trainingInstances) {
					instance.addCandidateAnnotations(extractGoldTreatments(instance));
				}
			}

			if (mainClassMode == EMainClassMode.GOLD) {
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
			}
			/**
			 * TODO: add training delivery method
			 */
		}
	}

	private void addCandidatesForInjury() {
		if (assignmentMode == EAssignmentMode.INJURY || assignmentMode == EAssignmentMode.INJURY_ORGANISM_MODEL
				|| assignmentMode == EAssignmentMode.INJURY_TREATMENT || assignmentMode == EAssignmentMode.ALL) {
			if (mainClassMode != EMainClassMode.GOLD) {
				addInjuryCandidatesFromNERLA();
			}

			if (mainClassMode != EMainClassMode.PREDICT) {
				for (Instance instance : trainingInstances) {
					instance.addCandidateAnnotations(extractGoldInjuryModels(instance));
				}
			}

			if (mainClassMode == EMainClassMode.GOLD) {
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
			}
			/**
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

//		hardConstraintsProvider.addHardConstraints(
//				new DistinctExperimentalGroupConstraint(predictionObjectiveFunction.getEvaluator()));

		SlotFillingExplorer slotFillingExplorer = new SlotFillingExplorer(predictionObjectiveFunction,
				hardConstraintsProvider);
		explorerList.add(slotFillingExplorer);

		if (cardinalityMode == ECardinalityMode.SAMPLE_CARDINALITY) {
			RootTemplateCardinalityExplorer.MAX_NUMBER_OF_ANNOTATIONS = CartesianEvaluator.MAXIMUM_PERMUTATION_SIZE;
			RootTemplateCardinalityExplorer rootTemplateCardinalityExplorer = new RootTemplateCardinalityExplorer(
					AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup));
			explorerList.add(rootTemplateCardinalityExplorer);

		}
		return explorerList;
	}

	public void applyModes() {
		if (groupNameMode == EExtractGroupNamesMode.EMPTY) {
			/*
			 * If groupNameMode is EMPTY exclude this slot in general
			 */
			SCIOSlotTypes.hasGroupName.exclude();
		} else if (groupNameMode == EExtractGroupNamesMode.GOLD_CLUSTERED) {
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
			switch (groupNameMode) {
			case EMPTY:
				break;
			case GOLD_CLUSTERED:// if clustered we do not need group names annotations for sampling.
				candidates.addAll(extractGroupNamesFromGold(instance));
				break;
			case GOLD_UNCLUSTERED:
				break;
			case NP_CHUNKS:
				candidates.addAll(extractGroupNamesWithPattern(instance));
				break;
			case PATTERN:
				candidates.addAll(extractGroupNamesWithNPCHunks(instance));
				break;
			case PATTERN_NP_CHUNKS:
				candidates.addAll(extractGroupNamesWithNPCHunks(instance));
				candidates.addAll(extractGroupNamesWithPattern(instance));
				break;
			}
			instance.addCandidateAnnotations(candidates);

			for (DocumentLinkedAnnotation ec : candidates) {
				instance.addCandidateAnnotation(AnnotationBuilder.toAnnotation(instance.getDocument(),
						SCIOEntityTypes.definedExperimentalGroup, ec.getSurfaceForm(), ec.getStartDocCharOffset()));
			}
		}
	}

	private List<AbstractFeatureTemplate<?>> getFeatureTemplates() {
		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();

		featureTemplates.add(new BOWCardinalityTemplate());

		featureTemplates.add(new TreatmentPriorTemplate());

		featureTemplates.add(new TreatmentCardinalityTemplate());

		featureTemplates.add(new ExGrBOWTemplate());
		//

		/**
		 * TODO: activate if cardinality can be predicted without gold data.
		 */
		if (cardinalityMode != ECardinalityMode.SAMPLE_CARDINALITY)
			featureTemplates.add(new ExGrAllUsedTemplate());
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
		return featureTemplates;
	}

	private Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> getParameter() {

		Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> parameter = new HashMap<>();
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
			if (a.asInstanceOfEntityTemplate().getRootAnnotation().entityType == EntityType
					.get("DefinedExperimentalGroup"))
				return a;
			return null;
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

				for (AbstractAnnotation slotFiller : newTreatments) {
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

		/*
		 * remove from annotation if it has no injury, treatment, groupName, and
		 * organism.
		 */
		goldModificationRules.add(a -> {
			if (a.asInstanceOfEntityTemplate().getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()
					|| a.asInstanceOfEntityTemplate().getMultiFillerSlot(SCIOSlotTypes.hasGroupName)
							.containsSlotFiller()
					|| a.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel)
							.containsSlotFiller()
					|| a.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel)
							.containsSlotFiller()
					|| a.asInstanceOfEntityTemplate().getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType)
							.containsSlotFiller())
				return a;
			return null;
		});

		return goldModificationRules;
	}

	/**
	 * The state initializer.
	 * 
	 */
	private IStateInitializer getStateInitializer() {

		switch (cardinalityMode) {
		case GOLD_CARDINALITY:
			return new GoldCardinalityInitializer(groupNameMode);
		case PREDICTED_CARDINALITY:
			return new PredictCardinalityInitializer(instanceProvider.getInstances());
		case SAMPLE_CARDINALITY:
			return new SampleCardinalityInitializer(0);
		case MULTI_CARDINALITIES:
			/**
			 * TODO: compute. avg + 1 std deviation
			 */
			int max = 5;
			return new MultiCardinalityInitializer(max);
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

	private List<DocumentLinkedAnnotation> extractGroupNamesFromGold(Instance instance) {
		return instance.getGoldAnnotations().getAbstractAnnotations().stream()
				.map(e -> e.asInstanceOfEntityTemplate().getMultiFillerSlot(SCIOSlotTypes.hasGroupName))
				.filter(s -> s.containsSlotFiller()).flatMap(s -> s.getSlotFiller().stream())
				.map(e -> e.asInstanceOfDocumentLinkedAnnotation()).collect(Collectors.toList());
	}

	private List<DocumentLinkedAnnotation> extractGroupNamesWithNPCHunks(Instance instance) {
		List<DocumentLinkedAnnotation> anns = new ArrayList<>();
		try {
			for (TermIndexPair groupName : new NPChunker(instance.getDocument()).getNPs()) {
				if (groupName.term.matches(".+(group|animals|rats|mice|rats|cats|dogs)")) {
					DocumentLinkedAnnotation annotation;
					try {
						annotation = AnnotationBuilder.toAnnotation(instance.getDocument(), "GroupName", groupName.term,
								groupName.index);
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
		for (PatternIndexPair p : CollectExpGroupNames.pattern) {
			Matcher m = p.pattern.matcher(instance.getDocument().documentContent);
			while (m.find()) {
				for (Integer group : p.groups) {
					DocumentLinkedAnnotation annotation;
					try {
						annotation = AnnotationBuilder.toAnnotation(instance.getDocument(), "GroupName", m.group(group),
								m.start(group));
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

	private void addMainClassCandidatesByDictionary() {

		if (mainClassMode == EMainClassMode.GOLD)
			return;

		Map<EntityType, Set<String>> trainDictionary = DictionaryFromInstanceHelper.toDictionary(trainingInstances);

		for (Instance instance : instanceProvider.getInstances()) {
			for (AbstractAnnotation nerla : DictionaryFromInstanceHelper.getAnnotationsForInstance(instance,
					trainDictionary)) {

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

	private void evaluateExpGroupSimple(Map<Instance, State> results) {

		Score vehicleScore = new Score();
		Score nonVehicleScore = new Score();
		Score bothS = new Score();
		double macroF1 = 0;
		double macroPrecision = 0;
		double macroRecall = 0;
		NerlaEvaluator eval = new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE);
		int i = 0;
		for (Entry<Instance, State> e : results.entrySet()) {
			/*
			 * 
			 * Evaluate clustering of Treatments
			 */

			i++;
			log.info(e.getKey().getName());

			List<EntityTemplate> goldAnnotations = e.getValue().getGoldAnnotations().getAnnotations();
			List<EntityTemplate> predictedAnnotations = e.getValue().getCurrentPredictions().getAnnotations();

			List<Integer> bestAssignment = ((CartesianEvaluator) predictionObjectiveFunction.getEvaluator())
					.getBestAssignment(goldAnnotations, predictedAnnotations);

			Score score;
			log.info("Both: " + (score = simpleExpGroupEvaluate(false, eval, bestAssignment, goldAnnotations,
					predictedAnnotations, ESimpleEvaluationMode.BOTH)));
			Score vs = simpleExpGroupEvaluate(false, eval, bestAssignment, goldAnnotations, predictedAnnotations,
					ESimpleEvaluationMode.VEHICLE);
			log.info("Vehicles: " + vs);
			Score nvs = simpleExpGroupEvaluate(false, eval, bestAssignment, goldAnnotations, predictedAnnotations,
					ESimpleEvaluationMode.NON_VEHICLE);
			log.info("Non Vehicles: " + nvs);

			vehicleScore.add(vs);
			nonVehicleScore.add(nvs);
			bothS.add(score);
			macroF1 += score.getF1();
			macroPrecision += score.getPrecision();
			macroRecall += score.getRecall();
			log.info("EXP GROUP INTERMEDIATE MACRO: F1 = " + macroF1 / i + ", P = " + macroPrecision / i + ", R = "
					+ macroRecall / i);
			log.info("EXP GROUP INTERMEDIATE MICRO: " + bothS);
			log.info("");
		}
		macroF1 /= results.entrySet().size();
		macroPrecision /= results.entrySet().size();
		macroRecall /= results.entrySet().size();
		log.info("EXP GROUP MACRO: F1 = " + macroF1 + ", P = " + macroPrecision + ", R = " + macroRecall);
		log.info("EXP GROUP MICRO: BOTH = " + bothS);
		log.info("EXP GROUP MICRO: Vehicle = " + vehicleScore);
		log.info("EXP GROUP MICRO: Non Vehicle = " + nonVehicleScore);
	}

	private void evaluateTreatments(Map<Instance, State> results, AbstractEvaluator eval) {

		Score vehicleScore = new Score();
		Score nonVehicleScore = new Score();
		Score bothS = new Score();
		double macroF1 = 0;
		double macroPrecision = 0;
		double macroRecall = 0;
		int i = 0;
		for (Entry<Instance, State> e : results.entrySet()) {
			/*
			 * Evaluate treatments
			 */

			List<EntityTemplate> goldAnnotations = new ArrayList<>(
					e.getValue().getGoldAnnotations().getAnnotations().stream()
							.flatMap(ex -> ex.asInstanceOfEntityTemplate()
									.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller().stream())
							.map(t -> t.asInstanceOfEntityTemplate()).collect(Collectors.toSet()));
			List<EntityTemplate> predictedAnnotations = new ArrayList<>(
					e.getValue().getCurrentPredictions().getAnnotations().stream()
							.flatMap(ex -> ex.asInstanceOfEntityTemplate()
									.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller().stream())
							.map(t -> t.asInstanceOfEntityTemplate()).collect(Collectors.toSet()));

			i++;
			log.info(e.getKey().getName());

			List<Integer> bestAssignment = ((CartesianEvaluator) predictionObjectiveFunction.getEvaluator())
					.getBestAssignment(goldAnnotations, predictedAnnotations);

			if (e.getKey().getName().startsWith("N147"))
				System.out.println("here");

			Score score;
			log.info("Both: " + (score = simpleTreatmentEvaluate(false, eval, bestAssignment, goldAnnotations,
					predictedAnnotations, ESimpleEvaluationMode.BOTH)));
			Score vs = simpleTreatmentEvaluate(false, eval, bestAssignment, goldAnnotations, predictedAnnotations,
					ESimpleEvaluationMode.VEHICLE);
			log.info("Vehicles: " + vs);
			Score nvs = simpleTreatmentEvaluate(false, eval, bestAssignment, goldAnnotations, predictedAnnotations,
					ESimpleEvaluationMode.NON_VEHICLE);
			log.info("Non Vehicles: " + nvs);

			vehicleScore.add(vs);
			nonVehicleScore.add(nvs);
			bothS.add(score);
			macroF1 += score.getF1();
			macroPrecision += score.getPrecision();
			macroRecall += score.getRecall();
			log.info("TREATMENTS INTERMEDIATE MACRO: F1 = " + macroF1 / i + ", P = " + macroPrecision / i + ", R = "
					+ macroRecall / i);
			log.info("TREATMENTS INTERMEDIATE MICRO: " + bothS);
			log.info("");
		}
		macroF1 /= results.entrySet().size();
		macroPrecision /= results.entrySet().size();
		macroRecall /= results.entrySet().size();
		log.info("TREATMENTS MACRO: F1 = " + macroF1 + ", P = " + macroPrecision + ", R = " + macroRecall);
		log.info("TREATMENTS MICRO: BOTH = " + bothS);
		log.info("TREATMENTS MICRO: Vehicle = " + vehicleScore);
		log.info("TREATMENTS MICRO: Non Vehicle = " + nonVehicleScore);
	}

	private Score simpleExpGroupEvaluate(boolean print, AbstractEvaluator evaluator, List<Integer> bestAssignment,
			List<EntityTemplate> goldAnnotations, List<EntityTemplate> predictedAnnotations,
			ESimpleEvaluationMode mode) {
		Score simpleScore = new Score();

		for (int goldIndex = 0; goldIndex < bestAssignment.size(); goldIndex++) {
			final int predictIndex = bestAssignment.get(goldIndex);
			/*
			 * Treatments
			 */
			List<AbstractAnnotation> goldTreatments = new ArrayList<>();
			if (goldAnnotations.size() > goldIndex)
				goldTreatments.addAll(goldAnnotations.get(goldIndex).getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType)
						.getSlotFiller());

			List<AbstractAnnotation> predictTreatments = new ArrayList<>();
			if (predictedAnnotations.size() > predictIndex)
				predictTreatments.addAll(predictedAnnotations.get(predictIndex)
						.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller());

			/*
			 * NOTE: Compare objects are used to tell whether a tp should be given for an
			 * empty prediction or not. If gold and predicted is empty AND the compare
			 * objects are also empty then a +1 tp is given
			 *
			 * E.g. g:Vehicle p:Vehicle would result in non vehicle mode to add 1 tp cause g
			 * and p are empty, however if we check the compare list (vehicle list) then
			 * they are not empty thus we do not add +1 We add only +1 for non vehicle,
			 * otherwise +1 would be added twice.
			 * 
			 *
			 */

			List<AbstractAnnotation> goldTreatmentsCompare = new ArrayList<>();
			if (goldAnnotations.size() > goldIndex)
				goldTreatmentsCompare.addAll(goldAnnotations.get(goldIndex)
						.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller());

			List<AbstractAnnotation> predictTreatmentsCompare = new ArrayList<>();
			if (predictedAnnotations.size() > predictIndex)
				predictTreatmentsCompare.addAll(predictedAnnotations.get(predictIndex)
						.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller());

			switch (mode) {
			case BOTH: {
				// Do nothing
				break;
			}
			case VEHICLE: {
				/*
				 * Filter for vehicles
				 */
				goldTreatments = goldTreatments.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList());
				predictTreatments = predictTreatments.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList());
				goldTreatmentsCompare.removeAll(goldTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList()));
				predictTreatmentsCompare.removeAll(predictTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList()));
				break;
			}
			case NON_VEHICLE: {
				goldTreatments.removeAll(goldTreatments.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList()));
				predictTreatments.removeAll(predictTreatments.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList()));
				goldTreatmentsCompare = goldTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList());
				predictTreatmentsCompare = predictTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList());
				break;
			}
			}

			Score s;
			if (goldTreatments.isEmpty() && predictTreatments.isEmpty()) {
				if (mode == ESimpleEvaluationMode.BOTH) {
					s = new Score(1, 0, 0);
				} else {
					if (goldTreatmentsCompare.isEmpty() && predictTreatmentsCompare.isEmpty()
							&& mode == ESimpleEvaluationMode.NON_VEHICLE) {
						s = new Score(1, 0, 0);
					} else {
						s = new Score(0, 0, 0);
					}
				}
			} else
				s = evaluator.scoreMultiValues(goldTreatments, predictTreatments);

			if (print) {
				log.info("Compare: g" + goldIndex);
				goldTreatments.forEach(g -> log.info(g.toPrettyString()));
				log.info("With: p" + predictIndex);
				predictTreatments.forEach(p -> log.info(p.toPrettyString()));
				log.info("Score: " + s);
				log.info("-----");
			}

			simpleScore.add(s);
			/*
			 * OrganismModel
			 */
			if (SCIOSlotTypes.hasOrganismModel.isIncluded()) {
				List<AbstractAnnotation> goldOrganismModel = Arrays.asList(goldAnnotations.get(goldIndex)
						.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel).getSlotFiller()).stream()
						.filter(a -> a != null).collect(Collectors.toList());
				List<AbstractAnnotation> predictOrganismModel;

				if (predictedAnnotations.size() > predictIndex)
					predictOrganismModel = Arrays
							.asList(predictedAnnotations.get(predictIndex)
									.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel).getSlotFiller())
							.stream().filter(a -> a != null).collect(Collectors.toList());
				else
					predictOrganismModel = Collections.emptyList();

				simpleScore.add(evaluator.scoreMultiValues(goldOrganismModel, predictOrganismModel));
			}

			/*
			 * InjuryModel
			 */
			if (SCIOSlotTypes.hasInjuryModel.isIncluded()) {
				List<AbstractAnnotation> goldInjuryModel = Arrays.asList(goldAnnotations.get(goldIndex)
						.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).getSlotFiller()).stream()
						.filter(a -> a != null).collect(Collectors.toList());
				List<AbstractAnnotation> predictInjuryModel;

				if (predictedAnnotations.size() > predictIndex)
					predictInjuryModel = Arrays
							.asList(predictedAnnotations.get(predictIndex)
									.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).getSlotFiller())
							.stream().filter(a -> a != null).collect(Collectors.toList());
				else
					predictInjuryModel = Collections.emptyList();

				simpleScore.add(evaluator.scoreMultiValues(goldInjuryModel, predictInjuryModel));
			}

		}

		return simpleScore;
	}

	private Score simpleTreatmentEvaluate(boolean print, AbstractEvaluator evaluator, List<Integer> bestAssignment,
			List<EntityTemplate> goldAnnotations, List<EntityTemplate> predictedAnnotations,
			ESimpleEvaluationMode mode) {
		Score simpleScore = new Score();

		for (int goldIndex = 0; goldIndex < bestAssignment.size(); goldIndex++) {
			final int predictIndex = bestAssignment.get(goldIndex);

			List<AbstractAnnotation> goldTreatments = new ArrayList<>();
			List<AbstractAnnotation> predictTreatments = new ArrayList<>();

			if (goldAnnotations.size() > goldIndex)
				goldTreatments.add(goldAnnotations.get(goldIndex));

			if (predictedAnnotations.size() > predictIndex)
				predictTreatments.add(predictedAnnotations.get(predictIndex));
			/*
			 * NOTE: Compare objects are used to tell whether a tp should be given for an
			 * empty prediction or not. If gold and predicted is empty AND the compare
			 * objects are also empty then a +1 tp is given
			 *
			 * E.g. g:Vehicle p:Vehicle would result in non vehicle mode to add 1 tp cause g
			 * and p are empty, however if we check the compare list (vehicle list) then
			 * they are not empty thus we do not add +1 We add only +1 for non vehicle,
			 * otherwise +1 would be added twice.
			 * 
			 *
			 */
			List<AbstractAnnotation> goldTreatmentsCompare = new ArrayList<>();
			List<AbstractAnnotation> predictTreatmentsCompare = new ArrayList<>();

			if (goldAnnotations.size() > goldIndex)
				goldTreatmentsCompare.add(goldAnnotations.get(goldIndex));

			if (predictedAnnotations.size() > predictIndex)
				predictTreatmentsCompare.add(predictedAnnotations.get(predictIndex));

			switch (mode) {
			case BOTH: {
				// Do nothing
				break;
			}
			case VEHICLE: {
				/*
				 * Filter for vehicles
				 */
				goldTreatments = goldTreatments.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList());
				predictTreatments = predictTreatments.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList());
				goldTreatmentsCompare.removeAll(goldTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList()));
				predictTreatmentsCompare.removeAll(predictTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList()));
				break;
			}
			case NON_VEHICLE: {
				goldTreatments.removeAll(goldTreatments.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList()));
				predictTreatments.removeAll(predictTreatments.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList()));
				goldTreatmentsCompare = goldTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList());
				predictTreatmentsCompare = predictTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList());
				break;
			}
			}

			Score s;
			if (goldTreatments.isEmpty() && predictTreatments.isEmpty()) {
				if (mode == ESimpleEvaluationMode.BOTH) {
					s = new Score(1, 0, 0);
				} else {
					if (goldTreatmentsCompare.isEmpty() && predictTreatmentsCompare.isEmpty()
							&& mode == ESimpleEvaluationMode.NON_VEHICLE) {
						s = new Score(1, 0, 0);
					} else {
						s = new Score(0, 0, 0);
					}
				}
			} else
				s = evaluator.scoreMultiValues(goldTreatments, predictTreatments);

			if (print) {
				log.info("Compare: g" + goldIndex);
				goldTreatments.forEach(g -> log.info(g.toPrettyString()));
				log.info("With: p" + predictIndex);
				predictTreatments.forEach(p -> log.info(p.toPrettyString()));
				log.info("Score: " + s);
				log.info("-----");
			}

			simpleScore.add(s);

		}

		return simpleScore;
	}

}
