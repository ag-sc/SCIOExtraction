package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;

import de.hterhors.semanticmr.candprov.helper.DictionaryFromInstanceHelper;
import de.hterhors.semanticmr.candprov.sf.AnnotationCandidateRetrievalCollection;
import de.hterhors.semanticmr.candprov.sf.GeneralCandidateProvider;
import de.hterhors.semanticmr.candprov.sf.ICandidateProvider;
import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.SemanticParsingCRF;
import de.hterhors.semanticmr.crf.exploration.IExplorationStrategy;
import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer;
import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.model.Model;
import de.hterhors.semanticmr.crf.of.IObjectiveFunction;
import de.hterhors.semanticmr.crf.of.SlotFillingObjectiveFunction;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.stopcrit.ISamplingStoppingCriterion;
import de.hterhors.semanticmr.crf.sampling.stopcrit.impl.ConverganceCrit;
import de.hterhors.semanticmr.crf.sampling.stopcrit.impl.MaxChainLengthCrit;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JsonNerlaProvider;
import de.hterhors.semanticmr.nerla.NerlaCollector;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;

public abstract class AbstractSlotFillingPredictor extends AbstractSemReadProject {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	public SemanticParsingCRF crf;

	protected final List<Instance> trainingInstances;
	protected final List<Instance> developmentInstances;
	protected final List<Instance> testInstances;

	public final String modelName;
	public final Map<String, Set<AbstractAnnotation>> annotations = new HashMap<>();
	private final IObjectiveFunction trainingObjectiveFunction = new SlotFillingObjectiveFunction(
			new CartesianEvaluator(EEvaluationDetail.DOCUMENT_LINKED));

