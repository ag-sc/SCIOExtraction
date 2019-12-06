package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

import de.hterhors.semanticmr.candprov.helper.DictionaryFromInstanceHelper;
import de.hterhors.semanticmr.candprov.sf.AnnotationCandidateRetrievalCollection;
import de.hterhors.semanticmr.candprov.sf.EntityTemplateCandidateProvider;
import de.hterhors.semanticmr.candprov.sf.GeneralCandidateProvider;
import de.hterhors.semanticmr.candprov.sf.ICandidateProvider;
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
import de.hterhors.semanticmr.crf.structure.annotations.EntityTypeAnnotation;
import de.hterhors.semanticmr.crf.structure.slots.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.eval.NerlaEvaluator;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JsonNerlaProvider;
import de.hterhors.semanticmr.nerla.INerlaProvider;
import de.hterhors.semanticmr.nerla.NerlaCollector;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.deliverymethod.DeliveryMethodPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.investigation.CollectExpGroupNames;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.investigation.CollectExpGroupNames.PatternIndexPair;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.treatment.TreatmentRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.treatment.TreatmentRestrictionProvider.ETreatmentModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.treatment.TreatmentSlotFilling;
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
//
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

	private final File instanceDirectory = new File(
			"src/main/resources/slotfilling/experimental_group/corpus/instances/");

	private InstanceProvider instanceProvider;

	List<Instance> trainingInstances;
	List<Instance> devInstances;
	List<Instance> testInstances;

	enum EExtractGroupNamesMode {

		/**
		 * Add no GroupNames for at all. Neither for sampling nor for gold data.
		 */
		EMPTY,
		/**
		 * Add GroupName annotations from gold data. GroupNames however are not
		 * co-referenced and assigned to the correct DefindExperimentalGroup.
		 */
		GOLD_UNCLUSTERED,
		/**
		 * Add GroupName annotations from gold data and assign the groupNames to the
		 * correct DefinedExperimentalGroup. If this is chosen, no annotations are
		 * created for GroupNames and the slot "hasgroupNme" is excluded from sampling.
		 */
		GOLD_CLUSTERED,
		/**
		 * Add GroupName annotations from a set of predefined regular expression
		 * pattern. Annotations are not clustered yet, as they are can be wrong.
		 * Annotations however are used during sampling.
		 */
		PATTERN,
		/**
		 * Add GroupName annotations from NP-Chunks extracted with Stanford Core NLP.
		 * Annotations are not clustered yet, as they are can be wrong. Annotations
		 * however are used during sampling.
		 */

		NP_CHUNKS,
		/**
		 * Combine Pattern and NP-Chunks
		 */
		PATTERN_NP_CHUNKS;
	}

	public final EExtractGroupNamesMode groupNameMode = EExtractGroupNamesMode.GOLD_CLUSTERED;

	public ExperimentalGroupSlotFilling() throws IOException {
		super(SystemScope.Builder.getScopeHandler().addScopeSpecification(ExperimentalGroupSpecifications.systemsScope)
				.apply().registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization()).build());

		if (groupNameMode == EExtractGroupNamesMode.GOLD_CLUSTERED)
			SlotType.get("hasGroupName").excludeFromExploration = true;

		TreatmentSlotFilling.rule = ETreatmentModifications.ROOT;

		/*
		 *
		 * 
		 * 
		 * 
		 */
//		SpecificationWriter w = new SpecificationWriter(scope);
//		w.writeEntitySpecificationFile(new File("src/main/resources/slotfilling/experimental_group/entities.csv"),
//				EntityType.get("DefinedExperimentalGroup"));
//		w.writeHierarchiesSpecificationFile(
//				new File("src/main/resources/slotfilling/experimental_group/hierarchies.csv"),
//				EntityType.get("DefinedExperimentalGroup"));
//		w.writeSlotsSpecificationFile(new File("src/main/resources/slotfilling/experimental_group/slots.csv"),
//				EntityType.get("DefinedExperimentalGroup"));
////		w.writeStructuresSpecificationFile(null, EntityType.get("Treatment"));
//
//		System.exit(1);

		List<String> docs = Files.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath());

