package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.trend.nerla;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.ISemanticParsingCRF;
import de.hterhors.semanticmr.crf.SemanticParsingCRF;
import de.hterhors.semanticmr.crf.exploration.IExplorationStrategy;
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
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.ISampler;
import de.hterhors.semanticmr.crf.sampling.impl.EpochSwitchSampler;
import de.hterhors.semanticmr.crf.sampling.stopcrit.ISamplingStoppingCriterion;
import de.hterhors.semanticmr.crf.sampling.stopcrit.impl.ConverganceCrit;
import de.hterhors.semanticmr.crf.sampling.stopcrit.impl.MaxChainLengthCrit;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.templates.et.ClusterTemplate;
import de.hterhors.semanticmr.crf.templates.et.ContextBetweenSlotFillerTemplate;
import de.hterhors.semanticmr.crf.templates.shared.IntraTokenTemplate;
import de.hterhors.semanticmr.crf.templates.shared.NGramTokenContextTemplate;
import de.hterhors.semanticmr.crf.templates.shared.SingleTokenContextTemplate;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.Instance.DeduplicationRule;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.BeamSearchEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AnalyzeComplexity;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.hardconstraints.DistinctEntityTemplateConstraint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.initializer.SampleCardinalityInitializer;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.EntityTypeContextTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.Stats;

public class TrendSlotFilling extends AbstractSemReadProject {

	public static void main(String[] args) throws Exception {

		new TrendSlotFilling(100L);

	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private int minCacheSize = 40_000_000;
	private int maxCacheSize = 80_000_000;

	private final File instanceDirectory;
	private final IObjectiveFunction predictionObjectiveFunction;
	private final IObjectiveFunction trainingObjectiveFunction;

	private final long dataRandomSeed;
	private final EScoreType scoreType;

	private InstanceProvider instanceProvider;

	private List<Instance> trainingInstances;
	private List<Instance> devInstances;
	private List<Instance> testInstances;
	private String modelName;

	public TrendSlotFilling(long dataRandomSeed) throws Exception {
		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Trend")).build();

		this.instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.trend);

		this.scoreType = EScoreType.MACRO;

//		this.trainingObjectiveFunction = new SlotFillingObjectiveFunction(scoreType,
//				new CartesianEvaluator(EEvaluationDetail.DOCUMENT_LINKED, EEvaluationDetail.DOCUMENT_LINKED));
//
//		this.predictionObjectiveFunction = new SlotFillingObjectiveFunction(scoreType,
//				new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE, EEvaluationDetail.DOCUMENT_LINKED));
		this.trainingObjectiveFunction = new SlotFillingObjectiveFunction(scoreType,
				new BeamSearchEvaluator(EEvaluationDetail.DOCUMENT_LINKED, 10));

		this.predictionObjectiveFunction = new SlotFillingObjectiveFunction(scoreType,
				new BeamSearchEvaluator(EEvaluationDetail.ENTITY_TYPE, 10));

		this.dataRandomSeed = dataRandomSeed;

		readData();

		Set<SlotType> slotTypesToConsider = new HashSet<>(
				Arrays.asList(SCIOSlotTypes.hasDifference, SCIOSlotTypes.hasPValue, SCIOSlotTypes.hasSignificance));

//		sentenceDistribution();

//		Stats.countVariables(0, instanceProvider.getInstances());

		Stats.countVariables(0, instanceProvider.getInstances(), slotTypesToConsider);

		//
//		int min = 1000;
//		for (Instance instance : instanceProvider.getInstances()) {
//			int c = 0;
//			System.out.println(instance.getName());
//			for (AbstractAnnotation aa : instance.getGoldAnnotations().getAbstractAnnotations()) {
//
//				c += trainingObjectiveFunction.getEvaluator().scoreSingle(aa, aa).getTp();
//			}
//	
//			System.out.println(instance.getGoldAnnotations().getAbstractAnnotations().size());
//			System.out.println(c);
//			System.out.println();
//			min = Math.min(min, c);
//		}
//		System.out.println(min);
//

		Stats.computeNormedVar(instanceProvider.getInstances(), SCIOEntityTypes.trend);

		
		for (SlotType slotType : slotTypesToConsider) {
			Stats.computeNormedVar(instanceProvider.getInstances(), slotType);
		}
		

		AnalyzeComplexity.analyze(SCIOEntityTypes.result, slotTypesToConsider, instanceProvider.getInstances(),
				predictionObjectiveFunction.getEvaluator());

		String rand = String.valueOf(new Random().nextInt(100000));

		modelName = "Result" + rand;
		log.info("Model name = " + modelName);

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

//		int overallResults = 0;
//		for (Instance instance : trainingInstances) {
//			System.out.println(instance.getName());
//
//			for (Entry<Integer, List<EntityType>> docLinkedAnnotations : readAnnotationsForInstance(instance)
//					.entrySet()) {
//				System.out.println(docLinkedAnnotations.getKey() + "\t" + docLinkedAnnotations.getValue());
//			}
//			for (AbstractAnnotation goldAnnotation : instance.getGoldAnnotations().getAnnotations()) {
//
//				System.out.println(goldAnnotation.toPrettyString());
//			}
//			overallResults += instance.getGoldAnnotations().getAnnotations().size();
//		}
//
//		System.out.println("overallResults: " + overallResults);

	}

//	private void sentenceDistribution() {
//		int one = 0;
//		int two = 0;
//		int three = 0;
//		for (Instance instance : instanceProvider.getTrainingInstances()) {
//
//			for (AbstractAnnotation trendAnnotation : instance.getGoldAnnotations().getAnnotations()) {
//				Trend t = new Trend(trendAnnotation.asInstanceOfEntityTemplate());
//
//				Set<Integer> sentences = new HashSet<>(t.getRelevantSentenceIndexes());
//
//				if (sentences.size() == 1)
//					one++;
//				else if (sentences.size() == 2)
//					two++;
//				else if (sentences.size() == 3)
//					three++;
//				else if (sentences.size() > 3) {
//					System.out.println(sentences.size());
//					System.exit(1);
//				}
//
//			}
//
//		}
//
//		System.out.println(one);
//		System.out.println(two);
//		System.out.println(three);
//
//		System.exit(1);
//	}

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
		ISemanticParsingCRF crf = new SemanticParsingCRF(model, explorerList, (AbstractSampler) sampler,
				trainingObjectiveFunction);

