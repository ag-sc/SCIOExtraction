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
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.Instance.DeduplicationRule;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;
import de.hterhors.semanticmr.eval.BeamSearchEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation.ESection;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.fasttext.FastTextSentenceClassification;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.heuristics.LocalTRENDSentenceHeuristic;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Result;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Trend;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.Stats;

public class TrendSlotFillingHeuristic extends AbstractSemReadProject {

	/**
	 * TODO: check which classes can be predicted best...
	 * 
	 * @param args
	 * @throws Exception
	 */

	public static void main(String[] args) throws Exception {

		if (args != null && args.length > 0)
			new TrendSlotFillingHeuristic(Long.parseLong(args[0]), args[1], Integer.parseInt(args[2]));
		else {
//			new TrendSlotFillingHeuristic(1000L, "GOLD", 0);
//			new ResultSlotFillingHeuristic(1000L, "PREDICT_COVERAGE");
//			new ResultSlotFillingHeuristic(1000L, "GOLD");
			new ResultSlotFillingHeuristic(1000L, "GOLD_COVERAGE", 0);
		}
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private final File instanceDirectory;
	private final IObjectiveFunction objectiveFunction;

	private final long dataRandomSeed;
	final int fold;
	static private final EScoreType scoreType = EScoreType.MACRO;
	private InstanceProvider instanceProvider;

	private List<Instance> trainingInstances;
	private List<Instance> devInstances;
	private List<Instance> testInstances;
	private String modelName;
	private ENERModus modus;

//	static Map<EntityType, Score> countInvest = new HashMap<>();

	public TrendSlotFillingHeuristic(long dataRandomSeed, String modusName, int fold) throws Exception {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).apply()
				//
				.build();

		this.instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.result);

//		this.objectiveFunction = new SlotFillingObjectiveFunction(scoreType,
//				new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE, EEvaluationDetail.DOCUMENT_LINKED));

		this.objectiveFunction = new SlotFillingObjectiveFunction(scoreType,
				new BeamSearchEvaluator(EEvaluationDetail.ENTITY_TYPE, 10));

		this.fold = fold;
		this.dataRandomSeed = dataRandomSeed;
		modus = ENERModus.valueOf(modusName.split("_")[0]);

		boolean isCoverage = modusName.contains("COVERAGE");

		readData();
		Stats.countVariables(0, instanceProvider.getInstances());
		System.exit(1);
//		analyze();

		boolean includeFastTextAnnotations = true;

		modelName = modus + "_Result_FinalDiss_" + dataRandomSeed + "_fold_" + fold;
//		modelName = "Result_PREDICT";
		log.info("Model name = " + modelName);

		Map<Instance, Set<DocumentLinkedAnnotation>> annotations = new HashMap<>();

		Map<Instance, Set<DocumentLinkedAnnotation>> goldAnnotations = readAnnotations(
				new File("data/annotations/result/"));

//

		if (modus == ENERModus.GOLD) {

			for (Instance instance : goldAnnotations.keySet()) {
				annotations.putIfAbsent(instance, new HashSet<>());

				for (DocumentLinkedAnnotation instance2 : goldAnnotations.get(instance)) {
//					if (!EntityType.get("InvestigationMethod").getRelatedEntityTypes()
//							.contains(instance2.getEntityType()))
					annotations.get(instance).add(instance2);
				}
			}
		}

		if (modus == ENERModus.PREDICT) {

			if (includeFastTextAnnotations) {
				FastTextSentenceClassification trend = new FastTextSentenceClassification(modelName, false,
						SCIOEntityTypes.trend, trainingInstances, true);

				Map<Instance, Set<DocumentLinkedAnnotation>> annotationsTredFT = trend
						.predictNerlasEvaluate(testInstances);

				for (Instance instance : annotationsTredFT.keySet()) {
					annotations.putIfAbsent(instance, new HashSet<>());
					System.out.println(annotationsTredFT.get(instance).size());
					annotations.get(instance).addAll(annotationsTredFT.get(instance));
				}

			}

		}

