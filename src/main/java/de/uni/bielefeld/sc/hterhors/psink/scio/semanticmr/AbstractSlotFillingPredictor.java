package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr;

import java.io.File;
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
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;

import de.hterhors.semanticmr.candidateretrieval.helper.DictionaryFromInstanceHelper;
import de.hterhors.semanticmr.candidateretrieval.sf.SlotFillingCandidateRetrieval.IFilter;
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
import de.hterhors.semanticmr.crf.model.Model;
import de.hterhors.semanticmr.crf.of.IObjectiveFunction;
import de.hterhors.semanticmr.crf.of.SlotFillingObjectiveFunction;
import de.hterhors.semanticmr.crf.sampling.AbstractBeamSampler;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
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
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.Instance.DeduplicationRule;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation.ESection;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.initializer.GenericMultiCardinalityInitializer;

public abstract class AbstractSlotFillingPredictor extends AbstractSemReadProject {
	public interface IModificationRule {

	}

	public ENERModus modus;

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	public ISemanticParsingCRF crf;

	protected final List<Instance> trainingInstances;
	protected final List<Instance> developmentInstances;
	protected final List<Instance> testInstances;

	public final String modelName;
	public final Map<String, Set<AbstractAnnotation>> annotations = new HashMap<>();

//	private final IObjectiveFunction trainingObjectiveFunction = new SlotFillingObjectiveFunction(EScoreType.MICRO,
//			new GreedySearchEvaluator(EEvaluationDetail.DOCUMENT_LINKED));
//
//	private final IObjectiveFunction predictionObjectiveFunction = new SlotFillingObjectiveFunction(EScoreType.MICRO,
//			new GreedySearchEvaluator(EEvaluationDetail.ENTITY_TYPE));

//	public final IObjectiveFunction trainingObjectiveFunction = new SlotFillingObjectiveFunction(EScoreType.MICRO,
//			new BeamSearchEvaluator(EEvaluationDetail.DOCUMENT_LINKED, 10));
//
//	public final IObjectiveFunction predictionObjectiveFunction = new SlotFillingObjectiveFunction(EScoreType.MICRO,
//			new BeamSearchEvaluator(EEvaluationDetail.ENTITY_TYPE, 10));

	public final IObjectiveFunction trainingObjectiveFunction = new SlotFillingObjectiveFunction(EScoreType.MICRO,
			new CartesianEvaluator(EEvaluationDetail.DOCUMENT_LINKED));

