package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup;

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
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.SemanticParsingBeamCRF;
import de.hterhors.semanticmr.crf.exploration.IExplorationStrategy;
import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer;
import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.learner.optimizer.SGD;
import de.hterhors.semanticmr.crf.learner.regularizer.L2;
import de.hterhors.semanticmr.crf.model.Model;
import de.hterhors.semanticmr.crf.of.IObjectiveFunction;
import de.hterhors.semanticmr.crf.of.SlotFillingObjectiveFunction;
import de.hterhors.semanticmr.crf.sampling.AbstractBeamSampler;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.impl.SamplerCollection;
import de.hterhors.semanticmr.crf.sampling.impl.beam.EpochSwitchBeamSampler;
import de.hterhors.semanticmr.crf.sampling.stopcrit.IBeamSamplingStoppingCriterion;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.templates.et.ClusterTemplate;
import de.hterhors.semanticmr.crf.templates.et.ContextBetweenSlotFillerTemplate;
import de.hterhors.semanticmr.crf.templates.et.LocalityTemplate;
import de.hterhors.semanticmr.crf.templates.et.SlotIsFilledTemplate;
import de.hterhors.semanticmr.crf.templates.shared.IntraTokenTemplate;
import de.hterhors.semanticmr.crf.templates.shared.NGramTokenContextTemplate;
import de.hterhors.semanticmr.crf.templates.shared.SingleTokenContextTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.hterhors.semanticmr.projects.examples.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.AgeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.investigation.CollectExpGroupNames;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.investigation.CollectExpGroupNames.PatternIndexPair;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.templates.PriorNumericInterpretationOrgModelTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.DocumentPartTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.EntityTypeContextTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.OlfactoryContextTemplate;

public class ExperimentalGroupBeamSlotFiller extends AbstractSemReadProject {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	public static void main(String[] args) throws IOException {

		new ExperimentalGroupBeamSlotFiller();

	}

	private final IObjectiveFunction trainingObjectiveFunction = new SlotFillingObjectiveFunction(
			new CartesianEvaluator(EEvaluationDetail.DOCUMENT_LINKED));

	private final IObjectiveFunction predictionObjectiveFunction = new SlotFillingObjectiveFunction(
			new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE));

	private final File instanceDirectory = new File(
			"src/main/resources/slotfilling/experimental_group/corpus/instances/");

	private InstanceProvider instanceProvider;

	public ExperimentalGroupBeamSlotFiller() throws IOException {
		super(SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadDataStructureReader("ExperimentalGroup")).apply()
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization()).build());

		List<String> docs = Files.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath());

