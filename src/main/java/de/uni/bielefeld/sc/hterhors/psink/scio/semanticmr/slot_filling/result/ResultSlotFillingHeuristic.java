package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result;

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
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.candidateretrieval.helper.DictionaryFromInstanceHelper;
import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.of.IObjectiveFunction;
import de.hterhors.semanticmr.crf.of.SlotFillingObjectiveFunction;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.Instance.DuplicationRule;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.BeamSearchEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.wrapper.DefinedExperimentalGroup;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.goldmodrules.OnlyDefinedExpGroupResults;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.heuristics.Heuristic;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.tools.AutomatedSectionifcation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.tools.AutomatedSectionifcation.ESection;

public class ResultSlotFillingHeuristic extends AbstractSemReadProject {

	public static void main(String[] args) throws Exception {

		new ResultSlotFillingHeuristic(100L);

	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private final File instanceDirectory;
	private final IObjectiveFunction objectiveFunction;

	private final long dataRandomSeed;
	private final EScoreType scoreType;

	private InstanceProvider instanceProvider;

	private List<Instance> trainingInstances;
	private List<Instance> devInstances;
	private List<Instance> testInstances;
	private String modelName;

	public ResultSlotFillingHeuristic(long dataRandomSeed) throws Exception {
		super(SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).build());

		this.instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.result);

		this.scoreType = EScoreType.MACRO;

//		this.objectiveFunction = new SlotFillingObjectiveFunction(scoreType,
//				new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE, EEvaluationDetail.DOCUMENT_LINKED));

		this.objectiveFunction = new SlotFillingObjectiveFunction(scoreType,
				new BeamSearchEvaluator(EEvaluationDetail.ENTITY_TYPE, 5));

		this.dataRandomSeed = dataRandomSeed;

		readData();

		String rand = String.valueOf(new Random().nextInt(100000));

		modelName = "Result" + rand;
		log.info("Model name = " + modelName);
		File documentAnnotationsDir = new File("data/annotations/result/");
//		File documentAnnotationsDir = new File("data/slot_filling/result/regex_nerla");

		Map<Instance, Set<DocumentLinkedAnnotation>> annotations = readAnnotations(documentAnnotationsDir);

//		addDictionaryBasedAnnotations(annotations);

		Heuristic heuristic = new Heuristic(annotations, trainingInstances);

		Map<Instance, State> results = heuristic.predictByHeuristic(trainingInstances);

		SCIOSlotTypes.hasGroupName.exclude();

		Score score = evaluate(log, results, this.objectiveFunction);
		log.info("Unsorted Score: " + score);

		Map<Instance, List<EntityTemplate>> goldResults = new HashMap<>();
		Map<Instance, List<EntityTemplate>> predictedResults = new HashMap<>();

		Score macroScore = new Score(EScoreType.MACRO);
		Score microScore = new Score(EScoreType.MICRO);

