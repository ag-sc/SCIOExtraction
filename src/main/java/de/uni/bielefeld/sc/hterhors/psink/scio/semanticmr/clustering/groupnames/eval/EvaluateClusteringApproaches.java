package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.eval;

import java.io.File;
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
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.LiteralAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.helper.GroupNameDataSetHelper;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.helper.GroupNamePair;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.kmeans.WordBasedKMeans;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.methods.weka.WEKAClustering;

public class EvaluateClusteringApproaches {

	private static final File instanceDirectory = new File(
			"src/main/resources/slotfilling/experimental_group/corpus/instances/");

	public static void main(String[] args) throws Exception {

		SystemScope.Builder.getScopeHandler().addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("ExperimentalGroup"))
				.build();
		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(100L)
				.setTrainingProportion(80).setTestProportion(20).setCorpusSizeFraction(1F).build();

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		EvaluateClusteringApproaches evaluator = new EvaluateClusteringApproaches();

		SlotType.excludeAll();
		SCIOSlotTypes.hasGroupName.include();

		List<Instance> trainInstances = instanceProvider.getRedistributedTrainingInstances();
		List<Instance> testInstances = instanceProvider.getRedistributedTestInstances();

		int k = 4;

		evaluator.wekaBasedKMeans(trainInstances, testInstances, k);
		System.out.println();
		System.out.println();
		evaluator.wordBasedKMeans(trainInstances, testInstances, k);

	}

	private final CartesianEvaluator cartesianEvaluator = new CartesianEvaluator(EEvaluationDetail.DOCUMENT_LINKED);

	public EvaluateClusteringApproaches() {

	}

	public void wekaBasedKMeans(List<Instance> trainInstances, List<Instance> testInstances, int k) throws Exception {
		WEKAClustering gnc = new WEKAClustering();

		gnc.train(trainInstances);

		Score binaryClassificationScore = gnc.test(testInstances);

		System.out.println("GroupNameClusteringWEKA binaryClassificationScore  = " + binaryClassificationScore);

		Score overallPostClusteringBinaryClassificationScore = new Score();
		Score overallClusteringScore = new Score();

		for (Instance instance : testInstances) {
			Map<Boolean, Set<GroupNamePair>> goldPairs = GroupNameDataSetHelper
					.getGroupNameClusterDataSet(Arrays.asList(instance));

			List<DocumentLinkedAnnotation> datapoints = GroupNameDataSetHelper.extractGroupNameAnnotations(goldPairs);

			if (datapoints.size() == 0)
				continue;

			List<List<DocumentLinkedAnnotation>> clusters = gnc.cluster(datapoints, k);

			Score clusteringScore = computeClusteringScore(instance, clusters);

			overallClusteringScore.add(clusteringScore);

			Score postClusteringBinaryClassificationScore = computeBinaryClassificationScore(goldPairs, clusters);

			overallPostClusteringBinaryClassificationScore.add(postClusteringBinaryClassificationScore);
		}

		System.out.println(k + " GroupNameClusteringWEKA overallBinaryClassificationScore  = "
				+ overallPostClusteringBinaryClassificationScore);
		System.out.println(k + " GroupNameClusteringWEKA overallClusteringScore  = " + overallClusteringScore);

	}

	public void wordBasedKMeans(List<Instance> trainInstances, List<Instance> testInstances, int k) {
		WordBasedKMeans<DocumentLinkedAnnotation> kMeans = new WordBasedKMeans<>();

		Score overallBinaryClassificationScore = new Score();
		Score overallClusteringScore = new Score();

		for (Instance instance : testInstances) {

			Map<Boolean, Set<GroupNamePair>> goldPairs = GroupNameDataSetHelper
					.getGroupNameClusterDataSet(Arrays.asList(instance));

			List<DocumentLinkedAnnotation> datapoints = GroupNameDataSetHelper.extractGroupNameAnnotations(goldPairs);

			if (datapoints.size() == 0)
				continue;

			List<List<DocumentLinkedAnnotation>> clusters = kMeans.cluster(datapoints, k);

			Score clusteringScore = computeClusteringScore(instance, clusters);

			overallClusteringScore.add(clusteringScore);

			Score binaryClassificationScore = computeBinaryClassificationScore(goldPairs, clusters);

			overallBinaryClassificationScore.add(binaryClassificationScore);
		}

		System.out.println(
				k + " WordBasedKMeans overallBinaryClassificationScore  = " + overallBinaryClassificationScore);
		System.out.println(k + " WordBasedKMeans overallClusteringScore  = " + overallClusteringScore);

	}

	private Score computeBinaryClassificationScore(Map<Boolean, Set<GroupNamePair>> goldPairs,
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

		return prf1(
				Streams.concat(goldPairs.get(true).stream(), goldPairs.get(false).stream()).collect(Collectors.toSet()),
				Streams.concat(predictedPairs.get(true).stream(), predictedPairs.get(false).stream())
						.collect(Collectors.toSet()));
	}

	public boolean validPairToTest(Map<Boolean, Set<GroupNamePair>> goldPairs, LiteralAnnotation l1,
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

	private Score computeClusteringScore(Instance instance, List<List<DocumentLinkedAnnotation>> clusters) {
		List<EntityTemplate> predictedCluster = new ArrayList<>();

		for (List<DocumentLinkedAnnotation> groupNames : clusters) {

			EntityTemplate et = new EntityTemplate(SCIOEntityTypes.definedExperimentalGroup);

			for (DocumentLinkedAnnotation groupName : groupNames) {
				et.addMultiSlotFiller(SCIOSlotTypes.hasGroupName, groupName);
			}
			predictedCluster.add(et);
		}

		return cartesianEvaluator.scoreMultiValues(instance.getGoldAnnotations().getAnnotations(), predictedCluster);

	}

	private Score prf1(Collection<GroupNamePair> goldAnnotations, Collection<GroupNamePair> predictedAnnotations) {

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
			if (a.sameCluster) {
				fn++;
			} else {
				if (a.sameCluster) {
					fp++;
				} else {
					inner: {
						for (GroupNamePair oa : predictedAnnotations) {
							if (oa.equals(a)) {
								break inner;
							}
						}
						tn++;
					}
				}
			}
		}

		return new Score(tp, fp, fn, tn);
	}
}
