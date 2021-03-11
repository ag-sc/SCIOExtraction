package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.trend;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;
import de.hterhors.semanticmr.eval.BeamSearchEvaluator;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation.ESection;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.PValueNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.result_sentences.AbstractIDFPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Result;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Trend;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.Stats;

public class TrendIDFPredictor extends AbstractIDFPredictor {

	public static void main(String[] args) throws IOException {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).apply()
				.registerNormalizationFunction(new PValueNormalization()).build();

		File instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.trend);

		AbstractCorpusDistributor trendCorpusDistributor = new ShuffleCorpusDistributor.Builder()
				.setTrainingProportion(80).setTestProportion(20).setSeed(1000L).build();

		InstanceProvider.maxNumberOfAnnotations = 300;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = false;

		InstanceProvider trendInstanceProvider = new InstanceProvider(instanceDirectory, trendCorpusDistributor);
	

		Set<SlotType> slotTypesToConsider = new HashSet<>();
		slotTypesToConsider.add(SCIOSlotTypes.hasSignificance);
		slotTypesToConsider.add(SCIOSlotTypes.hasPValue);
		slotTypesToConsider.add(SCIOSlotTypes.hasDifference);
		
//		Stats.countVariables(1,trendInstanceProvider.getInstances());
//		System.exit(1);
		
		
//		Stats.computeNormedVar(trendInstanceProvider.getInstances(), SCIOEntityTypes.trend);
//
//		for (SlotType slotType : slotTypesToConsider) {
//			Stats.computeNormedVar(trendInstanceProvider.getInstances(), slotType);
//		}
//		System.exit(1);
//		SystemScope.Builder.getScopeHandler()
//				/**
//				 * We add a scope reader that reads and interprets the 4 specification files.
//				 */
//				.addScopeSpecification(DataStructureLoader.loadNERDataStructureReader("Trend"))
//				/**
//				 * Finally, we build the systems scope.
//				 */
//				.build();

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
				.setSeed(1000L).setTestProportion(20).setCorpusSizeFraction(1F).build();

		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.maxNumberOfAnnotations = 300;
		InstanceProvider.removeInstancesWithToManyAnnotations = false;

		InstanceProvider instanceProvider = new InstanceProvider(
				NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.trend), corpusDistributor);

		int count = 0;
		for (Instance instance : instanceProvider.getInstances()) {
			AutomatedSectionifcation.getInstance(instance);
			count += instance.getGoldAnnotations().getAnnotations().size();
		}

		System.out.println(count);

		TrendIDFPredictor trendPredictor = new TrendIDFPredictor();
		trendPredictor.setAnnotationStopwords(
				Arrays.asList("rats", "either", "number", "group", "groups", "numbers", "treatment", "respectively"));
		trendPredictor.setRemoveEntityTypes(
				Arrays.asList(EntityType.get("AlphaSignificanceNiveau"), EntityType.get("RepeatedMeasureTrend")));
		trendPredictor.setEnableUniGram(true);
		trendPredictor.setSentenceStopWords(Arrays.asList("arrow", "asterisk", "*", "bar"));
		trendPredictor.setEnableBiGram(false);
		trendPredictor.setRestrictToSections(Arrays.asList(ESection.RESULTS));
		trendPredictor.setLocalNormalizing(true);
		trendPredictor.setEnableStemming(false);
		trendPredictor.setIncludeNameContains(false);
		trendPredictor.setMinTokenLength(2);
		trendPredictor.setEnableLowerCasing(false);
		trendPredictor.setTrehsold(1);
		trendPredictor.setMaxAnnotationsPerSentence(3);
		trendPredictor.setMinAnnotationsPerSentence(2);

		trendPredictor.train(instanceProvider.getTrainingInstances());

		Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> groundTruth = trendPredictor
				.getGroundTruthAnnotations(instanceProvider.getTestInstances());

		Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> predictions = trendPredictor
				.predictInstances(instanceProvider.getTestInstances());
		AbstractEvaluator eval = new BeamSearchEvaluator(EEvaluationDetail.ENTITY_TYPE, 5);
		Score allScore = new Score();
		Map<String, Score> scoreMap = new HashMap<>();
