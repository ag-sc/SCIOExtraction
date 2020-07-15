package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.eval;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
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

public class DetermineRSSLambda {

	public static void main(String[] args) throws Exception {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("ExperimentalGroup"))
				.build();
		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(100L)
				.setTrainingProportion(80).setTestProportion(20).setCorpusSizeFraction(1F).build();

		InstanceProvider instanceProvider = new InstanceProvider(
				SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.experimentalGroup),
				corpusDistributor);

		DetermineRSSLambda evaluator = new DetermineRSSLambda();

		SlotType.excludeAll();
		SCIOSlotTypes.hasGroupName.include();

		List<Instance> trainInstances = instanceProvider.getRedistributedTrainingInstances();
		List<Instance> testInstances = instanceProvider.getRedistributedTestInstances();

//		WordBasedKMeans<DocumentLinkedAnnotation> wordkMeans = new WordBasedKMeans<>();
//		WordBasedKMeansMedoid<DocumentLinkedAnnotation> medoidkMeans = new WordBasedKMeansMedoid<>();
		Set<String> words = new HashSet<>();
		for (Instance instance : trainInstances) {

			Map<Boolean, Set<GroupNamePair>> goldPairs = GroupNameDataSetHelper
					.getGroupNameClusterDataSet(Arrays.asList(instance));

			List<DocumentLinkedAnnotation> datapoints = GroupNameDataSetHelper.extractGroupNameAnnotations(goldPairs);

			for (DocumentLinkedAnnotation string : datapoints) {
				words.add(string.getSurfaceForm());
			}

		}
//		for (Instance instance : testInstances) {
//
//			Map<Boolean, Set<GroupNamePair>> goldPairs = GroupNameDataSetHelper
//					.getGroupNameClusterDataSet(Arrays.asList(instance));
//
//			List<DocumentLinkedAnnotation> datapoints = GroupNameDataSetHelper.extractGroupNameAnnotations(goldPairs);
//
//			for (DocumentLinkedAnnotation string : datapoints) {
//				words.add(string.getSurfaceForm());
//			}
//
//		}

