package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.group_names;

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

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.of.IObjectiveFunction;
import de.hterhors.semanticmr.crf.of.SlotFillingObjectiveFunction;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.Instance.DuplicationRule;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.eval.BeamSearchEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.goldmodrules.OnlyDefinedExpGroupResults;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Result;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Trend;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.tools.SCIOAutomatedSectionifcation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.tools.SCIOAutomatedSectionifcation.ESection;

/**
 * Extract group names in sentences with results:
 * 
 * @author hterhors
 *
 */
public class GNInSentencesWithResults {

	public static void main(String[] args) throws IOException {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).build();

		new GNInSentencesWithResults();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private final File instanceDirectory;
	private final IObjectiveFunction objectiveFunction;

	private final int dataRandomSeed;
	private final EScoreType scoreType;

	private InstanceProvider instanceProvider;

	private List<Instance> trainingInstances;
	private List<Instance> devInstances;
	private List<Instance> testInstances;
	private String modelName;
	File documentAnnotationsDir = new File("data/annotations/result/");

	public GNInSentencesWithResults() throws IOException {

		this.instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.result);

		this.scoreType = EScoreType.MACRO;

//		this.objectiveFunction = new SlotFillingObjectiveFunction(scoreType,
//				new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE, EEvaluationDetail.DOCUMENT_LINKED));

		this.objectiveFunction = new SlotFillingObjectiveFunction(scoreType,
				new BeamSearchEvaluator(EEvaluationDetail.ENTITY_TYPE, 10));

		this.dataRandomSeed = 100;

		readData();

