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

import org.apache.jena.ext.com.google.common.collect.Streams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.candprov.nerla.INerlaCandidateProvider;
import de.hterhors.semanticmr.candprov.nerla.NerlaCandidateProviderCollection;
import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.SemanticParsingCRF;
import de.hterhors.semanticmr.crf.exploration.EntityRecLinkExplorer;
import de.hterhors.semanticmr.crf.exploration.IExplorationStrategy;
import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.model.Model;
import de.hterhors.semanticmr.crf.of.IObjectiveFunction;
import de.hterhors.semanticmr.crf.of.NerlaObjectiveFunction;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.stopcrit.ISamplingStoppingCriterion;
import de.hterhors.semanticmr.crf.sampling.stopcrit.impl.ConverganceCrit;
import de.hterhors.semanticmr.crf.sampling.stopcrit.impl.MaxChainLengthCrit;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;

public abstract class AbstractNERLPredictor extends AbstractSemReadProject {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	public SemanticParsingCRF crf;

	private final List<Instance> trainingInstances;
	private final List<Instance> developmentInstances;
	private final List<Instance> testInstances;

	public final String modelName;

	public final Map<String, Set<AbstractAnnotation>> annotations = new HashMap<>();

	private final IObjectiveFunction objectiveFunction = new NerlaObjectiveFunction(EEvaluationDetail.LITERAL);

	private final IObjectiveFunction evaluationObjectiveFunction = new NerlaObjectiveFunction(
			EEvaluationDetail.LITERAL);

	private final InstanceProvider instanceProvider;

	public AbstractNERLPredictor(String modelName, SystemScope scope, List<String> trainingInstanceNames,
			List<String> developInstanceNames, List<String> testInstanceNames) {

		super(scope);
		this.modelName = modelName;

		AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
				.setTrainingInstanceNames(trainingInstanceNames).setDevelopInstanceNames(developInstanceNames)
				.setTestInstanceNames(testInstanceNames).build();

		/**
		 * We chose to remove all empty instances from the corpus.
		 */
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.maxNumberOfAnnotations = 50;
		InstanceProvider.removeInstancesWithToManyAnnotations = false;

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

		NerlaCandidateProviderCollection candidateRetrieval = new NerlaCandidateProviderCollection();

		for (INerlaCandidateProvider ap : getAdditionalCandidateProvider()) {
			candidateRetrieval.registerCandidateProvider(ap);
		}

		/**
		 * For the entity recognition and linking problem, the EntityRecLinkExplorer is
		 * added to perform changes during the exploration. This explorer is especially
		 * designed for NERLA and is parameterized with a candidate retrieval.
		 */
		EntityRecLinkExplorer explorer = new EntityRecLinkExplorer(candidateRetrieval);
		explorer.MAX_WINDOW_SIZE = 4;

		explorerList = Arrays.asList(explorer);

		maxStepCrit = new MaxChainLengthCrit(50);
		noModelChangeCrit = new ConverganceCrit(3 * explorerList.size(), s -> s.getModelScore());
		sampleStoppingCrits = new ISamplingStoppingCriterion[] { maxStepCrit, noModelChangeCrit };
	}

	ISamplingStoppingCriterion maxStepCrit;
	ISamplingStoppingCriterion noModelChangeCrit;
	ISamplingStoppingCriterion[] sampleStoppingCrits;

	List<IExplorationStrategy> explorerList;

	abstract protected File getInstanceDirectory();

	abstract protected AdvancedLearner getLearner();

	public Score computeCoverageOnTrainingInstances(boolean detailedLog) {
		return new SemanticParsingCRF(new Model(getFeatureTemplates(), getModelBaseDir(), modelName), explorerList,
				getSampler(), getStateInitializer(), objectiveFunction).computeCoverage(detailedLog, objectiveFunction,
						trainingInstances);
	}

	public Score computeCoverageOnDevelopmentInstances(boolean detailedLog) {
		return new SemanticParsingCRF(new Model(getFeatureTemplates(), getModelBaseDir(), modelName), explorerList,
				getSampler(), getStateInitializer(), objectiveFunction).computeCoverage(detailedLog, objectiveFunction,
						developmentInstances);
	}

