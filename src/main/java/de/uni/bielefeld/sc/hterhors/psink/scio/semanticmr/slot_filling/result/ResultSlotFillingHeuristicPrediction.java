package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

import de.hterhors.semanticmr.candidateretrieval.helper.DictionaryFromInstanceHelper;
import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.hterhors.semanticmr.json.JsonInstanceIO;
import de.hterhors.semanticmr.json.JsonInstanceReader;
import de.hterhors.semanticmr.json.converter.InstancesToJsonInstanceWrapper;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation.ESection;
import de.uni.bielefeld.sc.hterhors.psink.scio.rdf.ConvertToRDF;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.helper.GroupNameExtraction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.AgeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DistanceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DosageNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DurationNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ForceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.LengthNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.PressureNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ThicknessNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.VolumeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.fasttext.FastTextSentenceClassification;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.result_sentences.ExtractSentencesWithResults;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.ExperimentalGroupSlotFillingPrediction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EDistinctGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.wrapper.DefinedExperimentalGroup;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.heuristics.LocalSentenceHeuristic;

public class ResultSlotFillingHeuristicPrediction extends AbstractSemReadProject {

	public static void main(String[] args) throws Exception {

		new ResultSlotFillingHeuristicPrediction(4);
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private final File instanceDirectory = new File("prediction/instances");

	private final EScoreType scoreType;

	private InstanceProvider instanceProvider;

	private List<Instance> instances;

	private String modelName;
	public boolean includeGroups = true;
	public final int batchSize = 100;
	final int batch;

	public ResultSlotFillingHeuristicPrediction(int batchCount) throws Exception {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).apply()
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization())
				//
				.registerNormalizationFunction(new DosageNormalization())
				.registerNormalizationFunction(new DurationNormalization())
				.registerNormalizationFunction(new VolumeNormalization())
				.registerNormalizationFunction(new ForceNormalization())
				.registerNormalizationFunction(new ThicknessNormalization())
				.registerNormalizationFunction(new PressureNormalization())
				.registerNormalizationFunction(new LengthNormalization())
				.registerNormalizationFunction(new DistanceNormalization())
				//
				.build();
		this.batch = batchCount;
		this.scoreType = EScoreType.MACRO;

		// this.objectiveFunction = new SlotFillingObjectiveFunction(scoreType,
//				new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE, EEvaluationDetail.DOCUMENT_LINKED));
		List<String> instanceNames = Arrays.asList(new File("prediction/instances").list());
		Collections.sort(instanceNames);
		instanceNames = instanceNames.subList(batchCount * batchSize, (1 + batchCount) * batchSize);

		readData(instanceNames);

		/**
		 * TODO: use fast text as in evaluation setting
		 */
		boolean includeFastTextAnnotations = true;

		boolean includeIDFAnnotations = false;

		boolean addDicitionaryBasedAnnotations = false;
		boolean includeRegexData = false;

		Map<Instance, Set<EntityTemplate>> expGroups;

		modelName = "Result_PREDICT";
		log.info("Model name = " + modelName);

		Map<Instance, Set<DocumentLinkedAnnotation>> annotations = new HashMap<>();

		for (Instance instance : instanceProvider.getInstances()) {
			annotations.putIfAbsent(instance, new HashSet<>());
			annotations.get(instance).addAll(getGroupNameCandidates(instance));
		}

		if (includeRegexData) {
			Map<Instance, Set<DocumentLinkedAnnotation>> regexp = readAnnotations(
					new File("data/slot_filling/result/regex_nerla"));
			for (Instance instance : regexp.keySet()) {
				annotations.putIfAbsent(instance, new HashSet<>());
				annotations.get(instance).addAll(regexp.get(instance));
			}
		}

