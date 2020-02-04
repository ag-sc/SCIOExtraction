package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.groupnames.clustering;

import java.io.File;
import java.util.Collections;
import java.util.List;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.groupnames.clustering.weka.GroupNameClusteringWEKA;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.groupnames.helper.GroupNameExtraction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.specifications.ExperimentalGroupSpecifications;

public class PredictGroupNameCluster {

	private final GroupNameClusteringWEKA gnc;

	private static final File instanceDirectory = new File(
			"src/main/resources/slotfilling/experimental_group/corpus/instances/");

	public static void main(String[] args) throws Exception {
		SystemScope.Builder.getScopeHandler().addScopeSpecification(ExperimentalGroupSpecifications.systemsScope)
				.build();
		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(100L)
				.setTrainingProportion(80).setTestProportion(20).setCorpusSizeFraction(1F).build();

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		PredictGroupNameCluster clustering = new PredictGroupNameCluster(
				instanceProvider.getRedistributedTrainingInstances());

		for (Instance instance : instanceProvider.getRedistributedTestInstances()) {

			System.out.println("*********" + instance.getName() + "********");
			List<DocumentLinkedAnnotation> annotations = GroupNameExtraction.extractGroupNamesWithPattern(instance);
			List<List<DocumentLinkedAnnotation>> clusters = clustering.cluster(annotations, 4);

			for (List<DocumentLinkedAnnotation> cluster : clusters) {

				for (DocumentLinkedAnnotation groupName : cluster) {
					System.out.println(groupName.toPrettyString());
				}
				System.out.println("----------------------------");
			}
			System.out.println("******************************");
		}
	}

	public PredictGroupNameCluster(List<Instance> trainInstances) throws Exception {

		gnc = new GroupNameClusteringWEKA();

		gnc.train(trainInstances);
	}

	public List<List<DocumentLinkedAnnotation>> cluster(List<DocumentLinkedAnnotation> annotations, int k) {
		try {
			return gnc.cluster(annotations, k);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

}
