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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Random;
import java.util.Set;

import org.apache.jena.sparql.function.library.leviathan.root;
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
import de.hterhors.semanticmr.crf.structure.annotations.EntityTypeAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.Instance.DeduplicationRule;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.eval.BeamSearchEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.wrapper.DefinedExperimentalGroup;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.goldmodrules.OnlyDefinedExpGroupResults;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Result;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Trend;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.tools.SCIOAutomatedSectionifcation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.tools.SCIOAutomatedSectionifcation.ESection;

public class GroupNameMapping {

	public static void main(String[] args) throws IOException {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).build();
		new GroupNameMapping(100);

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
	private static SystemScope scope;
	File documentAnnotationsDir = new File("data/annotations/result/");

	public GroupNameMapping(int dataRandomSeed) throws IOException {

		this.instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.result);

		this.scoreType = EScoreType.MACRO;

//		this.objectiveFunction = new SlotFillingObjectiveFunction(scoreType,
//				new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE, EEvaluationDetail.DOCUMENT_LINKED));

		this.objectiveFunction = new SlotFillingObjectiveFunction(scoreType,
				new BeamSearchEvaluator(EEvaluationDetail.ENTITY_TYPE, 10));

		this.dataRandomSeed = dataRandomSeed;

		readData();

		// instance, local-group names, gold-group names, mapsTo
		Map<Instance, Map<Set<String>, Map<Set<String>, Boolean>>> trianingDataSet = getDataSet(trainingInstances);