//
//		Set<SlotType> slotTypesToConsider = new HashSet<>();
//		slotTypesToConsider.add(SCIOSlotTypes.hasSignificance);
//		slotTypesToConsider.add(SCIOSlotTypes.hasPValue);
//		slotTypesToConsider.add(SCIOSlotTypes.hasDifference);

		/**
		 * TODO: Extra eval for significance and trend and pvalue...
		 */
		Map<Instance, State> finalStates = new HashMap<>();
		Map<Instance, State> coverageStates = new HashMap<>();
		for (Instance instance : predictions.keySet()) {

			List<AbstractAnnotation> coverageTrends = extractTrend(groundTruth.get(instance));
			List<AbstractAnnotation> trends = extractTrend(predictions.get(instance));

			List<EntityTemplate> goldTrends = getByName(instance.getName(), trendInstanceProvider.getInstances())
					.getGoldAnnotations().getAnnotations();

			System.out.println("########PREDICTED########");
			for (AbstractAnnotation string : trends) {
				System.out.println(string.toPrettyString());
			}

			System.out.println("########GOLD########");
			for (EntityTemplate string : goldTrends) {
				System.out.println(string.toPrettyString());
			}

			Annotations currentPredictions = new Annotations(trends);
			finalStates.put(getByName(instance.getName(), trendInstanceProvider.getInstances()),
					new State(getByName(instance.getName(), trendInstanceProvider.getInstances()), currentPredictions));

			Annotations coveragePredictions = new Annotations(coverageTrends);
			coverageStates.put(getByName(instance.getName(), trendInstanceProvider.getInstances()), new State(
					getByName(instance.getName(), trendInstanceProvider.getInstances()), coveragePredictions));

		}

		PerSlotEvaluator.evalRoot(EScoreType.MACRO, finalStates, coverageStates, eval, scoreMap);

		PerSlotEvaluator.evalProperties(EScoreType.MACRO, finalStates, coverageStates, slotTypesToConsider, eval,
				scoreMap);

		PerSlotEvaluator.evalCardinality(EScoreType.MACRO, finalStates, coverageStates, scoreMap);

		PerSlotEvaluator.evalOverall(EScoreType.MACRO, finalStates, coverageStates, eval, scoreMap);

		System.out.println("\n\n\n*************************");

		for (Entry<String, Score> sm : scoreMap.entrySet()) {
			System.out.println(sm.getKey() + "\t" + sm.getValue().toTSVString());
		}

		System.out.println("	*************************");

		System.out.println(allScore);

		Score s = trendPredictor.evaluate(groundTruth, predictions);

		new File("idf/trend/" + trendPredictor.toString().hashCode() + "/").mkdirs();

		trendPredictor.printErrors("idf/trend/" + trendPredictor.toString().hashCode() + "/"
				+ trendPredictor.toString().hashCode() + "_trend_errors.csv", groundTruth, predictions);
		trendPredictor.printIDFs("idf/trend/" + trendPredictor.toString().hashCode() + "/"
				+ trendPredictor.toString().hashCode() + "_trend_idfs.csv");
		String info = trendPredictor.printInfo("idf/trend/" + trendPredictor.toString().hashCode() + "/"
				+ trendPredictor.toString().hashCode() + "_trend_info.csv", s);

		System.out.println(info);
	}
