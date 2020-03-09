package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.results;

import java.io.File;

import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.rdf.ConvertToRDF;

public class ResultSlotFilling extends AbstractSemReadProject {

	public ResultSlotFilling(SystemScope scope) {
		super(scope);
		

		
		// TODO Auto-generated constructor stub
	}
//
//	public static void main(String[] args) throws Exception {
//
//		if (args.length == 0)
//			new ResultSlotFilling(11);
//		else
//			new ResultSlotFilling(Integer.parseInt(args[0]));
//	}
//
//	private static Logger log = LogManager.getFormatterLogger("SlotFilling");
//
//	private final IObjectiveFunction trainingObjectiveFunction;
//
//	private final IObjectiveFunction predictionObjectiveFunction;
////
////	
////	private final IObjectiveFunction trainingObjectiveFunction = new SlotFillingObjectiveFunction(
////			new BeamSearchEvaluator(EEvaluationDetail.DOCUMENT_LINKED,3));
////	
////	private final IObjectiveFunction predictionObjectiveFunction = new SlotFillingObjectiveFunction(
////			new BeamSearchEvaluator(EEvaluationDetail.ENTITY_TYPE, 3));
//
////	private final IObjectiveFunction trainingObjectiveFunction = new SlotFillingObjectiveFunction(
////			new GreedySearchEvaluator(EEvaluationDetail.DOCUMENT_LINKED));
////
////	private final IObjectiveFunction predictionObjectiveFunction = new SlotFillingObjectiveFunction(
////			new GreedySearchEvaluator(EEvaluationDetail.ENTITY_TYPE));
//
//	private final File instanceDirectory;
//
//	private InstanceProvider instanceProvider;
//
//	private List<Instance> trainingInstances;
//	private List<Instance> devInstances;
//	private List<Instance> testInstances;
//
//	private String modelName;
//	private File outputFile;
//
//	private final String rand;
//
//	public ResultSlotFilling(int parameterID) throws Exception {
//		super(SystemScope.Builder.getScopeHandler()
//				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("ExperimentalGroup"))
//				.apply().registerNormalizationFunction(new WeightNormalization())
//				.registerNormalizationFunction(new AgeNormalization()).build());
//
//		this.instanceDirectory = SlotFillingCorpusBuilderBib
//				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.experimentalGroup);
//
//		Annotations.removeDuplicates = true;
//
//		CartesianEvaluator.MAXIMUM_PERMUTATION_SIZE = 4;
//
//		SlotFillingExplorer.MAX_NUMBER_OF_ANNOTATIONS = 8;
//
//		rand = String.valueOf(new Random().nextInt(100000));
////		modelName = "ExperimentalGroup" + 71310;
//		modelName = "ExperimentalGroup" + rand;
//		log.info("Model name = " + modelName);
//		setParameterByID(parameterID);
//
//		outputFile = new File(parameterID + "_ExperimentalGroupExtractionResults_" + rand);
//
//		trainingObjectiveFunction = new SlotFillingObjectiveFunction(
//				new CartesianEvaluator(explorationMode == EExplorationMode.TYPE_BASED ? EEvaluationDetail.ENTITY_TYPE
//						: EEvaluationDetail.DOCUMENT_LINKED));
//
//		predictionObjectiveFunction = new SlotFillingObjectiveFunction(
//				new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE));
//		/**
//		 * Exclude some slots that are not needed for now
//		 */
//		SCIOSlotTypes.hasNNumber.exclude();
//		SCIOSlotTypes.hasTotalPopulationSize.exclude();
//		SCIOSlotTypes.hasGroupNumber.exclude();
//
//		/**
//		 * Apply different modes that were previously set.
//		 */
//
//		List<IExplorationStrategy> explorerList = buildExplorer();
//
//
//
//		IStateInitializer initializer = buildStateInitializer();
//
//		List<AbstractFeatureTemplate<?>> featureTemplates = getFeatureTemplates();
//
//		Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> parameter = getParameter();
//
//		ISampler sampler = getSampler();
//
//		MaxChainLengthCrit maxStepCrit = new MaxChainLengthCrit(100);
//
//		ConverganceCrit noModelChangeCrit = new ConverganceCrit(10 * explorerList.size(), s -> s.getModelScore());
//
//		ISamplingStoppingCriterion[] sampleStoppingCrits = new ISamplingStoppingCriterion[] { maxStepCrit,
//				noModelChangeCrit };
//
//		run(explorerList, sampler, initializer, sampleStoppingCrits, maxStepCrit, featureTemplates, parameter);
//
//	}
//
//	private void run(List<IExplorationStrategy> explorerList, ISampler sampler, IStateInitializer initializer,
//			ISamplingStoppingCriterion[] sampleStoppingCrits, ISamplingStoppingCriterion maxStepCrit,
//			List<AbstractFeatureTemplate<?>> featureTemplates,
//			Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> parameter) throws Exception {
//		/**
//		 * Finally, we chose a model base directory and a name for the model.
//		 * 
//		 * NOTE: Make sure that the base model directory exists!
//		 */
//		final File modelBaseDir = getModelBaseDir();
//
//		Model model;
//
//		if (Model.exists(modelBaseDir, modelName)) {
//			/**
//			 * If the model exists load from the file system.
//			 */
//			model = Model.load(modelBaseDir, modelName);
//		} else {
//			/**
//			 * If the model does not exists, create a new model.
//			 */
//			model = new Model(featureTemplates, modelBaseDir, modelName);
//		}
//
//		model.setParameter(parameter);
//
//		/**
//		 * Create a new semantic parsing CRF and initialize with needed parameter.
//		 */
//		ISemanticParsingCRF crf;
////		if (cardinalityMode == ECardinalityMode.PARALLEL_CARDINALITIES)
////			crf = new SemanticParsingCRFMultiState(model, explorerList, (AbstractBeamSampler) sampler,
////					trainingObjectiveFunction);
////		else
//		crf = new SemanticParsingCRF(model, explorerList, (AbstractSampler) sampler, trainingObjectiveFunction);
//
//		crf.setInitializer(initializer);
//
////		log.info("Training instances coverage: "
////				+ crf.computeCoverage(true, predictionObjectiveFunction, trainingInstances));
////
////		log.info("Test instances coverage: " + crf.computeCoverage(true, predictionObjectiveFunction, testInstances));
////
////		System.exit(1);
//
//		/**
//		 * If the model was loaded from the file system, we do not need to train it.
//		 */
//		if (!model.isTrained()) {
//			/**
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
//		}
//
//		/**
//		 * At this position the model was either successfully loaded or trained. Now we
//		 * want to apply the model to unseen data. We select the redistributed test data
//		 * in this case. This method returns for each instances a final state (best
//		 * state based on the trained model) that contains annotations.
//		 */
//
//		Map<Instance, State> results = crf.predict(testInstances, maxStepCrit);
//
//		log.info(crf.getTrainingStatistics());
//		log.info(crf.getTestStatistics());
//		log.info("modelName: " + modelName);
//
//		/**
//		 * Evaluate with objective function
//		 */
//		evaluate(log, results, predictionObjectiveFunction);
//
//		/**
//		 * Evaluate assuming TT,OM,IM are one unit. Evaluate assuming TT,OM,IM are one
//		 * unit, distinguish vehicle-treatments and main-treatments.
//		 */
//
//		evaluateDetailed(results);
//
//		log.info(crf.getTrainingStatistics());
//		log.info(crf.getTestStatistics());
//		log.info("modelName: " + modelName);
//		log.info("States generated: " + SlotFillingExplorer.statesgenerated);
//
//	}
//
//	private IStateInitializer buildStateInitializer() {
//
//		switch (cardinalityMode) {
//		case GOLD_CARDINALITY:
//			return new GoldCardinalityInitializer(groupNameProviderMode, groupNameClusteringMode);
//		case PREDICTED_CARDINALITY:
//			return new PredictCardinalityInitializer(groupNameProviderMode, groupNameClusteringMode,
//					instanceProvider.getInstances());
//		case SAMPLE_CARDINALITY:
//			return new SampleCardinalityInitializer(1);
//		case PARALLEL_CARDINALITIES:
//			return new MultiCardinalityInitializer(groupNameProviderMode, groupNameClusteringMode,
//					instanceProvider.getInstances(), trainingInstances);
//		default:
//			return null;
//		}
//	}
//
//	private File getModelBaseDir() {
//		return new File("models/slotfilling/experimental_group/");
//	}
//
//	private AdvancedLearner newLearner() {
//		return new AdvancedLearner(new SGD(0.0001, 0), new L2(0.0000000));
//	}
//
//	private List<AbstractFeatureTemplate<?>> getFeatureTemplates() {
//		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();
//
//		featureTemplates.add(new TB_Word2VecClusterTemplate());
//		featureTemplates.add(new TB_IntraTokenCardinalityTemplate());
//		featureTemplates.add(new TB_ContextCardinalityTemplate());
//		featureTemplates.add(new TB_ContextBetweenSlotFillerTemplate());
//		featureTemplates.add(new TB_ExpGroupTreatmentLocalityTemplate());
//		featureTemplates.add(new TB_BOWCardinalityTemplate());
//		featureTemplates.add(new TB_ExGrBOWTemplate());
//		featureTemplates.add(new TB_SlotIsFilledTemplate());
//		featureTemplates.add(new TB_RemainingTypesTemplate());
//		featureTemplates.add(new TB_SameOrganismModelTemplate());
//		featureTemplates.add(new TB_SlotIsFilledTemplate());
//		featureTemplates.add(new TB_TreatmentPriorTemplate());
//
//		return featureTemplates;
//	}
}