	private final IObjectiveFunction predictionObjectiveFunction = new SlotFillingObjectiveFunction(
			new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE));
	protected final InstanceProvider instanceProvider;

	protected final List<String> trainingInstanceNames;
	protected final List<String> developInstanceNames;
	protected final List<String> testInstanceNames;

	private final ISamplingStoppingCriterion maxStepCrit;
	private final ISamplingStoppingCriterion noModelChangeCrit;
	private final ISamplingStoppingCriterion[] sampleStoppingCrits;

	private final List<IExplorationStrategy> explorerList;

	public AbstractSlotFillingPredictor(String modelName, SystemScope scope, List<String> trainingInstanceNames,
			List<String> developInstanceNames, List<String> testInstanceNames) {
		super(scope);
		this.modelName = modelName;
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
		InstanceProvider.maxNumberOfAnnotations = CartesianEvaluator.MAXIMUM_PERMUTATION_SIZE;

		/**
		 * And remove all instances that exceeds the maximum number.
		 */
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		/**
		 * The instance provider reads all json files in the given directory. We can set
		 * the distributor in the constructor. If not all instances should be read from
		 * the file system, we can add an additional parameter that specifies how many
		 * instances should be read. NOTE: in contrast to the corpusSizeFraction in the
		 * ShuffleCorpusDistributor, we initially set a limit to the number of files
		 * that should be read.
		 */
		instanceProvider = new InstanceProvider(getInstanceDirectory(), corpusDistributor);

		trainingInstances = instanceProvider.getRedistributedTrainingInstances();
		developmentInstances = instanceProvider.getRedistributedDevelopmentInstances();
		testInstances = instanceProvider.getRedistributedTestInstances();

		NerlaCollector nerlaProvider = new NerlaCollector(instanceProvider.getInstances());
		nerlaProvider.addNerlaProvider(new JsonNerlaProvider(getExternalNerlaAnnotations()));
		AnnotationCandidateRetrievalCollection candidateRetrieval = nerlaProvider.collect();

		for (ICandidateProvider ap : getAdditionalCandidateProvider()) {
			candidateRetrieval.registerCandidateProvider(ap);
		}

		Map<EntityType, Set<String>> trainDictionary = DictionaryFromInstanceHelper.toDictionary(trainingInstances);

		for (Instance instance : trainingInstances) {
			GeneralCandidateProvider ap = new GeneralCandidateProvider(instance);

			for (AbstractAnnotation annotation : DictionaryFromInstanceHelper.getAnnotationsForInstance(instance,
					trainDictionary)) {
				ap.addSlotFiller(annotation);
			}
			candidateRetrieval.registerCandidateProvider(ap);
		}

		/**
		 * For the slot filling problem, the SlotFillingExplorer is added to perform
		 * changes during the exploration. This explorer is especially designed for slot
		 * filling and is parameterized with a candidate retrieval and the
		 * constraintsProvider.
		 */
		SlotFillingExplorer explorer = new SlotFillingExplorer(predictionObjectiveFunction, candidateRetrieval);

		explorerList = Arrays.asList(explorer);

		maxStepCrit = new MaxChainLengthCrit(10);
		noModelChangeCrit = new ConverganceCrit(3 * explorerList.size(), s -> s.getModelScore());
		sampleStoppingCrits = new ISamplingStoppingCriterion[] { maxStepCrit, noModelChangeCrit };
	}

	protected List<? extends ICandidateProvider> getAdditionalCandidateProvider() {
		return Collections.emptyList();
	}

	abstract protected File getExternalNerlaAnnotations();

	abstract protected File getInstanceDirectory();

	abstract protected AdvancedLearner getLearner();

	public Score computeCoverageOnTrainingInstances(boolean detailedLog) {
		return new SemanticParsingCRF(new Model(getFeatureTemplates(), getModelBaseDir(), modelName), explorerList,
				getSampler(), getStateInitializer(), trainingObjectiveFunction).computeCoverage(detailedLog,
						trainingObjectiveFunction, trainingInstances);
	}

	public Score computeCoverageOnDevelopmentInstances(boolean detailedLog) {
		return new SemanticParsingCRF(new Model(getFeatureTemplates(), getModelBaseDir(), modelName), explorerList,
				getSampler(), getStateInitializer(), trainingObjectiveFunction).computeCoverage(detailedLog,
						predictionObjectiveFunction, developmentInstances);
	}

	public Score computeCoverageOnTestInstances(boolean detailedLog) {
		return new SemanticParsingCRF(new Model(getFeatureTemplates(), getModelBaseDir(), modelName), explorerList,
				getSampler(), getStateInitializer(), trainingObjectiveFunction).computeCoverage(detailedLog,
						predictionObjectiveFunction, testInstances);
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

		model.setParameter(getFeatureTemplateParameters());

		/**
		 * Create a new semantic parsing CRF and initialize with needed parameter.
		 */
		crf = new SemanticParsingCRF(model, explorerList, getSampler(), getStateInitializer(),
				trainingObjectiveFunction);
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
		}
	}

	protected abstract List<AbstractFeatureTemplate<?>> getFeatureTemplates();

	protected Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> getFeatureTemplateParameters() {
		return Collections.emptyMap();
	}

	protected abstract int getNumberOfEpochs();

	protected abstract IStateInitializer getStateInitializer();

	protected abstract AbstractSampler getSampler();

	protected abstract File getModelBaseDir();

	final public Map<String, Set<AbstractAnnotation>> predictBatchHighRecallInstanceByNames(Set<String> names, int n) {

		List<String> remainingNames = names.stream().filter(name -> !annotations.containsKey(name))
				.collect(Collectors.toList());

		Map<Instance, State> results = crf.predictHighRecall(instanceProvider.getInstances().stream()
				.filter(i -> remainingNames.contains(i.getName())).collect(Collectors.toList()), n, maxStepCrit,
				noModelChangeCrit);

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
			results = crf.predictHighRecall(instanceProvider.getInstances().stream()
					.filter(i -> i.getName().equals(name)).collect(Collectors.toList()), n, maxStepCrit,
					noModelChangeCrit);
		} else {
			results = crf.predict(instanceProvider.getInstances().stream().filter(i -> i.getName().equals(name))
					.collect(Collectors.toList()), maxStepCrit, noModelChangeCrit);
		}
		for (Entry<Instance, State> result : results.entrySet()) {

			for (AbstractAnnotation aa : result.getValue().getCurrentPredictions().getAnnotations()) {

				annotations.putIfAbsent(result.getKey().getName(), new HashSet<>());
				annotations.get(name).add(aa.asInstanceOfEntityTemplate());

			}

		}
		return annotations.getOrDefault(name, Collections.emptySet());
	}

	final public void evaluateOnDevelopment() {

		Map<Instance, State> results = crf.predict(instanceProvider.getRedistributedDevelopmentInstances(), maxStepCrit,
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

	final public Map<String, Set<AbstractAnnotation>> predictAllInstances(int n) {

		return predictBatchHighRecallInstanceByNames(Streams
				.concat(trainingInstances.stream(),
						Streams.concat(developmentInstances.stream(), testInstances.stream()))
				.map(i -> i.getName()).collect(Collectors.toSet()), n);
	}
}