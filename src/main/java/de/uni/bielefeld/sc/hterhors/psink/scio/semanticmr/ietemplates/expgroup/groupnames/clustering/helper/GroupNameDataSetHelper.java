package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.groupnames.clustering.helper;

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

public class GroupNameDataSetHelper {

	public static Map<Boolean, Set<GroupNamePair>> getGroupNameClusterDataSet(List<Instance> instances) {

		Map<Boolean, Set<GroupNamePair>> groupNameClusterDataSet = new HashMap<>();
		groupNameClusterDataSet.put(true, new HashSet<>());
		groupNameClusterDataSet.put(false, new HashSet<>());
		for (Instance instance : instances) {

			List<List<DocumentLinkedAnnotation>> clusteredGroupNames = getGoldClusteredGroupNames(instance);

			for (int i = 0; i < clusteredGroupNames.size(); i++) {
				for (int j = i; j < clusteredGroupNames.size(); j++) {
					for (int l = 0; l < clusteredGroupNames.get(i).size(); l++) {
						for (int k = l + 1; k < clusteredGroupNames.get(j).size(); k++) {
							groupNameClusterDataSet.get(i == j).add(new GroupNamePair(clusteredGroupNames.get(i).get(l),
									clusteredGroupNames.get(j).get(k), i == j, 1));
						}
					}
				}
			}

		}

		/**
		 * equal distribution
		 */
		groupNameClusterDataSet.put(false, groupNameClusterDataSet.get(false).stream()
				.limit(groupNameClusterDataSet.get(true).size()).collect(Collectors.toSet()));

		return groupNameClusterDataSet;
	}

	private static List<List<DocumentLinkedAnnotation>> getGoldClusteredGroupNames(Instance instance) {

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
		return clusteredGroupNames;
	}

	public static List<DocumentLinkedAnnotation> extractGroupNameAnnotations(
			Map<Boolean, Set<GroupNamePair>> goldPairs) {
		List<DocumentLinkedAnnotation> datapoints = new ArrayList<>();
		datapoints.addAll(goldPairs.get(true).stream().map(g -> g.groupName1.asInstanceOfDocumentLinkedAnnotation())
				.collect(Collectors.toList()));
		datapoints.addAll(goldPairs.get(true).stream().map(g -> g.groupName2.asInstanceOfDocumentLinkedAnnotation())
				.collect(Collectors.toList()));
		datapoints.addAll(goldPairs.get(false).stream().map(g -> g.groupName1.asInstanceOfDocumentLinkedAnnotation())
				.collect(Collectors.toList()));
		datapoints.addAll(goldPairs.get(false).stream().map(g -> g.groupName2.asInstanceOfDocumentLinkedAnnotation())
				.collect(Collectors.toList()));
		return datapoints;
	}

}
