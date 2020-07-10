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
import java.util.Map.Entry;
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
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.Instance.DeduplicationRule;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.BeamSearchEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.hterhors.semanticmr.json.JsonInstanceIO;
import de.hterhors.semanticmr.json.JsonInstanceReader;
import de.hterhors.semanticmr.json.converter.InstancesToJsonInstanceWrapper;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.ExperimentalGroupSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EDistinctGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.wrapper.DefinedExperimentalGroup;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.evaulation.CoarseGrainedResultEvaluation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.goldmodrules.OnlyDefinedExpGroupResults;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.heuristics.LocalSentenceHeuristic;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Result;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Trend;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.tools.SCIOAutomatedSectionifcation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.tools.SCIOAutomatedSectionifcation.ESection;

public class ResultSlotFillingHeuristic extends AbstractSemReadProject {

	public static void main(String[] args) throws Exception {

		new ResultSlotFillingHeuristic(1);

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
	public boolean includeGroups = true;

	public ResultSlotFillingHeuristic(int dataRandomSeed) throws Exception {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).apply()
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization())
				//
				.registerNormalizationFunction(new WeightNormalization())
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

		this.instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.result);

		this.scoreType = EScoreType.MACRO;

//		this.objectiveFunction = new SlotFillingObjectiveFunction(scoreType,
//				new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE, EEvaluationDetail.DOCUMENT_LINKED));

		this.objectiveFunction = new SlotFillingObjectiveFunction(scoreType,
				new BeamSearchEvaluator(EEvaluationDetail.ENTITY_TYPE, 10));

		this.dataRandomSeed = dataRandomSeed;

		readData();

//		getTFIDFAnnotations();

//		Map<Instance, Set<EntityTemplate>> expGroups = getGoldGroups();
		Map<Instance, Set<EntityTemplate>> expGroups = getPredictedGroups();
//		SCIOSlotTypes.hasOrganismModel.excludeRec();
//		SCIOSlotTypes.hasInjuryModel.excludeRec();

		SCIOSlotTypes.hasInjuryModel.excludeRec();
		SCIOSlotTypes.hasTreatmentType.excludeRec();

		SCIOSlotTypes.hasInjuryModel.include();
		SCIOSlotTypes.hasOrganismModel.includeRec();
		SCIOSlotTypes.hasInjuryDevice.includeRec();
		SCIOSlotTypes.hasInjuryLocation.includeRec();
		SCIOSlotTypes.hasInjuryAnaesthesia.includeRec();

		SCIOSlotTypes.hasTreatmentType.include();
		SCIOSlotTypes.hasCompound.include();

		String rand = String.valueOf(new Random().nextInt(100000));

		modelName = "Result" + rand;
		log.info("Model name = " + modelName);

		Map<Instance, Set<DocumentLinkedAnnotation>> annotations = new HashMap<>();

		Map<Instance, Set<DocumentLinkedAnnotation>> goldAnnotations = readAnnotations(
				new File("data/annotations/result/"));
//
		for (Instance instance : goldAnnotations.keySet()) {
			annotations.putIfAbsent(instance, new HashSet<>());
			annotations.get(instance).addAll(goldAnnotations.get(instance));
		}

//		annotations.values().stream().forEach(a -> {
//			for (Iterator<DocumentLinkedAnnotation> iterator = a.iterator(); iterator.hasNext();) {
//				DocumentLinkedAnnotation documentLinkedAnnotation = (DocumentLinkedAnnotation) iterator.next();
//				if (documentLinkedAnnotation.getEntityType() != SCIOEntityTypes.groupName) {
//					iterator.remove();
//				}
//			}
//		});

//		for (Instance instance : goldAnnotations.keySet()) {
//			annotations.putIfAbsent(instance, new HashSet<>());
//			annotations.get(instance).addAll(getGroupNameCandidates(instance));
//		}

//		Map<Instance, Set<DocumentLinkedAnnotation>> regexp = readAnnotations(
//				new File("data/slot_filling/result/regex_nerla"));
//		for (Instance instance : regexp.keySet()) {
//			annotations.putIfAbsent(instance, new HashSet<>());
//			annotations.get(instance).addAll(regexp.get(instance));
//		}