		crf.setInitializer(initializer);

//		log.info("Training instances coverage: "
//				+ ((SemanticParsingCRF) crf).computeCoverage(true, predictionObjectiveFunction, trainingInstances));
//
//		log.info("Test instances coverage: "
//				+ ((SemanticParsingCRF) crf).computeCoverage(true, predictionObjectiveFunction, testInstances));

//		System.exit(1);

		/**
		 * If the model was loaded from the file system, we do not need to train it.
		 */
		if (!model.isTrained())

		{
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

		log.info(crf.getTrainingStatistics());
		log.info(crf.getTestStatistics());
		log.info("modelName: " + modelName);
		log.info("States generated in total: " + SlotFillingExplorer.statesgenerated);
	}

	private int getNumberOfEpochs() {
		return 1;
	}

	private ISampler getSampler() {
//			AbstractSampler sampler = SamplerCollection.topKModelDistributionSamplingStrategy(2);
//			AbstractSampler sampler = SamplerCollection.greedyModelStrategy();
//			AbstractSampler sampler = SamplerCollection.greedyObjectiveStrategy();
		return new EpochSwitchSampler(epoch -> epoch % 2 == 0);
//			AbstractSampler sampler = new EpochSwitchSampler(new RandomSwitchSamplingStrategy());
//			AbstractSampler sampler = new EpochSwitchSampler(e -> new Random(e).nextBoolean());
	}

	private IStateInitializer buildStateInitializer() {
		return new SampleCardinalityInitializer(SCIOEntityTypes.result, 1);
	}

