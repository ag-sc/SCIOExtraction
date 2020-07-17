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

import de.hterhors.semanticmr.candidateretrieval.helper.DictionaryFromInstanceHelper;
import de.hterhors.semanticmr.candidateretrieval.sf.SlotFillingCandidateRetrieval.IFilter;
import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.NERSemanticParsingCRF;
import de.hterhors.semanticmr.crf.exploration.IExplorationStrategy;
import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.model.FactorPoolCache;
import de.hterhors.semanticmr.crf.model.Model;
import de.hterhors.semanticmr.crf.of.IObjectiveFunction;
import de.hterhors.semanticmr.crf.of.NerlaObjectiveFunction;
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
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.hterhors.semanticmr.tools.KeyTermExtractor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.SectionizedEntityRecLinkExplorer;

public abstract class AbstractNERLPredictor extends AbstractSemReadProject {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	public NERSemanticParsingCRF crf;

	private final List<Instance> trainingInstances;
	private final List<Instance> developmentInstances;
	private final List<Instance> testInstances;

	public final String modelName;

	public final Map<String, Set<AbstractAnnotation>> annotations = new HashMap<>();

//	private final IObjectiveFunction objectiveFunction = new NerlaObjectiveFunction(EEvaluationDetail.LITERAL);
	public final IObjectiveFunction objectiveFunction = new NerlaObjectiveFunction(EEvaluationDetail.DOCUMENT_LINKED);

	public final IObjectiveFunction evaluationObjectiveFunction = new NerlaObjectiveFunction(EEvaluationDetail.LITERAL);

	public final InstanceProvider instanceProvider;

	public final ISamplingStoppingCriterion maxStepCrit;
	public final ISamplingStoppingCriterion noModelChangeCrit;
	public final ISamplingStoppingCriterion[] sampleStoppingCrits;

	private final List<IExplorationStrategy> explorerList;

	public AbstractNERLPredictor(String modelName, List<String> trainingInstanceNames,
			List<String> developInstanceNames, List<String> testInstanceNames) {

		this.modelName = modelName;

		AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
				.setTrainingInstanceNames(trainingInstanceNames).setDevelopInstanceNames(developInstanceNames)
				.setTestInstanceNames(testInstanceNames).build();

		/**
		 * We chose to remove all empty instances from the corpus.
		 */
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.maxNumberOfAnnotations = 300;
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

		trainingInstances = instanceProvider.getTrainingInstances();
		developmentInstances = instanceProvider.getDevelopmentInstances();
		testInstances = instanceProvider.getTestInstances();

//		Map<EntityType, Set<String>> words = new HashMap<>();
//		for (Instance trainingInstance : trainingInstances) {
//
//			for (DocumentLinkedAnnotation documentLinkedAnnotation : trainingInstance.getGoldAnnotations()
//					.<DocumentLinkedAnnotation>getAnnotations()) {
//				words.putIfAbsent(documentLinkedAnnotation.getEntityType(), new HashSet<>());
//				words.get(documentLinkedAnnotation.getEntityType()).addAll(documentLinkedAnnotation.relatedTokens
//						.stream().map(t -> t.getText()).collect(Collectors.toSet()));
//				words.get(documentLinkedAnnotation.getEntityType())
//						.add(documentLinkedAnnotation.textualContent.surfaceForm);
//			}
//
//		}
//
//		Map<Instance, Set<AbstractAnnotation>> groupNameAnnotations = new HashMap<>();
//
//		JSONNerlaReader nerlaJSONReader = new JSONNerlaReader(
//				new File("data/slot_filling/investigation_method/regex_nerla"));
//
//		for (Instance instance : instanceProvider.getInstances()) {
//
//			groupNameAnnotations.put(instance, new HashSet<>(nerlaJSONReader.getForInstance(instance)));
//		}
//
		for (Instance instance : instanceProvider.getInstances()) {
//
//			for (AbstractAnnotation annotation : groupNameAnnotations.get(instance)) {
//				instance.addCandidate(annotation.getEntityType(),
//						annotation.asInstanceOfLiteralAnnotation().getSurfaceForm());
//			}
//
//			for (Entry<EntityType, Set<String>> wm : words.entrySet()) {
//				instance.addCandidate(wm.getKey(), wm.getValue());
//			}
			instance.addCandidates(getAdditionalCandidates());
//			if (getDictionaryFile() != null)
//				instance.addCandidates(getDictionaryFile());
		}
		Map<EntityType, Set<String>> trainDictionary = DictionaryFromInstanceHelper.toDictionary(trainingInstances);

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
		/**
		 * For the entity recognition and linking problem, the EntityRecLinkExplorer is
		 * added to perform changes during the exploration. This explorer is especially
		 * designed for NERLA and is parameterized with a candidate retrieval.
		 */
//		EntityRecLinkExplorerIterator explorer = new EntityRecLinkExplorerIterator();
//		explorer.MAX_WINDOW_SIZE = 3;
//		EntityRecLinkExplorer explorer = new EntityRecLinkExplorer();
//		explorer.MAX_WINDOW_SIZE = 3;
		SectionizedEntityRecLinkExplorer explorer = new SectionizedEntityRecLinkExplorer();

		explorerList = Arrays.asList(explorer);

		maxStepCrit = new MaxChainLengthCrit(50);
		noModelChangeCrit = new ConverganceCrit(3 * explorerList.size(), s -> s.getModelScore());
		sampleStoppingCrits = new ISamplingStoppingCriterion[] { maxStepCrit, noModelChangeCrit };
	}

