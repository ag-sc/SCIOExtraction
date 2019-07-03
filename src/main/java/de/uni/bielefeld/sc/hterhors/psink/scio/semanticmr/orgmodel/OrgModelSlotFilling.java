package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.orgmodel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.candprov.sf.AnnotationCandidateRetrievalCollection;
import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.OriginalCorpusDistributor;
import de.hterhors.semanticmr.crf.SemanticParsingCRF;
import de.hterhors.semanticmr.crf.exploration.IExplorationStrategy;
import de.hterhors.semanticmr.crf.exploration.RootTemplateCardinalityExplorer;
import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer;
import de.hterhors.semanticmr.crf.exploration.constraints.EHardConstraintType;
import de.hterhors.semanticmr.crf.exploration.constraints.HardConstraintsProvider;
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
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
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
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JsonNerlaProvider;
import de.hterhors.semanticmr.nerla.NerlaCollector;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.hterhors.semanticmr.projects.examples.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.AgeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.orgmodel.specs.OrgModelSpecs;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.orgmodel.templates.PriorNumericInterpretationOrgModelTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.EntityTypeContextTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.OlfactoryContextTemplate;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 *
 */
public class OrgModelSlotFilling extends AbstractSemReadProject {

	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {
		new OrgModelSlotFilling();
	}

	private static Logger log = LogManager.getFormatterLogger(OrgModelSlotFilling.class);

	/**
	 * File of additional hard constraints during exploration. There are different
	 * types of hard constraints (not all of them are implemented yet). This file
	 * contains slot pair exclusion pairs. E.g. if Slot A is filled by Value X than
	 * Slot B must not be filled with value Y.
	 */
	private final File slotPairConstraitsSpecifications = new File(
			"src/main/resources/slotfilling/organism_model/constraints/slotPairExclusionConstraints.csv");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("src/main/resources/slotfilling/organism_model/corpus/instances/");

	/**
	 * A file that contains named entity recognition and linking annotations for
	 * each instance. These annotations are used as candidate retrieval during the
	 * search space exploration.
	 */
	private final File externalNerlaAnnotations = new File(
			"src/main/resources/slotfilling/organism_model/corpus/nerla/");

	public OrgModelSlotFilling() {

		/**
		 * Initialize the system.
		 * 
		 * The scope represents the specifications of the 4 defined specification files.
		 * The scope mainly affects the exploration.
		 */
		super(SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(OrgModelSpecs.systemsScopeReader)
				/**
				 * We apply the scope, so that we can add normalization functions for various
				 * literal entity types, if necessary.
				 */
				.apply()
				/**
				 * Now normalization functions can be added. A normalization function is
				 * especially used for literal-based annotations. In case a normalization
				 * function is provided for a specific entity type, the normalized value is
				 * compared during evaluation instead of the actual surface form. A
				 * normalization function normalizes different surface forms so that e.g. the
				 * weights "500 g", "0.5kg", "500g" are all equal. Each normalization function
				 * is bound to exactly one entity type.
				 */
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization())
				/**
				 * Finally, we build the systems scope.
				 */
				.build());

		/**
		 * Read and distribute the corpus.
		 * 
		 * We define a corpus distribution strategy. In this case we want to
		 * redistribute the corpus based on a random shuffle. We set the corpus size to
		 * 1F which is 100%. This value can be reduced ion order to read less data
		 * during development e.g. We set the training proportion to 80 (%) and test
		 * proportion to 20 (%). Finally we can define a seed to ensure the same random
		 * instance assignment during development.
		 * 
		 */
		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
				.build();
//		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setCorpusSizeFraction(1F)
//				.setTrainingProportion(80).setTestProportion(20).setDevelopmentProportion(20).setSeed(300L).build();

		/**
		 * The instance provider reads all json files in the given directory. We can set
		 * the distributor in the constructor. If not all instances should be read from
		 * the file system, we can add an additional parameter that specifies how many
		 * instances should be read. NOTE: in contrast to the corpusSizeFraction in the
		 * ShuffleCorpusDistributor, we initially set a limit to the number of files
		 * that should be read.
		 */
		InstanceProvider instanceProvider;

		/**
		 * We chose to remove all empty instances from the corpus.
		 */
		InstanceProvider.removeEmptyInstances = true;