//		Map<Instance, Set<DocumentLinkedAnnotation>> annotationsInvFT = readAnnotations(
//				new File("data/annotations/fasttext/InvestigationMethod"));
//		for (Instance instance : annotationsInvFT.keySet()) {
//			annotations.putIfAbsent(instance, new HashSet<>());
//			annotations.get(instance).addAll(annotationsInvFT.get(instance));
//		}
//		Map<Instance, Set<DocumentLinkedAnnotation>> annotationsTrendFT = readAnnotations(
//				new File("data/annotations/fasttext/Trend"));
//		for (Instance instance : annotationsTrendFT.keySet()) {
//			annotations.putIfAbsent(instance, new HashSet<>());
//			annotations.get(instance).addAll(annotationsTrendFT.get(instance));
//		}
//
//		Map<Instance, Set<DocumentLinkedAnnotation>> annotationsInv = readAnnotations(
//				new File("data/annotations/invMTFIDF/"));
//		for (Instance instance : annotationsInv.keySet()) {
//			annotations.putIfAbsent(instance, new HashSet<>());
//			annotations.get(instance).addAll(annotationsInv.get(instance));
//		}
//
//		Map<Instance, Set<DocumentLinkedAnnotation>> annotationsTrend = readAnnotations(
//				new File("data/annotations/trendTFIDF/"));
//		for (Instance instance : annotationsTrend.keySet()) {
//			annotations.putIfAbsent(instance, new HashSet<>());
//			annotations.get(instance).addAll(annotationsTrend.get(instance));
//		}

		addDictionaryBasedAnnotations(annotations);

		LocalSentenceHeuristic heuristic = new LocalSentenceHeuristic(annotations, expGroups, trainingInstances);

		Map<Instance, State> results = heuristic.predictInstancesByHeuristic(testInstances);

		SCIOSlotTypes.hasGroupName.exclude();

		Score score = evaluate(log, results, this.objectiveFunction);
		log.info("Unsorted Score: " + score);

		scoreDetailed(results);

		CoarseGrainedResultEvaluation.evaluateCoarsGrained(objectiveFunction, results);

	}

	/**
	 * Calculates and annotations investigation methods and trend based on tfidf
	 * 
	 * @return
	 * @throws IOException
	 */