//	score	Score [getF1()=0.219, getPrecision()=0.137, getRecall()=0.546, tp=231, fp=1456, fn=192, tn=0]
//	score	Score [getF1()=0.293, getPrecision()=0.210, getRecall()=0.489, tp=207, fp=781, fn=216, tn=0]

	private static Instance getByName(String name, List<Instance> instances) {

		for (Instance instance : instances) {
			if (instance.getName().equals(name)) {
				return instance;
			}
		}
		return null;
	}

	private static List<DocumentLinkedAnnotation> getRelatedClassesOf(Set<DocumentLinkedAnnotation> annotations,
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

	private static List<AbstractAnnotation> extractTrend(
			Map<Integer, Set<DocumentLinkedAnnotation>> annotationsPerSentence) {

		List<AbstractAnnotation> results = new ArrayList<>();

		for (Integer sentenceID : annotationsPerSentence.keySet()) {

			Set<DocumentLinkedAnnotation> annotations = annotationsPerSentence.get(sentenceID);

			List<DocumentLinkedAnnotation> trendRelated = null;

			if ((trendRelated = getRelatedClassesOf(annotations, EntityType.get("Trend"))).isEmpty()) {
				continue;
			}

			TrendData resultData = new TrendData();
			for (DocumentLinkedAnnotation t : trendRelated) {
				if (SCIOEntityTypes.trend == t.getEntityType()
						|| SCIOEntityTypes.trend.isSuperEntityOf(t.getEntityType())) {
					resultData.trend = t;
					break;
				}
			}
			for (DocumentLinkedAnnotation t : trendRelated) {
				if (SCIOEntityTypes.significance == t.getEntityType()
						|| SCIOEntityTypes.significance.isSuperEntityOf(t.getEntityType())) {
					resultData.significance = t;
					break;
				}
			}
			for (DocumentLinkedAnnotation t : trendRelated) {
				if (SCIOEntityTypes.observedDifference == t.getEntityType()
						|| SCIOEntityTypes.observedDifference.isSuperEntityOf(t.getEntityType())) {
					resultData.difference = t;
					break;
				}
			}
			for (DocumentLinkedAnnotation t : trendRelated) {
				if (SCIOEntityTypes.pValue == t.getEntityType()) {
					resultData.pValue = t;
					break;
				}
			}

			if (resultData != null)
				results.add(resultData.toTrend());
		}

		System.out.println("results.size() = " + results.size());
		return results;

	}

	protected List<DocumentLinkedAnnotation> extractData(Instance trainInstance) {

		List<DocumentLinkedAnnotation> ts = new ArrayList<>();

		for (AbstractAnnotation a : trainInstance.getGoldAnnotations().getAbstractAnnotations()) {
			if (a.isInstanceOfEntityTemplate()) {

				Result r = new Result(a.asInstanceOfEntityTemplate().asInstanceOfEntityTemplate());
				EntityTemplate trend = r.getTrend();

				if (trend == null)
					continue;

				Trend t = new Trend(trend);

				DocumentLinkedAnnotation root = t.getRootAnntoationAsDocumentLinkedAnnotation();

				if (root != null)
					ts.add(root);
				DocumentLinkedAnnotation diff = t.getDifferenceAsDocumentLinkedAnnotation();

				if (diff != null)
					ts.add(diff);

				SingleFillerSlot sigSlot = trend.getSingleFillerSlot(SCIOSlotTypes.hasSignificance);

				if (!sigSlot.containsSlotFiller()) {
					continue;
				}

				EntityTemplate significance = sigSlot.getSlotFiller().asInstanceOfEntityTemplate();

				DocumentLinkedAnnotation sig = significance.getRootAnnotation() != null
						&& significance.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()
								? significance.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation()
								: null;

				if (sig != null)
					ts.add(sig);

				SingleFillerSlot alphaSlot = significance.getSingleFillerSlot(SCIOSlotTypes.hasAlphaSignificanceNiveau);

				if (alphaSlot.containsSlotFiller()) {

					ts.add(alphaSlot.getSlotFiller().asInstanceOfDocumentLinkedAnnotation());

				}
				SingleFillerSlot pvalueSlot = significance.getSingleFillerSlot(SCIOSlotTypes.hasPValue);

				if (pvalueSlot.containsSlotFiller()
						&& pvalueSlot.getSlotFiller().isInstanceOfDocumentLinkedAnnotation()) {
					ts.add(pvalueSlot.getSlotFiller().asInstanceOfDocumentLinkedAnnotation());

				}

			} else {
				ts.add(a.asInstanceOfDocumentLinkedAnnotation());
			}

		}
		return ts;
	}

	static class TrendData {
		public DocumentLinkedAnnotation trend;
		public DocumentLinkedAnnotation difference;
		public DocumentLinkedAnnotation significance;
		public DocumentLinkedAnnotation pValue;

		public TrendData() {
		}

		public TrendData(TrendData resultData) {

			this.trend = resultData.trend;
			this.difference = resultData.difference;
			this.significance = resultData.significance;
			this.pValue = resultData.pValue;

		}

		public EntityTemplate toTrend() {

			EntityTemplate tr = null;

			if (trend == null && difference == null && significance == null && pValue == null) {
			} else {

				if (trend != null)
					tr = new EntityTemplate(trend);

				if (difference != null) {

					if (tr == null)
						tr = new EntityTemplate(SCIOEntityTypes.trend);

					tr.setSingleSlotFiller(SCIOSlotTypes.hasDifference, difference);
				}

				EntityTemplate sig = null;
				if (significance != null) {

					if (tr == null)
						tr = new EntityTemplate(SCIOEntityTypes.trend);

					sig = new EntityTemplate(significance);
				}

				if (pValue != null) {

					if (tr == null)
						tr = new EntityTemplate(SCIOEntityTypes.trend);
					if (sig == null)
						sig = new EntityTemplate(SCIOEntityTypes.significance);

					sig.setSingleSlotFiller(SCIOSlotTypes.hasPValue, pValue);
				}

				if (sig != null)
					tr.setSingleSlotFiller(SCIOSlotTypes.hasSignificance, sig);
			}

			return tr;
		}

	}

}
