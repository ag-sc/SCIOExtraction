package de.uni.bielefeld.sc.hterhors.psink.scio.tools;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.OriginalCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.examples.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.specifications.ExperimentalGroupSpecifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.AgeNormalization;

public class FindInvestigationDuplicates {

	public static void main(String[] args) {

		new FindInvestigationDuplicates();

	}

	private final File instanceDirectory = new File("src/main/resources/slotfilling/investigation/corpus/instances/");

	public FindInvestigationDuplicates() {

		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
				.build();

		SystemScope scope = SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(ExperimentalGroupSpecifications.systemsScope).apply()
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization()).build();

		InstanceProvider.maxNumberOfAnnotations = 100;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		for (Instance i : instanceProvider.getInstances()) {

			Map<String, Set<String>> dups = new HashMap<>();

			Set<AbstractAnnotation> dis = new HashSet<>();

			for (AbstractAnnotation a : i.getGoldAnnotations().getAnnotations()) {

				String Id1 = a.asInstanceOfEntityTemplate().getSingleFillerSlot("hasID").getSlotFiller()
						.asInstanceOfLiteralAnnotation().getSurfaceForm();

				Set<AbstractAnnotation> set2 = new HashSet<>();

				for (AbstractAnnotation a2 : i.getGoldAnnotations().getAnnotations()) {
					set2.add(a2.deepCopy());
				}

				for (AbstractAnnotation a2 : set2) {

					String Id2 = a2.asInstanceOfEntityTemplate().getSingleFillerSlot("hasID").getSlotFiller()
							.asInstanceOfLiteralAnnotation().getSurfaceForm();

					a.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasID")).clear();
					a2.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasID")).clear();

					if (a.equals(a2)) {
						dups.putIfAbsent(Id1, new HashSet<>());
						dups.get(Id1).add(Id2);
						dis.add(a.deepCopy());
					}

				}
				a.asInstanceOfEntityTemplate().setSingleSlotFiller(SlotType.get("hasID"),
						AnnotationBuilder.toAnnotation("ID", Id1));

			}

			Set<Set<String>> ids = new HashSet<>();
			for (Entry<String, Set<String>> du : dups.entrySet()) {
				Set<String> s = new HashSet<>(du.getValue());
				s.add(du.getKey());
				ids.add(s);
			}

			for (Set<String> set : ids) {
				if(set.size()==1)
					continue;
				
				StringBuffer sb = new StringBuffer(i.getName() + "\t");
				for (String duplicates : set) {
					sb.append(duplicates);
					sb.append(",");
				}
				System.out.println(sb.toString().substring(0, sb.length() - 1));

			}

		}
	}

}