		for (Instance instance : trianingDataSet.keySet()) {
			System.out.println(instance.getName());
			for (Entry<Set<String>, Map<Set<String>, Boolean>> string : trianingDataSet.get(instance).entrySet()) {
				for (Entry<Set<String>, Boolean> string2 : string.getValue().entrySet()) {
					System.out.println(string.getKey() + "\t" + string2.getKey() + "\t" + string2.getValue());
				}
			}

		}

//		System.out.println("------------");
//		Map<Instance, Map<Set<String>, Map<Set<String>, Boolean>>> devDataSet = getDataSet(devInstances);
//
//		for (Instance instance : devDataSet.keySet()) {
//			System.out.println(instance.getName());
//
//			for (Entry<Set<String>, Map<Set<String>, Boolean>> string : devDataSet.get(instance).entrySet()) {
//				for (Entry<Set<String>, Boolean> string2 : string.getValue().entrySet()) {
//					System.out.println(string.getKey() + "\t" + string2.getKey() + "\t" + string2.getValue());
//				}
//			}
//		}
//
//		System.out.println("------------");
//		Map<Instance, Map<Set<String>, Map<Set<String>, Boolean>>> testDataSet = getDataSet(testInstances);
//
//		for (Instance instance : testDataSet.keySet()) {
//			System.out.println(instance.getName());
//
//			for (Entry<Set<String>, Map<Set<String>, Boolean>> string : testDataSet.get(instance).entrySet()) {
//				for (Entry<Set<String>, Boolean> string2 : string.getValue().entrySet()) {
//					System.out.println(string.getKey() + "\t" + string2.getKey() + "\t" + string2.getValue());
//				}
//			}
//		}

	}

	public Map<Set<String>, Map<Set<String>, Boolean>> getDataSet(Instance instance) {
		return getDataSet(Arrays.asList(instance)).get(instance);
	}

	public Map<Instance, Map<Set<String>, Map<Set<String>, Boolean>>> getDataSet(List<Instance> trainingInstances2) {
		Map<Instance, Set<DocumentLinkedAnnotation>> annotations = readAnnotations(documentAnnotationsDir,
				trainingInstances2);

		Map<Instance, Map<Set<String>, Map<Set<String>, Boolean>>> dataSet = new HashMap<>();

		for (Instance instance : annotations.keySet()) {

			dataSet.put(instance, new HashMap<>());
//			System.out.println(instance.getName());

			/*
			 * RKey, Result,DefinedExpGroups
			 */
			Map<RKey, Map<EntityTemplate, Set<EntityTemplate>>> grouping = new HashMap<>();
			/*
			 * Group Results by Trend & investigtaionMethod
			 */
			for (AbstractAnnotation resultAnnotation : instance.getGoldAnnotations().getAnnotations()) {
				Result result = new Result(resultAnnotation.asInstanceOfEntityTemplate());
				RKey rKey = new RKey(result.getTrend(), result.getInvestigationMethod());
				grouping.putIfAbsent(rKey, new HashMap<>());
				grouping.get(rKey).putIfAbsent(resultAnnotation.asInstanceOfEntityTemplate(), new HashSet<>());
				grouping.get(rKey).get(resultAnnotation.asInstanceOfEntityTemplate()).addAll(
						result.getDefinedExperimentalGroups().stream().map(d -> d.get()).collect(Collectors.toSet()));

			}

			for (RKey relatedResultKey : grouping.keySet()) {

				Set<Integer> sentencesPerResultKey = new HashSet<>();
				for (EntityTemplate result : grouping.get(relatedResultKey).keySet()) {
					sentencesPerResultKey.addAll(getSentenceForResult(new Result(result)));
				}
				Set<String> localGroupNamesAnnotations = getLocalGroupNames(annotations, instance,
						sentencesPerResultKey).stream().map(d -> d.getSurfaceForm()).collect(Collectors.toSet());

				if (localGroupNamesAnnotations.isEmpty())
					continue;

				dataSet.get(instance).putIfAbsent(localGroupNamesAnnotations, new HashMap<>());

				Set<Set<String>> rel = new HashSet<>();
				Set<Set<String>> unrel = new HashSet<>();

				for (RKey rKey : grouping.keySet()) {

					for (Set<EntityTemplate> experimentalGroups : grouping.get(rKey).values()) {

						for (EntityTemplate experimentalGroup : experimentalGroups) {
							Set<String> groupNames = getGroupNames(experimentalGroup);

							if (rKey == relatedResultKey) {
								rel.add(groupNames);
							} else {
								unrel.add(groupNames);
							}
						}
					}

				}
				unrel.removeAll(rel);

				for (Set<String> groupNames : rel) {
					dataSet.get(instance).get(localGroupNamesAnnotations).put(groupNames, true);
				}
				for (Set<String> groupNames : unrel) {
					dataSet.get(instance).get(localGroupNamesAnnotations).put(groupNames, false);
				}

//				Set<String> allRelatedGroupNames = rel.stream().flatMap(s -> s.stream()).collect(Collectors.toSet());
//				Set<String> allUnrelatedGroupNames = unrel.stream().flatMap(s -> s.stream())
//						.collect(Collectors.toSet());

//				System.out.println("#Results: " + grouping.get(relatedResultKey).size());
//				System.out.println("#Groups: "
//						+ grouping.get(relatedResultKey).values().stream().flatMap(s -> s.stream()).distinct().count());
//				System.out.println("sentencesPerResultKey: " + sentencesPerResultKey);
//				System.out.println(allRelatedGroupNames.size() + ": allRelatedGroupNames: " + allRelatedGroupNames);
//				System.out
//						.println(allUnrelatedGroupNames.size() + ": allUnrelatedGroupNames: " + allUnrelatedGroupNames);
//				System.out.println(localGroupNamesAnnotations.size() + ": localGroupNamesAnnotations: "
//						+ localGroupNamesAnnotations);
//				System.out.println("----");

			}

		}
		return dataSet;
	}

	private Set<String> getGroupNames(EntityTemplate experimentalGroup) {
		Set<String> groupNames = new HashSet<>();
		DefinedExperimentalGroup def = new DefinedExperimentalGroup(experimentalGroup);
		groupNames.addAll(def.getGroupNames().stream().map(d -> d.getSurfaceForm()).collect(Collectors.toSet()));
		if (def.getGroupRootName() != null)
			groupNames.add(def.getGroupRootName().getSurfaceForm());
		return groupNames;
	}

	static class RKey {
		public final EntityTemplate trend;
		public final EntityTemplate investigationmethod;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((investigationmethod == null) ? 0 : investigationmethod.hashCode());
			result = prime * result + ((trend == null) ? 0 : trend.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RKey other = (RKey) obj;
			if (investigationmethod == null) {
				if (other.investigationmethod != null)
					return false;
			} else if (!investigationmethod.equals(other.investigationmethod))
				return false;
			if (trend == null) {
				if (other.trend != null)
					return false;
			} else if (!trend.equals(other.trend))
				return false;
			return true;
		}

		public RKey(EntityTemplate trend, EntityTemplate investigationmethod) {
			super();
			this.trend = trend;
			this.investigationmethod = investigationmethod;
		}

		@Override
		public String toString() {
			return "RKey [trend=" + (trend == null ? null : trend.toPrettyString()) + ", investigationmethod="
					+ (investigationmethod == null ? null : investigationmethod.toPrettyString()) + "]";
		}

	}

	static class Data {

		public final List<String> localGroupNames;
		public final Set<EntityType> investigationMethods;

		public Data(List<String> localGroupNames, Set<EntityType> investigationMethods) {
			super();
			this.localGroupNames = localGroupNames;
			this.investigationMethods = investigationMethods;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((investigationMethods == null) ? 0 : investigationMethods.hashCode());
			result = prime * result + ((localGroupNames == null) ? 0 : localGroupNames.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Data other = (Data) obj;
			if (investigationMethods == null) {
				if (other.investigationMethods != null)
					return false;
			} else if (!investigationMethods.equals(other.investigationMethods))
				return false;
			if (localGroupNames == null) {
				if (other.localGroupNames != null)
					return false;
			} else if (!localGroupNames.equals(other.localGroupNames))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Data [localGroupNames=" + localGroupNames + ", investigationMethods=" + investigationMethods + "]";
		}

	}

	private Set<Integer> getSentenceForResult(Result result) {
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

	private List<DocumentLinkedAnnotation> getLocalGroupNames(Map<Instance, Set<DocumentLinkedAnnotation>> annotations,
			Instance instance, Set<Integer> sentencesWithResultMention) {
		List<DocumentLinkedAnnotation> localGroupNames = new ArrayList<>();

		Set<DocumentLinkedAnnotation> documentAnnotations = annotations.get(instance);

		Map<Integer, Set<DocumentLinkedAnnotation>> entitiesPerSentence = toSentencebasedAnnotations(
				documentAnnotations);

		Set<DocumentLinkedAnnotation> names = new HashSet<>();
		final int range = 10;
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

	public void collectAnnotationForResult(SCIOAutomatedSectionifcation sectionification,
			Map<Integer, List<EntityType>> entitiesPerSentence, AbstractAnnotation et) {

		if (et.isInstanceOfDocumentLinkedAnnotation() && sectionification
				.getSection(et.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex()) == ESection.RESULTS) {

			entitiesPerSentence.putIfAbsent(et.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex(),
					new ArrayList<>());

			entitiesPerSentence.get(et.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex())
					.add(et.getEntityType());

		}
		if (et.isInstanceOfEntityTemplate()) {
			collectAnnotationForResult(sectionification, entitiesPerSentence,
					et.asInstanceOfEntityTemplate().getRootAnnotation());
			for (Entry<SlotType, SingleFillerSlot> instance2 : et.asInstanceOfEntityTemplate().getSingleFillerSlots()
					.entrySet()) {
				collectAnnotationForResult(sectionification, entitiesPerSentence, instance2.getValue().getSlotFiller());

			}
			for (Entry<SlotType, MultiFillerSlot> instance2 : et.asInstanceOfEntityTemplate().getMultiFillerSlots()
					.entrySet()) {
				for (AbstractAnnotation instance : instance2.getValue().getSlotFiller()) {
					collectAnnotationForResult(sectionification, entitiesPerSentence, instance);
				}
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
		DeduplicationRule deduplicationRule = (a1, a2) -> {
			return a1.evaluateEquals(objectiveFunction.getEvaluator(), a2);
		};

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor, goldModificationRules,
				deduplicationRule);

		trainingInstances = instanceProvider.getRedistributedTrainingInstances();
		devInstances = instanceProvider.getRedistributedDevelopmentInstances();
		testInstances = instanceProvider.getRedistributedTestInstances();

	}

	private Map<Instance, Set<EntityTemplate>> getGoldGroups() {
		Map<Instance, Set<EntityTemplate>> groupsMap = new HashMap<>();

		for (Instance instance : instanceProvider.getInstances()) {
			Set<EntityTemplate> groupsSet;
			groupsMap.put(instance, groupsSet = new HashSet<>());

			for (AbstractAnnotation resultAnnotation : instance.getGoldAnnotations().getAnnotations()) {
				Result result = new Result(resultAnnotation);

				List<DefinedExperimentalGroup> groups = result.getDefinedExperimentalGroups();
				for (DefinedExperimentalGroup group : groups) {
					groupsSet.add(group.get());
				}
			}
		}
		return groupsMap;
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
}