		if (includeFastTextAnnotations) {
			FastTextSentenceClassification invest = new FastTextSentenceClassification(modelName + "ION", false,
					SCIOEntityTypes.investigationMethod, instances, true);
			Map<Instance, Set<DocumentLinkedAnnotation>> annotationsInvFT = invest.predictNerlas(instances);
			for (Instance instance : annotationsInvFT.keySet()) {
				annotations.putIfAbsent(instance, new HashSet<>());
				annotations.get(instance).addAll(annotationsInvFT.get(instance));
			}

			FastTextSentenceClassification trend = new FastTextSentenceClassification(modelName + "ION", false,
					SCIOEntityTypes.trend, instances, true);
			Map<Instance, Set<DocumentLinkedAnnotation>> annotationsTredFT = trend.predictNerlas(instances);
			for (Instance instance : annotationsTredFT.keySet()) {
				annotations.putIfAbsent(instance, new HashSet<>());
				annotations.get(instance).addAll(annotationsTredFT.get(instance));
			}

		}

//		if (includeIDFAnnotations) {
//			ExtractSentencesWithResults r = new ExtractSentencesWithResults(trainingInstanceNames, testInstanceNames);
//
//			Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> trends = r.predictTrendInstances();
//			Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> investigationMethods = r
//					.predictInvestigationMethodInstances();
//
//			r.filter();
//
//			for (Instance instance : trends.keySet()) {
//				AutomatedSectionifcation sectionification = AutomatedSectionifcation.getInstance(instance);
//				annotations.putIfAbsent(instance, new HashSet<>());
//
//				for (Set<DocumentLinkedAnnotation> trendAnnotations : trends.get(instance).values()) {
//					for (DocumentLinkedAnnotation trendAnnotation : trendAnnotations) {
//
//						if (sectionification.getSection(trendAnnotation) != ESection.RESULTS) {
//							continue;
//						}
//
//						annotations.get(instance).add(trendAnnotation);
//					}
//				}
//			}
//			for (Instance instance : investigationMethods.keySet()) {
//				AutomatedSectionifcation sectionification = AutomatedSectionifcation.getInstance(instance);
//				annotations.putIfAbsent(instance, new HashSet<>());
//
//				for (Set<DocumentLinkedAnnotation> invMAnnotations : investigationMethods.get(instance).values()) {
//					for (DocumentLinkedAnnotation invMAnnotation : invMAnnotations) {
//
//						if (sectionification.getSection(invMAnnotation) != ESection.RESULTS) {
//							continue;
//						}
//						annotations.get(instance).add(invMAnnotation);
//					}
//				}
//			}
//
//		}

		if (addDicitionaryBasedAnnotations)
			addDictionaryBasedAnnotations(annotations);

		expGroups = getPredictedGroups();

		LocalSentenceHeuristic heuristic = new LocalSentenceHeuristic(annotations, expGroups, instances);

		Map<Instance, State> results = heuristic.predictInstancesByHeuristic(instances);

//		Map<Instance, List<EntityTemplate>> predictedResults = new HashMap<>();
//		for (State finalState : results.values()) {
//
//			for (AbstractAnnotation result : finalState.getCurrentPredictions().getAnnotations()) {
//
//				predictedResults.get(finalState.getInstance()).add(sortResult(result));
//
//			}
//		}
		Map<String, List<EntityTemplate>> ansMap = new HashMap<>();

		for (Entry<Instance, State> instance : results.entrySet()) {
			List<EntityTemplate> ans = new ArrayList<>();
			for (AbstractAnnotation entityTemplate : instance.getValue().getCurrentPredictions().getAnnotations()) {
				System.out.println(instance.getKey() + "\t" + entityTemplate.toPrettyString());
				ans.add(entityTemplate.asInstanceOfEntityTemplate());
			}
			ansMap.put(instance.getKey().getName(), ans);
		}

		File outPutFile = new File("result_batch_count_" + batchCount + "_" + batchSize + ".n-triples");

