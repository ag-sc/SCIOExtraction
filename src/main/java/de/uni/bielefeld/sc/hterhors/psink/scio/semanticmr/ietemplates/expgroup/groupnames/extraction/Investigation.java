package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.groupnames.extraction;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.groupnames.helper.GroupNameExtraction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.AutomatedSectionifcation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.AutomatedSectionifcation.ESection;

/**
 * IDEA:
 * 
 * Search for groupnames in context of results. Limit groupNames to appearance
 * in Materials and Abstract and Result section
 * 
 * @author hterhors
 *
 */
public class Investigation {

	private static final File instanceDirectory = new File(
			"src/main/resources/slotfilling/experimental_group/corpus/instances/");

	Set<String> indicator = new HashSet<>(Arrays.asList("group", "rats", "animals", "groups", "control", "p", "OEC",
			"compared", "significantly", "P", "significant", "n", "treated", "BBB", "higher", "controls", "increased",
			"score", "differences", "vs", "statistically", "versus", "difference", "different", "increase", "lower",
			"statistical", "positive", "differ", "significance", "change", "decreased", "indicated", "decrease",
			"slightly", "smaller", "distance"));

	public static void main(String[] args) {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadDataStructureReader("ExperimentalGroup")).build();
		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(100L)
				.setTrainingProportion(80).setTestProportion(20).setCorpusSizeFraction(1F).build();

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		Map<String, Integer> count = new HashMap<>();

		for (Instance instance : instanceProvider.getInstances()) {

//			System.out.println("Instance: " + instance.getName());
			AutomatedSectionifcation sectionification = AutomatedSectionifcation.getInstance(instance);

			List<DocumentLinkedAnnotation> goldGroupNames = GroupNameExtraction.extractGroupNamesFromGold(instance);

			for (DocumentLinkedAnnotation groupName : goldGroupNames) {

				if (sectionification.getSection(groupName) != ESection.RESULTS)
					continue;

//				System.out.println(sectionification.getSection(groupName) + " - " + groupName.toPrettyString());

//				System.out.println(instance.getDocument().getContentOfSentence(groupName.getSentenceIndex()));

				for (DocumentToken token : instance.getDocument().getSentenceByIndex(groupName.getSentenceIndex())) {
					if (token.isPunctuation())
						continue;
					if (token.isStopWord())
						continue;
					count.put(token.getText(), count.getOrDefault(token.getText(), 0) + 1);
				}

			}

			// System.out.println("----------------------");
//			List<DocumentLinkedAnnotation> groupNames = GroupNameExtraction.extractGroupNamesWithPattern(instance);
//
//			for (DocumentLinkedAnnotation groupName : groupNames) {
//				if (sectionification.getSection(groupName) == ESection.RESULTS)
//					System.out.println(sectionification.getSection(groupName) + " - " + groupName.toPrettyString());
//			}

		}

		List<Map.Entry<String, Integer>> sortedWortCount = new ArrayList<>(count.entrySet());
		Collections.sort(sortedWortCount, new Comparator<Map.Entry<String, Integer>>() {

			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return -Integer.compare(o1.getValue(), o2.getValue());
			}

		});
		sortedWortCount.forEach(System.out::println);
	}

	private static void countAppearances(InstanceProvider instanceProvider) {
		Map<ESection, Integer> count = new HashMap<>();

		for (Instance instance : instanceProvider.getInstances()) {

			System.out.println("Instance: " + instance.getName());
			AutomatedSectionifcation sectionification = AutomatedSectionifcation.getInstance(instance);

			List<DocumentLinkedAnnotation> groupNames = GroupNameExtraction.extractGroupNamesWithPattern(instance);

			for (DocumentLinkedAnnotation groupName : groupNames) {
//				System.out.println(sectionification.getSection(groupName) + " - " + groupName.toPrettyString());

				count.put(sectionification.getSection(groupName),
						count.getOrDefault(sectionification.getSection(groupName), 0) + 1);

			}
		}
		System.out.println(count);
	}

}
