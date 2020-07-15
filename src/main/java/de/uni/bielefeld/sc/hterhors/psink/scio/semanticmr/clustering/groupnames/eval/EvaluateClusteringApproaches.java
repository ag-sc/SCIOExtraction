package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.eval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.LiteralAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.helper.GroupNameDataSetHelper;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.helper.GroupNamePair;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.kmeans.Word2VecBasedKMeans;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.kmeans.WordBasedKMeansMedoid;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.weka.WEKAClustering;

public class EvaluateClusteringApproaches {

	public static void main(String[] args) throws Exception {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("ExperimentalGroup"))
				.build();
		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(100L)
				.setTrainingProportion(80).setTestProportion(20).setCorpusSizeFraction(1F).build();

		InstanceProvider instanceProvider = new InstanceProvider(
				SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.experimentalGroup),
				corpusDistributor);

		EvaluateClusteringApproaches evaluator = new EvaluateClusteringApproaches();

		SlotType.excludeAll();
		SCIOSlotTypes.hasGroupName.include();
		List<Instance> trainInstances = instanceProvider.getRedistributedTrainingInstances();
		List<Instance> testInstances = instanceProvider.getRedistributedTestInstances();

		int k = 4;

		evaluator.word2VecBasedMeans(trainInstances, testInstances, k);
