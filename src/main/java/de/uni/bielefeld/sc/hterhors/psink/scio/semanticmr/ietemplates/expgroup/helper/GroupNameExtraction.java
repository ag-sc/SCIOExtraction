package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.investigation.CollectExpGroupNames;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.investigation.CollectExpGroupNames.PatternIndexPair;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EDistinctGroupNamesMode;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.EExtractGroupNamesMode;
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
			groupNames.addAll(GroupNameExtraction.extractGroupNamesWithNPCHunks(distinctGroupNamesMode, instance));
			break;
		case PATTERN:
			groupNames.addAll(GroupNameExtraction.extractGroupNamesWithPattern(distinctGroupNamesMode, instance));
			break;
		case PATTERN_NP_CHUNKS:
			groupNames.addAll(GroupNameExtraction.extractGroupNamesWithNPCHunks(distinctGroupNamesMode, instance));
			groupNames.addAll(GroupNameExtraction.extractGroupNamesWithPattern(distinctGroupNamesMode, instance));
			break;
		case PATTERN_NP_CHUNKS_GOLD:
			groupNames.addAll(GroupNameExtraction.extractGroupNamesFromGold(instance));
			groupNames.addAll(GroupNameExtraction.extractGroupNamesWithNPCHunks(distinctGroupNamesMode, instance));
			groupNames.addAll(GroupNameExtraction.extractGroupNamesWithPattern(distinctGroupNamesMode, instance));
			break;
		}
		return groupNames;
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
						String term = m.group(group);
						if (term.length() > NPChunker.maxLength)
							continue;

						if (CollectExpGroupNames.STOP_TERM_LIST.contains(term))
							continue;

						if (distinctGroupNamesMode == EDistinctGroupNamesMode.DISTINCT) {

							if (distinct.contains(term))
								continue;

							distinct.add(term);
						}
						annotation = AnnotationBuilder.toAnnotation(instance.getDocument(), SCIOEntityTypes.groupName,
								term, m.start(group));
					} catch (Exception e) {
						annotation = null;
					}
					if (annotation != null)
						anns.add(annotation);
				}
			}
		}
		return anns;
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
				if (groupName.term.matches(".+(group|animals|rats|mice|rats|cats|dogs)")) {
					DocumentLinkedAnnotation annotation;
					if (distinctGroupNamesMode == EDistinctGroupNamesMode.DISTINCT) {

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
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return anns;
	}

}
