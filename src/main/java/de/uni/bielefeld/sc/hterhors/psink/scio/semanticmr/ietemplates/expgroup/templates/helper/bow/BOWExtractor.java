package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.helper.bow;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.variables.DocumentToken;

public class BOWExtractor {

	public static Set<TypedBOW> extractTypedBOW(EntityTemplate annotation) {

		Set<TypedBOW> typedBOW = new HashSet<>();

		AbstractAnnotation rootAnnotation = annotation.getRootAnnotation();

		if (rootAnnotation.isInstanceOfDocumentLinkedAnnotation()) {
			typedBOW.add(
					new TypedBOW(extractDocLinkedBOW(rootAnnotation.asInstanceOfDocumentLinkedAnnotation()), null));
		}

		final Map<SlotType, Set<AbstractAnnotation>> propertyAnnotations = annotation.filter().docLinkedAnnoation()
				.multiSlots().singleSlots().merge().nonEmpty().build().getMergedAnnotations();

		for (Entry<SlotType, Set<AbstractAnnotation>> prop : propertyAnnotations.entrySet()) {

			Set<String> tmp = new HashSet<>();
			for (AbstractAnnotation val : prop.getValue()) {
				tmp.addAll(extractDocLinkedBOW(val.asInstanceOfDocumentLinkedAnnotation()));
			}

			typedBOW.add(new TypedBOW(tmp, prop.getKey()));

		}

		/*
		 * Recursive search for terms.
		 */
//		for (Set<AbstractAnnotation> props : annotation.filter().entityTemplateAnnoation().multiSlots().singleSlots()
//				.merge().nonEmpty().build().getMergedAnnotations().values()) {
//			for (AbstractAnnotation prop : props) {
//				typedBOW.addAll(extractTypedBOW(prop.asInstanceOfEntityTemplate()));
//			}
//		}

		return typedBOW;
	}

	public static Set<String> extractDocLinkedBOW(DocumentLinkedAnnotation docLinkedAnnotation) {
		Set<String> bow = new HashSet<>();
		for (DocumentToken docToken : docLinkedAnnotation.relatedTokens) {
			bow.add(docToken.getText());
		}

		if (!docLinkedAnnotation.entityType.isLiteral) {
			for (String part : camelCaseSplit(docLinkedAnnotation.entityType.entityName)) {
				bow.add(part);
			}
		}

		return bow;

	}

	private static String[] camelCaseSplit(String entityName) {
		return entityName.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
	}

	public static Set<String> getExpGroupBOW(EntityTemplate experimentalGroup) {

		final Set<String> bow;

		final AbstractAnnotation rootAnnotation = experimentalGroup.getRootAnnotation();

		if (rootAnnotation.isInstanceOfDocumentLinkedAnnotation())
			bow = BOWExtractor.extractDocLinkedBOW(rootAnnotation.asInstanceOfDocumentLinkedAnnotation());
		else
			bow = new HashSet<>();

		for (AbstractAnnotation groupName : experimentalGroup.getMultiFillerSlot("hasGroupName").getSlotFiller()) {

			if (groupName.isInstanceOfDocumentLinkedAnnotation())
				bow.addAll(BOWExtractor.extractDocLinkedBOW(groupName.asInstanceOfDocumentLinkedAnnotation()));
		}

		return bow;
	}
}