		new ConvertToRDF(outPutFile, ansMap);
	}

	private Map<Instance, Set<EntityTemplate>> getPredictedGroups() throws Exception {

		File root = new File("prediction/data/annotations/slot_filling/experimental_group_Full" + modelName + "/batch_"
				+ batch + "_" + batchSize + "/");
		Map<Instance, Set<EntityTemplate>> expGroups;
		if (root.exists() && root.list().length != 0) {
			// Cache
			expGroups = new HashMap<>();
			log.info("Read cached files: " + root.list().length);

			JsonInstanceReader reader = new JsonInstanceReader(root, Collections.emptyList(), (a, b) -> false);

			List<Instance> prePredictedInstances = reader.readInstances(1000);

			for (Instance instance : prePredictedInstances) {
				expGroups.put(instance, new HashSet<>(instance.getGoldAnnotations().<EntityTemplate>getAnnotations()));
				log.info(instance.getName() + " read expgroup annotations: "
						+ instance.getGoldAnnotations().<EntityTemplate>getAnnotations().size());

			}

		} else {
			Map<SlotType, Boolean> storage = SlotType.storeExcludance();
			SlotType.includeAll();
//			ExperimentalGroupSlotFillingPredictorFinalEvaluation.maxCacheSize = 800_000;
//			ExperimentalGroupSlotFillingPredictorFinalEvaluation.minCacheSize = 400_000;

			ExperimentalGroupSlotFillingPrediction a = new ExperimentalGroupSlotFillingPrediction(instances);
			root.mkdirs();
			log.info("Create annotations root directory... " + root);
			for (Entry<Instance, Set<EntityTemplate>> extr : a.extraction.entrySet()) {
				log.info("Write: " + extr.getKey().getDocument());
				log.info("Write # of annotations: " + extr.getValue().size());
				List<Instance> i = new ArrayList<>();

				Annotations goldAnnotations = new Annotations(new ArrayList<>(new HashSet<>(extr.getValue())));

				i.add(new Instance(extr.getKey().getOriginalContext(), extr.getKey().getDocument(), goldAnnotations));

				InstancesToJsonInstanceWrapper conv = new InstancesToJsonInstanceWrapper(i);

				JsonInstanceIO writer = new JsonInstanceIO(true);
				writer.writeInstances(new File(root, extr.getKey().getName() + ".json"),
						conv.convertToWrapperInstances());

			}

			expGroups = a.extraction;
			SlotType.restoreExcludance(storage);
		}

		Map<Instance, Set<EntityTemplate>> groupsMap = new HashMap<>();

		for (Entry<Instance, Set<EntityTemplate>> extr : expGroups.entrySet()) {
			Instance keyInstance = null;
			/**
			 * Ugly map between instances of result and instances of exp group extraction
			 */
			for (Instance instance : instanceProvider.getInstances()) {
				if (instance.getName().equals(extr.getKey().getName())) {
					keyInstance = instance;
					break;
				}
			}

			groupsMap.put(keyInstance, extr.getValue());

		}

		return groupsMap;
	}

	private List<DocumentLinkedAnnotation> getGroupNameCandidates(Instance instance) {
		List<DocumentLinkedAnnotation> list = GroupNameExtraction
				.extractGroupNamesWithPattern(EDistinctGroupNamesMode.NOT_DISTINCT, instance);

		/**
		 * TODO: apply NPChunks to 8000 docs!
		 */

//		List<DocumentLinkedAnnotation> list2 = GroupNameExtraction
//				.extractGroupNamesWithNPCHunks(EDistinctGroupNamesMode.NOT_DISTINCT, instance);
//		list.addAll(list2);

		return list;

//		switch (groupNameProviderMode) {
//		case EMPTY:
//			break;
//		case GOLD:
//				return GroupNameExtraction.extractGroupNamesFromGold(instance));
//		case TRAINING_PATTERN_NP_CHUNKS:
//			addGroupNameTrainingPattern();
//		case NP_CHUNKS:
//			for (Instance instance : instanceProvider.getInstances()) {
//				instance.addCandidateAnnotations(
//						GroupNameExtraction.extractGroupNamesWithNPCHunks(distinctGroupNamesMode, instance));
//			}
//			break;
//		case TRAINING_MANUAL_PATTERN:
//			addGroupNameTrainingPattern();
//		case MANUAL_PATTERN:
//			for (Instance instance : instanceProvider.getInstances()) {
//				instance.addCandidateAnnotations(
//						GroupNameExtraction.extractGroupNamesWithPattern(distinctGroupNamesMode, instance));
//			}
//			break;
//		case TRAINING_MANUAL_PATTERN_NP_CHUNKS:
//			addGroupNameTrainingPattern();
//		case MANUAL_PATTERN_NP_CHUNKS:
//			for (Instance instance : instanceProvider.getInstances()) {
//				instance.addCandidateAnnotations(
//						GroupNameExtraction.extractGroupNamesWithNPCHunks(distinctGroupNamesMode, instance));
//				instance.addCandidateAnnotations(
//						GroupNameExtraction.extractGroupNamesWithPattern(distinctGroupNamesMode, instance));
//			}
//			break;
//		case TRAINING_PATTERN:
//			addGroupNameTrainingPattern();
//			break;
//		case PREDICTED:
//			int k = 1;
//			for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictGroupName(instanceProvider.getInstances(),
//					k).entrySet()) {
//				prediction.getKey().addCandidateAnnotations(prediction.getValue());
//			}
////			for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictGroupName(devInstances, k).entrySet()) {
////				prediction.getKey().addCandidateAnnotations(prediction.getValue());
////			}
////			for (Entry<Instance, Set<AbstractAnnotation>> prediction : predictGroupName(testInstances, k).entrySet()) {
////				prediction.getKey().addCandidateAnnotations(prediction.getValue());
////			}
//			break;
	}

	private EntityTemplate sortResult(AbstractAnnotation result) {

		if (!includeGroups) {
			return result.asInstanceOfEntityTemplate();
		}

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
		Map<EntityType, Set<String>> trainDictionary = DictionaryFromInstanceHelper.toDictionary(instances);

		for (Instance instance : instanceProvider.getInstances()) {

			AutomatedSectionifcation sectionification = AutomatedSectionifcation.getInstance(instance);

			/**
			 * Apply dictionary to document
			 */
			for (AbstractAnnotation nerla : DictionaryFromInstanceHelper.getAnnotationsForInstance(instance,
					trainDictionary)) {

				DocumentLinkedAnnotation annotation = null;

				if (nerla.isInstanceOfDocumentLinkedAnnotation())
					annotation = nerla.asInstanceOfDocumentLinkedAnnotation();

				if (nerla.isInstanceOfEntityTemplate())
					annotation = nerla.asInstanceOfEntityTemplate().getRootAnnotation()
							.asInstanceOfDocumentLinkedAnnotation();

				if (annotation == null)
					continue;

				if (sectionification.getSection(annotation.asInstanceOfDocumentLinkedAnnotation()) != ESection.RESULTS)
					continue;

				annotations.putIfAbsent(instance, new HashSet<>());
				annotations.get(instance).add(annotation.asInstanceOfDocumentLinkedAnnotation());
			}
		}
	}

	public void readData(List<String> instanceNames) throws IOException {

		SlotType.excludeAll();

		if (includeGroups) {
			SCIOSlotTypes.hasTargetGroup.include();
			SCIOSlotTypes.hasReferenceGroup.include();

			SCIOSlotTypes.hasOrganismModel.includeRec();
			SCIOSlotTypes.hasInjuryModel.includeRec();
			SCIOSlotTypes.hasTreatmentType.include();
			SCIOSlotTypes.hasCompound.include();

			SCIOSlotTypes.hasGroupName.include();
//			SCIOSlotTypes.hasOrganismModel.excludeRec();
//			SCIOSlotTypes.hasInjuryModel.excludeRec();

			SCIOSlotTypes.hasInjuryModel.excludeRec();
			SCIOSlotTypes.hasTreatmentType.excludeRec();

			SCIOSlotTypes.hasOrganismModel.include();
			SCIOSlotTypes.hasInjuryModel.include();
			SCIOSlotTypes.hasOrganismModel.includeRec();
			SCIOSlotTypes.hasInjuryDevice.includeRec();
			SCIOSlotTypes.hasInjuryLocation.includeRec();
			SCIOSlotTypes.hasInjuryAnaesthesia.includeRec();
			SCIOSlotTypes.hasDeliveryMethod.includeRec();

			SCIOSlotTypes.hasTreatmentType.include();
			SCIOSlotTypes.hasCompound.include();
		}
		SCIOSlotTypes.hasTrend.includeRec();
		SCIOSlotTypes.hasInvestigationMethod.include();
//		SCIOSlotTypes.hasJudgement.includeRec();

		AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
				.setTrainingInstanceNames(instanceNames).build();

		InstanceProvider.maxNumberOfAnnotations = 1000;
		InstanceProvider.removeEmptyInstances = false;
		InstanceProvider.removeInstancesWithToManyAnnotations = false;

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		instances = instanceProvider.getTrainingInstances();

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
