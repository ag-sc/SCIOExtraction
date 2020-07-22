package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.result_sentences;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation.ESection;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.helper.GroupNameExtraction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.fasttext.FastTextSentenceClassification;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.investigationMethod.InvestigationMethodIDFPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.trend.TrendIDFPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EDistinctGroupNamesMode;

/**
 * Combines Trend extraction and InvestigationMethod extraction via TF IDF
 * 
 * @author hterhors
 *
 */

public class ExtractSentencesWithResults {

	public static void main(String[] args) throws IOException {
		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).build();

		ExtractSentencesWithResults.evaluatRandom9010Split(1000L);

	}

	private List<Instance> investigationMethodInstances;
	private List<Instance> trendInstances;

	private List<Instance> investigationMethodTrainingInstances;
	private List<Instance> investigationMethodTestInstances;
	private List<Instance> trendTrainingInstances;
	private List<Instance> trendTestInstances;

	private TrendIDFPredictor trendPredictor;
	private InvestigationMethodIDFPredictor investigationPredictor;

	private Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> trendPredictions;
	private Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> trendGroundTruth;
	private Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> investigationMethodsPredictions;
	private Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> investigationMethodsGroundTruth;

	public ExtractSentencesWithResults() {
		investigationMethodInstances = readRandomInstances(SCIOEntityTypes.investigationMethod);
		trendInstances = readRandomInstances(SCIOEntityTypes.trend);
	}

	public ExtractSentencesWithResults(List<String> trainingInstanceNames, List<String> testInstanceNames) {

		investigationMethodInstances = readInstances(SCIOEntityTypes.investigationMethod, trainingInstanceNames,
				testInstanceNames);
		trendInstances = readInstances(SCIOEntityTypes.trend, trainingInstanceNames, testInstanceNames);

		investigationMethodTrainingInstances = investigationMethodInstances.stream()
				.filter(in -> trainingInstanceNames.contains(in.getName())).collect(Collectors.toList());
		investigationMethodTestInstances = investigationMethodInstances.stream()
				.filter(in -> testInstanceNames.contains(in.getName())).collect(Collectors.toList());
		trendTrainingInstances = trendInstances.stream().filter(in -> trainingInstanceNames.contains(in.getName()))
				.collect(Collectors.toList());
		trendTestInstances = trendInstances.stream().filter(in -> testInstanceNames.contains(in.getName()))
				.collect(Collectors.toList());

		buildTreandPredictor(trendTrainingInstances);

		buildInvestigationMethodPredictor(investigationMethodTrainingInstances);

		trendGroundTruth = trendPredictor.getGroundTruthAnnotations(trendTestInstances);

		investigationMethodsGroundTruth = investigationPredictor
				.getGroundTruthAnnotations(investigationMethodTestInstances);
	}

//	public Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> predictTrendInstances() {
//		try {
//			FastTextSentenceClassification trend;
//			trend = new FastTextSentenceClassification("trendTEST", false, SCIOEntityTypes.trend,
//					trendTrainingInstances);
//			Map<Instance, Set<DocumentLinkedAnnotation>> annotationsTredFT = trend.predictNerlas(trendTestInstances);
//			trendPredictions = new HashMap<>();
//			for (Instance instance : annotationsTredFT.keySet()) {
//				trendPredictions.putIfAbsent(instance, new HashMap<>());
//				trendPredictions.get(instance).putAll(toSentenceBasedAnnotations(annotationsTredFT.get(instance)));
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//		return trendPredictions;
//	}
//	
//	public Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> predictInvestigationMethodInstances() {
//		try {
//			FastTextSentenceClassification trend;
//			trend = new FastTextSentenceClassification("invMTEST", false, SCIOEntityTypes.investigationMethod,
//					investigationMethodTrainingInstances);
//			Map<Instance, Set<DocumentLinkedAnnotation>> annotationsTredFT = trend
//					.predictNerlas(investigationMethodTestInstances);
//			investigationMethodsPredictions = new HashMap<>();
//			for (Instance instance : annotationsTredFT.keySet()) {
//				investigationMethodsPredictions.putIfAbsent(instance, new HashMap<>());
//				investigationMethodsPredictions.get(instance)
//				.putAll(toSentenceBasedAnnotations(annotationsTredFT.get(instance)));
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//		return investigationMethodsPredictions;
//	}