		Map<Instance, Set<DocumentLinkedAnnotation>> annotations = readAnnotations(documentAnnotationsDir,
				trainingInstances);
		for (Instance trainingInstance : trainingInstances) {
			System.out.println(trainingInstance.getName());

			for (AbstractAnnotation result : trainingInstance.getGoldAnnotations().getAnnotations()) {
				Set<Integer> sentencesThatContainResults = new HashSet<>();
				Result r = new Result(result);
				sentencesThatContainResults.addAll(getSentenceForResultGold(r));

				List<DocumentLinkedAnnotation> localGroupNamesAnnotations = getLocalGroupNames(annotations,
						trainingInstance, sentencesThatContainResults);

				for (Integer sentenceIndex : sentencesThatContainResults) {
					System.out.println(trainingInstance.getDocument().getContentOfSentence(sentenceIndex));
				}
				System.out.println(localGroupNamesAnnotations);
			}

		}

	}

	private Map<Instance, Set<DocumentLinkedAnnotation>> readAnnotations(File groupNamesCacheDir,
			List<Instance> instances) {

		JSONNerlaReader nerlaJSONReader = new JSONNerlaReader(groupNamesCacheDir);

		Map<Instance, Set<DocumentLinkedAnnotation>> annotations = new HashMap<>();
		for (Instance instance : instances) {

			SCIOAutomatedSectionifcation sectionification = SCIOAutomatedSectionifcation.getInstance(instance);

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

	private List<DocumentLinkedAnnotation> getRelatedClassesOf(Set<DocumentLinkedAnnotation> annotations,
			EntityType entityType) {
		List<DocumentLinkedAnnotation> subList = new ArrayList<>();

		for (EntityType et : entityType.getRelatedEntityTypes()) {

			for (DocumentLinkedAnnotation documentLinkedAnnotation : annotations) {
				if (documentLinkedAnnotation.getEntityType() == et)
					subList.add(documentLinkedAnnotation);
			}
		}
		return subList;
	}

	private List<DocumentLinkedAnnotation> getSubClassOf(Set<DocumentLinkedAnnotation> annotations,
			EntityType entityType) {
		List<DocumentLinkedAnnotation> subList = new ArrayList<>();
		for (DocumentLinkedAnnotation documentLinkedAnnotation : annotations) {

			if (documentLinkedAnnotation.getEntityType() == entityType)
				subList.add(documentLinkedAnnotation);
			for (EntityType et : entityType.getTransitiveClosureSubEntityTypes()) {

				if (documentLinkedAnnotation.getEntityType() == et)
					subList.add(documentLinkedAnnotation);
			}
		}

		return subList;
	}

	private Map<Integer, Set<DocumentLinkedAnnotation>> toSentencebasedAnnotations(
			Set<DocumentLinkedAnnotation> documentAnnotations) {
		Map<Integer, Set<DocumentLinkedAnnotation>> entitiesPerSentence = new HashMap<>();

		for (DocumentLinkedAnnotation ann : documentAnnotations) {

			entitiesPerSentence.putIfAbsent(ann.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex(),
					new HashSet<>());
			entitiesPerSentence.get(ann.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex()).add(ann);
		}
		return entitiesPerSentence;
	}

	private List<DocumentLinkedAnnotation> getLocalGroupNames(Map<Instance, Set<DocumentLinkedAnnotation>> annotations,
			Instance instance, Set<Integer> sentencesWithResultMention) {
		List<DocumentLinkedAnnotation> localGroupNames = new ArrayList<>();

		Set<DocumentLinkedAnnotation> documentAnnotations = annotations.get(instance);

		Map<Integer, Set<DocumentLinkedAnnotation>> entitiesPerSentence = toSentencebasedAnnotations(
				documentAnnotations);

		Set<DocumentLinkedAnnotation> names = new HashSet<>();
		final int range = 1;
		for (Integer trendSentenceIndex : sentencesWithResultMention) {

			/**
			 * Look behind
			 * 
			 * For the last range number of sentences look for group names / experimental
			 * groups.
			 */

			for (int sentenceIndex = trendSentenceIndex; sentenceIndex >= trendSentenceIndex - range; sentenceIndex--) {

				Set<DocumentLinkedAnnotation> annotationsInSentence = entitiesPerSentence.get(sentenceIndex);

				if (annotationsInSentence == null)
					continue;

				names.addAll(getRelatedClassesOf(annotationsInSentence, SCIOEntityTypes.groupName));
				names.addAll(getSubClassOf(annotationsInSentence, SCIOEntityTypes.experimentalGroup));

				if (names.size() >= 2) {
					break;
				}
			}
			localGroupNames.addAll(names);
		}
		return localGroupNames;
	}

	private Set<Integer> getSentenceForResultGold(Result result) {
		Set<Integer> sentence = new HashSet<>();

		Trend trend = new Trend(result.getTrend());
		if (trend.get() == null)
			return sentence;

		DocumentLinkedAnnotation rootTrend = trend.getRootAnntoationAsDocumentLinkedAnnotation();

		if (rootTrend != null)
			sentence.add(rootTrend.getSentenceIndex());

		DocumentLinkedAnnotation difference = trend.getDifferenceAsDocumentLinkedAnnotation();

		if (difference != null) {
//			System.out.println(difference.getSentenceIndex() + difference.getEntityType().name);
			sentence.add(difference.getSentenceIndex());
		}

		SingleFillerSlot significanceSlot = trend.get().getSingleFillerSlot(SCIOSlotTypes.hasSignificance);

		if (significanceSlot.containsSlotFiller()) {
			EntityTemplate significance = significanceSlot.getSlotFiller().asInstanceOfEntityTemplate();

			AbstractAnnotation rootAnnotation = significance.getRootAnnotation();
			if (rootAnnotation != null && rootAnnotation.isInstanceOfDocumentLinkedAnnotation()) {
//				System.out.println(rootAnnotation.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex()
//						+ rootAnnotation.getEntityType().name);
				sentence.add(rootAnnotation.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex());
			}

			AbstractAnnotation PValueSlotFiller = significance.getSingleFillerSlot(SCIOSlotTypes.hasPValue)
					.getSlotFiller();
			AbstractAnnotation alphaSigSlotFiller = significance
					.getSingleFillerSlot(SCIOSlotTypes.hasAlphaSignificanceNiveau).getSlotFiller();

			if (PValueSlotFiller != null) {
				if (PValueSlotFiller.isInstanceOfDocumentLinkedAnnotation()) {
//					System.out.println(PValueSlotFiller.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex()
//							+ PValueSlotFiller.getEntityType().name);

					sentence.add(PValueSlotFiller.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex());
				}
			}
			if (alphaSigSlotFiller != null) {
				if (alphaSigSlotFiller.isInstanceOfDocumentLinkedAnnotation()) {
//					System.out.println(alphaSigSlotFiller.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex()
//							+ alphaSigSlotFiller.getEntityType().name);
					sentence.add(alphaSigSlotFiller.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex());
				}
			}

		}
		return sentence;
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
}