//	private Map<Instance, Set<DocumentLinkedAnnotation>> buildTFIDFAnnotations() throws IOException {
//		TFIDFTrendExtractor trendExtractor = new TFIDFTrendExtractor(trainingInstances);
//		TFIDFInvestigationMethodExtractor invMExtractor = new TFIDFInvestigationMethodExtractor(trainingInstances);
//		Map<Instance, Set<DocumentLinkedAnnotation>> annotations = new HashMap<>();
//		for (Instance instance : testInstances) {
//			System.out.println("NAME = " + instance.getName());
//			annotations.putIfAbsent(instance, new HashSet<>());
//			Set<DocumentLinkedAnnotation> invAnns = new HashSet<>();
//			for (List<DocumentToken> sentence : instance.getDocument().getSentences()) {
//
//				invAnns.addAll(invMExtractor.getInvestigationMethodForSentence(instance.getDocument(), sentence));
//			}
//			annotations.get(instance).addAll(invAnns);
//			new JsonNerlaIO(true).writeNerlas(new File("data/annotations/invMTFIDF/" + instance.getName()), invAnns);
//			Set<DocumentLinkedAnnotation> trendAnns = new HashSet<>();
//			for (List<DocumentToken> sentence : instance.getDocument().getSentences()) {
//				trendAnns.addAll(trendExtractor.getTrendsForSentence(instance.getDocument(), sentence));
//			}
//			annotations.get(instance).addAll(trendAnns);
//			new JsonNerlaIO(true).writeNerlas(new File("data/annotations/trendTFIDF/" + instance.getName()), trendAnns);
//			System.out.println("SIZE = " + annotations.get(instance).size());
//		}
//
//		return annotations;
//
//	}

	private Map<Instance, Set<EntityTemplate>> getPredictedGroups() throws Exception {

		File root = new File("data/annotations/slot_filling/experimental_group_Full/");
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
			ExperimentalGroupSlotFillingPredictor.maxCacheSize = 800_000;
			ExperimentalGroupSlotFillingPredictor.minCacheSize = 400_000;

			ExperimentalGroupSlotFillingPredictor a = new ExperimentalGroupSlotFillingPredictor(17, dataRandomSeed,
					trainingInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					devInstances.stream().map(i -> i.getName()).collect(Collectors.toList()),
					testInstances.stream().map(i -> i.getName()).collect(Collectors.toList()));
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
//return GroupNameExtraction.extractGroupNamesWithNPCHunks(EDistinctGroupNamesMode.NOT_DISTINCT, instance);
		return GroupNameExtraction.extractGroupNamesWithPattern(EDistinctGroupNamesMode.NOT_DISTINCT, instance);

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

	private Score scoreCardinality(List<EntityTemplate> goldAnnotations, List<EntityTemplate> predictedAnnotations) {
		int tp = Math.min(goldAnnotations.size(), predictedAnnotations.size());
		int fp = predictedAnnotations.size() > goldAnnotations.size()
				? predictedAnnotations.size() - goldAnnotations.size()
				: 0;
		int fn = predictedAnnotations.size() < goldAnnotations.size()
				? goldAnnotations.size() - predictedAnnotations.size()
				: 0;
		return new Score(tp, fp, fn);
	}

	private Score scoreIndividualProperty(SlotType property, List<Integer> bestAssignment,
			List<EntityTemplate> goldAnnotations, List<EntityTemplate> predictedAnnotations) {

		Score score = new Score();

		for (int goldIndex = 0; goldIndex < bestAssignment.size(); goldIndex++) {
			final int predictionIndex = bestAssignment.get(goldIndex);

			EntityTemplate goldAnnotation = goldAnnotations.size() > goldIndex ? goldAnnotations.get(goldIndex) : null;
			EntityTemplate predictedAnnotation = predictedAnnotations.size() > predictionIndex
					? predictedAnnotations.get(predictionIndex)
					: null;

			AbstractAnnotation goldProperty = null;
			AbstractAnnotation predictedProperty = null;

			if (goldAnnotation != null) {
				goldProperty = goldAnnotation.getSingleFillerSlot(property).getSlotFiller();
			}
			if (predictedAnnotation != null) {
				predictedProperty = predictedAnnotation.getSingleFillerSlot(property).getSlotFiller();
			}
			if (goldAnnotation != null || predictedAnnotation != null)
				score.add(this.objectiveFunction.getEvaluator().scoreSingle(goldProperty, predictedProperty));

//			if (goldProperty != null && predictedProperty != null) {
//				if (goldProperty.equals(predictedProperty)) {
//					score.increaseTruePositive();
//				} else {
//					score.increaseFalsePositive();
//					score.increaseFalseNegative();
//				}
//			} else {
//				if (goldProperty == null) {
//					score.increaseFalsePositive();
//				} else {
//					score.increaseFalseNegative();
//				}
//			}

		}

		return score.toMacro();
	}

	private void scoreDetailed(Map<Instance, State> results) {
		Map<Instance, List<EntityTemplate>> goldResults = new HashMap<>();
		Map<Instance, List<EntityTemplate>> predictedResults = new HashMap<>();

		Score macroRefScore = new Score(EScoreType.MACRO);
		Score macroTargetScore = new Score(EScoreType.MACRO);
		Score macroTrendScore = new Score(EScoreType.MACRO);
		Score macroInvestScore = new Score(EScoreType.MACRO);
		Score macroCardinalScore = new Score(EScoreType.MACRO);

		Score macroFullScore = new Score(EScoreType.MACRO);
		Score microFullScore = new Score(EScoreType.MICRO);

		Score macroBothScore = new Score(EScoreType.MACRO);
		Score macroResultSentenceScore = new Score(EScoreType.MACRO);
		for (State finalState : results.values()) {
			goldResults.put(finalState.getInstance(), new ArrayList<>());
			predictedResults.put(finalState.getInstance(), new ArrayList<>());

			for (AbstractAnnotation result : finalState.getGoldAnnotations().getAnnotations()) {

				goldResults.get(finalState.getInstance()).add(sortResult(result));

			}
			for (AbstractAnnotation result : finalState.getCurrentPredictions().getAnnotations()) {

				predictedResults.get(finalState.getInstance()).add(sortResult(result));

			}

			macroResultSentenceScore.add(evaluateResultSentences(goldResults.get(finalState.getInstance()),
					predictedResults.get(finalState.getInstance())).toMacro());

			Score scoreFullPart = objectiveFunction.getEvaluator().scoreMultiValues(
					goldResults.get(finalState.getInstance()), predictedResults.get(finalState.getInstance()),
					EScoreType.MICRO);
			microFullScore.add(scoreFullPart);
			macroFullScore.add(scoreFullPart.toMacro());

			
			/**
			 * TODO:
			 * 
			 * 
			 * MAYOR ERROR?
			 */
			
//			List<EntityTemplate> goldAnnotations = finalState.getInstance().getGoldAnnotations().getAnnotations();
			List<EntityTemplate> goldAnnotations = goldResults.get(finalState.getInstance());
			
			List<EntityTemplate> predictedAnnotations = predictedResults.get(finalState.getInstance());

			List<Integer> bestAssignment = ((BeamSearchEvaluator) objectiveFunction.getEvaluator())
					.getBestAssignment(goldAnnotations, predictedAnnotations, scoreType);

			Score macroFlatPart = new Score(EScoreType.MACRO);

			if (includeGroups) {
				Score referenceGroupScore = scoreIndividualProperty(SCIOSlotTypes.hasReferenceGroup, bestAssignment,
						goldAnnotations, predictedAnnotations);
				macroRefScore.add(referenceGroupScore);

				Score targetGroupScore = scoreIndividualProperty(SCIOSlotTypes.hasTargetGroup, bestAssignment,
						goldAnnotations, predictedAnnotations);
				macroTargetScore.add(targetGroupScore);
				macroFlatPart.add(referenceGroupScore);
				macroFlatPart.add(targetGroupScore);
			}

			Score trendScore = scoreIndividualProperty(SCIOSlotTypes.hasTrend, bestAssignment, goldAnnotations,
					predictedAnnotations);
			macroTrendScore.add(trendScore);
			Score investigationMethodScore = scoreIndividualProperty(SCIOSlotTypes.hasInvestigationMethod,
					bestAssignment, goldAnnotations, predictedAnnotations);
			macroInvestScore.add(investigationMethodScore);
			Score cardinality = scoreCardinality(goldAnnotations, predictedAnnotations);
			macroCardinalScore.add(cardinality.toMacro());

			macroFlatPart.add(trendScore);
			macroFlatPart.add(investigationMethodScore);
			macroFlatPart.add(cardinality);

			macroBothScore.add(macroFlatPart);
		}
		log.info("Sorted macro Result Sentence Score = " + macroResultSentenceScore);
		log.info("Sorted micro Full score = " + microFullScore);
		log.info("Sorted macro Full score = " + macroFullScore);
		log.info("Sorted macro Reference score = " + macroRefScore);
		log.info("Sorted macro Target score = " + macroTargetScore);
		log.info("Sorted macro Trend score = " + macroTrendScore);
		log.info("Sorted macro Investigation score = " + macroInvestScore);
		log.info("Sorted macro Cardinality score = " + macroCardinalScore);
		log.info("Sorted macro Both score = " + macroBothScore);
	}

	private Score evaluateResultSentences(List<EntityTemplate> gold, List<EntityTemplate> pred) {

		List<Integer> goldSentences = gold.stream().filter(a -> a != null).map(a -> new Result(a).getTrend())
				.filter(a -> a != null).flatMap(a -> new Trend(a).getRelevantSentenceIndexes().stream()).distinct()
				.sorted().collect(Collectors.toList());

		List<Integer> predSentences = pred.stream().filter(a -> a != null).map(a -> new Result(a).getTrend())
				.filter(a -> a != null).flatMap(a -> new Trend(a).getRelevantSentenceIndexes().stream()).distinct()
				.sorted().collect(Collectors.toList());

		return prf1(goldSentences, predSentences);
	}

	public Score prf1(Collection<Integer> annotations, Collection<Integer> otherAnnotations) {

		int tp = 0;
		int fp = 0;
		int fn = 0;

		outer: for (Integer a : annotations) {
			for (Integer oa : otherAnnotations) {
				if (oa.equals(a)) {
					tp++;
					continue outer;
				}
			}

			fn++;
		}

		fp = Math.max(otherAnnotations.size() - tp, 0);

		return new Score(tp, fp, fn);

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
		Map<EntityType, Set<String>> trainDictionary = DictionaryFromInstanceHelper.toDictionary(trainingInstances);

		for (Instance instance : instanceProvider.getInstances()) {

			SCIOAutomatedSectionifcation sectionification = SCIOAutomatedSectionifcation.getInstance(instance);

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

	public void readData() throws IOException {

		SlotType.excludeAll();

		if (includeGroups) {
			SCIOSlotTypes.hasTargetGroup.include();
			SCIOSlotTypes.hasReferenceGroup.include();

			SCIOSlotTypes.hasOrganismModel.includeRec();
			SCIOSlotTypes.hasInjuryModel.includeRec();
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
//		List<String> trainingInstanceNames = docs.subList(0, 10);
//		List<String> testInstanceNames = docs.subList(10, 15);
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

//		for (Instance instance : testInstances) {
//			for (AbstractAnnotation goldA : instance.getGoldAnnotations().getAbstractAnnotations()) {
//				System.out.println(instance.getName() + "\t" + goldA.toPrettyString());
//			}
//		}

	}

	private Map<Instance, Set<DocumentLinkedAnnotation>> readAnnotations(File groupNamesCacheDir) {

		JSONNerlaReader nerlaJSONReader = new JSONNerlaReader(groupNamesCacheDir);

		Map<Instance, Set<DocumentLinkedAnnotation>> annotations = new HashMap<>();
		for (Instance instance : instanceProvider.getInstances()) {

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

}
