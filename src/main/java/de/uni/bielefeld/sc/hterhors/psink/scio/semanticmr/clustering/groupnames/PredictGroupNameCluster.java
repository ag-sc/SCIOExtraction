package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames;

import java.util.Collections;
import java.util.List;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.helper.GroupNameExtraction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.kmeans.WordBasedKMeans;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.weka.WEKAClustering;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EDistinctGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.EExtractGroupNamesMode;

public class PredictGroupNameCluster {

	private final WEKAClustering gnc;

	public static void main(String[] args) throws Exception {
		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("ExperimentalGroup"))
				.build();
		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(100L)
				.setTrainingProportion(80).setTestProportion(20).setCorpusSizeFraction(1F).build();

		InstanceProvider instanceProvider = new InstanceProvider(
				SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.experimentalGroup),
				corpusDistributor);

		PredictGroupNameCluster clustering = new PredictGroupNameCluster(
				instanceProvider.getTrainingInstances());

		for (Instance instance : instanceProvider.getTestInstances()) {
			System.out.println("*********" + instance.getName() + "********");
			System.out.println(instance.getGoldAnnotations().getAbstractAnnotations().size());
			List<DocumentLinkedAnnotation> annotations = GroupNameExtraction.extractGroupNames(instance,
					EDistinctGroupNamesMode.NOT_DISTINCT, EExtractGroupNamesMode.NP_CHUNKS);
			List<List<DocumentLinkedAnnotation>> clusters = clustering.clusterWordBased(annotations, 4);

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

		gnc = new WEKAClustering("TEST");

		gnc.trainOrLoad(trainInstances);
	}

	public List<List<DocumentLinkedAnnotation>> cluster(List<DocumentLinkedAnnotation> annotations, int k) {
		try {
			return gnc.cluster(annotations, k);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	public List<List<DocumentLinkedAnnotation>> clusterWordBased(List<DocumentLinkedAnnotation> annotations, int k) {
		WordBasedKMeans<DocumentLinkedAnnotation> kMeans = new WordBasedKMeans<>();
		return kMeans.clusterRSS(annotations, 1, 8);
	}

}