//		Collections.shuffle(docs, new Random(100L));

		List<String> trainingInstanceNames = docs.subList(0, 50);

		trainingInstanceNames.forEach(System.out::println);

		List<String> testInstanceNames = docs.subList(50, docs.size());

		testInstanceNames.forEach(System.out::println);

		System.out.println("Num of train: " + trainingInstanceNames.size());
		System.out.println("Num of test: " + testInstanceNames.size());

		AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
				.setTrainingInstanceNames(trainingInstanceNames).setTestInstanceNames(testInstanceNames).build();

		InstanceProvider.maxNumberOfAnnotations = 8;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		Collection<GoldModificationRule> goldModificationRules = getGoldModificationRules();

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor, goldModificationRules);

		List<Instance> trainingInstances = instanceProvider.getRedistributedTrainingInstances();
		List<Instance> devInstances = instanceProvider.getRedistributedDevelopmentInstances();
		List<Instance> testInstances = instanceProvider.getRedistributedTestInstances();

		String rand = String.valueOf(new Random().nextInt());

		/*
		 * Initialize candidate provider for search space exploration.
		 */

		/*
		 * Create annotation with pattern.
		 */

		Map<Instance, List<DocumentLinkedAnnotation>> gold = new HashMap<>();

		for (Instance instance : instanceProvider.getInstances()) {
			instance.addCandidateAnnotations(instance.getGoldAnnotations().getAbstractAnnotations().stream()
					.map(e -> e.asInstanceOfEntityTemplate().getMultiFillerSlot(SCIOSlotTypes.hasGroupName))
					.filter(s -> s.containsSlotFiller()).flatMap(s -> s.getSlotFiller().stream())
					.map(e -> e.asInstanceOfDocumentLinkedAnnotation()).collect(Collectors.toList()));
		}

		/*
		 * Create annotations for treatments/organismModel/injury with gold data.
		 */

		for (Instance instance : instanceProvider.getRedistributedTrainingInstances()) {
			extractCandidatesFromGold(instance);
		}
		for (Instance instance : instanceProvider.getRedistributedDevelopmentInstances()) {
			extractCandidatesFromGold(instance);
		}

		for (Instance instance : instanceProvider.getRedistributedTestInstances()) {
			extractCandidatesFromGold(instance);
		}

		List<IExplorationStrategy> explorerList = Arrays.asList(new SlotFillingExplorer(predictionObjectiveFunction));

		AdvancedLearner learner = new AdvancedLearner(new SGD(0.001, 0), new L2(0.0001));

		/**
		 * Next, we need to specify the actual feature templates. In this example we
		 * provide 3 templates that implements standard features like context-, surface
		 * form, and linguistic dependency features.
		 * 
		 * TODO: Implement further templates / features to solve your problem.
		 * 
		 */
		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();
		featureTemplates.add(new IntraTokenTemplate());

		Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> parameter = new HashMap<>();

		/**
		 * Number of epochs, the system should train.
		 * 
		 */
		int numberOfEpochs = 10;

		AbstractBeamSampler sampler = new EpochSwitchBeamSampler(epoch -> epoch % 2 == 0);

		IBeamSamplingStoppingCriterion maxStepCrit = new IBeamSamplingStoppingCriterion() {

			@Override
			public boolean meetsCondition(List<List<State>> producedStateChain) {
				return producedStateChain.size() == 15;
			}

		};
		/**
		 * The next stopping criterion checks for no or only little (based on a
		 * threshold) changes in the model score of the produced chain. In this case, if
		 * the last three states were scored equally, we assume the system to be
		 * converged.
		 */

		IBeamSamplingStoppingCriterion noModelChangeCrit = new IBeamSamplingStoppingCriterion() {

			final private double threshold = 0.001;

			final private int maxTimesNoChange = 3;
			final private Function<State, Double> f = s -> s.getModelScore();

			@Override
			public boolean meetsCondition(List<List<State>> producedStateChains) {

				if (producedStateChains.size() < maxTimesNoChange)
					return false;

				boolean meet = true;

				for (List<State> producedStateChain : producedStateChains) {

					meet &= check(producedStateChain);
					if (!meet)
						return false;
				}

				return meet;
			}

			public boolean check(List<State> producedStateChain) {

				final double latestValue = f.apply(producedStateChain.get(producedStateChain.size() - 1));

				int countNoChange = 0;

				for (int i = producedStateChain.size() - 2; i >= 0; i--) {
					final double v = f.apply(producedStateChain.get(i)).doubleValue();

					if (latestValue - v <= threshold) {
						countNoChange++;

						if (countNoChange == maxTimesNoChange)
							return true;
					}
				}

				return false;
			}
		};

		IBeamSamplingStoppingCriterion[] sampleStoppingCrits = new IBeamSamplingStoppingCriterion[] { maxStepCrit,
				noModelChangeCrit };
		/**
		 * Finally, we chose a model base directory and a name for the model.
		 * 
		 * NOTE: Make sure that the base model directory exists!
		 */
		final File modelBaseDir = new File("models/slotfilling/expGroup/");
		final String modelName = "ExperimentalGroup" + new Random().nextInt(100000);

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
		SemanticParsingBeamCRF crf = new SemanticParsingBeamCRF(model, explorerList, sampler, getStateInitializer(),
				trainingObjectiveFunction, 4);

		/**
		 * Chose a different evaluation for prediction than for training. During
		 * training we are interested in finding the correct document linked annotation
		 * but for prediction we are only interested in the entity type.
		 */
		IObjectiveFunction predictionOF = new SlotFillingObjectiveFunction(
				new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE));

		/**
		 * If the model was loaded from the file system, we do not need to train it.
		 */
		if (!model.isTrained()) {
			/**
			 * Train the CRF.
			 */
			crf.train(learner, trainingInstances, numberOfEpochs, sampleStoppingCrits);

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
		Map<Instance, State> results = crf.predict(devInstances, maxStepCrit);

		/**
		 * Finally, we evaluate the produced states and print some statistics.
		 */
		evaluate(log, results, predictionOF);

		log.info(crf.getTrainingStatistics());
		log.info(crf.getTestStatistics());

		/**
		 * Computes the coverage of the given instances. The coverage is defined by the
		 * objective mean score that can be reached relying on greedy objective function
		 * sampling strategy. The coverage can be seen as the upper bound of the system.
		 * The upper bound depends only on the exploration strategy, e.g. the provided
		 * NER-annotations during slot-filling.
		 */
		log.info("modelName: " + modelName);
		/**
		 * TODO: Compare results with results when changing some parameter. Implement
		 * more sophisticated feature-templates.
		 */

	}

	private Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> getFeatureTemplateParameters() {
		return Collections.emptyMap();
	}

	public Map<Instance, List<DocumentLinkedAnnotation>> annotateGroupNamesWithPattern(List<Instance> instances) {
		Map<Instance, List<DocumentLinkedAnnotation>> annotations = new HashMap<>();

		for (Instance instance : instances) {
			final List<DocumentLinkedAnnotation> findings = new ArrayList<>();
			annotations.put(instance, findings);

			for (PatternIndexPair p : CollectExpGroupNames.pattern) {
				Matcher m = p.pattern.matcher(instance.getDocument().documentContent);
				while (m.find()) {
					for (Integer group : p.groups) {
						DocumentLinkedAnnotation annotation;
						try {
							annotation = AnnotationBuilder.toAnnotation(instance.getDocument(), "GroupName",
									m.group(group), m.start(group));
						} catch (Exception e) {
							annotation = null;
						}
						if (annotation != null)
							findings.add(annotation);
					}
				}
			}
		}
		return annotations;
	}

	public Collection<GoldModificationRule> getGoldModificationRules() {
		Collection<GoldModificationRule> goldModificationRules = new ArrayList<>();
		goldModificationRules.add(a -> {
			if (a.asInstanceOfEntityTemplate().getRootAnnotation().entityType == EntityType
					.get("DefinedExperimentalGroup"))
				return a;
			return null;
		});
		goldModificationRules.add(a -> {
			a.asInstanceOfEntityTemplate().reduceRootToEntityType();
			return a;
		});
		goldModificationRules.add(a -> {
			a.asInstanceOfEntityTemplate().clearSlotOfName("hasInjuryModel");
			return a;
		});
		goldModificationRules.add(a -> {
			a.asInstanceOfEntityTemplate().clearSlotOfName("hasOrganismModel");
			return a;
		});
		goldModificationRules.add(a -> {
			a.asInstanceOfEntityTemplate().clearSlot(SCIOSlotTypes.hasTreatmentType);
			return a;
		});
		goldModificationRules.add(a -> {
			a.asInstanceOfEntityTemplate().clearSlotOfName("hasNNumber");
			return a;
		});
		goldModificationRules.add(a -> {
			a.asInstanceOfEntityTemplate().clearSlotOfName("hasTotalPopulationSize");
			return a;
		});
		goldModificationRules.add(a -> {
			a.asInstanceOfEntityTemplate().clearSlotOfName("hasGroupNumber");
			return a;
		});
		goldModificationRules.add(a -> {
			if (a.asInstanceOfEntityTemplate().getMultiFillerSlot(SCIOSlotTypes.hasGroupName).containsSlotFiller()
					|| a.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel)
							.containsSlotFiller()
					|| a.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel)
							.containsSlotFiller()
					|| a.asInstanceOfEntityTemplate().getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType)
							.containsSlotFiller())
				return a;
			return null; // remove from annotation if it has no injury, treatment or organism.
		});
		return goldModificationRules;
	}

	protected int getNumberOfEpochs() {
		return 10;
	}

	protected IStateInitializer getStateInitializer() {
		return (instance -> {

			List<AbstractAnnotation> as = new ArrayList<>();

			for (int i = 0; i < instance.getGoldAnnotations().getAnnotations().size(); i++) {
				as.add(new EntityTemplate(AnnotationBuilder.toAnnotation(SCIOEntityTypes.definedExperimentalGroup)));
			}
			return new State(instance, new Annotations(as));
		});

//		return ((instance) -> new State(instance,
//				new Annotations(new EntityTemplate(AnnotationBuilder.toAnnotation("OrganismModel")))));
	}

	protected AbstractSampler getSampler() {
//		AbstractSampler sampler = SamplerCollection.topKModelDistributionSamplingStrategy(2);
		AbstractSampler sampler = SamplerCollection.greedyModelStrategy();
//		AbstractSampler sampler = SamplerCollection.greedyObjectiveStrategy();
//		AbstractSampler sampler = new EpochSwitchSampler(epoch -> epoch % 2 == 0);
//		AbstractSampler sampler = new EpochSwitchSampler(new RandomSwitchSamplingStrategy());
//		AbstractSampler sampler = new EpochSwitchSampler(e -> new Random(e).nextBoolean());
		return sampler;
	}

	protected File getModelBaseDir() {
		return new File("models/slotfilling/org_model/");
	}

	protected AdvancedLearner getLearner() {
		return new AdvancedLearner(new SGD(0.001, 0), new L2(0.0001));
	}

	protected List<AbstractFeatureTemplate<?>> getFeatureTemplates() {
		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();

		featureTemplates.add(new IntraTokenTemplate());

		featureTemplates.add(new NGramTokenContextTemplate());
		featureTemplates.add(new SingleTokenContextTemplate());
		featureTemplates.add(new ContextBetweenSlotFillerTemplate());
		featureTemplates.add(new ClusterTemplate());
		featureTemplates.add(new EntityTypeContextTemplate());
		featureTemplates.add(new OlfactoryContextTemplate());
		featureTemplates.add(new LocalityTemplate());
		featureTemplates.add(new SlotIsFilledTemplate());
		featureTemplates.add(new DocumentPartTemplate());
		featureTemplates.add(new PriorNumericInterpretationOrgModelTemplate());

//		featureTemplates.add(new LevenshteinTemplate());
//		featureTemplates.add(new NumericInterpretationTemplate());
		return featureTemplates;
	}

	public void extractCandidatesFromGold(Instance instance) {
		instance.addCandidateAnnotations(extractGoldTreatments(instance));
		instance.addCandidateAnnotations(extractGoldOrganismModels(instance));
		instance.addCandidateAnnotations(extractGoldInjuryModels(instance));
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
}
