package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

import de.hterhors.semanticmr.candprov.helper.DictionaryFromInstanceHelper;
import de.hterhors.semanticmr.candprov.sf.AnnotationCandidateRetrievalCollection;
import de.hterhors.semanticmr.candprov.sf.GeneralCandidateProvider;
import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.SemanticParsingCRF;
import de.hterhors.semanticmr.crf.exploration.IExplorationStrategy;
import de.hterhors.semanticmr.crf.exploration.RootTemplateCardinalityExplorer;
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
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
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

public abstract class AbstractSCIOPredictor extends AbstractSemReadProject {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	public SemanticParsingCRF crf;

	private final List<Instance> trainingInstances;
	private final List<Instance> developmentInstances;
	private final List<Instance> testInstances;

	public final String modelName;
	public final Map<String, Set<AbstractAnnotation>> annotations = new HashMap<>();
	private final IObjectiveFunction trainingObjectiveFunction = new SlotFillingObjectiveFunction(
			new CartesianEvaluator(EEvaluationDetail.DOCUMENT_LINKED));

	private final IObjectiveFunction predictionObjectiveFunction = new SlotFillingObjectiveFunction(
			new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE));
	private final InstanceProvider instanceProvider;

	public AbstractSCIOPredictor(String modelName, SystemScope scope, List<String> trainingInstanceNames,
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

		Map<EntityType, Set<String>> trainDictionary = DictionaryFromInstanceHelper.toDictionary(trainingInstances);

		for (Instance instance : trainingInstances) {
			GeneralCandidateProvider ap = new GeneralCandidateProvider(instance);
//			ap.addSlotFiller(AnnotationBuilder.toAnnotation(EntityType.get("DeliveryMethod")));

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

//		RootTemplateCardinalityExplorer cardExplorer = new RootTemplateCardinalityExplorer(candidateRetrieval,
//				AnnotationBuilder.toAnnotation("DeliveryMethod"));

		explorerList = Arrays.asList(explorer
//				
//				, cardExplorer
//			
		);

		maxStepCrit = new MaxChainLengthCrit(15);
		noModelChangeCrit = new ConverganceCrit(3 * explorerList.size(), s -> s.getModelScore());
		sampleStoppingCrits = new ISamplingStoppingCriterion[] { maxStepCrit, noModelChangeCrit };
	}

	ISamplingStoppingCriterion maxStepCrit;
	ISamplingStoppingCriterion noModelChangeCrit;
	ISamplingStoppingCriterion[] sampleStoppingCrits;

	List<IExplorationStrategy> explorerList;

	abstract protected File getExternalNerlaAnnotations();

	abstract protected File getInstanceDirectory();

	abstract protected AdvancedLearner getLearner();

	public Score computeCoverageOnTrainingInstances(boolean detailedLog) {
		return new SemanticParsingCRF(new Model(getFeatureTemplates(), getModelBaseDir(), modelName), explorerList,
				getSampler(), getStateInitializer(), trainingObjectiveFunction).computeCoverage(detailedLog,
						predictionObjectiveFunction, trainingInstances);
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

	protected abstract int getNumberOfEpochs();

	protected abstract IStateInitializer getStateInitializer();

	protected abstract AbstractSampler getSampler();

	protected abstract File getModelBaseDir();

	final public Set<AbstractAnnotation> predictHighRecallInstanceByName(String name, int n) {

		if (annotations.containsKey(name))
			return annotations.get(name);

		Map<Instance, State> results = crf.predictHighRecall(instanceProvider.getInstances().stream()
				.filter(i -> i.getName().equals(name)).collect(Collectors.toList()), n, maxStepCrit, noModelChangeCrit);

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

}
