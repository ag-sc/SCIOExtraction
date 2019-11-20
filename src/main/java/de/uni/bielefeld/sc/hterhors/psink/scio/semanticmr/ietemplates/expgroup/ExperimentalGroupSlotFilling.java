package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

import de.hterhors.semanticmr.candprov.sf.AnnotationCandidateRetrievalCollection;
import de.hterhors.semanticmr.candprov.sf.EntityTemplateCandidateProvider;
import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.SemanticParsingCRF;
import de.hterhors.semanticmr.crf.exploration.IExplorationStrategy;
import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer;
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
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.Instance.ModifyGoldRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.eval.NerlaEvaluator;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.nerla.INerlaProvider;
import de.hterhors.semanticmr.nerla.NerlaCollector;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.investigation.CollectExpGroupNames;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.investigation.CollectExpGroupNames.PatternIndexPair;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.specifications.ExperimentalGroupSpecifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.BOWCardinalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrAllUsedTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrBOWTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExpGroupTreatmentLocalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.SlotIsFilledTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.TreatmentCardinalityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.Word2VecClusterTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.AgeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.NPChunker;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.NPChunker.TermIndexPair;

/***
 * 
 * Overall score = Score [getF1()=0.790, getPrecision()=0.843,
 * getRecall()=0.744, tp=215, fp=40, fn=74, tn=0]
 * 
 * @author hterhors
 *
 */

public class ExperimentalGroupSlotFilling extends AbstractSemReadProject {

	public static void main(String[] args) throws IOException {
		new ExperimentalGroupSlotFilling();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private final IObjectiveFunction trainingObjectiveFunction = new SlotFillingObjectiveFunction(
			new CartesianEvaluator(EEvaluationDetail.DOCUMENT_LINKED));

	private final IObjectiveFunction predictionObjectiveFunction = new SlotFillingObjectiveFunction(
			new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE));

	private final File instanceDirectory = new File(
			"src/main/resources/slotfilling/experimental_group/corpus/instances/");

	private InstanceProvider instanceProvider;

	public ExperimentalGroupSlotFilling() throws IOException {
		super(SystemScope.Builder.getScopeHandler().addScopeSpecification(ExperimentalGroupSpecifications.systemsScope)
				.apply().registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization()).build());

		SlotType.get("hasGroupName").excludeFromExploration = true;

		List<String> docs = Files.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath());