	public final IObjectiveFunction predictionObjectiveFunction = new SlotFillingObjectiveFunction(EScoreType.MICRO,
			new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE, EEvaluationDetail.ENTITY_TYPE));

	public final InstanceProvider instanceProvider;

	protected final List<String> trainingInstanceNames;
	protected final List<String> developInstanceNames;
	protected final List<String> testInstanceNames;

	public final ISamplingStoppingCriterion maxStepCrit;
	public final ISamplingStoppingCriterion noModelChangeCrit;
	public final ISamplingStoppingCriterion[] sampleStoppingCrits;

	private final List<IExplorationStrategy> explorerList;

	public List<Instance> getTrainingInstances() {
		return Collections.unmodifiableList(trainingInstances);
	}

	public List<Instance> getDevelopmentInstances() {
		return Collections.unmodifiableList(developmentInstances);
	}

	public List<Instance> getTestInstances() {
		return Collections.unmodifiableList(testInstances);
	}

	public AbstractSlotFillingPredictor(String modelName, List<String> trainingInstanceNames,
			List<String> developInstanceNames, List<String> testInstanceNames, IModificationRule modificationRule,
			ENERModus modus) {
		this.modelName = modelName;
		this.modus = modus;
		this.trainingInstanceNames = trainingInstanceNames;
		this.developInstanceNames = developInstanceNames;
		this.testInstanceNames = testInstanceNames;

		AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
				.setTrainingInstanceNames(trainingInstanceNames).setDevelopInstanceNames(developInstanceNames)
				.setTestInstanceNames(testInstanceNames).build();

		/**
		 * Remove empty instances from corpus
		 */
		InstanceProvider.removeEmptyInstances = true;

		/**
		 * Set maximum to maximum of Cartesian evaluator (8)
		 */
		InstanceProvider.maxNumberOfAnnotations = 8;

//		InstanceProvider.maxNumberOfAnnotations = CartesianEvaluator.MAXIMUM_PERMUTATION_SIZE;

		/**
		 * And remove all instances that exceeds the maximum number.
		 */
		InstanceProvider.removeInstancesWithToManyAnnotations = false;

		DeduplicationRule deduplicationRule = (a1, a2) -> {
			return a1.evaluateEquals(predictionObjectiveFunction.getEvaluator(), a2);
		};
		/**
		 * The instance provider reads all json files in the given directory. We can set
		 * the distributor in the constructor. If not all instances should be read from
		 * the file system, we can add an additional parameter that specifies how many
		 * instances should be read. NOTE: in contrast to the corpusSizeFraction in the
		 * ShuffleCorpusDistributor, we initially set a limit to the number of files
		 * that should be read.
		 */

		Collection<GoldModificationRule> goldModificationRules = new ArrayList<>();

		goldModificationRules.add(a -> {
			if (a.isInstanceOfEntityTemplate() && a.asInstanceOfEntityTemplate().isEmpty()
					&& a.getEntityType().getTransitiveClosureSuperEntityTypes().isEmpty())
				return null;
			return a;
		});

		goldModificationRules.addAll(getGoldModificationRules(modificationRule));

		instanceProvider = new InstanceProvider(getInstanceDirectory(), corpusDistributor, goldModificationRules,
				deduplicationRule);

		trainingInstances = instanceProvider.getTrainingInstances();
		developmentInstances = instanceProvider.getDevelopmentInstances();
		testInstances = instanceProvider.getTestInstances();

		JSONNerlaReader nerlaJSONReader = new JSONNerlaReader(getExternalNerlaFile());

		for (Instance instance : instanceProvider.getInstances()) {
			instance.addCandidateAnnotations(nerlaJSONReader.getForInstance(instance));
		}
		Map<EntityType, Set<String>> trainDictionary = DictionaryFromInstanceHelper.toDictionary(trainingInstances);

		for (Instance instance : instanceProvider.getInstances()) {
			instance.addCandidateAnnotations(
					DictionaryFromInstanceHelper.getAnnotationsForInstance(instance, trainDictionary));
		}

		for (Entry<Instance, Collection<AbstractAnnotation>> ap : getAdditionalCandidateProvider(modificationRule)
				.entrySet()) {
			ap.getKey().addCandidateAnnotations(ap.getValue());
		}

//		IFilter goldFilter = new IFilter() {
//
//			Map<Document, Map<Integer, Classification>> cache = buildCache();
//
//			@Override
//			public boolean remove(AbstractAnnotation candidate) {
//
//				if (!candidate.isInstanceOfDocumentLinkedAnnotation())
//					return false;
//
//				Document doc = candidate.asInstanceOfDocumentLinkedAnnotation().document;
//
//				Map<Integer, Classification> classification = cache.get(doc);
//
//				if (classification == null) {
//					return false;
//				}
//
//				if (!classification.get(candidate.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex()).isRelevant)
//					return true;
//
//				return false;
//			}
//
//			private Map<Document, Map<Integer, Classification>> buildCache() {
//				Map<Document, Map<Integer, Classification>> cache = new HashMap<>();
//				Map<Integer, Classification> classification;
//				
////				for (Instance instance : instanceProvider.getInstances()) {
////				for (Instance instance : developmentInstances) {
//				for (Instance instance : trainingInstances) {
//					if ((classification = cache.get(instance.getDocument())) == null) {
//						classification = new HashMap<>();
//
//						Set<Integer> ints = new HashSet<>();
//
//						for (EntityTemplate gold : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {
//
//							Collection<Set<AbstractAnnotation>> x = gold.filter().docLinkedAnnoation().merge()
//									.rootAnnotation().singleSlots().multiSlots().nonEmpty().build()
//									.getMergedAnnotations().values();
//
//							for (Set<AbstractAnnotation> string : x) {
//								for (AbstractAnnotation a : string) {
//									if (!a.isInstanceOfDocumentLinkedAnnotation())
//										continue;
//									ints.add(a.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex());
//								}
//							}
//
//						}
//						for (int i = 0; i < instance.getDocument().getNumberOfSentences(); i++) {
//							if (ints.contains(i))
//								classification.put(i, new Classification(i, true, 1));
//							else
//								classification.put(i, new Classification(i, false, 1));
//						}
//
//						cache.put(instance.getDocument(), classification);
//					}
//				}
//				return cache;
//			}
//
//		};

//		IFilter predictFilter = new IFilter() {
//
//			SentenceClassificationWEKA senClassification = new SentenceClassificationWEKA(trainingInstances);
//
//			Map<Document, Map<Integer, Classification>> cache = new HashMap<>();
//
//			@Override
//			public boolean remove(AbstractAnnotation candidate) {
//
//				if (!candidate.isInstanceOfDocumentLinkedAnnotation())
//					return false;
//
//				Document doc = candidate.asInstanceOfDocumentLinkedAnnotation().document;
//
//				Map<Integer, Classification> classification;
//
//				if ((classification = cache.get(doc)) == null) {
//					classification = senClassification.classifyDocument(doc);
//					cache.put(doc, classification);
//				}
//
//				if (candidate.isInstanceOfEntityTemplate()) {
//
//					Collection<Set<AbstractAnnotation>> x = candidate.asInstanceOfEntityTemplate().filter()
//							.docLinkedAnnoation().merge().rootAnnotation().singleSlots().multiSlots().nonEmpty().build()
//							.getMergedAnnotations().values();
//
//					boolean allRelevant = true;
//
//					for (Set<AbstractAnnotation> string : x) {
//						for (AbstractAnnotation a : string) {
//							allRelevant &= classification
//									.get(a.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex()).isRelevant;
//							if (!allRelevant) {
//								return true;
//							}
//						}
//					}
//
//				} else if (candidate.isInstanceOfDocumentLinkedAnnotation()) {
//					if (!classification
//							.get(candidate.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex()).isRelevant)
//						return true;
//				}
//
//				return false;
//			}
//
//		};

		IFilter sectionFilter = new IFilter() {

			Map<Document, AutomatedSectionifcation> cache = new HashMap<>();
			{

				for (Instance instance : instanceProvider.getInstances()) {
					cache.put(instance.getDocument(), AutomatedSectionifcation.getInstance(instance));
				}

			}

			@Override
			public boolean remove(AbstractAnnotation candidate) {

				if (!candidate.isInstanceOfDocumentLinkedAnnotation())
					return false;

				Document doc = candidate.asInstanceOfDocumentLinkedAnnotation().document;

				AutomatedSectionifcation sectionification = cache.get(doc);

				if (candidate.isInstanceOfEntityTemplate()) {

					Collection<Set<AbstractAnnotation>> x = candidate.asInstanceOfEntityTemplate().filter()
							.docLinkedAnnoation().merge().rootAnnotation().singleSlots().multiSlots().nonEmpty().build()
							.getMergedAnnotations().values();

					boolean allRelevant = true;

					for (Set<AbstractAnnotation> string : x) {
						for (AbstractAnnotation a : string) {
							if (a.isInstanceOfDocumentLinkedAnnotation()) {
								allRelevant &= isRelevant(sectionification, a.asInstanceOfDocumentLinkedAnnotation());
								if (!allRelevant) {
									return true;
								}
							}
						}
					}

				} else if (candidate.isInstanceOfDocumentLinkedAnnotation()) {
					if (!isRelevant(sectionification, candidate.asInstanceOfDocumentLinkedAnnotation()))
						return true;
				}

				return false;
			}

			private boolean isRelevant(AutomatedSectionifcation sectionification, DocumentLinkedAnnotation a) {
				ESection sec = sectionification.getSection(a.asInstanceOfDocumentLinkedAnnotation());
				return sec == ESection.METHODS || sec == ESection.UNKNOWN;
//				return sec != ESection.REFERENCES;
//				return true;
			}

		};

		for (Instance instance : instanceProvider.getInstances()) {
//			instance.removeCandidateAnnotation(predictFilter);
//			instance.removeCandidateAnnotation(goldFilter);
//			instance.removeCandidateAnnotation(sectionFilter);
			instance.removeCandidateAnnotation(new IFilter() {

				@Override
				public boolean remove(AbstractAnnotation candidate) {
					return !trainDictionary.keySet().contains(candidate.getEntityType());
				}

			});
		}

		HardConstraintsProvider prov = getHardConstraints();

//		prov.addHardConstraints(new AbstractHardConstraint() {
//			
//			Set<Instance> trainInstancesSet = new HashSet<>(trainingInstances);
//			
//			SentenceClassificationWEKA senClassification = new SentenceClassificationWEKA(trainingInstances);
//			Map<Instance, Map<Integer, Classification>> cache = new HashMap<>();
//			
//			@Override
//			public boolean violatesConstraint(State currentState, EntityTemplate entityTemplate) {
//				Map<Integer, Classification> classification;
//				if (isTrainingInstance(currentState.getInstance()))
//					return false;
//				if ((classification = cache.get(currentState.getInstance())) == null) {
//					classification = senClassification.classify(currentState.getInstance(), new Score());
//					cache.put(currentState.getInstance(), classification);
//				}
//				
//				Collection<Set<AbstractAnnotation>> x = entityTemplate.filter().docLinkedAnnoation().merge()
//						.rootAnnotation().singleSlots().multiSlots().nonEmpty().build().getMergedAnnotations().values();
//				
//				boolean allRelevant = true;
//				
//				for (Set<AbstractAnnotation> string : x) {
//					for (AbstractAnnotation a : string) {
//						allRelevant &= classification
//								.get(a.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex()).isRelevant;
//						if (!allRelevant) {
//							return true;
//						}
//					}
//				}
//				
//				return false;
//			}
//			
//			private boolean isTrainingInstance(Instance instance) {
//				return trainInstancesSet.contains(instance);
//			}
//		});

		/**
		 * For the slot filling problem, the SlotFillingExplorer is added to perform
		 * changes during the exploration. This explorer is especially designed for slot
		 * filling and is parameterized with a candidate retrieval and the
		 * constraintsProvider.
		 */
		SlotFillingExplorer explorer = new SlotFillingExplorer(predictionObjectiveFunction, prov);

		explorerList = new ArrayList<>(Arrays.asList(explorer));
		explorerList.addAll(getAdditionalExplorer());

		maxStepCrit = new MaxChainLengthCrit(100);
		noModelChangeCrit = new ConverganceCrit(3 * explorerList.size(), s -> s.getModelScore());
		sampleStoppingCrits = new ISamplingStoppingCriterion[] { maxStepCrit, noModelChangeCrit };

	}

	public List<IExplorationStrategy> getAdditionalExplorer() {
		return Collections.emptyList();
	}

	public HardConstraintsProvider getHardConstraints() {
		return new HardConstraintsProvider();
	}

	protected Map<Instance, Collection<AbstractAnnotation>> getAdditionalCandidateProvider(IModificationRule rule) {
		return Collections.emptyMap();
	}

	abstract protected File getExternalNerlaFile();

	abstract protected File getInstanceDirectory();

	abstract protected AdvancedLearner getLearner();

	public Score computeCoverageOnTrainingInstances(boolean detailedLog) {

		Score meanTrainOFScore = new Score();
		for (Entry<Instance, State> finalState : new SemanticParsingCRF(
				new Model(getFeatureTemplates(), getModelBaseDir(), modelName), explorerList, getSampler(),
				getStateInitializer(), trainingObjectiveFunction)
						.computeCoverage(detailedLog, trainingObjectiveFunction, trainingInstances).entrySet()) {
			trainingObjectiveFunction.score(finalState.getValue());
			if (detailedLog)
				log.info(
						finalState.getKey().getName().substring(0, Math.min(finalState.getKey().getName().length(), 10))
								+ "... \t"
								+ SemanticParsingCRF.SCORE_FORMAT.format(finalState.getValue().getObjectiveScore()));
			meanTrainOFScore.add(finalState.getValue().getMicroScore());
		}

		return meanTrainOFScore;

	}

	public Score computeCoverageOnDevelopmentInstances(boolean detailedLog) {
		Score meanTrainOFScore = new Score();
		for (Entry<Instance, State> finalState : new SemanticParsingCRF(
				new Model(getFeatureTemplates(), getModelBaseDir(), modelName), explorerList, getSampler(),
				getStateInitializer(), predictionObjectiveFunction)
						.computeCoverage(detailedLog, predictionObjectiveFunction, developmentInstances).entrySet()) {
			predictionObjectiveFunction.score(finalState.getValue());
			if (detailedLog)
				log.info(
						finalState.getKey().getName().substring(0, Math.min(finalState.getKey().getName().length(), 10))
								+ "... \t"
								+ SemanticParsingCRF.SCORE_FORMAT.format(finalState.getValue().getObjectiveScore()));
			meanTrainOFScore.add(finalState.getValue().getMicroScore());
		}

		return meanTrainOFScore;

	}

	public Map<Instance, State> coverageOnDevelopmentInstances(EntityType type, boolean detailedLog) {
		List<IExplorationStrategy> explorer = new ArrayList<>(explorerList);
		explorer.addAll(getCoverageAdditionalExplorer(type));

		Map<Instance, State> coverage = new SemanticParsingCRF(
				new Model(getFeatureTemplates(), getModelBaseDir(), modelName), explorer, getSampler(),
				getCoverageStateInitializer(type), predictionObjectiveFunction).computeCoverage(detailedLog,
						predictionObjectiveFunction, developmentInstances);

		for (Entry<Instance, State> res : coverage.entrySet()) {

			for (Iterator<AbstractAnnotation> iterator = res.getValue().getCurrentPredictions().getAnnotations()
					.iterator(); iterator.hasNext();) {
				AbstractAnnotation a = iterator.next();
				if (a.isInstanceOfEntityTemplate() && a.asInstanceOfEntityTemplate().isEmpty()
						&& a.getEntityType().getTransitiveClosureSuperEntityTypes().isEmpty()) {
					iterator.remove();
				}
			}
		}
		return coverage;
	}

	private IStateInitializer getCoverageStateInitializer(EntityType type) {
		return (instance -> {
			return new State(instance, new Annotations(new EntityTemplate(type)));
		});
	}

	private List<IExplorationStrategy> getCoverageAdditionalExplorer(EntityType type) {
		return Arrays.asList(new RootTemplateCardinalityExplorer(predictionObjectiveFunction.getEvaluator(),
				EExplorationMode.ANNOTATION_BASED, AnnotationBuilder.toAnnotation(type)));
	}

	public Score computeCoverageOnTestInstances(boolean detailedLog) {
		Score meanTrainOFScore = new Score();
		for (Entry<Instance, State> finalState : new SemanticParsingCRF(
				new Model(getFeatureTemplates(), getModelBaseDir(), modelName), explorerList, getSampler(),
				getStateInitializer(), predictionObjectiveFunction)
						.computeCoverage(detailedLog, predictionObjectiveFunction, testInstances).entrySet()) {
			predictionObjectiveFunction.score(finalState.getValue());
			if (detailedLog)
				log.info(
						finalState.getKey().getName().substring(0, Math.min(finalState.getKey().getName().length(), 10))
								+ "... \t"
								+ SemanticParsingCRF.SCORE_FORMAT.format(finalState.getValue().getObjectiveScore()));
			meanTrainOFScore.add(finalState.getValue().getMicroScore());
		}

		return meanTrainOFScore;
	}

	public void trainOrLoadModel() {
		Model model;

		if (Model.exists(getModelBaseDir(), modelName)) {
			/**
			 * If the model exists load from the file system.
			 */
			model = Model.load(getModelBaseDir(), modelName);
		} else {
			/**
			 * If the model does not exists, create a new model.
			 */
			model = new Model(getFeatureTemplates(), getModelBaseDir(), modelName);
		}

		model.setFeatureTemplateParameter(getFeatureTemplateParameters());

		/**
		 * Create a new semantic parsing CRF and initialize with needed parameter.
		 */

		IStateInitializer initializer = getStateInitializer();
		if (initializer instanceof GenericMultiCardinalityInitializer) {
			crf = new SemanticParsingCRFMultiState(model, explorerList, getBeamSampler(), trainingObjectiveFunction);
			((SemanticParsingCRFMultiState) crf).parallelModelUpdate = true;

			crf.setInitializer(initializer);
		} else {
			crf = new SemanticParsingCRF(model, explorerList, getSampler(), initializer, trainingObjectiveFunction);
			crf.setInitializer(initializer);

//			log.info("Training instances coverage: "
//					+ ((SemanticParsingCRF) crf).computeCoverage(true, predictionObjectiveFunction, trainingInstances));
//
//			log.info("Test instances coverage: "
//					+ ((SemanticParsingCRF) crf).computeCoverage(true, predictionObjectiveFunction, testInstances));
		}

		/**
		 * If the model was loaded from the file system, we do not need to train it.
		 */
		if (!model.isTrained()) {
			/**
			 * Train the CRF.
			 */
			crf.train(getLearner(), trainingInstances, getNumberOfEpochs(), sampleStoppingCrits);

			/**
			 * Save the model as binary. Do not override, in case a file already exists for
			 * that name.
			 */
			model.save();

			/**
			 * Print the model in a readable format.
			 */
			model.printReadable();
			log.info(crf.getTrainingStatistics());
		}
	}

	protected abstract List<AbstractFeatureTemplate<?>> getFeatureTemplates();

	protected Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> getFeatureTemplateParameters() {
		return Collections.emptyMap();
	}

	protected abstract int getNumberOfEpochs();

	protected abstract IStateInitializer getStateInitializer();

	protected abstract AbstractSampler getSampler();

	protected AbstractBeamSampler getBeamSampler() {
		return new EpochSwitchBeamSampler(epoch -> epoch % 2 == 0);
	}

	protected abstract File getModelBaseDir();

	final public Map<String, Set<AbstractAnnotation>> predictBatchHighRecallInstanceByNames(Set<String> names, int n) {

		List<String> remainingNames = names.stream().filter(name -> !annotations.containsKey(name))
				.collect(Collectors.toList());

		Map<Instance, State> results = crf.predictHighRecall(instanceProvider.getInstances().stream()
				.filter(i -> remainingNames.contains(i.getName())).collect(Collectors.toList()), n, maxStepCrit,
				noModelChangeCrit);

		for (Entry<Instance, State> res : results.entrySet()) {
			for (Iterator<AbstractAnnotation> iterator = res.getValue().getCurrentPredictions().getAnnotations()
					.iterator(); iterator.hasNext();) {
				AbstractAnnotation a = iterator.next();
				if (a.isInstanceOfEntityTemplate() && a.asInstanceOfEntityTemplate().isEmpty()
						&& a.getEntityType().getTransitiveClosureSuperEntityTypes().isEmpty()) {
					iterator.remove();
				}
			}
		}

		for (Entry<Instance, State> result : results.entrySet()) {

			for (AbstractAnnotation aa : result.getValue().getCurrentPredictions().getAnnotations()) {

				annotations.putIfAbsent(result.getKey().getName(), new HashSet<>());
				annotations.get(result.getKey().getName()).add(aa.asInstanceOfEntityTemplate());

			}

		}

		final Map<String, Set<AbstractAnnotation>> batchAnnotations = new HashMap<>();
		for (String instanceName : names) {
			batchAnnotations.put(instanceName, annotations.getOrDefault(instanceName, Collections.emptySet()));
		}

		return batchAnnotations;
	}

	final public Set<AbstractAnnotation> predictHighRecallInstanceByName(String name, int n) {

		if (annotations.containsKey(name))
			return annotations.get(name);

		final Map<Instance, State> results;

		if (n == 1) {
			results = crf.predict(instanceProvider.getInstances().stream().filter(i -> i.getName().equals(name))
					.collect(Collectors.toList()), maxStepCrit, noModelChangeCrit);
		} else {
			results = ((SemanticParsingCRF) crf).predictHighRecall(instanceProvider.getInstances().stream()
					.filter(i -> i.getName().equals(name)).collect(Collectors.toList()), n, maxStepCrit,
					noModelChangeCrit);
		}
		for (Entry<Instance, State> res : results.entrySet()) {
			for (Iterator<AbstractAnnotation> iterator = res.getValue().getCurrentPredictions().getAnnotations()
					.iterator(); iterator.hasNext();) {
				AbstractAnnotation a = iterator.next();
				if (a.isInstanceOfEntityTemplate() && a.asInstanceOfEntityTemplate().isEmpty()
						&& a.getEntityType().getTransitiveClosureSuperEntityTypes().isEmpty()) {
					iterator.remove();
				}
			}
		}
		for (Entry<Instance, State> result : results.entrySet()) {

			for (AbstractAnnotation aa : result.getValue().getCurrentPredictions().getAnnotations()) {

				annotations.putIfAbsent(result.getKey().getName(), new HashSet<>());
				annotations.get(name).add(aa.asInstanceOfEntityTemplate());

			}

		}
		return annotations.getOrDefault(name, Collections.emptySet());
	}

	final public Map<Instance, State> evaluateOnDevelopment() {

		Map<Instance, State> results = crf.predict(instanceProvider.getDevelopmentInstances(), maxStepCrit,
				noModelChangeCrit);

		log.info(crf.getTestStatistics());
		for (Entry<Instance, State> res : results.entrySet()) {

			for (Iterator<AbstractAnnotation> iterator = res.getValue().getCurrentPredictions().getAnnotations()
					.iterator(); iterator.hasNext();) {
				AbstractAnnotation a = iterator.next();
				if (a.isInstanceOfEntityTemplate() && a.asInstanceOfEntityTemplate().isEmpty()
						&& a.getEntityType().getTransitiveClosureSuperEntityTypes().isEmpty()) {
					iterator.remove();
				}
			}
		}

		return results;

	}

	final public void evaluateOnTest() {

		Map<Instance, State> results = crf.predict(instanceProvider.getTestInstances(), maxStepCrit,
				noModelChangeCrit);

		AbstractSemReadProject.evaluate(log, results, predictionObjectiveFunction);

		log.info(crf.getTrainingStatistics());
		log.info(crf.getTestStatistics());
	}

	final public Set<AbstractAnnotation> predictInstanceByName(String name) {
		return predictHighRecallInstanceByName(name, 1);
	}

	final public Map<String, Set<AbstractAnnotation>> predictAllInstances() {

		return predictBatchHighRecallInstanceByNames(Streams
				.concat(trainingInstances.stream(),
						Streams.concat(developmentInstances.stream(), testInstances.stream()))
				.map(i -> i.getName()).collect(Collectors.toSet()), 1);
	}

	final public Map<Instance, Set<AbstractAnnotation>> predictInstances(List<Instance> instanceToPredict, int k) {
		Map<String, Instance> instanceNameMapToPredict = new HashMap<>();

		for (Instance instance : instanceToPredict) {
			instanceNameMapToPredict.put(instance.getName(), instance);
		}

		Map<String, Set<AbstractAnnotation>> annotations = predictBatchHighRecallInstanceByNames(
				new HashSet<>(instanceNameMapToPredict.keySet()), k);

		Map<Instance, Set<AbstractAnnotation>> predictions = new HashMap<>();

		for (Entry<String, Set<AbstractAnnotation>> pred : annotations.entrySet()) {
			Set<AbstractAnnotation> filteredSet = new HashSet<>();
			for (AbstractAnnotation aa : pred.getValue()) {

				if (getStateInitializer().getInitState(instanceNameMapToPredict.get(pred.getKey()))
						.getCurrentPredictions().getAnnotations().contains(aa))
					continue;

				filteredSet.add(aa);
			}
			predictions.put(instanceNameMapToPredict.get(pred.getKey()), filteredSet);
		}
		return predictions;
	}

	final public Map<String, Set<AbstractAnnotation>> predictAllInstances(int n) {

		return predictBatchHighRecallInstanceByNames(Streams
				.concat(trainingInstances.stream(),
						Streams.concat(developmentInstances.stream(), testInstances.stream()))
				.map(i -> i.getName()).collect(Collectors.toSet()), n);
	}

	protected Collection<GoldModificationRule> getGoldModificationRules(IModificationRule rule) {
		return Collections.emptyList();
	}

}