		/**
		 * Further, we chose to remove all instances that exceeds a specific number
		 * annotations. NOTE: We do this in order to speed up the system in this
		 * example. When working with real data, this should be set to false. However,
		 * the evaluation in complex slot filling tasks can get highly computational
		 * complex. For n annotations n! pairs need to be compared to compute the real
		 * objective score. See choice of objective function below for more details.
		 */
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		List<Instance> trainingInstances = instanceProvider.getRedistributedTrainingInstances();
		List<Instance> devInstances = instanceProvider.getRedistributedDevelopmentInstances();
		List<Instance> testInstances = instanceProvider.getRedistributedTestInstances();
		/**
		 * The choice of the objective function is crucial for interpreting the systems
		 * performance. For now, we provide two different evaluator-strategies that can
		 * be considered:
		 * 
		 * S1: CartesianEvaluator computes the real objective score in a brute force
		 * way. A correct learning signal helps the model to perform better updates
		 * during training. However, the computation is of complexity O(n!) where n is
		 * the number of annotations per state. Usually n>8 is already intractable!
		 * 
		 * S2: BeamSearchEvaluator approximates the objective score based on a beam
		 * search. Use this for problems that have high number of complex annotations.
		 * Example beam search evaluator with beam size of 10.
		 * 
		 * new BeamSearchEvaluator(EEvaluationDetail.ENTITY_TYPE,10);
		 *
		 *
		 * Further, each objective function is parameterized with an
		 * EvaluationDetail-parameter. The most strict is the
		 * DOCUMENT_LINKED-evaluation. Here, the position in the text, the annotation
		 * text and the assigned entity is compared and needs to be correct to count as
		 * true positive.
		 *
		 * For many slot filling scenarios EEvaluationDetail.ENTITY_TYPE is sufficient.
		 * Here only the entity type is required.
		 *
		 */
		IObjectiveFunction objectiveFunction = new SlotFillingObjectiveFunction(
				new CartesianEvaluator(EEvaluationDetail.DOCUMENT_LINKED));

		/**
		 * The provision of existing entities is an important part in slot filling for
		 * optimizing runtime and performance.
		 * 
		 * During the exploration of the search space, entities are chosen as slot
		 * filler values. The more accurate the list of possible slot filler is the
		 * better the systems performance. In this example we provide an external list
		 * of entity-annotations that serve as potential slot filler for each instance.
		 * 
		 * NOTE: This list of annotations was created offline using a simple symbolic
		 * approach. As the list of annotations serves as candidate retrieval for all
		 * slots, the systems performance highly depends on its quality as it directly
		 * defines the upper bound!
		 * 
		 */
		NerlaCollector nerlaProvider = new NerlaCollector(instanceProvider.getInstances());
		nerlaProvider.addNerlaProvider(new JsonNerlaProvider(externalNerlaAnnotations));

		/**
		 * We can add more candidate provider if necessary.
		 * 
		 * If no external candidate retrieval is available one can also guide the
		 * exploration based on the hierarchical and structural specifications. When
		 * setting
		 * 
		 * candidateProvider.setEntityTypeCandidateProvider();
		 *
		 * All possible (based on the specifications) entity types are retrieved for
		 * each slot.
		 *
		 */
		AnnotationCandidateRetrievalCollection candidateRetrieval = nerlaProvider.collect();

		/**
		 * To further increase the systems performance, we can specify a set of hard
		 * constraints that are considered during exploration.
		 * 
		 * E.g. if Slot A is filled by Value X than Slot B must not be filled with value
		 * Y.
		 * 
		 */
		HardConstraintsProvider constraintsProvider = new HardConstraintsProvider();
		constraintsProvider.addHardConstraints(EHardConstraintType.SLOT_PAIR_EXCLUSION,
				slotPairConstraitsSpecifications);

		/**
		 * For the slot filling problem, the SlotFillingExplorer is added to perform
		 * changes during the exploration. This explorer is especially designed for slot
		 * filling and is parameterized with a candidate retrieval and the
		 * constraintsProvider.
		 */
		SlotFillingExplorer explorer = new SlotFillingExplorer(objectiveFunction, candidateRetrieval,
				constraintsProvider);

		RootTemplateCardinalityExplorer cardExplorer = new RootTemplateCardinalityExplorer(candidateRetrieval,
				AnnotationBuilder.toAnnotation("OrganismModel"));

		List<IExplorationStrategy> explorerList = Arrays.asList(explorer
				, cardExplorer
		);

		/**
		 * The learner defines the update strategy of learned weights. parameters are
		 * the alpha value that is specified in the SGD (first parameter) and the
		 * L2-regularization value.
		 * 
		 * TODO: find best alpha value in combination with L2-regularization.
		 */
		AdvancedLearner learner = new AdvancedLearner(new SGD(0.001, 0), new L2(0.0001));