	public List<IExplorationStrategy> buildExplorer() {

		List<IExplorationStrategy> explorerList = new ArrayList<>();

		HardConstraintsProvider hardConstraintsProvider = new HardConstraintsProvider();

		hardConstraintsProvider
				.addHardConstraints(new DistinctEntityTemplateConstraint(predictionObjectiveFunction.getEvaluator()));

		SlotFillingExplorer slotFillingExplorer = new SlotFillingExplorer(EExplorationMode.TYPE_BASED,
				predictionObjectiveFunction, hardConstraintsProvider);

		explorerList.add(slotFillingExplorer);

//		RootTemplateCardinalityExplorer rootTemplateCardinalityExplorer = new RootTemplateCardinalityExplorer(
//				hardConstraintsProvider, predictionObjectiveFunction.getEvaluator(), EExplorationMode.TYPE_BASED,
//				AnnotationBuilder.toAnnotation(SCIOEntityTypes.result));
//		
//		explorerList.add(rootTemplateCardinalityExplorer);

		return explorerList;
	}

	public void readData() throws IOException {

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

		InstanceProvider.maxNumberOfAnnotations = 100;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		Collection<GoldModificationRule> goldModificationRules = getGoldModificationRules();
		DeduplicationRule deduplicationRule = (a1, a2) -> {
			return a1.evaluateEquals(predictionObjectiveFunction.getEvaluator(), a2);
		};

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor, goldModificationRules,
				deduplicationRule);

		trainingInstances = instanceProvider.getTrainingInstances();
		devInstances = instanceProvider.getDevelopmentInstances();
		testInstances = instanceProvider.getTestInstances();

	}

	private Collection<GoldModificationRule> getGoldModificationRules() {
		Collection<GoldModificationRule> goldModificationRules = new ArrayList<>();
//
//		goldModificationRules.add(a -> {
//
//			if (SCIOSlotTypes.hasGroupName.isIncluded())
//				if (a.asInstanceOfEntityTemplate().getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
//					a.asInstanceOfEntityTemplate()
//							.addMultiSlotFiller(SCIOSlotTypes.hasGroupName,
//									AnnotationBuilder.toAnnotation(
//											a.asInstanceOfEntityTemplate().getRootAnnotation()
//													.asInstanceOfDocumentLinkedAnnotation().document,
//											SCIOEntityTypes.groupName,
//											a.asInstanceOfEntityTemplate().getRootAnnotation()
//													.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm(),
//											a.asInstanceOfEntityTemplate().getRootAnnotation()
//													.asInstanceOfDocumentLinkedAnnotation().getStartDocCharOffset()));
//
//			a.asInstanceOfEntityTemplate().reduceRootToEntityType();
//			return a;
//		});

		return goldModificationRules;
	}

	private File getModelBaseDir() {
		return new File("models/slotfilling/trend/");
	}

//
	private AdvancedLearner newLearner() {
		return new AdvancedLearner(new SGD(0.0001, 0), new L2(0.0000000));
	}
//

	private List<AbstractFeatureTemplate<?>> getFeatureTemplates() {
		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();

		featureTemplates.add(new IntraTokenTemplate());
		featureTemplates.add(new NGramTokenContextTemplate());
		featureTemplates.add(new SingleTokenContextTemplate());
		featureTemplates.add(new ContextBetweenSlotFillerTemplate());
		featureTemplates.add(new ClusterTemplate());
		featureTemplates.add(new EntityTypeContextTemplate());
//		featureTemplates.add(new OlfactoryContextTemplate());
//		featureTemplates.add(new LocalityTemplate());
//		featureTemplates.add(new SlotIsFilledTemplate());
//		featureTemplates.add(new DocumentPartTemplate());

//		featureTemplates.add(new PriorNumericInterpretationOrgModelTemplate());

//		featureTemplates.add(new DocumentSectionTemplate());

//		featureTemplates.add(new LevenshteinTemplate());
//		featureTemplates.add(new NumericInterpretationTemplate());

		return featureTemplates;
	}

	private Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> getParameter() {
		return Collections.emptyMap();
	}

}
