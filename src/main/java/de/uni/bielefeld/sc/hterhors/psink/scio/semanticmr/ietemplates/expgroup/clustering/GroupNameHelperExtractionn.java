package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class GroupNameHelperExtractionn {

	public static Map<Boolean, Set<GroupNamePair>> extractData(List<Instance> allInstances) {

		Map<Boolean, Set<GroupNamePair>> pairs = new HashMap<>();
		pairs.put(true, new HashSet<>());
		pairs.put(false, new HashSet<>());
		for (Instance instance : allInstances) {
			extractPairs(pairs, instance);
		}

		/**
		 * equal distribution
		 */
		pairs.put(false, pairs.get(false).stream().limit(pairs.get(true).size()).collect(Collectors.toSet()));
		return pairs;
	}

	private static void extractPairs(Map<Boolean, Set<GroupNamePair>> pairs, Instance instance) {

		List<List<DocumentLinkedAnnotation>> clusteredGroupNames = new ArrayList<>();
		for (AbstractAnnotation experimentalgroup : instance.getGoldAnnotations().getAnnotations()) {
			List<DocumentLinkedAnnotation> cluster = new ArrayList<>();
			clusteredGroupNames.add(cluster);
			for (AbstractAnnotation groupName : experimentalgroup.asInstanceOfEntityTemplate()
					.getMultiFillerSlot(SCIOSlotTypes.hasGroupName).getSlotFiller()) {

				if (!groupName.isInstanceOfDocumentLinkedAnnotation())
					continue;
				cluster.add(groupName.asInstanceOfDocumentLinkedAnnotation());
			}
		}

		for (int i = 0; i < clusteredGroupNames.size(); i++) {
			for (int j = i; j < clusteredGroupNames.size(); j++) {
				for (int l = 0; l < clusteredGroupNames.get(i).size(); l++) {
					for (int k = l + 1; k < clusteredGroupNames.get(j).size(); k++) {
						pairs.get(i == j).add(new GroupNamePair(clusteredGroupNames.get(i).get(l),
								clusteredGroupNames.get(j).get(k), i == j));
					}
				}
			}
		}

	}

}