		LocalTRENDSentenceHeuristic heuristic = new LocalTRENDSentenceHeuristic(annotations);

		Map<Instance, State> results = new HashMap<>();
		if (isCoverage)
			results = computeCoverage(annotations, testInstances);
		else
			results = heuristic.predictInstancesByHeuristic(testInstances);

		Map<Instance, Set<AbstractAnnotation>> goldTrends = new HashMap<>();
		Map<Instance, Set<AbstractAnnotation>> predictTrends = new HashMap<>();

		for (Entry<Instance, State> instance : results.entrySet()) {
			predictTrends.put(instance.getKey(), new HashSet<>());
			for (AbstractAnnotation trend : instance.getValue().getCurrentPredictions().getAnnotations()) {
				predictTrends.get(instance.getKey()).add(trend);
			}

		}
		for (Entry<Instance, State> instance : results.entrySet()) {
			goldTrends.put(instance.getKey(), new HashSet<>());
			for (AbstractAnnotation trend : instance.getValue().getGoldAnnotations().getAnnotations()) {

				AbstractAnnotation t = new Result(trend).getTrend();
				if (t != null)
					goldTrends.get(instance.getKey()).add(t);
			}

		}

//		System.exit(1);
		/**
		 * TREND EVALUATION
		 */

		SlotType.excludeAll();

		Set<SlotType> slotTypesToConsider = new HashSet<>();
		slotTypesToConsider.add(SCIOSlotTypes.hasSignificance);
		slotTypesToConsider.add(SCIOSlotTypes.hasPValue);
		slotTypesToConsider.add(SCIOSlotTypes.hasDifference);

		for (SlotType slotType : slotTypesToConsider) {
			slotType.include();
		}

		Map<Instance, State> finalStates = new HashMap<>();
		Map<Instance, State> coverageStates = new HashMap<>();

		for (Instance instance : results.keySet()) {

			List<AbstractAnnotation> coverageTrends = new ArrayList<>();
			List<AbstractAnnotation> trends = new ArrayList<>(predictTrends.get(instance));

			List<AbstractAnnotation> gt = new ArrayList<>(goldTrends.get(instance));

			log.info("########PREDICTED########");
			for (AbstractAnnotation string : trends) {
				log.info(string.toPrettyString());
			}

			log.info("########GOLD########");
			for (AbstractAnnotation string : gt) {
				log.info(string.toPrettyString());
			}

			Instance instanceT = new Instance(instance.getOriginalContext(), instance.getDocument(),
					new Annotations(gt));

			Annotations currentPredictions = new Annotations(trends);
			finalStates.put(instanceT, new State(instanceT, currentPredictions));

			Annotations coveragePredictions = new Annotations(coverageTrends);
			coverageStates.put(instanceT, new State(instanceT, coveragePredictions));

		}

		AbstractEvaluator eval = new BeamSearchEvaluator(EEvaluationDetail.ENTITY_TYPE, 20);

		Score allScore = new Score();
		Map<String, Score> scoreMap = new HashMap<>();
//
		PerSlotEvaluator.evalRoot(EScoreType.MACRO, finalStates, coverageStates, eval, scoreMap);

		PerSlotEvaluator.evalProperties(EScoreType.MACRO, finalStates, coverageStates, slotTypesToConsider, eval,
				scoreMap);

		PerSlotEvaluator.evalCardinality(EScoreType.MACRO, finalStates, coverageStates, scoreMap);

		PerSlotEvaluator.evalOverall(EScoreType.MACRO, finalStates, coverageStates, eval, scoreMap);

		log.info("\n\n\n*************************");

		for (Entry<String, Score> sm : scoreMap.entrySet()) {
			log.info(sm.getKey() + "\t" + sm.getValue().toTSVString());
		}

		log.info("*************************");