//		Collections.shuffle(docs, new Random(100L));

		List<String> trainingInstanceNames = docs.subList(0, 50);
		List<String> testInstanceNames = docs.subList(50, docs.size());

		AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
				.setTrainingInstanceNames(trainingInstanceNames).setTestInstanceNames(testInstanceNames).build();

		InstanceProvider.maxNumberOfAnnotations = 8;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		Collection<GoldModificationRule> goldModificationRules = getGoldModificationRules();

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor, goldModificationRules);

		trainingInstances = instanceProvider.getRedistributedTrainingInstances();
		devInstances = instanceProvider.getRedistributedDevelopmentInstances();
		testInstances = instanceProvider.getRedistributedTestInstances();

		String rand = String.valueOf(new Random().nextInt(100000));
		rand = "16500";
		final String modelName = "ExperimentalGroup" + rand;
		log.info("Model name = " + modelName);
		/*
		 * Initialize candidate provider for search space exploration.
		 */

		/*
		 * Create annotation with pattern.
		 */
		NerlaCollector nerlaProvider = new NerlaCollector(instanceProvider.getInstances());

		/*
		 * add external treatment annotations
		 */
//		nerlaProvider.addNerlaProvider(
//				new JsonNerlaProvider(new File("src/main/resources/slotfilling/treatment/corpus/nerla/")));

		/*
		 * TODO: get Gold treatment annotations.
		 */
//		nerlaProvider.addNerlaProvider(new JsonNerlaProvider(getGoldTreatmentNerlaAnnotations()));
		/*
		 * Add groupNames
		 */
		nerlaProvider.addNerlaProvider(extractGroupNames(groupNameMode));

		/*
		 * Create annotations for treatments/organismModel/injury with gold data.
		 */
		AnnotationCandidateRetrievalCollection candidateRetrieval = nerlaProvider.collect();