//	public Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> predictTrendInstances() {
//		try {
//			FastTextSentenceClassification trend;
//			trend = new FastTextSentenceClassification("trendTEST", false, SCIOEntityTypes.trend,
//					trendTrainingInstances);
//			Map<Instance, Set<DocumentLinkedAnnotation>> annotationsTredFT = trend.predictNerlas(trendTestInstances);
//			trendPredictions = new HashMap<>();
//			for (Instance instance : annotationsTredFT.keySet()) {
//				trendPredictions.putIfAbsent(instance, new HashMap<>());
//				trendPredictions.get(instance).putAll(toSentenceBasedAnnotations(annotationsTredFT.get(instance)));
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		for (Entry<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> instance : trendPredictor
//				.predictInstances(trendTestInstances).entrySet()) {
//			trendPredictions.putIfAbsent(instance.getKey(), new HashMap<>());
//			trendPredictions.get(instance.getKey()).keySet().retainAll(instance.getValue().keySet());
//		}
//
//		return trendPredictions;
//	}
//
//	public Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> predictInvestigationMethodInstances() {
//		try {
//			FastTextSentenceClassification trend;
//			trend = new FastTextSentenceClassification("invMTEST", false, SCIOEntityTypes.investigationMethod,
//					investigationMethodTrainingInstances);
//			Map<Instance, Set<DocumentLinkedAnnotation>> annotationsTredFT = trend
//					.predictNerlas(investigationMethodTestInstances);
//			investigationMethodsPredictions = new HashMap<>();
//			for (Instance instance : annotationsTredFT.keySet()) {
//				investigationMethodsPredictions.putIfAbsent(instance, new HashMap<>());
//				investigationMethodsPredictions.get(instance)
//						.putAll(toSentenceBasedAnnotations(annotationsTredFT.get(instance)));
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		for (Entry<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> instance : investigationPredictor
//				.predictInstances(investigationMethodTestInstances).entrySet()) {
//			investigationMethodsPredictions.putIfAbsent(instance.getKey(), new HashMap<>());
//			investigationMethodsPredictions.get(instance.getKey()).keySet().retainAll(instance.getValue().keySet());
//		}
//
//		return investigationMethodsPredictions;
//	}
//	public Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> predictTrendInstances() {
//		try {
//			FastTextSentenceClassification trend;
//			trend = new FastTextSentenceClassification("trendTEST", false, SCIOEntityTypes.trend,
//					trendTrainingInstances);
//			Map<Instance, Set<DocumentLinkedAnnotation>> annotationsTredFT = trend.predictNerlas(trendTestInstances);
//			trendPredictions = new HashMap<>();
//			for (Instance instance : annotationsTredFT.keySet()) {
//				trendPredictions.putIfAbsent(instance, new HashMap<>());
//				trendPredictions.get(instance).putAll(toSentenceBasedAnnotations(annotationsTredFT.get(instance)));
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//		for (Entry<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> instance : trendPredictor
//				.predictInstances(trendTestInstances).entrySet()) {
//			trendPredictions.putIfAbsent(instance.getKey(), new HashMap<>());
//			for (Entry<Integer, Set<DocumentLinkedAnnotation>> in : instance.getValue().entrySet()) {
//				trendPredictions.get(instance.getKey()).putIfAbsent(in.getKey(), new HashSet<>());
//				trendPredictions.get(instance.getKey()).get(in.getKey()).addAll(in.getValue());
//			}
//		}
//		
//		return trendPredictions;
//	}
//	
//	public Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> predictInvestigationMethodInstances() {
//		try {
//			FastTextSentenceClassification trend;
//			trend = new FastTextSentenceClassification("invMTEST", false, SCIOEntityTypes.investigationMethod,
//					investigationMethodTrainingInstances);
//			Map<Instance, Set<DocumentLinkedAnnotation>> annotationsTredFT = trend
//					.predictNerlas(investigationMethodTestInstances);
//			investigationMethodsPredictions = new HashMap<>();
//			for (Instance instance : annotationsTredFT.keySet()) {
//				investigationMethodsPredictions.putIfAbsent(instance, new HashMap<>());
//				investigationMethodsPredictions.get(instance)
//				.putAll(toSentenceBasedAnnotations(annotationsTredFT.get(instance)));
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//		for (Entry<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> instance : investigationPredictor
//				.predictInstances(investigationMethodTestInstances).entrySet()) {
//			investigationMethodsPredictions.putIfAbsent(instance.getKey(), new HashMap<>());
//			for (Entry<Integer, Set<DocumentLinkedAnnotation>> in : instance.getValue().entrySet()) {
//				investigationMethodsPredictions.get(instance.getKey()).putIfAbsent(in.getKey(), new HashSet<>());
//				investigationMethodsPredictions.get(instance.getKey()).get(in.getKey()).addAll(in.getValue());
//			}
//		}
//		
//		return investigationMethodsPredictions;
//	}

	public Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> predictTrendInstances() {

		Map<Instance, Set<DocumentLinkedAnnotation>> regexp = readAnnotations(
				new File("data/slot_filling/trend/regex_nerla"), trendInstances);
		trendPredictions = new HashMap<>();
		for (Instance instance : regexp.keySet()) {
			trendPredictions.putIfAbsent(instance, new HashMap<>());
			trendPredictions.get(instance).putAll(toSentenceBasedAnnotations(regexp.get(instance)));
		}

		return trendPredictions;
	}

	public Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> predictInvestigationMethodInstances() {

		Map<Instance, Set<DocumentLinkedAnnotation>> regexp = readAnnotations(
				new File("data/slot_filling/investigation_method/regex_nerla"), investigationMethodInstances);
		investigationMethodsPredictions = new HashMap<>();
		for (Instance instance : regexp.keySet()) {
			investigationMethodsPredictions.putIfAbsent(instance, new HashMap<>());
			investigationMethodsPredictions.get(instance).putAll(toSentenceBasedAnnotations(regexp.get(instance)));
		}
		return investigationMethodsPredictions;
	}
//	public Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> predictTrendInstances() {
//		trendPredictions = trendPredictor.predictInstances(trendTestInstances);
//		return trendPredictions;
//	}
//	
//	public Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> predictInvestigationMethodInstances() {
//		investigationMethodsPredictions = investigationPredictor.predictInstances(investigationMethodTestInstances);
//		
//		return investigationMethodsPredictions;
//	}

	private Map<Instance, Set<DocumentLinkedAnnotation>> readAnnotations(File groupNamesCacheDir,
			List<Instance> instances) {

		JSONNerlaReader nerlaJSONReader = new JSONNerlaReader(groupNamesCacheDir);

		Map<Instance, Set<DocumentLinkedAnnotation>> annotations = new HashMap<>();
		for (Instance instance : instances) {

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

	private void buildInvestigationMethodPredictor(List<Instance> investigationMethodTrainingInstances) {
		investigationPredictor = new InvestigationMethodIDFPredictor();
		investigationPredictor.setAnnotationStopwords(Arrays.asList("either", "number", "group", "groups", "numbers"));
		investigationPredictor.setSentenceStopWords(Arrays.asList("arrow", "asterisk", "*", "bar"));
		investigationPredictor.setRemoveEntityTypes(Arrays.asList(EntityType.get("InvestigationMethod")));
		investigationPredictor.setEnableUniGram(true);
		investigationPredictor.setEnableBiGram(false);
		investigationPredictor.setRestrictToSections(Arrays.asList(ESection.RESULTS));
		investigationPredictor.setLocalNormalizing(true);
		investigationPredictor.setEnableStemming(true);
		investigationPredictor.setEnableLowerCasing(true);
		investigationPredictor.train(investigationMethodTrainingInstances);
	}

	private void buildTreandPredictor(List<Instance> trendTrainingInstances) {
		trendPredictor = new TrendIDFPredictor();
		trendPredictor.setAnnotationStopwords(Arrays.asList("rat", "rats", "either", "number", "group", "groups",
				"numbers", "treatment", "respectively"));
		trendPredictor.setRemoveEntityTypes(
				Arrays.asList(EntityType.get("AlphaSignificanceNiveau"), EntityType.get("RepeatedMeasureTrend")));
		trendPredictor.setSentenceStopWords(Arrays.asList("arrow", "asterisk", "*", "bar"));
		trendPredictor.setEnableUniGram(true);
		trendPredictor.setEnableBiGram(false);
		trendPredictor.setLocalNormalizing(true);
		trendPredictor.setEnableStemming(true);
		trendPredictor.setRestrictToSections(Arrays.asList(ESection.RESULTS));
		trendPredictor.setEnableLowerCasing(true);
		trendPredictor.train(trendTrainingInstances);
	}

	private Instance instanceForName(List<Instance> instances, String instanceName) {
		for (Instance instance : instances) {
			if (instance.getName().equals(instanceName))
				return instance;
		}
		return null;
	}

	public static void evaluatRandom9010Split(long randomSeed) throws IOException {

		Random rand = new Random(randomSeed);
		Score macroTrendScorePreFiltered = new Score(EScoreType.MICRO);
		Score macroTrendScorePostFiltered = new Score(EScoreType.MICRO);
		Score macroInvestigationScorePreFiltered = new Score(EScoreType.MICRO);
		Score macroInvestigationScorePostFiltered = new Score(EScoreType.MICRO);
//		Score macroTrendScorePreFiltered = new Score(EScoreType.MACRO);
//		Score macroTrendScorePostFiltered = new Score(EScoreType.MACRO);
//		Score macroInvestigationScorePreFiltered = new Score(EScoreType.MACRO);
//		Score macroInvestigationScorePostFiltered = new Score(EScoreType.MACRO);

		List<Instance> investigationMethodInstances = readRandomInstances(SCIOEntityTypes.investigationMethod);

		for (int i = 0; i < 10; i++) {
			System.out.println("PROGRESS: " + i);

			Collections.sort(investigationMethodInstances);
			Collections.shuffle(investigationMethodInstances, rand);
			final int x = (int) (((double) investigationMethodInstances.size() / 100D) * 90D);

			List<String> trainingInstanceNames = investigationMethodInstances.subList(0, x).stream()
					.map(in -> in.getName()).collect(Collectors.toList());
			List<String> testInstanceNames = investigationMethodInstances
					.subList(x, investigationMethodInstances.size()).stream().map(in -> in.getName())
					.collect(Collectors.toList());

			ExtractSentencesWithResults t = new ExtractSentencesWithResults(trainingInstanceNames, testInstanceNames);

			t.predictTrendInstances();
			t.predictInvestigationMethodInstances();

			macroTrendScorePreFiltered.add(t.evaluateTrend());
			macroInvestigationScorePreFiltered.add(t.evaluateInvestigationMethod());

			t.filter();

			macroTrendScorePostFiltered.add(t.evaluateTrend());
			macroInvestigationScorePostFiltered.add(t.evaluateInvestigationMethod());
//			macroTrendScorePreFiltered.add(t.evaluateTrend().toMacro());
//			macroTrendScorePostFiltered.add(t.evaluateInvestigationMethod().toMacro());
//			
//			t.filter();
//			
//			macroInvestigationScorePreFiltered.add(t.evaluateTrend().toMacro());
//			macroInvestigationScorePostFiltered.add(t.evaluateInvestigationMethod().toMacro());

			t.trendPredictor.printIDFs("idf/trend_idf.csv");
			t.investigationPredictor.printIDFs("idf/investigationMethod_idf.csv");
//			break;
			System.out.println("trend pre filter:" + macroTrendScorePreFiltered);
			System.out.println("trend post filter:" + macroTrendScorePostFiltered);
			System.out.println("inv method pre filter:" + macroInvestigationScorePreFiltered);
			System.out.println("inv method post filter:" + macroInvestigationScorePostFiltered);
		}
//		Score [macroF1=0.258, macroPrecision=0.204, macroRecall=0.351, macroAddCounter=10] trend

		System.out.println("Final trend pre filter:" + macroTrendScorePreFiltered);
		System.out.println("Final trend post filter:" + macroTrendScorePostFiltered);
		System.out.println("Final inv method pre filter:" + macroInvestigationScorePreFiltered);
		System.out.println("Final inv method post filter:" + macroInvestigationScorePostFiltered);
	}

	private Map<Integer, Set<DocumentLinkedAnnotation>> toSentenceBasedAnnotations(
			Set<DocumentLinkedAnnotation> documentAnnotations) {
		Map<Integer, Set<DocumentLinkedAnnotation>> entitiesPerSentence = new HashMap<>();
		if (documentAnnotations != null)
			for (DocumentLinkedAnnotation ann : documentAnnotations) {

				entitiesPerSentence.putIfAbsent(ann.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex(),
						new HashSet<>());
				entitiesPerSentence.get(ann.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex()).add(ann);
			}
		return entitiesPerSentence;
	}

//	Score [macroF1=0.137, macroPrecision=0.081, macroRecall=0.462, macroAddCounter=10] trend
//			Score [macroF1=0.394, macroPrecision=0.459, macroRecall=0.345, macroAddCounter=10]trend filter with gold inv
//	Score [macroF1=0.139, macroPrecision=0.082, macroRecall=0.460, macroAddCounter=10] trend filter with predictions inv

//	Score [macroF1=0.137, macroPrecision=0.080, macroRecall=0.474, macroAddCounter=10] inv 
//			Score [macroF1=0.293, macroPrecision=0.216, macroRecall=0.457, macroAddCounter=10] inv filter with gold trend
//	Score [macroF1=0.063, macroPrecision=0.034, macroRecall=0.496, macroAddCounter=10]inv filter with predictions trend 

	public void filter() {
		/*
		 * FILTER by Same Sentence Appearance
		 */

		Set<String> instanceNames = new HashSet<>(
				investigationMethodTestInstances.stream().map(e -> e.getName()).collect(Collectors.toSet()));

		for (String instanceName : instanceNames) {

			final Instance trendInstance = instanceForName(trendTestInstances, instanceName);
			final Instance investigationInstance = instanceForName(investigationMethodTestInstances, instanceName);

			Map<Integer, Set<DocumentLinkedAnnotation>> trendAnnotations = trendPredictions.getOrDefault(trendInstance,
					Collections.emptyMap());
			Map<Integer, Set<DocumentLinkedAnnotation>> investigationMethodAnnotations = investigationMethodsPredictions
					.getOrDefault(investigationInstance, Collections.emptyMap());

//			List<DocumentLinkedAnnotation> list = GroupNameExtraction
//					.extractGroupNamesWithPattern(EDistinctGroupNamesMode.NOT_DISTINCT, trendInstance);

//			Map<Integer, Set<DocumentLinkedAnnotation>> groupNameAnnotations = toSentenceBasedAnnotations(
//					new HashSet<>(list));

			for (Iterator<Integer> iterator = trendAnnotations.keySet().iterator(); iterator.hasNext();) {
				Integer sentenceIndex = iterator.next();
				if (trendAnnotations.get(sentenceIndex).isEmpty())
					iterator.remove();
			}
			for (Iterator<Integer> iterator = investigationMethodAnnotations.keySet().iterator(); iterator.hasNext();) {
				Integer sentenceIndex = iterator.next();
				if (investigationMethodAnnotations.get(sentenceIndex).isEmpty())
					iterator.remove();
			}

//			for (Iterator<Integer> iterator = groupNameAnnotations.keySet().iterator(); iterator.hasNext();) {
//				Integer sentenceIndex = iterator.next();
//				if (groupNameAnnotations.get(sentenceIndex).isEmpty())
//					iterator.remove();
//			}

//			trendAnnotations.keySet().retainAll(groupNameAnnotations.keySet());
//			investigationMethodAnnotations.keySet().retainAll(groupNameAnnotations.keySet());
			trendAnnotations.keySet().retainAll(investigationMethodAnnotations.keySet());
			investigationMethodAnnotations.keySet().retainAll(trendAnnotations.keySet());
		}

	}

	private Score evaluateInvestigationMethod() {
		return investigationPredictor.evaluate(investigationMethodsGroundTruth, investigationMethodsPredictions);
	}

	private Score evaluateTrend() {
		return trendPredictor.evaluate(trendGroundTruth, trendPredictions);
	}

	private static List<Instance> readRandomInstances(EntityType type) {

		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.maxNumberOfAnnotations = 300;
		InstanceProvider.removeInstancesWithToManyAnnotations = false;

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
				.setSeed(1000L).setSeed(1000L).setDevelopmentProportion(20).setCorpusSizeFraction(1F).build();

		InstanceProvider instanceProvider = new InstanceProvider(
				NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(type), corpusDistributor);

		for (Instance instance : instanceProvider.getInstances()) {
			AutomatedSectionifcation.getInstance(instance);
		}

		return instanceProvider.getInstances();
	}

	private static List<Instance> readInstances(EntityType type, List<String> trainingInstanceNames,
			List<String> testInstanceNames) {

		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.maxNumberOfAnnotations = 300;
		InstanceProvider.removeInstancesWithToManyAnnotations = false;

		AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
				.setTrainingInstanceNames(trainingInstanceNames).setTestInstanceNames(testInstanceNames).build();

		InstanceProvider instanceProvider = new InstanceProvider(
				NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(type), corpusDistributor);

		for (Instance instance : instanceProvider.getInstances()) {
			AutomatedSectionifcation.getInstance(instance);
		}

		return instanceProvider.getInstances();
	}

}

//FT (ohne pretrained)
//Final trend pre filter:Score [getF1()=0.229, getPrecision()=0.173, getRecall()=0.337, tp=706, fp=3373, fn=1389, tn=0]
//Final trend post filter:Score [getF1()=0.259, getPrecision()=0.223, getRecall()=0.309, tp=647, fp=2250, fn=1448, tn=0]
//Final inv method pre filter:Score [getF1()=0.076, getPrecision()=0.048, getRecall()=0.182, tp=162, fp=3200, fn=726, tn=0]
//Final inv method post filter:Score [getF1()=0.082, getPrecision()=0.054, getRecall()=0.175, tp=155, fp=2742, fn=733, tn=0]

//IDF
//Final trend pre filter:Score [getF1()=0.137, getPrecision()=0.080, getRecall()=0.472, tp=988, fp=11296, fn=1107, tn=0]
//Final trend post filter:Score [getF1()=0.139, getPrecision()=0.082, getRecall()=0.470, tp=984, fp=11072, fn=1111, tn=0]
//Final inv method pre filter:Score [getF1()=0.053, getPrecision()=0.028, getRecall()=0.471, tp=418, fp=14528, fn=470, tn=0]
//Final inv method post filter:Score [getF1()=0.057, getPrecision()=0.031, getRecall()=0.465, tp=413, fp=13065, fn=475, tn=0]

//FT + IDF AND
//Final trend pre filter:Score [getF1()=0.234, getPrecision()=0.182, getRecall()=0.330, tp=691, fp=3108, fn=1404, tn=0]
//Final trend post filter:Score [getF1()=0.262, getPrecision()=0.232, getRecall()=0.302, tp=633, fp=2098, fn=1462, tn=0]
//Final inv method pre filter:Score [getF1()=0.079, getPrecision()=0.050, getRecall()=0.179, tp=159, fp=3002, fn=729, tn=0]
//Final inv method post filter:Score [getF1()=0.083, getPrecision()=0.055, getRecall()=0.170, tp=151, fp=2580, fn=737, tn=0]

//FT + IDF OR
//Final trend pre filter:Score [getF1()=0.155, getPrecision()=0.088, getRecall()=0.633, tp=1326, fp=13690, fn=769, tn=0]
//Final trend post filter:Score [getF1()=0.159, getPrecision()=0.091, getRecall()=0.632, tp=1325, fp=13276, fn=770, tn=0]
//Final inv method pre filter:Score [getF1()=0.054, getPrecision()=0.028, getRecall()=0.562, tp=499, fp=17078, fn=389, tn=0]
//Final inv method post filter:Score [getF1()=0.057, getPrecision()=0.030, getRecall()=0.557, tp=495, fp=15863, fn=393, tn=0]
