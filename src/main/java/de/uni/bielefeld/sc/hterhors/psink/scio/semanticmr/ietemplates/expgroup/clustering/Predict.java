package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering.weka.GroupNameClusteringWEKA;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.helper.GroupNameExtraction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EDistinctGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EExtractGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.specifications.ExperimentalGroupSpecifications;

public class Predict {

	private static final File instanceDirectory = new File(
			"src/main/resources/slotfilling/experimental_group/corpus/instances/");

	public static void main(String[] args) throws Exception {

		SystemScope.Builder.getScopeHandler().addScopeSpecification(ExperimentalGroupSpecifications.systemsScope)
				.build();
		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(100L)
				.setTrainingProportion(80).setTestProportion(20).setCorpusSizeFraction(1F).build();

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		new Predict(instanceProvider.getRedistributedTrainingInstances(),
				instanceProvider.getRedistributedTestInstances());

	}

	private EExtractGroupNamesMode groupNameProviderMode = EExtractGroupNamesMode.GOLD;

	private EDistinctGroupNamesMode distinctGroupNamesMode = EDistinctGroupNamesMode.DISTINCT;

	public Predict(List<Instance> trainingInstances) throws Exception {
		this(trainingInstances, trainingInstances);
	}

	public Predict(List<Instance> redistributedTrainingInstances, List<Instance> redistributedTestInstances)
			throws Exception {

		GroupNameClusteringWEKA gnc = new GroupNameClusteringWEKA();

		gnc.train(redistributedTrainingInstances);
		gnc.test(redistributedTestInstances);

		for (Instance instance : redistributedTestInstances) {
			if (!instance.getName().startsWith("N141"))
				continue;

			System.out.println("Predict instance: " + instance.getName());

			List<DocumentLinkedAnnotation> anns = extractGroupNames(instance);

			System.out.println("Number of groupNames = " + anns.size());
			List<List<DocumentLinkedAnnotation>> clusters = gnc.cluster(anns);
			System.out.println("Number of clusters = " + clusters.size());
			for (List<DocumentLinkedAnnotation> cluster : clusters) {
				System.out.println("Cluster size = " + cluster.size());
			}

			for (List<DocumentLinkedAnnotation> cluster : clusters) {
				for (DocumentLinkedAnnotation groupName : cluster) {
					System.out.println(groupName.getSurfaceForm());
				}
				System.out.println("-------------------");
			}
			System.out.println();
			System.out.println();
		}
	}

	private List<DocumentLinkedAnnotation> extractGroupNames(Instance instance) {
		List<DocumentLinkedAnnotation> groupNames = new ArrayList<>();
		switch (groupNameProviderMode) {
		case EMPTY:
			break;
		case GOLD:
			groupNames.addAll(GroupNameExtraction.extractGroupNamesFromGold(instance));
			break;
		case NP_CHUNKS:
			groupNames.addAll(GroupNameExtraction.extractGroupNamesWithNPCHunks(distinctGroupNamesMode, instance));
			break;
		case PATTERN:
			groupNames.addAll(GroupNameExtraction.extractGroupNamesWithPattern(distinctGroupNamesMode, instance));
			break;
		case PATTERN_NP_CHUNKS:
			groupNames.addAll(GroupNameExtraction.extractGroupNamesWithNPCHunks(distinctGroupNamesMode, instance));
			groupNames.addAll(GroupNameExtraction.extractGroupNamesWithPattern(distinctGroupNamesMode, instance));
			break;
		case PATTERN_NP_CHUNKS_GOLD:
			groupNames.addAll(GroupNameExtraction.extractGroupNamesFromGold(instance));
			groupNames.addAll(GroupNameExtraction.extractGroupNamesWithNPCHunks(distinctGroupNamesMode, instance));
			groupNames.addAll(GroupNameExtraction.extractGroupNamesWithPattern(distinctGroupNamesMode, instance));
			break;
		}
		return groupNames;
	}

}
