package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering;

import java.util.Collections;
import java.util.List;

import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering.weka.GroupNameClusteringWEKA;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EDistinctGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EExtractGroupNamesMode;

public class PredictGroupNameCluster {

	private final GroupNameClusteringWEKA gnc;

	public PredictGroupNameCluster(List<Instance> trainInstances) throws Exception {

		gnc = new GroupNameClusteringWEKA();

		gnc.train(trainInstances);
	}

	public List<List<DocumentLinkedAnnotation>> cluster(List<DocumentLinkedAnnotation> annotations,
			EDistinctGroupNamesMode distinctGroupNamesMode, EExtractGroupNamesMode extractGroupNamesMode, int k) {

		try {
			return gnc.cluster(annotations, k);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

}