//		Word2VecBasedKMeans<DocumentLinkedAnnotation> w2vkMeans = new Word2VecBasedKMeans<>(words);
		WEKAClustering wekakMeans = new WEKAClustering("TEST");
		wekakMeans.trainOrLoad(trainInstances);

		for (double i = 0; i < 20; i += 0.1) {
			wekakMeans.lambda = i;
//			medoidkMeans.lambda = i;

//			Score score = evaluator.wordBasedKMeans(wordkMeans, trainInstances, testInstances, 4);
//			Score score = evaluator.wordBasedKMeans(medoidkMeans, trainInstances, testInstances, 4);
//			Score score = evaluator.word2VecBasedMeans(w2vkMeans, trainInstances, testInstances, 4);
			Score score = evaluator.wekaBasedKMeans(wekakMeans, trainInstances, testInstances, 4);
			System.out.println(i + "\t" + score.getF1());
		}

	}

	public Score wekaBasedKMeans(WEKAClustering gnc, List<Instance> trainInstances, List<Instance> testInstances, int k)
			throws Exception {

//		Score binaryClassificationScore = gnc.test(testInstances);

//		System.out.println("GroupNameClusteringWEKA binaryClassificationScore  = " + binaryClassificationScore);

		Score overallPostClusteringBinaryClassificationScore = new Score();
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

//			List<List<DocumentLinkedAnnotation>> clusters = gnc.cluster(datapoints, k+1);
			List<List<DocumentLinkedAnnotation>> clusters = gnc.clusterRSS(datapoints, 1, 8);

			// cardinalityRMSE = computeRMSE(cardinalityRMSE, instance, clusters);
//			computeIntervallCardinality(intervallCardinality, instance, clusters);

			Score clusteringScore = EvaluateClusteringApproaches.computeClusteringScore(instance, clusters);
//			Score cardinalityScore = EvaluateClusteringApproaches.computeCardinalityScore(instance, clusters);
//			overallCardinalityScore.add(cardinalityScore);
////
			overallClusteringScore.add(clusteringScore);

//			Score postClusteringBinaryClassificationScore = EvaluateClusteringApproaches.computeBinaryClassificationScore(goldPairs, clusters);

//			overallPostClusteringBinaryClassificationScore.add(postClusteringBinaryClassificationScore);

		}
		return overallClusteringScore;
	}

	public Score word2VecBasedMeans(Word2VecBasedKMeans<DocumentLinkedAnnotation> gnc, List<Instance> trainInstances,
			List<Instance> testInstances, int k) throws Exception {

		Score overallBinaryClassificationScore = new Score();
		Score overallClusteringScore = new Score();

		Score overallCardinalityScore = new Score();

		double cardinalityRMSE = 0;

		Map<Integer, Score> intervallCardinality = new HashMap<>();

		for (Instance instance : trainInstances) {

			Map<Boolean, Set<GroupNamePair>> goldPairs = GroupNameDataSetHelper
					.getGroupNameClusterDataSet(Arrays.asList(instance));

			List<DocumentLinkedAnnotation> datapoints = GroupNameDataSetHelper.extractGroupNameAnnotations(goldPairs);

			if (datapoints.size() == 0)
				continue;

//			List<List<DocumentLinkedAnnotation>> clusters = gnc.cluster(datapoints,
//					instance.getGoldAnnotations().getAnnotations().size());
//			List<List<DocumentLinkedAnnotation>> clusters = gnc.cluster(datapoints, 1);
//			List<List<DocumentLinkedAnnotation>> clusters = gnc.clusterRSS(datapoints,
//					instance.getGoldAnnotations().getAnnotations().size(),
//					instance.getGoldAnnotations().getAnnotations().size());
			List<List<DocumentLinkedAnnotation>> clusters = gnc.clusterRSS(datapoints, 1, 8);

//			cardinalityRMSE = computeRMSE(cardinalityRMSE, instance, clusters);
//
//			computeIntervallCardinality(intervallCardinality, instance, clusters);

			Score clusteringScore = EvaluateClusteringApproaches.computeClusteringScore(instance, clusters);

//			Score cardinalityScore = computeCardinalityScore(instance, clusters);
//
//			overallCardinalityScore.add(cardinalityScore);

			overallClusteringScore.add(clusteringScore);

//			Score binaryClassificationScore = computeBinaryClassificationScore(goldPairs, clusters);
//
//			overallBinaryClassificationScore.add(binaryClassificationScore);
		}
		return overallClusteringScore;
	}

	public Score wordBasedKMeans(WordBasedKMeansMedoid<DocumentLinkedAnnotation> kMeans, List<Instance> trainInstances,
			List<Instance> testInstances, int k) {

		Score overallBinaryClassificationScore = new Score();
		Score overallClusteringScore = new Score();

		Score overallCardinalityScore = new Score();

		double cardinalityRMSE = 0;

		Map<Integer, Score> intervallCardinality = new HashMap<>();

		for (Instance instance : trainInstances) {

			Map<Boolean, Set<GroupNamePair>> goldPairs = GroupNameDataSetHelper
					.getGroupNameClusterDataSet(Arrays.asList(instance));

			List<DocumentLinkedAnnotation> datapoints = GroupNameDataSetHelper.extractGroupNameAnnotations(goldPairs);

			if (datapoints.size() == 0)
				continue;

//			List<List<DocumentLinkedAnnotation>> clusters = kMeans.cluster(datapoints,k);
//			List<List<DocumentLinkedAnnotation>> clusters = kMeans.cluster(datapoints,
//					instance.getGoldAnnotations().getAnnotations().size());
			List<List<DocumentLinkedAnnotation>> clusters = kMeans.clusterRSS(datapoints, 1, 8);

//			cardinalityRMSE = EvaluateClusteringApproaches.computeRMSE(cardinalityRMSE, instance, clusters);
//
//			EvaluateClusteringApproaches.computeIntervallCardinality(intervallCardinality, instance, clusters);

			Score clusteringScore = EvaluateClusteringApproaches.computeClusteringScore(instance, clusters);

//			Score cardinalityScore = EvaluateClusteringApproaches.computeCardinalityScore(instance, clusters);
//
//			overallCardinalityScore.add(cardinalityScore);

			overallClusteringScore.add(clusteringScore);

//			Score binaryClassificationScore = EvaluateClusteringApproaches.computeBinaryClassificationScore(goldPairs,
//					clusters);
//
//			overallBinaryClassificationScore.add(binaryClassificationScore);
		}
//
//		System.out.println(k + " " + kMeans.getClass().getSimpleName() + " overallBinaryClassificationScore  = "
//				+ overallBinaryClassificationScore);
//		System.out.println(
//				k + " " + kMeans.getClass().getSimpleName() + " overallClusteringScore  = " + overallClusteringScore);
//		System.out.println(k + " " + kMeans.getClass().getSimpleName() + " overall cardinality score  = "
//				+ overallCardinalityScore);
//
//		final StringBuffer cardString = new StringBuffer();
//		intervallCardinality.entrySet().forEach(e -> cardString.append("\n\t" + e.getKey() + ":" + e.getValue()));
//		System.out.println(
//				"" + kMeans.getClass().getSimpleName() + " INTERVALL CARDINALITY = " + cardString.toString().trim());
//		System.out.println("" + kMeans.getClass().getSimpleName() + " CARDINALITY RMSE = "
//				+ Math.sqrt(cardinalityRMSE / testInstances.size()));
		return overallClusteringScore;
	}
}