//		System.out.println();
//		System.out.println();
//		evaluator.wekaBasedKMeans(trainInstances, testInstances, k);
//		System.out.println();
//		System.out.println();
//		evaluator.wordBasedKMeans(trainInstances, testInstances, k);

	}

	private static final CartesianEvaluator cartesianEvaluator = new CartesianEvaluator(
			EEvaluationDetail.DOCUMENT_LINKED);

	public EvaluateClusteringApproaches() {

	}

	public void word2VecBasedMeans(List<Instance> trainInstances, List<Instance> testInstances, int k)
			throws Exception {
		Set<String> words = new HashSet<>();
		for (Instance instance : trainInstances) {

			Map<Boolean, Set<GroupNamePair>> goldPairs = GroupNameDataSetHelper
					.getGroupNameClusterDataSet(Arrays.asList(instance));

			List<DocumentLinkedAnnotation> datapoints = GroupNameDataSetHelper.extractGroupNameAnnotations(goldPairs);

			for (DocumentLinkedAnnotation string : datapoints) {
				words.add(string.getSurfaceForm());
			}

		}
		for (Instance instance : testInstances) {

			Map<Boolean, Set<GroupNamePair>> goldPairs = GroupNameDataSetHelper
					.getGroupNameClusterDataSet(Arrays.asList(instance));

			List<DocumentLinkedAnnotation> datapoints = GroupNameDataSetHelper.extractGroupNameAnnotations(goldPairs);

			for (DocumentLinkedAnnotation string : datapoints) {
				words.add(string.getSurfaceForm());
			}

		}
		Word2VecBasedKMeans<DocumentLinkedAnnotation> gnc = new Word2VecBasedKMeans<>(words);

		Score overallBinaryClassificationScore = new Score();
		Score overallClusteringScore = new Score();

		Score overallCardinalityScore = new Score();

		double cardinalityRMSE = 0;

		Map<Integer, Score> intervallCardinality = new HashMap<>();

		for (Instance instance : testInstances) {

			Map<Boolean, Set<GroupNamePair>> goldPairs = GroupNameDataSetHelper
					.getGroupNameClusterDataSet(Arrays.asList(instance));

			List<DocumentLinkedAnnotation> datapoints = GroupNameDataSetHelper.extractGroupNameAnnotations(goldPairs);

			if (datapoints.size() == 0)
				continue;

			List<List<DocumentLinkedAnnotation>> clusters = gnc.cluster(datapoints,
					instance.getGoldAnnotations().getAnnotations().size());
//			List<List<DocumentLinkedAnnotation>> clusters = gnc.cluster(datapoints, 1);
//			List<List<DocumentLinkedAnnotation>> clusters = gnc.clusterRSS(datapoints,
//					instance.getGoldAnnotations().getAnnotations().size(),
//					instance.getGoldAnnotations().getAnnotations().size());
//			List<List<DocumentLinkedAnnotation>> clusters = gnc.clusterRSS(datapoints, k - 1, k + 1);

			cardinalityRMSE = computeRMSE(cardinalityRMSE, instance, clusters);

			computeIntervallCardinality(intervallCardinality, instance, clusters);

			Score clusteringScore = computeClusteringScore(instance, clusters);

			Score cardinalityScore = computeCardinalityScore(instance, clusters);

			overallCardinalityScore.add(cardinalityScore);

			overallClusteringScore.add(clusteringScore);

			Score binaryClassificationScore = computeBinaryClassificationScore(goldPairs, clusters);

			overallBinaryClassificationScore.add(binaryClassificationScore);
		}

		System.out.println(
				k + " Word2VecBasedKMeans overallBinaryClassificationScore  = " + overallBinaryClassificationScore);
		System.out.println(k + " Word2VecBasedKMeans overallClusteringScore  = " + overallClusteringScore);
		System.out.println(k + " Word2VecBasedKMeans overall cardinality score  = " + overallCardinalityScore);

		final StringBuffer cardString = new StringBuffer();
		intervallCardinality.entrySet().forEach(e -> cardString.append("\n\t" + e.getKey() + ":" + e.getValue()));
		System.out.println("Word2VecBasedKMeans INTERVALL CARDINALITY = " + cardString.toString().trim());
		System.out
				.println("Word2VecBasedKMeans CARDINALITY RMSE = " + Math.sqrt(cardinalityRMSE / testInstances.size()));
	}

	public Score wekaBasedKMeans(List<Instance> trainInstances, List<Instance> testInstances, int k) throws Exception {
		WEKAClustering gnc = new WEKAClustering("TEST");

		gnc.trainOrLoad(trainInstances);

		Score binaryClassificationScore = gnc.test(testInstances);

		System.out.println("GroupNameClusteringWEKA binaryClassificationScore  = " + binaryClassificationScore);

		Score overallPostClusteringBinaryClassificationScore = new Score();
		Score overallClusteringScore = new Score();
		Score overallCardinalityScore = new Score(EScoreType.MACRO);

		double cardinalityRMSE = 0;

		Map<Integer, Score> intervallCardinality = new HashMap<>();

		for (Instance instance : testInstances) {
			Map<Boolean, Set<GroupNamePair>> goldPairs = GroupNameDataSetHelper
					.getGroupNameClusterDataSet(Arrays.asList(instance));

			List<DocumentLinkedAnnotation> datapoints = GroupNameDataSetHelper.extractGroupNameAnnotations(goldPairs);

			if (datapoints.size() == 0)
				continue;

//			List<List<DocumentLinkedAnnotation>> clusters = gnc.cluster(datapoints,
//					instance.getGoldAnnotations().getAnnotations().size());
//			List<List<DocumentLinkedAnnotation>> clusters = gnc.cluster(datapoints, k);
			List<List<DocumentLinkedAnnotation>> clusters = gnc.clusterRSS(datapoints, 1, 8);

			cardinalityRMSE = computeRMSE(cardinalityRMSE, instance, clusters);
			computeIntervallCardinality(intervallCardinality, instance, clusters);

			Score clusteringScore = computeClusteringScore(instance, clusters);
			Score cardinalityScore = computeCardinalityScore(instance, clusters).toMacro();
			overallCardinalityScore.add(cardinalityScore);

			overallClusteringScore.add(clusteringScore);

			Score postClusteringBinaryClassificationScore = computeBinaryClassificationScore(goldPairs, clusters);

			overallPostClusteringBinaryClassificationScore.add(postClusteringBinaryClassificationScore);

		}
		System.out.println(k + " GroupNameClusteringWEKA overallPostClusteringBinaryClassificationScore  = "
				+ overallPostClusteringBinaryClassificationScore);
		System.out.println(k + " GroupNameClusteringWEKA overallClusteringScore  = " + overallClusteringScore);
		System.out.println(k + " GroupNameClusteringWEKA overall cardinality score  = " + overallCardinalityScore);
		final StringBuffer cardString = new StringBuffer();
		intervallCardinality.entrySet().forEach(e -> cardString.append("\n\t" + e.getKey() + ":" + e.getValue()));
		System.out.println("GroupNameClusteringWEKA INTERVALL CARDINALITY = " + cardString.toString().trim());
		System.out.println(
				"GroupNameClusteringWEKA CARDINALITY RMSE = " + Math.sqrt(cardinalityRMSE / testInstances.size()));

		return overallCardinalityScore;
	}

	public Score wordBasedKMeans(List<Instance> trainInstances, List<Instance> testInstances, int k) {
		WordBasedKMeansMedoid<DocumentLinkedAnnotation> kMeans = new WordBasedKMeansMedoid<>();
		kMeans.lambda = 5.2;
		Score overallBinaryClassificationScore = new Score();
		Score overallClusteringScore = new Score();

		Score overallCardinalityScore = new Score();

		double cardinalityRMSE = 0;

		Map<Integer, Score> intervallCardinality = new HashMap<>();

		for (Instance instance : testInstances) {

			Map<Boolean, Set<GroupNamePair>> goldPairs = GroupNameDataSetHelper
					.getGroupNameClusterDataSet(Arrays.asList(instance));

			List<DocumentLinkedAnnotation> datapoints = GroupNameDataSetHelper.extractGroupNameAnnotations(goldPairs);

			if (datapoints.size() == 0)
				continue;

			List<List<DocumentLinkedAnnotation>> clusters = kMeans.cluster(datapoints, k);
//			List<List<DocumentLinkedAnnotation>> clusters = kMeans.cluster(datapoints,
//					instance.getGoldAnnotations().getAnnotations().size());
//			List<List<DocumentLinkedAnnotation>> clusters = kMeans.clusterRSS(datapoints, k - 1, k + 1);

			cardinalityRMSE = computeRMSE(cardinalityRMSE, instance, clusters);

			computeIntervallCardinality(intervallCardinality, instance, clusters);

			Score clusteringScore = computeClusteringScore(instance, clusters);

			Score cardinalityScore = computeCardinalityScore(instance, clusters);

			overallCardinalityScore.add(cardinalityScore);

			overallClusteringScore.add(clusteringScore);

			Score binaryClassificationScore = computeBinaryClassificationScore(goldPairs, clusters);

			overallBinaryClassificationScore.add(binaryClassificationScore);
		}

		System.out.println(
				k + " WordBasedKMeans overallBinaryClassificationScore  = " + overallBinaryClassificationScore);
		System.out.println(k + " WordBasedKMeans overallClusteringScore  = " + overallClusteringScore);
		System.out.println(k + " WordBasedKMeans overall cardinality score  = " + overallCardinalityScore);

		final StringBuffer cardString = new StringBuffer();
		intervallCardinality.entrySet().forEach(e -> cardString.append("\n\t" + e.getKey() + ":" + e.getValue()));
		System.out.println("WordBasedKMeans INTERVALL CARDINALITY = " + cardString.toString().trim());
		System.out.println("WordBasedKMeans CARDINALITY RMSE = " + Math.sqrt(cardinalityRMSE / testInstances.size()));
		return overallClusteringScore;
	}

	public static void computeIntervallCardinality(Map<Integer, Score> intervallCardinality, Instance instance,
			List<List<DocumentLinkedAnnotation>> clusters) {
		for (int spread = 0; spread < 4; spread++) {
			intervallCardinality.putIfAbsent(spread, new Score());

			int tp = Math.abs(instance.getGoldAnnotations().getAbstractAnnotations().size() - clusters.size()) <= spread
					? 1
					: 0;
			int fn = tp == 1 ? 0 : 1;
			intervallCardinality.get(spread).add(new Score(tp, 0, fn));

		}
	}

	public static double computeRMSE(double cardinalityRMSE, Instance instance,
			List<List<DocumentLinkedAnnotation>> clusters) {
		cardinalityRMSE += Math.pow(clusters.size() - instance.getGoldAnnotations().getAbstractAnnotations().size(), 2);
		return cardinalityRMSE;
	}

	public static Score computeCardinalityScore(Instance instance, List<List<DocumentLinkedAnnotation>> clusters) {
		int tp = Math.min(instance.getGoldAnnotations().getAnnotations().size(), clusters.size());
		int fp = clusters.size() > instance.getGoldAnnotations().getAnnotations().size()
				? clusters.size() - instance.getGoldAnnotations().getAnnotations().size()
				: 0;
		int fn = clusters.size() < instance.getGoldAnnotations().getAnnotations().size()
				? instance.getGoldAnnotations().getAnnotations().size() - clusters.size()
				: 0;
		return new Score(tp, fp, fn);

	}

	public static Score computeBinaryClassificationScore(Map<Boolean, Set<GroupNamePair>> goldPairs,
			List<List<DocumentLinkedAnnotation>> clusters) {

		Map<Boolean, Set<GroupNamePair>> predictedPairs = new HashMap<>();

		predictedPairs.put(true, new HashSet<>());
		predictedPairs.put(false, new HashSet<>());

		for (int i = 0; i < clusters.size(); i++) {
			for (int j = i; j < clusters.size(); j++) {
				for (int l = 0; l < clusters.get(i).size(); l++) {
					for (int m = l + 1; m < clusters.get(j).size(); m++) {

						LiteralAnnotation l1 = clusters.get(i).get(l);
						LiteralAnnotation l2 = clusters.get(j).get(m);

						/*
						 * test if this pair is in gold set.
						 */
						boolean validPair = validPairToTest(goldPairs, l1, l2);

						if (!validPair)
							continue;

						predictedPairs.get(i == j).add(new GroupNamePair(l1, l2, i == j, 1));
					}
				}
			}
		}
//		for (Entry<Boolean, Set<GroupNamePair>> goldC : goldPairs.entrySet()) {
//			for (GroupNamePair instance2 : goldC.getValue()) {
//				System.out.println(instance2.groupName1.getSurfaceForm() + "\t" + instance2.groupName2.getSurfaceForm()
//						+ "\t" + instance2.sameCluster);
//			}
//		}
//		System.out.println("-----------------");
//		for (Entry<Boolean, Set<GroupNamePair>> goldC : predictedPairs.entrySet()) {
//			for (GroupNamePair instance2 : goldC.getValue()) {
//				System.out.println(instance2.groupName1.getSurfaceForm() + "\t" + instance2.groupName2.getSurfaceForm()
//						+ "\t" + instance2.sameCluster);
//			}
//		}

		return prf1(
				Streams.concat(goldPairs.get(true).stream(), goldPairs.get(false).stream()).collect(Collectors.toSet()),
				Streams.concat(predictedPairs.get(true).stream(), predictedPairs.get(false).stream())
						.collect(Collectors.toSet()));
	}

	public static boolean validPairToTest(Map<Boolean, Set<GroupNamePair>> goldPairs, LiteralAnnotation l1,
			LiteralAnnotation l2) {
		Set<LiteralAnnotation> sPred = new HashSet<>();
		sPred.add(l1);
		sPred.add(l2);
		boolean validPair = false;
		for (GroupNamePair gnp : goldPairs.get(true)) {
			Set<LiteralAnnotation> sGold = new HashSet<>();
			sGold.add(gnp.groupName1);
			sGold.add(gnp.groupName2);

			if (sPred.equals(sGold)) {
				validPair = true;
				break;
			}
		}
		if (!validPair)
			for (GroupNamePair gnp : goldPairs.get(false)) {
				Set<LiteralAnnotation> sGold = new HashSet<>();
				sGold.add(gnp.groupName1);
				sGold.add(gnp.groupName2);

				if (sPred.equals(sGold)) {
					validPair = true;
					break;
				}
			}
		return validPair;
	}

	public static Score computeClusteringScore(Instance instance, List<List<DocumentLinkedAnnotation>> clusters) {
		List<EntityTemplate> predictedCluster = new ArrayList<>();

		for (List<DocumentLinkedAnnotation> groupNames : clusters) {

			EntityTemplate et = new EntityTemplate(SCIOEntityTypes.definedExperimentalGroup);

			for (DocumentLinkedAnnotation groupName : groupNames) {
				et.addMultiSlotFiller(SCIOSlotTypes.hasGroupName, groupName);
			}
			predictedCluster.add(et);
		}

		return cartesianEvaluator.scoreMultiValues(instance.getGoldAnnotations().getAnnotations(), predictedCluster,
				EScoreType.MICRO);

	}

	public static Score prf1(Collection<GroupNamePair> goldAnnotations,
			Collection<GroupNamePair> predictedAnnotations) {

		int tp = 0;
		int fp = 0;
		int fn = 0;
		int tn = 0;

		outer: for (GroupNamePair a : goldAnnotations) {
			for (GroupNamePair oa : predictedAnnotations) {
				if (a.equals(oa)) {
					if (a.sameCluster) {
						tp++;
					} else {
						tn++;
					}
					continue outer;
				}
			}

			fn++;
		}

		outer: for (GroupNamePair a : predictedAnnotations) {
			for (GroupNamePair oa : goldAnnotations) {
				if (oa.equals(a)) {
					continue outer;
				}
			}
			fp++;
		}

		return new Score(tp, fp, fn, tn);
	}
}