//		Collections.shuffle(docs, new Random(100L));

		List<String> trainingInstanceNames = docs.subList(0, 50);
		List<String> testInstanceNames = docs.subList(50, docs.size());

		AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
				.setTrainingInstanceNames(trainingInstanceNames).setTestInstanceNames(testInstanceNames).build();

		InstanceProvider.maxNumberOfAnnotations = 8;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		Collection<ModifyGoldRule> goldModificationRules = getGoldModificationRules();

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor, goldModificationRules);

		List<Instance> trainingInstances = instanceProvider.getRedistributedTrainingInstances();
		List<Instance> devInstances = instanceProvider.getRedistributedDevelopmentInstances();
		List<Instance> testInstances = instanceProvider.getRedistributedTestInstances();

		String rand = String.valueOf(new Random().nextInt(100000));

		/*
		 * Initialize candidate provider for search space exploration.
		 */

		/*
		 * Create annotation with pattern.
		 */
		NerlaCollector nerlaProvider = new NerlaCollector(instanceProvider.getInstances());
		nerlaProvider.addNerlaProvider(new INerlaProvider() {

			@Override
			public Map<Instance, List<DocumentLinkedAnnotation>> getForInstances(List<Instance> instances) {

//				return annotateGroupNamesWithPattern(instances);

//				try {
//					return annotateGroupNamesWithNPCHunksAndPattern(instances);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//				return null;

				// try {
////					return annotateGroupNamesWithNPCHunks(instances);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//				return null;

				return extractCandidatesFromGold(instances);
			}

			private Map<Instance, List<DocumentLinkedAnnotation>> extractCandidatesFromGold(List<Instance> instances) {

				Map<Instance, List<DocumentLinkedAnnotation>> gold = new HashMap<>();

				for (Instance instance : instances) {
					gold.put(instance, new ArrayList<>());
					gold.get(instance)
							.addAll(instance.getGoldAnnotations().getAbstractAnnotations().stream()
									.map(e -> e.asInstanceOfEntityTemplate().getMultiFillerSlot("hasGroupName"))
									.filter(s -> s.containsSlotFiller()).flatMap(s -> s.getSlotFiller().stream())
									.map(e -> e.asInstanceOfDocumentLinkedAnnotation()).collect(Collectors.toList()));
				}

				return gold;
			}

		});

		/*
		 * Create annotations for treatments/organismModel/injury with gold data.
		 */
		AnnotationCandidateRetrievalCollection candidateRetrieval = nerlaProvider.collect();

		for (Instance instance : instanceProvider.getRedistributedTrainingInstances()) {
			candidateRetrieval.registerCandidateProvider(extractCandidatesFromGold(instance));
		}
		for (Instance instance : instanceProvider.getRedistributedDevelopmentInstances()) {
			candidateRetrieval.registerCandidateProvider(extractCandidatesFromGold(instance));
		}
		for (Instance instance : instanceProvider.getRedistributedTestInstances()) {
			candidateRetrieval.registerCandidateProvider(extractCandidatesFromGold(instance));
		}

		List<IExplorationStrategy> explorerList = Arrays
				.asList(new SlotFillingExplorer(predictionObjectiveFunction, candidateRetrieval));

		List<AbstractFeatureTemplate<?>> featureTemplates = getFeatureTemplates();

		Map<Instance, Map<SlotType, Integer>> numberToPredictParameter = new HashMap<>();
		for (Instance instance : instanceProvider.getInstances()) {
			numberToPredictParameter.put(instance, new HashMap<>());
			numberToPredictParameter.get(instance).put(SlotType.get("hasOrganismModel"),
					extractGoldOrganismModels(instance).size());
			numberToPredictParameter.get(instance).put(SlotType.get("hasInjuryModel"),
					extractGoldInjuryModels(instance).size());
			numberToPredictParameter.get(instance).put(SlotType.get("hasTreatmentType"),
					extractGoldTreatments(instance).size());
		}
		Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> parameter = getParameter(numberToPredictParameter);

		AbstractSampler sampler = newSampler();

		MaxChainLengthCrit maxStepCrit = new MaxChainLengthCrit(10);
		ConverganceCrit noModelChangeCrit = new ConverganceCrit(3 * explorerList.size(), s -> s.getModelScore());
		ISamplingStoppingCriterion[] sampleStoppingCrits = new ISamplingStoppingCriterion[] { maxStepCrit,
				noModelChangeCrit };
		/**
		 * Finally, we chose a model base directory and a name for the model.
		 * 
		 * NOTE: Make sure that the base model directory exists!
		 */
		final File modelBaseDir = getModelBaseDir();
		final String modelName = "ExperimentalGroup" + rand;

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
		SemanticParsingCRF crf = new SemanticParsingCRF(model, explorerList, sampler, getStateInitializer(),
				trainingObjectiveFunction);

		/**
		 * Chose a different evaluation for prediction than for training. During
		 * training we are interested in finding the correct document linked annotation
		 * but for prediction we are only interested in the entity type.
		 */
//		IObjectiveFunction predictionOF = new SlotFillingObjectiveFunction(
//				new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE));

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

