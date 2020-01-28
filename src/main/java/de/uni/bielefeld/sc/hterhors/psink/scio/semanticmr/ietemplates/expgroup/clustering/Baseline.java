package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering;

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

import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.clustering.kmeans.KMeansWords;

public class Baseline {

	public Baseline(List<Instance> allInstances) {
		Score overall = new Score();
		for (Instance instance : allInstances) {

			Map<Boolean, Set<GroupNamePair>> goldPairs = GroupNameHelperExtractionn
					.extractTrainingData(Arrays.asList(instance));

			List<DocumentLinkedAnnotation> datapoints = new ArrayList<>();
			datapoints.addAll(goldPairs.get(true).stream().map(g -> g.groupName1).collect(Collectors.toList()));
			datapoints.addAll(goldPairs.get(true).stream().map(g -> g.groupName2).collect(Collectors.toList()));
			datapoints.addAll(goldPairs.get(false).stream().map(g -> g.groupName1).collect(Collectors.toList()));
			datapoints.addAll(goldPairs.get(false).stream().map(g -> g.groupName2).collect(Collectors.toList()));

			if (datapoints.size() == 0)
				continue;

			List<List<DocumentLinkedAnnotation>> clusters = KMeansWords.cluster(datapoints, 4);

			Map<Boolean, Set<GroupNamePair>> kMeansPairs = new HashMap<>();

			kMeansPairs.put(true, new HashSet<>());
			kMeansPairs.put(false, new HashSet<>());

			for (int i = 0; i < clusters.size(); i++) {
				for (int j = i; j < clusters.size(); j++) {
					for (int l = 0; l < clusters.get(i).size(); l++) {
						for (int k = l + 1; k < clusters.get(j).size(); k++) {
							kMeansPairs.get(i == j)
									.add(new GroupNamePair(clusters.get(i).get(l), clusters.get(j).get(k), i == j));
						}
					}
				}
			}

			Score s = prf1(
					Streams.concat(kMeansPairs.get(true).stream(), kMeansPairs.get(false).stream())
							.collect(Collectors.toSet()),
					Streams.concat(goldPairs.get(true).stream(), goldPairs.get(false).stream())
							.collect(Collectors.toSet()));

			overall.add(s);
		}

		System.out.println("Baseline score = " + overall);

	}

	public Score prf1(Collection<GroupNamePair> annotations, Collection<GroupNamePair> otherAnnotations) {
//		System.out.println("annotations.size(): " + annotations.size());
//		System.out.println(annotations);
//		System.out.println("otherAnnotations.size(): " + otherAnnotations.size());
//		System.out.println(otherAnnotations);
		int tp = 0;
		int fp = 0;
		int fn = 0;

		outer: for (GroupNamePair a : annotations) {
			for (GroupNamePair oa : otherAnnotations) {
				if (oa.equals(a)) {
					tp++;
					continue outer;
				}
			}

			fn++;
		}

		outer: for (GroupNamePair a : otherAnnotations) {
			for (GroupNamePair oa : annotations) {
				if (oa.equals(a)) {
					continue outer;
				}
			}
			fp++;
		}
//		System.out.println(new Score(tp, fp, fn));
//		System.out.println();
		return new Score(tp, fp, fn);

	}
}
