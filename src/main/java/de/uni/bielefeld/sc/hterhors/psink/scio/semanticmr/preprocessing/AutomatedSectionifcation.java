package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.InjuryRestrictionProvider.EInjuryModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.specs.InjurySpecs;

public class AutomatedSectionifcation {

	private static final File instanceDirectory = new File("src/main/resources/slotfilling/injury/corpus/instances/");

	public static void main(String[] args) throws IOException {

		SystemScope.Builder.getScopeHandler().addScopeSpecification(InjurySpecs.systemsScope).build();

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(1000L)
				.setTrainingProportion(80).setTestProportion(20).setCorpusSizeFraction(1F).build();

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		for (Instance instance : instanceProvider.getInstances()) {

			AutomatedSectionifcation sectionify = new AutomatedSectionifcation(instance);

			for (int i = 0; i < 200; i++) {
				System.out.println(i + " = " + sectionify.getSection(i));
			}

			break;
		}

	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	public static EInjuryModifications rule;

	public final String header = "Mode\tF1\tPrecision\tRecall";

	private static Map<String, ESection> map = new HashMap<>();

	public static enum ESection {
		ABSTRACT(new HashSet<>(Arrays.asList("Abstract", "ABSTRACT")), 0),

		INTRO(new HashSet<>(Arrays.asList("Background", "Introduction", "Intro", "INTRODUCTION")), 5),

		METHODS(new HashSet<>(Arrays.asList("Experimental", "Methods", "Material", "Materials", "Experiments",
				"MATERIALS", "MATERIAL", "METHODS")), 15),

		RESULTS(new HashSet<>(Arrays.asList("RESULTS", "Result", "Results")), 30),

		DISCUSSION(new HashSet<>(Arrays.asList("Conclusions", "Summary", "Discussion", "DISCUSSION")), 50),

		REFERENCES(new HashSet<>(
				Arrays.asList("Acknowledgements", "Acknowledgement", "REFERENCES", "ACKNOWLEDGMENTS", "References")),
				100),
		UNDEFINED(Collections.emptySet(), -1), BEGIN(Collections.emptySet(), -1);

		public final Set<String> synonyms;
		public final int ealiestAppearance;

		private ESection(Set<String> synonyms, int ealiestAppearance) {
			this.synonyms = synonyms;
			this.ealiestAppearance = ealiestAppearance;
			for (String syn : synonyms) {

				if (map.put(syn, this) != null) {
					throw new IllegalArgumentException("Synonym was used twice: " + syn);
				}

			}

		}

		public static ESection getSectionForTerm(String term) {
			return map.get(term);
		}

	}

	private static Set<String> sectionWhiteList = Arrays.stream(ESection.values()).flatMap(e -> e.synonyms.stream())
			.collect(Collectors.toSet());

	private static Set<String> sectionBlackList = new HashSet<>(Arrays.asList("The", "In", "Next", "New"));

	private final Instance instance;

	private final List<Section> sections;

	private AutomatedSectionifcation(Instance instance) {
		this.instance = instance;
		sections = sectionify(this.instance);
		Collections.sort(sections, new Comparator<Section>() {

			@Override
			public int compare(Section o1, Section o2) {
				return Integer.compare(o1.sentenceIndex, o2.sentenceIndex);
			}
		});
		if (sections.isEmpty() || sections.get(0).sentenceIndex != 0) {
			sections.add(0, new Section(0, ESection.BEGIN));
		}

	}

	private final static Map<Instance, AutomatedSectionifcation> factory = new ConcurrentHashMap<>();

	public static AutomatedSectionifcation getInstance(Instance instance) {
		AutomatedSectionifcation sec;
		if ((sec = factory.get(instance)) == null)
			synchronized (factory) {
				factory.put(instance, sec = new AutomatedSectionifcation(instance));
			}

		return sec;
	}

	private final Map<Integer, ESection> sectionMap = new HashMap<>();

	private ESection computeSection(int sentenceindex) {

		for (int i = 0; i < sections.size(); i++) {
			if (sentenceindex <= sections.get(i).sentenceIndex)
				return sections.get(i == 0 ? i : i - 1).section;
		}

		return ESection.UNDEFINED;
	}

	public ESection getSection(int sentenceindex) {

		ESection sec;
		if ((sec = sectionMap.get(sentenceindex)) == null) {
			sectionMap.put(sentenceindex, sec = computeSection(sentenceindex));
		}

		return sec;
	}

	static class Section {

		final public int sentenceIndex;
		final public ESection section;

		public Section(int sentenceIndex, ESection section) {
			super();
			this.sentenceIndex = sentenceIndex;
			this.section = section;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((section == null) ? 0 : section.hashCode());
			result = prime * result + sentenceIndex;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Section other = (Section) obj;
			if (section != other.section)
				return false;
			if (sentenceIndex != other.sentenceIndex)
				return false;
			return true;
		}

	}

	public static List<Section> sectionify(Instance instance) {
		List<DocumentToken> sectionTokens = new ArrayList<>();

		for (List<DocumentToken> sentence : instance.getDocument().getSentences()) {

			final String firstToken = sentence.get(0).getText();
			final String secondToken = sentence.get(1).getText();

			if (sectionBlackList.contains(firstToken))
				continue;

			if (sentence.get(0).isPunctuation())
				continue;

			if (!sectionWhiteList.contains(firstToken)) {

				if (sentence.get(1).isPunctuation())
					continue;

				if (sentence.get(0).isStopWord())
					continue;

				if (!secondToken.equals("and") && sentence.get(1).isStopWord())
					continue;
			}

			if (firstToken.length() <= 2) {
				boolean matches = false;
				for (String sectionTerm : sectionWhiteList) {
					matches |= sectionTerm.startsWith(firstToken)
							&& sectionTerm.substring(firstToken.length()).startsWith(secondToken);
				}
				if (!matches)
					continue;
			}

			if (firstToken.toLowerCase().equals("materials") && !secondToken.toLowerCase().equals("and"))
				continue;

			if (firstToken.toLowerCase().equals("experimental")
					&& !(secondToken.toLowerCase().equals("protocol") || secondToken.toLowerCase().equals("paradigm")))
				continue;

			if (secondToken.matches("[a-zA-Z]+") && secondToken.toLowerCase().equals(secondToken)
					&& !secondToken.equals("and") && !secondToken.equals("protocol"))
				continue;

			if (secondToken.matches("[a-zA-Z]+") && firstToken.matches("[a-zA-Z]+")
					&& secondToken.toUpperCase().equals(secondToken) && !firstToken.toUpperCase().equals(firstToken))
				continue;

			if (sectionWhiteList.contains(firstToken)) {
				sectionTokens.add(sentence.get(0));
			}

		}

		for (Iterator<DocumentToken> iterator = sectionTokens.iterator(); iterator.hasNext();) {
			DocumentToken sectionToken = (DocumentToken) iterator.next();

			ESection section = ESection.getSectionForTerm(sectionToken.getText());
			if (section.ealiestAppearance > sectionToken.getSentenceIndex())
				iterator.remove();

		}

		return sectionTokens.stream()
				.map(d -> new Section(d.getSentenceIndex(), ESection.getSectionForTerm(d.getText())))
				.collect(Collectors.toList());
	}

}
