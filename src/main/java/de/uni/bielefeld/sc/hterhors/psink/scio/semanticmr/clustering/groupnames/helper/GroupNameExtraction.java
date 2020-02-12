package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.AutomatedSectionifcation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.AutomatedSectionifcation.ESection;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.investigation.CollectExpGroupNames;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.investigation.CollectExpGroupNames.PatternIndexPair;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.modes.Modes.EDistinctGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.modes.Modes.EExtractGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.NPChunker;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.NPChunker.TermIndexPair;

public class GroupNameExtraction {

	public static List<DocumentLinkedAnnotation> extractGroupNames(Instance instance,
			EDistinctGroupNamesMode distinctGroupNamesMode, EExtractGroupNamesMode groupNameProviderMode) {

		List<DocumentLinkedAnnotation> groupNames = new ArrayList<>();
		switch (groupNameProviderMode) {
		case EMPTY:
			break;
		case GOLD:
			groupNames.addAll(GroupNameExtraction.extractGroupNamesFromGold(instance));
			break;
		case NP_CHUNKS:
		case TRAINING_PATTERN_NP_CHUNKS:
			groupNames.addAll(GroupNameExtraction.extractGroupNamesWithNPCHunks(distinctGroupNamesMode, instance));
			break;
		case TRAINING_MANUAL_PATTERN:
		case MANUAL_PATTERN:
			groupNames.addAll(GroupNameExtraction.extractGroupNamesWithPattern(distinctGroupNamesMode, instance));
			break;
		case MANUAL_PATTERN_NP_CHUNKS:
		case TRAINING_MANUAL_PATTERN_NP_CHUNKS:
			groupNames.addAll(GroupNameExtraction.extractGroupNamesWithNPCHunks(distinctGroupNamesMode, instance));
			groupNames.addAll(GroupNameExtraction.extractGroupNamesWithPattern(distinctGroupNamesMode, instance));
			break;
		case TRAINING_PATTERN:
			break;
		}
		return groupNames;
	}

	public static List<DocumentLinkedAnnotation> filter(Instance instance, List<DocumentLinkedAnnotation> groupNames) {
		AutomatedSectionifcation sectionification = AutomatedSectionifcation.getInstance(instance);

		for (Iterator<DocumentLinkedAnnotation> iterator = groupNames.iterator(); iterator.hasNext();) {
			DocumentLinkedAnnotation groupName = iterator.next();
			ESection section = sectionification.getSection(groupName);
			if (section != ESection.RESULTS && section != ESection.ABSTRACT && section != ESection.METHODS)
				iterator.remove();

		}

		return groupNames;
	}

	public static List<DocumentLinkedAnnotation> extractGroupNamesWithPattern(Instance instance) {
		return filter(instance, extractGroupNamesWithPattern(EDistinctGroupNamesMode.NOT_DISTINCT, instance));
	}

	public static List<DocumentLinkedAnnotation> extractGroupNamesWithPattern(
			EDistinctGroupNamesMode distinctGroupNamesMode, Instance instance) {
		List<DocumentLinkedAnnotation> anns = new ArrayList<>();
		Set<String> distinct = new HashSet<>();
		for (PatternIndexPair p : CollectExpGroupNames.pattern) {
			Matcher m = p.pattern.matcher(instance.getDocument().documentContent);
			while (m.find()) {
				for (Integer group : p.groups) {
					DocumentLinkedAnnotation annotation;
					try {
						String groupName = m.group(group);
						if (groupName.length() > CollectExpGroupNames.maxLength)
							continue;

						if (CollectExpGroupNames.STOP_TERM_LIST.contains(groupName))
							continue;

						if (distinctGroupNamesMode == EDistinctGroupNamesMode.STRING_DISTINCT) {

							if (distinct.contains(groupName))
								continue;

							distinct.add(groupName);
						}
						annotation = AnnotationBuilder.toAnnotation(instance.getDocument(), SCIOEntityTypes.groupName,
								groupName, m.start(group));
					} catch (Exception e) {
						annotation = null;
					}
					if (annotation != null)
						anns.add(annotation);
				}
			}
		}
		return filter(instance, anns);
	}

	public static List<DocumentLinkedAnnotation> extractGroupNamesFromGold(Instance instance) {
		return instance.getGoldAnnotations().getAbstractAnnotations().stream()
				.map(e -> e.asInstanceOfEntityTemplate().getMultiFillerSlot(SCIOSlotTypes.hasGroupName))
				.filter(s -> s.containsSlotFiller()).flatMap(s -> s.getSlotFiller().stream())
				.map(e -> e.asInstanceOfDocumentLinkedAnnotation()).collect(Collectors.toList());
	}

	public static List<DocumentLinkedAnnotation> extractGroupNamesWithNPCHunks(
			EDistinctGroupNamesMode distinctGroupNamesMode, Instance instance) {
		Set<String> distinct = new HashSet<>();

		List<DocumentLinkedAnnotation> anns = new ArrayList<>();
		try {
			for (TermIndexPair groupName : new NPChunker(instance.getDocument()).getNPs()) {
				DocumentLinkedAnnotation annotation;
				if (CollectExpGroupNames.STOP_TERM_LIST.contains(groupName.term))
					continue;

				if (groupName.term.length() > NPChunker.maxLength)
					continue;

				if (distinctGroupNamesMode == EDistinctGroupNamesMode.STRING_DISTINCT) {

					if (distinct.contains(groupName.term))
						continue;

					distinct.add(groupName.term);
				}
				try {
					annotation = AnnotationBuilder.toAnnotation(instance.getDocument(), SCIOEntityTypes.groupName,
							groupName.term, groupName.index);
				} catch (Exception e) {
					annotation = null;
				}
				if (annotation != null)
					anns.add(annotation);

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return filter(instance, anns);
	}

}