//		System.out.println("Training instances coverage: "
//				+ crf.computeCoverage(false, predictionObjectiveFunction, trainingInstances));
//		System.out.println(
//				"Test instances coverage: " + crf.computeCoverage(false, predictionObjectiveFunction, testInstances));

		/**
		 * At this position the model was either successfully loaded or trained. Now we
		 * want to apply the model to unseen data. We select the redistributed test data
		 * in this case. This method returns for each instances a final state (best
		 * state based on the trained model) that contains annotations.
		 */
		Map<Instance, State> results = crf.predict(testInstances, maxStepCrit);

		/**
		 * Finally, we evaluate the produced states and print some statistics.
		 */
		evaluate(log, results, predictionObjectiveFunction);

		log.info(crf.getTrainingStatistics());
		log.info(crf.getTestStatistics());


		Score s = new Score();
		NerlaEvaluator eval = new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE);

		for (Entry<Instance, State> e : results.entrySet()) {

			log.info(e.getKey().getName());

			List<EntityTemplate> goldAnnotations = e.getValue().getGoldAnnotations().getAnnotations();
			List<EntityTemplate> predictedAnnotations = e.getValue().getCurrentPredictions().getAnnotations();

			List<Integer> bestAssignment = ((CartesianEvaluator) predictionObjectiveFunction.getEvaluator())
					.getBestAssignment(goldAnnotations, predictedAnnotations);
			Score score;
			log.info(score = simpleEvaluate(false, eval, bestAssignment, goldAnnotations, predictedAnnotations));

			s.add(score);

		}

		System.out.println("Overall score = " + s);
		log.info("modelName: " + modelName);

	}

	private List<AbstractFeatureTemplate<?>> getFeatureTemplates() {
		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();

		featureTemplates.add(new BOWCardinalityTemplate());

		// featureTemplates.add(new TreatmentPriorTemplate());
//		Overall score = Score [getF1()=0.761, getPrecision()=0.803, getRecall()=0.722, tp=208, fp=51, fn=80, tn=0]

		featureTemplates.add(new TreatmentCardinalityTemplate());

		featureTemplates.add(new ExGrBOWTemplate());

		featureTemplates.add(new ExGrAllUsedTemplate());
		featureTemplates.add(new SlotIsFilledTemplate());

		featureTemplates.add(new ExpGroupTreatmentLocalityTemplate());
//		Overall score = Score [getF1()=0.751, getPrecision()=0.797, getRecall()=0.711, tp=204, fp=52, fn=83, tn=0]

//		featureTemplates.add(new ContextBetweenSlotFillerTemplate());

		featureTemplates.add(new Word2VecClusterTemplate());
		return featureTemplates;
	}

	private Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> getParameter(
			Map<Instance, Map<SlotType, Integer>> numberToPredictParameter) {
		Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> parameter = new HashMap<>();
		parameter.put(ExGrAllUsedTemplate.class, new Object[] { numberToPredictParameter });

//		parameter.put(Word2VecClusterTemplate.class,
//				new Object[] { new File("wordvector/small_kmeans++_1000_ranking_reduce10.vec"),
//						new File("wordvector/kmeans_200_distances.vec") });
		parameter.put(Word2VecClusterTemplate.class,
				new Object[] { new File("wordvector/kmeans++_1000_ranking_reduce10.vec"),
						new File("wordvector/kmeans_200_distances.vec") });
//		parameter.put(Word2VecClusterTemplate.class,
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
		return 1;
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

	private Map<Instance, List<DocumentLinkedAnnotation>> annotateGroupNamesWithNPCHunksAndPattern(
			List<Instance> instances) throws IOException {

		Map<Instance, List<DocumentLinkedAnnotation>> annotations = new HashMap<>();

		for (Instance instance : instances) {
			final List<DocumentLinkedAnnotation> findings = new ArrayList<>();
			extractWithNPCHunks(instance, findings);
			extractWithPattern(instance, findings);
			annotations.put(instance, findings);
		}

		return annotations;
	}

	private void extractWithNPCHunks(Instance instance, final List<DocumentLinkedAnnotation> findings)
			throws IOException {
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
					findings.add(annotation);

			}
		}
	}

	private void extractWithPattern(Instance instance, final List<DocumentLinkedAnnotation> findings) {
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
						findings.add(annotation);
				}
			}
		}
	}

	private Map<Instance, List<DocumentLinkedAnnotation>> annotateGroupNamesWithNPCHunks(List<Instance> instances)
			throws IOException {

		Map<Instance, List<DocumentLinkedAnnotation>> annotations = new HashMap<>();

		for (Instance instance : instances) {
			final List<DocumentLinkedAnnotation> findings = new ArrayList<>();
			extractWithNPCHunks(instance, findings);
			annotations.put(instance, findings);
		}

		return annotations;
	}

	public Map<Instance, List<DocumentLinkedAnnotation>> annotateGroupNamesWithPattern(List<Instance> instances) {
		Map<Instance, List<DocumentLinkedAnnotation>> annotations = new HashMap<>();

		for (Instance instance : instances) {
			final List<DocumentLinkedAnnotation> findings = new ArrayList<>();
			annotations.put(instance, findings);
			extractWithPattern(instance, findings);
		}
		return annotations;
	}

	public Collection<ModifyGoldRule> getGoldModificationRules() {
		Collection<ModifyGoldRule> goldModificationRules = new ArrayList<>();
		goldModificationRules.add(a -> {
			if (a.asInstanceOfEntityTemplate().getRootAnnotation().entityType == EntityType
					.get("DefinedExperimentalGroup"))
				return a;
			return null;
		});

//		goldModificationRules.add(a -> {
//			a.asInstanceOfEntityTemplate().reduceRootToEntityType();
//			return a;
//		});

		goldModificationRules.add(a -> {
			a.asInstanceOfEntityTemplate().getSingleFillerSlot("hasInjuryModel").clear();
			return a;
		});

		goldModificationRules.add(a -> {
			a.asInstanceOfEntityTemplate().getSingleFillerSlot("hasOrganismModel").clear();
			return a;
		});
//
//		goldModificationRules.add(a -> {
//			a.asInstanceOfEntityTemplate().getMultiFillerSlot("hasTreatmentType").clear();
//			return a;
//		});

		goldModificationRules.add(a -> {
			a.asInstanceOfEntityTemplate().getSingleFillerSlot("hasNNumber").clear();
			return a;
		});
		goldModificationRules.add(a -> {
			a.asInstanceOfEntityTemplate().getSingleFillerSlot("hasTotalPopulationSize").clear();
			return a;
		});
		goldModificationRules.add(a -> {
			a.asInstanceOfEntityTemplate().getSingleFillerSlot("hasGroupNumber").clear();
			return a;
		});
		goldModificationRules.add(a -> {
			if (a.asInstanceOfEntityTemplate().getMultiFillerSlot(SlotType.get("hasGroupName")).containsSlotFiller()
					|| a.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasInjuryModel"))
							.containsSlotFiller()
					|| a.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasOrganismModel"))
							.containsSlotFiller()
					|| a.asInstanceOfEntityTemplate().getMultiFillerSlot(SlotType.get("hasTreatmentType"))
							.containsSlotFiller())
				return a;
			return null; // remove from annotation if it has no injury, treatment or organism.
		});
		return goldModificationRules;
	}

	/**
	 * The state initializer.
	 * 
	 * There are different options implemented:
	 * 
	 * 1) standard: Creates an state with only one DefinedExperimentalGroup
	 * annotations.
	 * 
	 * 2) cardinality: Creates a state with the correct number of defined exp
	 * groups.
	 * 
	 * 3) card+groupNames: Creates states with the correct cardinality + each
	 * initialized exp group contains the correct root annotation and groupNames.
	 * 
	 * @return
	 */

	private IStateInitializer getStateInitializer() {
		/**
		 * 3)
		 */
		return (instance -> {
			List<AbstractAnnotation> as = new ArrayList<>();

			for (EntityTemplate goldAnnotation : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {

				EntityTemplate init = new EntityTemplate(goldAnnotation.getRootAnnotation());

				for (AbstractAnnotation groupName : goldAnnotation.asInstanceOfEntityTemplate()
						.getMultiFillerSlot("hasGroupName").getSlotFiller()) {
					init.addMultiSlotFiller(SlotType.get("hasGroupName"), groupName);
				}

				as.add(init);
			}

			return new State(instance, new Annotations(as));
		});

		/**
		 * 2)
		 */
//		return (instance -> {
//			
//			List<AbstractAnnotation> as = new ArrayList<>();
//			
//			for (int i = 0; i < instance.getGoldAnnotations().getAnnotations().size(); i++) {
//				as.add(new EntityTemplate(AnnotationBuilder.toAnnotation("DefinedExperimentalGroup")));
//			}
//			return new State(instance, new Annotations(as));
//		});

		/**
		 * 1)
		 */

//return ((instance) -> new State(instance,
//new Annotations(new EntityTemplate(AnnotationBuilder.toAnnotation("DefinedExperimentalGroup")))));
	}

	private File getModelBaseDir() {
		return new File("models/slotfilling/experimental_group/");
	}

	private AdvancedLearner newLearner() {
		return new AdvancedLearner(new SGD(0.0001, 0), new L2(0.0000000));
	}

	public EntityTemplateCandidateProvider extractCandidatesFromGold(Instance instance) {
		EntityTemplateCandidateProvider entityTemplateCandidateProvider = new EntityTemplateCandidateProvider(instance);
		entityTemplateCandidateProvider.addBatchSlotFiller(extractGoldTreatments(instance));
		entityTemplateCandidateProvider.addBatchSlotFiller(extractGoldOrganismModels(instance));
		entityTemplateCandidateProvider.addBatchSlotFiller(extractGoldInjuryModels(instance));
		return entityTemplateCandidateProvider;
	}

	@Deprecated
	private void predictAll() {
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

	private Set<AbstractAnnotation> extractGoldTreatments(Instance instance) {

		Set<AbstractAnnotation> treatments = new HashSet<>();
		for (EntityTemplate expGroup : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {
			treatments.addAll(expGroup.getMultiFillerSlot(SlotType.get("hasTreatmentType")).getSlotFiller().stream()
					.filter(a -> a != null).collect(Collectors.toList()));
		}

		return treatments;
	}

	private Set<AbstractAnnotation> extractGoldOrganismModels(Instance instance) {

		Set<AbstractAnnotation> orgModels = new HashSet<>();
		for (EntityTemplate expGroup : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {
			if (expGroup.getSingleFillerSlot(SlotType.get("hasOrganismModel")).containsSlotFiller())
				orgModels.add(expGroup.getSingleFillerSlot(SlotType.get("hasOrganismModel")).getSlotFiller());
		}

		return orgModels;
	}

	private Set<AbstractAnnotation> extractGoldInjuryModels(Instance instance) {

		Set<AbstractAnnotation> injuries = new HashSet<>();
		for (EntityTemplate expGroup : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {
			if (expGroup.getSingleFillerSlot(SlotType.get("hasInjuryModel")).containsSlotFiller())
				injuries.add(expGroup.getSingleFillerSlot(SlotType.get("hasInjuryModel")).getSlotFiller());
		}

		return injuries;
	}

	private Score simpleEvaluate(boolean print, NerlaEvaluator evaluator, List<Integer> bestAssignment,
			List<EntityTemplate> goldAnnotations, List<EntityTemplate> predictedAnnotationsBaseline) {
		Score simpleScore = new Score();

		for (int goldIndex = 0; goldIndex < bestAssignment.size(); goldIndex++) {
			final int predictIndex = bestAssignment.get(goldIndex);
			/*
			 * Treatments
			 */
			List<AbstractAnnotation> goldTreatments = new ArrayList<>(
					goldAnnotations.get(goldIndex).getMultiFillerSlot("hasTreatmentType").getSlotFiller());

			List<AbstractAnnotation> predictTreatments = new ArrayList<>(predictedAnnotationsBaseline.get(predictIndex)
					.getMultiFillerSlot("hasTreatmentType").getSlotFiller());
			Score s;
			if (goldTreatments.isEmpty() && predictTreatments.isEmpty())
				s = new Score(1, 0, 0);
			else
				s = evaluator.prf1(goldTreatments, predictTreatments);

			if (print && s.getF1() != 1.0D) {
				System.out.println("Compare: g" + goldIndex);
				goldTreatments.forEach(g -> System.out.println(g.toPrettyString()));
				System.out.println("With: p" + predictIndex);
				predictTreatments.forEach(p -> System.out.println(p.toPrettyString()));
				System.out.println("Score: " + s);
				System.out.println("-----");
			}

			simpleScore.add(s);
			/*
			 * OrganismModel
			 */
			List<AbstractAnnotation> goldOrganismModel = Arrays
					.asList(goldAnnotations.get(goldIndex).getSingleFillerSlot("hasOrganismModel").getSlotFiller())
					.stream().filter(a -> a != null).collect(Collectors.toList());
			List<AbstractAnnotation> predictOrganismModel = Arrays.asList(predictedAnnotationsBaseline.get(predictIndex)
					.getSingleFillerSlot("hasOrganismModel").getSlotFiller()).stream().filter(a -> a != null)
					.collect(Collectors.toList());

			simpleScore.add(evaluator.prf1(goldOrganismModel, predictOrganismModel));

			/*
			 * InjuryModel
			 */
			List<AbstractAnnotation> goldInjuryModel = Arrays
					.asList(goldAnnotations.get(goldIndex).getSingleFillerSlot("hasInjuryModel").getSlotFiller())
					.stream().filter(a -> a != null).collect(Collectors.toList());
			List<AbstractAnnotation> predictInjuryModel = Arrays.asList(predictedAnnotationsBaseline.get(predictIndex)
					.getSingleFillerSlot("hasInjuryModel").getSlotFiller()).stream().filter(a -> a != null)
					.collect(Collectors.toList());

			simpleScore.add(evaluator.prf1(goldInjuryModel, predictInjuryModel));

		}

		return simpleScore;
	}
}