		/**
		 * Next, we need to specify the actual feature templates. In this example we
		 * provide 3 templates that implements standard features like context-, surface
		 * form, and linguistic dependency features.
		 * 
		 * TODO: Implement further templates / features to solve your problem.
		 * 
		 */
		List<AbstractFeatureTemplate> featureTemplates = new ArrayList<>();
//		featureTemplates.add(new LevenshteinTemplate());
		featureTemplates.add(new IntraTokenTemplate());
		featureTemplates.add(new NGramTokenContextTemplate());
		featureTemplates.add(new SingleTokenContextTemplate());
		featureTemplates.add(new ContextBetweenSlotFillerTemplate());
		featureTemplates.add(new ClusterTemplate());
		featureTemplates.add(new EntityTypeContextTemplate());
		featureTemplates.add(new OlfactoryContextTemplate());
		featureTemplates.add(new LocalityTemplate());
		featureTemplates.add(new SlotIsFilledTemplate());
		featureTemplates.add(new PriorNumericInterpretationOrgModelTemplate(trainingInstances));
//		featureTemplates.add(new NumericInterpretationTemplate());

		/**
		 * During exploration we initialize each state with an empty
		 * OrganismModel-annotation.
		 */
		IStateInitializer stateInitializer = ((instance) -> new State(instance,
				new Annotations(new EntityTemplate(AnnotationBuilder.toAnnotation("OrganismModel")))));

		/**
		 * Number of epochs, the system should train.
		 * 
		 * TODO: Find perfect number of epochs.
		 */
		int numberOfEpochs = 10;

		/**
		 * Sampling strategy that defines how the system should be trained. We
		 * distinguish between two sampling and strategies and two sampling modes.
		 *
		 * The two modes are:
		 * 
		 * M1: Sample based on objective function.
		 * 
		 * M2: Sample based on model score.
		 * 
		 * The two strategies are:
		 * 
		 * S1: Select the best state greedily
		 * 
		 * S2: Select the next state based on the distribution of model or objective
		 * score, respectively.
		 * 
		 * 
		 * For now, we chose a simple epoch switch strategy that switches between greedy
		 * objective score and greedy models score every epoch.
		 * 
		 * TODO: Although many problems seem to work well with this strategy there are
		 * certainly better strategies.
		 */
//		AbstractSampler sampler = SamplerCollection.greedyModelStrategy();
//		AbstractSampler sampler = SamplerCollection.greedyObjectiveStrategy();
		AbstractSampler sampler = new EpochSwitchSampler(epoch -> epoch % 2 == 0);
//		AbstractSampler sampler = new EpochSwitchSampler(new RandomSwitchSamplingStrategy());
//		AbstractSampler sampler = new EpochSwitchSampler(e -> new Random(e).nextBoolean());
		/**
		 * To increase the systems speed performance, we add two stopping criterion for
		 * sampling. The first one is a maximum chain length of produced states. In this
		 * example we set the maximum chain length to 10. That means, only 10 changes
		 * (annotations) can be added to each document.
		 */
		ISamplingStoppingCriterion maxStepCrit = new MaxChainLengthCrit(15);
		/**
		 * The next stopping criterion checks for no or only little (based on a
		 * threshold) changes in the model score of the produced chain. In this case, if
		 * the last three states were scored equally, we assume the system to be
		 * converged.
		 */

		ISamplingStoppingCriterion noModelChangeCrit = new ConverganceCrit(3 * explorerList.size(),
				s -> s.getModelScore());
		ISamplingStoppingCriterion[] sampleStoppingCrits = new ISamplingStoppingCriterion[] { maxStepCrit,
				noModelChangeCrit };
		/**
		 * Finally, we chose a model base directory and a name for the model.
		 * 
		 * NOTE: Make sure that the base model directory exists!
		 */
		final File modelBaseDir = new File("models/slotfilling/org_model/");
		final String modelName = "OrgModel" + new Random().nextInt(100000);

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

		/**
		 * Create a new semantic parsing CRF and initialize with needed parameter.
		 */
		SemanticParsingCRF crf = new SemanticParsingCRF(model, explorerList, sampler, stateInitializer,
				objectiveFunction);

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
		Map<Instance, State> results = crf.predict(devInstances, maxStepCrit, noModelChangeCrit);

		/**
		 * Chose a different evaluation for prediction than for training. During
		 * training we are interested in finding the correct document linked annotation
		 * but for prediction we are only interested in the entity type.
		 */
		IObjectiveFunction predictionOF = new SlotFillingObjectiveFunction(
				new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE));

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
		final Score coverage = crf.computeCoverage(false, predictionOF, devInstances);
		log.info("Coverage: " + coverage);
		log.info(modelName);
		/**
		 * TODO: Compare results with results when changing some parameter. Implement
		 * more sophisticated feature-templates.
		 */
	}

}