	public Score computeCoverageOnTestInstances(boolean detailedLog) {
		return new SemanticParsingCRF(new Model(getFeatureTemplates(), getModelBaseDir(), modelName), explorerList,
				getSampler(), getStateInitializer(), objectiveFunction).computeCoverage(detailedLog, objectiveFunction,
						testInstances);
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

		/**
		 * Create a new semantic parsing CRF and initialize with needed parameter.
		 */
		crf = new SemanticParsingCRF(model, explorerList, getSampler(), getStateInitializer(), objectiveFunction);
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

	protected List<? extends INerlaCandidateProvider> getAdditionalCandidateProvider() {
		return Collections.emptyList();
	}

	protected abstract List<AbstractFeatureTemplate<?>> getFeatureTemplates();

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
				annotations.get(result.getKey().getName()).add(aa.asInstanceOfDocumentLinkedAnnotation());

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

		Map<Instance, State> results = crf.predictHighRecall(instanceProvider.getInstances().stream()
				.filter(i -> name.equals(i.getName())).collect(Collectors.toList()), n, maxStepCrit, noModelChangeCrit);

		for (Entry<Instance, State> result : results.entrySet()) {

			for (AbstractAnnotation aa : result.getValue().getCurrentPredictions().getAnnotations()) {

				annotations.putIfAbsent(result.getKey().getName(), new HashSet<>());
				annotations.get(result.getKey().getName()).add(aa.asInstanceOfEntityTemplate());

			}

		}
		return annotations.getOrDefault(name, Collections.emptySet());
	}

	final public Map<String, Set<AbstractAnnotation>> predictBatchInstanceByNames(Set<String> names) {

		List<String> remainingNames = names.stream().filter(name -> !annotations.containsKey(name))
				.collect(Collectors.toList());

		Map<Instance, State> results = crf.predict(instanceProvider.getInstances().stream()
				.filter(i -> remainingNames.contains(i.getName())).collect(Collectors.toList()), maxStepCrit,
				noModelChangeCrit);

		for (Entry<Instance, State> result : results.entrySet()) {

			for (AbstractAnnotation aa : result.getValue().getCurrentPredictions().getAnnotations()) {

				annotations.putIfAbsent(result.getKey().getName(), new HashSet<>());
				annotations.get(result.getKey().getName()).add(aa.asInstanceOfDocumentLinkedAnnotation());

			}

		}

		final Map<String, Set<AbstractAnnotation>> batchAnnotations = new HashMap<>();
		for (String instanceName : names) {
			batchAnnotations.put(instanceName, annotations.getOrDefault(instanceName, Collections.emptySet()));
		}

		return batchAnnotations;
	}

	final public Set<AbstractAnnotation> predictInstanceByName(String name) {

		if (annotations.containsKey(name))
			return annotations.get(name);

		Map<Instance, State> results = crf.predict(instanceProvider.getInstances().stream()
				.filter(i -> name.equals(i.getName())).collect(Collectors.toList()), maxStepCrit, noModelChangeCrit);

		for (Entry<Instance, State> result : results.entrySet()) {

			for (AbstractAnnotation aa : result.getValue().getCurrentPredictions().getAnnotations()) {

				annotations.putIfAbsent(result.getKey().getName(), new HashSet<>());
				annotations.get(result.getKey().getName()).add(aa.asInstanceOfEntityTemplate());

			}

		}
		return annotations.getOrDefault(name, Collections.emptySet());
	}

	final public void evaluateOnDevelopment() {

		Map<Instance, State> results = crf.predict(instanceProvider.getRedistributedDevelopmentInstances(), maxStepCrit,
				noModelChangeCrit);

		AbstractSemReadProject.evaluate(log, results, evaluationObjectiveFunction);

		log.info(crf.getTrainingStatistics());
		log.info(crf.getTestStatistics());
	}

	final public Map<String, Set<AbstractAnnotation>> predictBatchInstances() {
		return predictBatchInstanceByNames(Streams.concat(trainingInstances.stream().map(i -> i.getName()),
				developmentInstances.stream().map(i -> i.getName()), testInstances.stream().map(i -> i.getName()))
				.collect(Collectors.toSet()));
	}

	final public Map<String, Set<AbstractAnnotation>> predictBatchHighRecallInstances(int n) {
		return predictBatchHighRecallInstanceByNames(Streams.concat(trainingInstances.stream().map(i -> i.getName()),
				developmentInstances.stream().map(i -> i.getName()), testInstances.stream().map(i -> i.getName()))
				.collect(Collectors.toSet()), n);
	}

}