//		Map<EntityType, Set<String>> trainDictionary = DictionaryFromInstanceHelper.toDictionary(trainingInstances);
//		
//		for (Instance instance : trainingInstances) {
//			GeneralCandidateProvider ap = new GeneralCandidateProvider(instance);
//			
//			for (AbstractAnnotation annotation : DictionaryFromInstanceHelper.getAnnotationsForInstance(instance,
//					trainDictionary)) {
//				ap.addSlotFiller(annotation);
//			}
//			candidateRetrieval.registerCandidateProvider(ap);
//		}

		extractTreatmentsFromNERLA(instanceProvider.getInstances())
				.forEach(p -> candidateRetrieval.registerCandidateProvider(p));

		extractTreatmentsFromTRAIN().forEach(p -> candidateRetrieval.registerCandidateProvider(p));

		for (Instance instance : trainingInstances) {
			candidateRetrieval.registerCandidateProvider(extractCandidatesFromGold(instance));
		}
		for (Instance instance : devInstances) {
			candidateRetrieval.registerCandidateProvider(extractCandidatesFromGold(instance));
		}
		for (Instance instance : testInstances) {
			candidateRetrieval.registerCandidateProvider(extractCandidatesFromGold(instance));
		}

		for (ICandidateProvider ap : getAdditionalCandidateProvider(modelName)) {
			candidateRetrieval.registerCandidateProvider(ap);
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
		SemanticParsingCRF crf = new SemanticParsingCRF(model, explorerList, sampler, getStateInitializer(),
				trainingObjectiveFunction);

//		log.info("Training instances coverage: "
//				+ crf.computeCoverage(true, predictionObjectiveFunction, trainingInstances));
//
//		log.info("Test instances coverage: " + crf.computeCoverage(true, predictionObjectiveFunction, testInstances));

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
		evaluateTreatments(results, predictionObjectiveFunction.getEvaluator());

		log.info(crf.getTrainingStatistics());
		log.info(crf.getTestStatistics());
		log.info("modelName: " + modelName);

	}

	public INerlaProvider extractGroupNames(EExtractGroupNamesMode mode) {
		return new INerlaProvider() {

			@Override
			public Map<Instance, List<DocumentLinkedAnnotation>> getForInstances(List<Instance> instances) {

				switch (mode) {
				case EMPTY:
				case GOLD_CLUSTERED:// if clustered we do not need group names annotations for sampling.
					return returnEmptyMap(instances);
				case GOLD_UNCLUSTERED:
					return extractCandidatesFromGold(instances);
				case NP_CHUNKS:
					return annotateGroupNamesWithPattern(instances);
				case PATTERN:
					return annotateGroupNamesWithNPCHunks(instances);
				case PATTERN_NP_CHUNKS:
					return annotateGroupNamesWithNPCHunksAndPattern(instances);

				}

				return null;

			}

		};
	}

	private File getGoldTreatmentNerlaAnnotations() {
		// TODO Auto-generated method stub
		return null;
	}

	private List<? extends ICandidateProvider> getAdditionalCandidateProvider(String modelName) {

		List<GeneralCandidateProvider> candList = new ArrayList<>();
		DeliveryMethodPredictor deliveryMethodPrediction = null;
		if (TreatmentSlotFilling.rule != ETreatmentModifications.ROOT) {

			String deliveryMethodModelName = "DeliveryMethod" + modelName;

			deliveryMethodPrediction = new DeliveryMethodPredictor(deliveryMethodModelName, scope,
					trainingInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					devInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					testInstances.stream().map(i -> i.getName()).collect(Collectors.toList()));

			deliveryMethodPrediction.trainOrLoadModel();
			deliveryMethodPrediction.predictAllInstances(2);

			for (Instance instance : instanceProvider.getInstances()) {
				GeneralCandidateProvider ap = new GeneralCandidateProvider(instance);
//				if (TreatmentSlotFilling.rule != ETreatmentModifications.ROOT) {
				ap.addBatchSlotFiller(deliveryMethodPrediction.predictHighRecallInstanceByName(instance.getName(), 2));
//				}
//			ap.addSlotFiller(AnnotationBuilder.toAnnotation(EntityType.get("CompoundTreatment")));
				candList.add(ap);
			}
		}
		return candList;
	}

	private List<AbstractFeatureTemplate<?>> getFeatureTemplates() {
		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();

		featureTemplates.add(new BOWCardinalityTemplate());

		featureTemplates.add(new TreatmentPriorTemplate());

		featureTemplates.add(new TreatmentCardinalityTemplate());

		featureTemplates.add(new ExGrBOWTemplate());

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

	private Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> getParameter(
			Map<Instance, Map<SlotType, Integer>> numberToPredictParameter) {

		Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> parameter = new HashMap<>();
		parameter.put(ExGrAllUsedTemplate.class, new Object[] { numberToPredictParameter });

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

	public Collection<GoldModificationRule> getGoldModificationRules() {
		Collection<GoldModificationRule> goldModificationRules = new ArrayList<>();

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

//		goldModificationRules.add(a -> {
//			a.asInstanceOfEntityTemplate().getMultiFillerSlot("hasTreatmentType").clear();
//			return a;
//		});

		goldModificationRules.add(a -> {

			List<AbstractAnnotation> newTreatments = new ArrayList<>();

			MultiFillerSlot treatments = a.asInstanceOfEntityTemplate().getMultiFillerSlot("hasTreatmentType");

			for (AbstractAnnotation treatment : treatments.getSlotFiller()) {

				AbstractAnnotation treat = treatment;
				for (GoldModificationRule goldModificationRule : TreatmentRestrictionProvider
						.getByRule(TreatmentSlotFilling.rule)) {
					treat = goldModificationRule.modify(treat);

					if (treat == null) {
						break;
					}

				}
				if (treat != null)
					newTreatments.add(treat);

			}

			treatments.clear();

			for (AbstractAnnotation slotFiller : newTreatments) {
				treatments.add(slotFiller);
			}

			return a;
		});

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
	 * 1) standard: Creates a state with only one DefinedExperimentalGroup
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
		if (groupNameMode == EExtractGroupNamesMode.GOLD_CLUSTERED) {

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
		} else {

			/**
			 * 2)
			 */
			return (instance -> {

				List<AbstractAnnotation> as = new ArrayList<>();

				for (int i = 0; i < instance.getGoldAnnotations().getAnnotations().size(); i++) {
					as.add(new EntityTemplate(AnnotationBuilder.toAnnotation("DefinedExperimentalGroup")));
				}
				return new State(instance, new Annotations(as));
			});
		}

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
//		entityTemplateCandidateProvider.addBatchSlotFiller(extractGoldTreatments(instance));
		entityTemplateCandidateProvider.addBatchSlotFiller(extractGoldOrganismModels(instance));
		entityTemplateCandidateProvider.addBatchSlotFiller(extractGoldInjuryModels(instance));
		return entityTemplateCandidateProvider;
	}

	private List<? extends ICandidateProvider> extractTreatmentsFromNERLA(List<Instance> instances) {

		List<EntityTemplateCandidateProvider> provider = new ArrayList<>();

		JsonNerlaProvider prov = new JsonNerlaProvider(
				new File("src/main/resources/slotfilling/treatment/corpus/nerla/"));

		Map<Instance, List<DocumentLinkedAnnotation>> nerlas = prov.getForInstances(instances);
		for (Entry<Instance, List<DocumentLinkedAnnotation>> instanceNerla : nerlas.entrySet()) {

			Instance instance = instanceNerla.getKey();
			EntityTemplateCandidateProvider entityTemplateCandidateProvider = new EntityTemplateCandidateProvider(
					instance);

			for (DocumentLinkedAnnotation nerla : instanceNerla.getValue()) {

				if (nerla.getEntityType() == EntityType.get("Compound")
						|| nerla.getEntityType().isSubEntityOf(EntityType.get("Compound"))) {

					EntityTemplate compoundTreatment = new EntityTemplate(
							AnnotationBuilder.toAnnotation(EntityType.get("CompoundTreatment")))
									.setSingleSlotFiller(SlotType.get("hasCompound"), new EntityTemplate(nerla));
					entityTemplateCandidateProvider.addSlotFiller(compoundTreatment);
				} else if (nerla.getEntityType() != EntityType.get("CompoundTreatment")
						&& nerla.getEntityType().isSubEntityOf(EntityType.get("Treatment"))) {
					entityTemplateCandidateProvider.addSlotFiller(new EntityTemplate(nerla));
				}

			}

			provider.add(entityTemplateCandidateProvider);
		}

		return provider;
	}

	private List<? extends ICandidateProvider> extractTreatmentsFromTRAIN() {

		List<EntityTemplateCandidateProvider> provider = new ArrayList<>();

		Map<EntityType, Set<String>> trainDictionary = DictionaryFromInstanceHelper.toDictionary(trainingInstances);

		for (Instance instance : trainingInstances) {
			EntityTemplateCandidateProvider entityTemplateCandidateProvider = new EntityTemplateCandidateProvider(
					instance);
			for (AbstractAnnotation nerla : DictionaryFromInstanceHelper.getAnnotationsForInstance(instance,
					trainDictionary)) {

				if (nerla.getEntityType() == EntityType.get("Compound")
						|| nerla.getEntityType().isSubEntityOf(EntityType.get("Compound"))) {
					EntityTemplate compoundTreatment = new EntityTemplate(
							AnnotationBuilder.toAnnotation(EntityType.get("CompoundTreatment")))
									.setSingleSlotFiller(SlotType.get("hasCompound"), nerla);
					entityTemplateCandidateProvider.addSlotFiller(compoundTreatment);
				} else if (nerla.getEntityType() != EntityType.get("CompoundTreatment")
						&& nerla.getEntityType().isSubEntityOf(EntityType.get("Treatment"))) {
					entityTemplateCandidateProvider.addSlotFiller(nerla);
				}
			}
			provider.add(entityTemplateCandidateProvider);
		}
		return provider;
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
							.flatMap(ex -> ex.asInstanceOfEntityTemplate().getMultiFillerSlot("hasTreatmentType")
									.getSlotFiller().stream())
							.map(t -> t.asInstanceOfEntityTemplate()).collect(Collectors.toSet()));
			List<EntityTemplate> predictedAnnotations = new ArrayList<>(
					e.getValue().getCurrentPredictions().getAnnotations().stream()
							.flatMap(ex -> ex.asInstanceOfEntityTemplate().getMultiFillerSlot("hasTreatmentType")
									.getSlotFiller().stream())
							.map(t -> t.asInstanceOfEntityTemplate()).collect(Collectors.toSet()));

			i++;
			log.info(e.getKey().getName());

			List<Integer> bestAssignment = ((CartesianEvaluator) predictionObjectiveFunction.getEvaluator())
					.getBestAssignment(goldAnnotations, predictedAnnotations);

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

	enum ESimpleEvaluationMode {
		/**
		 * Only vehicle
		 */
		VEHICLE,
		/**
		 * ONLY NON_VEHICLE
		 */
		NON_VEHICLE,
		/**
		 * 
		 */
		BOTH;
	}

	private Score simpleExpGroupEvaluate(boolean print, AbstractEvaluator evaluator, List<Integer> bestAssignment,
			List<EntityTemplate> goldAnnotations, List<EntityTemplate> predictedAnnotationsBaseline,
			ESimpleEvaluationMode mode) {
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
			List<AbstractAnnotation> goldTreatmentsCompare = new ArrayList<>(
					goldAnnotations.get(goldIndex).getMultiFillerSlot("hasTreatmentType").getSlotFiller());

			List<AbstractAnnotation> predictTreatmentsCompare = new ArrayList<>(predictedAnnotationsBaseline
					.get(predictIndex).getMultiFillerSlot("hasTreatmentType").getSlotFiller());

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
						.filter(a -> a.getEntityType() == EntityType.get("CompoundTreatment"))
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList());
				predictTreatments = predictTreatments.stream()
						.filter(a -> a.getEntityType() == EntityType.get("CompoundTreatment"))
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList());
				goldTreatmentsCompare.removeAll(goldTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == EntityType.get("CompoundTreatment"))
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList()));
				predictTreatmentsCompare.removeAll(predictTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == EntityType.get("CompoundTreatment"))
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList()));
				break;
			}
			case NON_VEHICLE: {
				goldTreatments.removeAll(goldTreatments.stream()
						.filter(a -> a.getEntityType() == EntityType.get("CompoundTreatment"))
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList()));
				predictTreatments.removeAll(predictTreatments.stream()
						.filter(a -> a.getEntityType() == EntityType.get("CompoundTreatment"))
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList()));
				goldTreatmentsCompare = goldTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == EntityType.get("CompoundTreatment"))
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList());
				predictTreatmentsCompare = predictTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == EntityType.get("CompoundTreatment"))
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
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
			List<AbstractAnnotation> goldOrganismModel = Arrays
					.asList(goldAnnotations.get(goldIndex).getSingleFillerSlot("hasOrganismModel").getSlotFiller())
					.stream().filter(a -> a != null).collect(Collectors.toList());
			List<AbstractAnnotation> predictOrganismModel = Arrays.asList(predictedAnnotationsBaseline.get(predictIndex)
					.getSingleFillerSlot("hasOrganismModel").getSlotFiller()).stream().filter(a -> a != null)
					.collect(Collectors.toList());
			simpleScore.add(evaluator.scoreMultiValues(goldOrganismModel, predictOrganismModel));

			/*
			 * InjuryModel
			 */
			List<AbstractAnnotation> goldInjuryModel = Arrays
					.asList(goldAnnotations.get(goldIndex).getSingleFillerSlot("hasInjuryModel").getSlotFiller())
					.stream().filter(a -> a != null).collect(Collectors.toList());
			List<AbstractAnnotation> predictInjuryModel = Arrays.asList(predictedAnnotationsBaseline.get(predictIndex)
					.getSingleFillerSlot("hasInjuryModel").getSlotFiller()).stream().filter(a -> a != null)
					.collect(Collectors.toList());

			simpleScore.add(evaluator.scoreMultiValues(goldInjuryModel, predictInjuryModel));

		}

		return simpleScore;
	}

	private Score simpleTreatmentEvaluate(boolean print, AbstractEvaluator evaluator, List<Integer> bestAssignment,
			List<EntityTemplate> goldTreatments, List<EntityTemplate> predictTreatments, ESimpleEvaluationMode mode) {
		Score simpleScore = new Score();

		for (int goldIndex = 0; goldIndex < bestAssignment.size(); goldIndex++) {
			final int predictIndex = bestAssignment.get(goldIndex);
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
			List<AbstractAnnotation> goldTreatmentsCompare = new ArrayList<>(goldTreatments);

			List<AbstractAnnotation> predictTreatmentsCompare = new ArrayList<>(predictTreatments);

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
						.filter(a -> a.getEntityType() == EntityType.get("CompoundTreatment"))
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList());
				predictTreatments = predictTreatments.stream()
						.filter(a -> a.getEntityType() == EntityType.get("CompoundTreatment"))
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList());
				goldTreatmentsCompare.removeAll(goldTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == EntityType.get("CompoundTreatment"))
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList()));
				predictTreatmentsCompare.removeAll(predictTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == EntityType.get("CompoundTreatment"))
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList()));
				break;
			}
			case NON_VEHICLE: {
				goldTreatments.removeAll(goldTreatments.stream()
						.filter(a -> a.getEntityType() == EntityType.get("CompoundTreatment"))
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList()));
				predictTreatments.removeAll(predictTreatments.stream()
						.filter(a -> a.getEntityType() == EntityType.get("CompoundTreatment"))
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList()));
				goldTreatmentsCompare = goldTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == EntityType.get("CompoundTreatment"))
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(EntityType.get("Vehicle")))
						.collect(Collectors.toList());
				predictTreatmentsCompare = predictTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == EntityType.get("CompoundTreatment"))
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
								.getSlotFiller().getEntityType() == EntityType.get("Vehicle")
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasCompound"))
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

	private Map<Instance, List<DocumentLinkedAnnotation>> returnEmptyMap(List<Instance> instances) {

		Map<Instance, List<DocumentLinkedAnnotation>> map = new HashMap<>();
		for (Instance instance : instances)
			map.put(instance, Collections.emptyList());
		return map;
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

	private Map<Instance, List<DocumentLinkedAnnotation>> annotateGroupNamesWithNPCHunksAndPattern(
			List<Instance> instances) {

		Map<Instance, List<DocumentLinkedAnnotation>> annotations = new HashMap<>();

		for (Instance instance : instances) {
			final List<DocumentLinkedAnnotation> findings = new ArrayList<>();
			extractWithNPCHunks(instance, findings);
			extractWithPattern(instance, findings);
			annotations.put(instance, findings);
		}

		return annotations;
	}

	private void extractWithNPCHunks(Instance instance, final List<DocumentLinkedAnnotation> findings) {
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
						findings.add(annotation);

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
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

	private Map<Instance, List<DocumentLinkedAnnotation>> annotateGroupNamesWithNPCHunks(List<Instance> instances) {

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
}
