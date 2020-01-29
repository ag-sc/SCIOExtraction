package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering;

import java.io.File;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering.weka.GroupNameClusteringWEKA;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.specifications.ExperimentalGroupSpecifications;

public class Evaluate {

	private static final File instanceDirectory = new File(
			"src/main/resources/slotfilling/experimental_group/corpus/instances/");

	public static void main(String[] args) throws Exception {

		SystemScope.Builder.getScopeHandler().addScopeSpecification(ExperimentalGroupSpecifications.systemsScope)
				.build();
		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(100L)
				.setTrainingProportion(80).setTestProportion(20).setCorpusSizeFraction(1F).build();

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

//		new KMeansBaseline(instanceProvider.getRedistributedTestInstances(), 1);
//		new KMeansBaseline(instanceProvider.getRedistributedTestInstances(), 2);
//		new KMeansBaseline(instanceProvider.getRedistributedTestInstances(), 3);
//		new KMeansBaseline(instanceProvider.getRedistributedTestInstances(), 4);
//		new KMeansBaseline(instanceProvider.getRedistributedTestInstances(), 5);
//		new KMeansBaseline(instanceProvider.getRedistributedTestInstances(), 6);

		GroupNameClusteringWEKA gnc = new GroupNameClusteringWEKA();

		gnc.train(instanceProvider.getRedistributedTrainingInstances());
		gnc.test(instanceProvider.getRedistributedTestInstances());
	}

}