	protected File getDictionaryFile() {
		return null;
	}

	abstract protected File getInstanceDirectory();

	abstract protected AdvancedLearner getLearner();

	public Score computeCoverageOnTrainingInstances(boolean detailedLog) {
		return new NERSemanticParsingCRF(new Model(getFeatureTemplates(), getModelBaseDir(), modelName), explorerList,
				getSampler(), getStateInitializer(), objectiveFunction)
						.setKeyTerms(KeyTermExtractor.getKeyTerms(trainingInstances))
						.computeCoverage(detailedLog, objectiveFunction, trainingInstances);
	}

	public Score computeCoverageOnDevelopmentInstances(boolean detailedLog) {
		return new NERSemanticParsingCRF(new Model(getFeatureTemplates(), getModelBaseDir(), modelName), explorerList,
				getSampler(), getStateInitializer(), objectiveFunction)
						.setKeyTerms(KeyTermExtractor.getKeyTerms(trainingInstances))
						.computeCoverage(detailedLog, objectiveFunction, developmentInstances);
	}

	public Score computeCoverageOnTestInstances(boolean detailedLog) {
		return new NERSemanticParsingCRF(new Model(getFeatureTemplates(), getModelBaseDir(), modelName), explorerList,
				getSampler(), getStateInitializer(), objectiveFunction)
						.setKeyTerms(KeyTermExtractor.getKeyTerms(trainingInstances))
						.computeCoverage(detailedLog, objectiveFunction, testInstances);
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
		model.setCache(new FactorPoolCache(model, 32_000, 16_000));
		/**
		 * Create a new semantic parsing CRF and initialize with needed parameter.
		 */
		crf = new NERSemanticParsingCRF(model, explorerList, getSampler(), getStateInitializer(), objectiveFunction)
				.setKeyTerms(KeyTermExtractor.getKeyTerms(trainingInstances));
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

	protected Set<EntityType> getAdditionalCandidates() {
		return Collections.emptySet();
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

	final public Map<String, Set<AbstractAnnotation>> predictBatchInstances() {
		return predictBatchInstanceByNames(Streams.concat(trainingInstances.stream().map(i -> i.getName()),
				developmentInstances.stream().map(i -> i.getName()), testInstances.stream().map(i -> i.getName()))
				.collect(Collectors.toSet()));
	}

	final public Map<Instance, Set<AbstractAnnotation>> predictBatchHighRecallInstances(List<Instance> instances,
			int n) {

		Map<String, Instance> instanceMap = new HashMap<>();

		for (Instance instance : instances) {
			instanceMap.put(instance.getName(), instance);
		}

		Map<String, Set<AbstractAnnotation>> groupNames = predictBatchHighRecallInstanceByNames(instanceMap.keySet(),
				n);

		Map<Instance, Set<AbstractAnnotation>> pi = new HashMap<>();

		for (Entry<String, Set<AbstractAnnotation>> gne : groupNames.entrySet()) {
			pi.put(instanceMap.get(gne.getKey()), gne.getValue());
		}

		return pi;

	}

}