		for (State finalState : results.values()) {
			goldResults.put(finalState.getInstance(), new ArrayList<>());
			predictedResults.put(finalState.getInstance(), new ArrayList<>());
			for (AbstractAnnotation result : finalState.getGoldAnnotations().getAnnotations()) {

				goldResults.get(finalState.getInstance()).add(sortResult(result));

			}
			for (AbstractAnnotation result : finalState.getCurrentPredictions().getAnnotations()) {

				predictedResults.get(finalState.getInstance()).add(sortResult(result));

			}
//			System.out.println("Gold");
//			for (EntityTemplate instance : goldResults.get(finalState.getInstance())) {
//				System.out.println(instance.toPrettyString());
//			}
//			System.out.println("Pred");
//			for (EntityTemplate instance : predictedResults.get(finalState.getInstance())) {
//				System.out.println(instance.toPrettyString());
//			}
			Score scorePart = objectiveFunction.getEvaluator().scoreMultiValues(
					goldResults.get(finalState.getInstance()), predictedResults.get(finalState.getInstance()),
					EScoreType.MICRO);
//			log.info(finalState.getInstance().getName() + " : " + scorePart);
//			System.out.println();
			microScore.add(scorePart);
			macroScore.add(scorePart.toMacro());
		}
		System.out.println("Sorted micro score = " + microScore);
		System.out.println("Sorted macro score = " + macroScore);

	}

	private EntityTemplate sortResult(AbstractAnnotation result) {

		AbstractAnnotation referenceFiller = result.asInstanceOfEntityTemplate()
				.getSingleFillerSlot(SCIOSlotTypes.hasReferenceGroup).getSlotFiller();
		AbstractAnnotation targetFiller = result.asInstanceOfEntityTemplate()
				.getSingleFillerSlot(SCIOSlotTypes.hasTargetGroup).getSlotFiller();

		if (referenceFiller == null && targetFiller == null)
			return result.asInstanceOfEntityTemplate();
		if (referenceFiller == null && targetFiller != null)
			return result.asInstanceOfEntityTemplate();

		DefinedExperimentalGroup def1 = new DefinedExperimentalGroup(referenceFiller.asInstanceOfEntityTemplate());

		DefinedExperimentalGroup def2 = new DefinedExperimentalGroup(targetFiller.asInstanceOfEntityTemplate());

		DefinedExperimentalGroup ref = null;
		DefinedExperimentalGroup target = null;

		boolean change = !doNotSwitchPos(def1, def2);
		if (change) {
			ref = def2;
			target = def1;
		} else {
			ref = def1;
			target = def2;
		}

		EntityTemplate newResult = result.asInstanceOfEntityTemplate().deepCopy();

		if (change) {

			AbstractAnnotation trend = result.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasTrend)
					.getSlotFiller();

			if (trend != null) {

				AbstractAnnotation differenceFiller = trend.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasDifference).getSlotFiller();

				if (differenceFiller != null) {

					EntityType difference = differenceFiller.getEntityType();

					if (difference == EntityType.get("Higher"))
						difference = EntityType.get("Lower");
					else if (difference == EntityType.get("Lower"))
						difference = EntityType.get("Higher");
					else if (difference == EntityType.get("FasterIncrease"))
						difference = EntityType.get("SlowerIncrease");
					else if (difference == EntityType.get("SlowerIncrease"))
						difference = EntityType.get("FasterIncrease");

					newResult.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasTrend).getSlotFiller()
							.asInstanceOfEntityTemplate()
							.setSingleSlotFiller(SCIOSlotTypes.hasDifference, AnnotationBuilder.toAnnotation(
									differenceFiller.asInstanceOfDocumentLinkedAnnotation().document, difference,
									differenceFiller.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm(),
									differenceFiller.asInstanceOfDocumentLinkedAnnotation().getStartDocCharOffset()));
				}

			}
		}

		newResult.setSingleSlotFiller(SCIOSlotTypes.hasReferenceGroup, ref.get());
		newResult.setSingleSlotFiller(SCIOSlotTypes.hasTargetGroup, target.get());
		return newResult;
	}

	private static boolean doNotSwitchPos(DefinedExperimentalGroup toCheck, DefinedExperimentalGroup basedOn) {

		Set<EntityType> referenceTreats = toCheck.getRelevantTreatments().stream().map(e -> e.getEntityType())
				.collect(Collectors.toSet());

		Set<EntityType> targetTreats = basedOn.getRelevantTreatments().stream().map(e -> e.getEntityType())
				.collect(Collectors.toSet());

		boolean referenceContainsVehicle = containsVehicle(referenceTreats);
		boolean targetContainsVehicle = containsVehicle(targetTreats);

		boolean referenceContainsOEC = containsOEC(referenceTreats);
		boolean targetContainsOEC = containsOEC(targetTreats);

		if (targetTreats.containsAll(referenceTreats))
			return true;

		if (referenceContainsOEC && targetContainsOEC)
			return false;

		if (referenceTreats.isEmpty() && !targetTreats.isEmpty())
			return true;

		if (!referenceTreats.isEmpty() && targetTreats.isEmpty())
			return false;

		if (referenceContainsOEC && !targetContainsOEC) {
			return false;
		}

		if (!referenceContainsOEC && targetContainsOEC) {
			return true;
		}

		if (referenceContainsVehicle && !targetContainsVehicle) {
			return true;
		}
		if (!referenceContainsVehicle && targetContainsVehicle) {
			return false;
		}

		if (!referenceContainsVehicle && !referenceContainsOEC && targetContainsOEC) {
			return true;
		}

		if (!referenceContainsOEC && !targetContainsOEC)
			return true;

		throw new IllegalStateException();
	}

	private static boolean containsVehicle(Set<EntityType> toCheckTreats) {
		boolean toCheckContainsVehicle = false;
		for (EntityType entityType : EntityType.get("Vehicle").getRelatedEntityTypes()) {
			toCheckContainsVehicle |= toCheckTreats.contains(entityType);
			if (toCheckContainsVehicle)
				break;
		}
		return toCheckContainsVehicle;
	}

	private static boolean containsOEC(Set<EntityType> r) {
		return r.contains(EntityType.get("OlfactoryEnsheathingGliaCell"));
	}

	private void addDictionaryBasedAnnotations(Map<Instance, Set<DocumentLinkedAnnotation>> annotations) {

		/**
		 * Get surface forms of training data annotations.
		 */
		Map<EntityType, Set<String>> trainDictionary = DictionaryFromInstanceHelper.toDictionary(trainingInstances);

		for (Instance instance : instanceProvider.getInstances()) {

			AutomatedSectionifcation sectionification = AutomatedSectionifcation.getInstance(instance);

			/**
			 * Apply dictionary to document
			 */
			for (AbstractAnnotation nerla : DictionaryFromInstanceHelper.getAnnotationsForInstance(instance,
					trainDictionary)) {

				if (!nerla.isInstanceOfDocumentLinkedAnnotation())
					continue;

				if (sectionification.getSection(nerla.asInstanceOfDocumentLinkedAnnotation()) != ESection.RESULTS)
					continue;

				annotations.get(instance).add(nerla.asInstanceOfDocumentLinkedAnnotation());

			}
		}
	}

	public void readData() throws IOException {

		SlotType.excludeAll();

		boolean includeGroups = true;

		if (includeGroups) {
			SCIOSlotTypes.hasTargetGroup.include();
			SCIOSlotTypes.hasReferenceGroup.include();

			SCIOSlotTypes.hasTreatmentType.include();
			SCIOSlotTypes.hasCompound.include();

			SCIOSlotTypes.hasGroupName.include();
		}
		SCIOSlotTypes.hasTrend.includeRec();
		SCIOSlotTypes.hasInvestigationMethod.include();
//		SCIOSlotTypes.hasJudgement.includeRec();

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

		Collection<GoldModificationRule> goldModificationRules;

		if (includeGroups) {
			goldModificationRules = Arrays.asList(new OnlyDefinedExpGroupResults());
		} else {
			goldModificationRules = Arrays.asList();
		}
		DuplicationRule deduplicationRule = (a1, a2) -> {
			return a1.evaluateEquals(objectiveFunction.getEvaluator(), a2);
		};

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor, goldModificationRules,
				deduplicationRule);

		trainingInstances = instanceProvider.getRedistributedTrainingInstances();
		devInstances = instanceProvider.getRedistributedDevelopmentInstances();
		testInstances = instanceProvider.getRedistributedTestInstances();

	}

	private Map<Instance, Set<DocumentLinkedAnnotation>> readAnnotations(File groupNamesCacheDir) {

		JSONNerlaReader nerlaJSONReader = new JSONNerlaReader(groupNamesCacheDir);

		Map<Instance, Set<DocumentLinkedAnnotation>> annotations = new HashMap<>();
		for (Instance instance : instanceProvider.getInstances()) {

			AutomatedSectionifcation sectionification = AutomatedSectionifcation.getInstance(instance);

			annotations.putIfAbsent(instance, new HashSet<>());
			for (DocumentLinkedAnnotation eta : new HashSet<>(nerlaJSONReader.getForInstance(instance))) {

				if (sectionification.getSection(eta.asInstanceOfDocumentLinkedAnnotation()) != ESection.RESULTS)
					continue;

				annotations.get(instance).add(eta.asInstanceOfDocumentLinkedAnnotation());

			}
			/**
			 * If no result annotations exist, simply add all as fallback.
			 */
			if (annotations.get(instance).isEmpty()) {
				annotations.get(instance).addAll(nerlaJSONReader.getForInstance(instance));
			}

		}
		return annotations;
	}

}