		log.info(allScore);

	}

	private Map<Instance, State> computeCoverage(Map<Instance, Set<DocumentLinkedAnnotation>> annotations,
			List<Instance> testInstances) {

		Map<Instance, State> coverage = new HashMap<>();

		for (Instance instance : testInstances) {
			List<AbstractAnnotation> resultAnnotations = new ArrayList<>();

			for (AbstractAnnotation goldResult : instance.getGoldAnnotations().getAnnotations()) {

				AbstractAnnotation coveredResult = toCoveredResult(
						new Trend(new Result(goldResult.asInstanceOfEntityTemplate()).getTrend()),
						annotations.getOrDefault(instance, Collections.emptySet()));
				if (coveredResult != null)
					resultAnnotations.add(coveredResult);
			}

			Annotations coverageAnnotations = new Annotations(resultAnnotations);
			coverage.put(instance, new State(instance, coverageAnnotations));
		}

		return coverage;
	}

	private AbstractAnnotation toCoveredResult(Trend goldTrend, Set<DocumentLinkedAnnotation> annotations) {

		TrendData resultData = new TrendData();

		Score bestScore;
		DocumentLinkedAnnotation goldAnn;
		DocumentLinkedAnnotation bestAnnotation;

		if (goldTrend.get() != null) {
			bestScore = new Score();
			goldAnn = goldTrend.getRootAnntoationAsDocumentLinkedAnnotation();
			bestAnnotation = null;
			if (goldAnn != null) {
				for (DocumentLinkedAnnotation annotation : annotations) {

					Score s = objectiveFunction.getEvaluator().scoreSingle(annotation, goldAnn);

					if (s.getF1() > bestScore.getF1()) {
						bestScore = s;
						bestAnnotation = annotation;
					}

				}
			}
			resultData.trend = bestAnnotation;

			bestScore = new Score();
			goldAnn = goldTrend.getDifferenceAsDocumentLinkedAnnotation();
			bestAnnotation = null;
			if (goldAnn != null) {
				for (DocumentLinkedAnnotation annotation : annotations) {

					Score s = objectiveFunction.getEvaluator().scoreSingle(annotation, goldAnn);

					if (s.getF1() > bestScore.getF1()) {
						bestScore = s;
						bestAnnotation = annotation;
					}

				}
			}
			resultData.difference = bestAnnotation;

			bestScore = new Score();
			goldAnn = goldTrend.getPValueAsDocumentLinkedAnnotation();
			bestAnnotation = null;
			if (goldAnn != null) {
				for (DocumentLinkedAnnotation annotation : annotations) {

					Score s = objectiveFunction.getEvaluator().scoreSingle(annotation, goldAnn);

					if (s.getF1() > bestScore.getF1()) {
						bestScore = s;
						bestAnnotation = annotation;
					}

				}
			}
			resultData.pValue = bestAnnotation;

			bestScore = new Score();
			goldAnn = goldTrend.getSignificanceRootAsDocumentLinkedAnnotation();
			bestAnnotation = null;
			if (goldAnn != null) {
				for (DocumentLinkedAnnotation annotation : annotations) {

					Score s = objectiveFunction.getEvaluator().scoreSingle(annotation, goldAnn);

					if (s.getF1() > bestScore.getF1()) {
						bestScore = s;
						bestAnnotation = annotation;
					}

				}
			}
			resultData.significance = bestAnnotation;
		}

		return resultData.toResult(true);
	}

	public static Score scoreCardinality(List<EntityTemplate> goldAnnotations,
			List<EntityTemplate> predictedAnnotations) {
		int tp = Math.min(goldAnnotations.size(), predictedAnnotations.size());
		int fp = predictedAnnotations.size() > goldAnnotations.size()
				? predictedAnnotations.size() - goldAnnotations.size()
				: 0;
		int fn = predictedAnnotations.size() < goldAnnotations.size()
				? goldAnnotations.size() - predictedAnnotations.size()
				: 0;
		return new Score(tp, fp, fn);
	}

	public static Score scoreIndividualProperty(SlotType property, List<Integer> bestAssignment,
			List<EntityTemplate> goldAnnotations, List<EntityTemplate> predictedAnnotations,
			IObjectiveFunction objectiveFunction) {

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
			if (goldAnnotation != null || predictedAnnotation != null) {
				Score s = objectiveFunction.getEvaluator().scoreSingle(goldProperty, predictedProperty);
				score.add(s);
				if (property == SCIOSlotTypes.hasInvestigationMethod) {

					EntityType e;
					if (goldProperty != null) {
						e = goldProperty.getEntityType();
					} else {
						e = predictedProperty.getEntityType();
					}
//					countInvest.putIfAbsent(e, new Score());
//					countInvest.get(e)
//							.add(objectiveFunction.getEvaluator().scoreSingle(goldAnnotation, predictedAnnotation));
				}

			}

		}

		return score.toMacro();
	}

	public static void countCorrectEntityTypes(SlotType property, List<Integer> bestAssignment,
			List<EntityTemplate> goldAnnotations, List<EntityTemplate> predictedAnnotations,
			IObjectiveFunction objectiveFunction) {

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
			if (goldAnnotation != null || predictedAnnotation != null) {
				Score s = objectiveFunction.getEvaluator().scoreSingle(goldProperty, predictedProperty);
//				score.add(s);
			}

		}

	}

	public static class EvalPair {

		final public Instance instance;

		final public List<AbstractAnnotation> gold;
		final public List<AbstractAnnotation> pred;

		public EvalPair(Instance instance, List<AbstractAnnotation> gold, List<AbstractAnnotation> pred) {
			this.instance = instance;
			this.gold = gold;
			this.pred = pred;
		}

		public static List<EvalPair> toListOf(Map<Instance, State> results) {

			List<EvalPair> list = new ArrayList<>();

			for (Entry<Instance, State> r : results.entrySet()) {
				list.add(new EvalPair(r.getKey(), r.getValue().getGoldAnnotations().getAnnotations(),
						r.getValue().getCurrentPredictions().getAnnotations()));
			}

			return list;
		}

		@Override
		public String toString() {
			return "EvalPair [instance=" + instance + ", gold=" + gold + ", pred=" + pred + "]";
		}

	}

	public static Score prf1(Collection<Integer> annotations, Collection<Integer> otherAnnotations) {

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

	private List<String> trainingInstanceNames;
	private List<String> testInstanceNames;

	public void readData() throws IOException {

		List<String> docs = Files.readAllLines(new File("src/main/resources/corpus_docs.csv").toPath());
		Collections.sort(docs);

		Collections.shuffle(docs, new Random(dataRandomSeed));

		final int x = (int) (((double) docs.size() / 100D) * 90D);

		int sum = (docs.size() - x);

		List<String> testI = docs.subList(fold * sum, Math.min((fold + 1) * sum, docs.size()));
		List<String> trainI = new ArrayList<>(docs);
		trainI.removeAll(testI);
		List<String> itr = new ArrayList<>();
		itr.addAll(trainI);
		itr.addAll(testI);

		/**
		 * TODO: FULL MODEL
		 */

		trainingInstanceNames = itr.subList(0, x);
		testInstanceNames = itr.subList(x, docs.size());

//		List<String> trainingInstanceNames = docs.subList(0, 10);
//		List<String> testInstanceNames = docs.subList(10, 15);
//		trainingInstanceNames = docs.subList(0, x);
//		testInstanceNames = docs.subList(x, docs.size());

		AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
				.setTrainingInstanceNames(trainingInstanceNames).setTestInstanceNames(testInstanceNames).build();

//		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
//				.setTestProportion(20).setSeed(1000L).build();

		InstanceProvider.maxNumberOfAnnotations = 100;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		Collection<GoldModificationRule> goldModificationRules;

		goldModificationRules = Arrays.asList();
		DeduplicationRule deduplicationRule = (a1, a2) -> {
			return a1.evaluateEquals(objectiveFunction.getEvaluator(), a2);
		};

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor, goldModificationRules,
				deduplicationRule);

		trainingInstances = instanceProvider.getTrainingInstances();
		devInstances = instanceProvider.getDevelopmentInstances();
		testInstances = instanceProvider.getTestInstances();

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
