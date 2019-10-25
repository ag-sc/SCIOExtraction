package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.helper.bow;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.variables.DocumentToken;

public class BOWExtractor {

	public static Set<TypedBOW> extractTypedBOW(EntityTemplate annotation) {

		Set<TypedBOW> typedBOW = new HashSet<>();

		AbstractAnnotation rootAnnotation = annotation.getRootAnnotation();

		final Set<String> bow = extractEntityTypeBOW(rootAnnotation.getEntityType());
		if (rootAnnotation.isInstanceOfDocumentLinkedAnnotation()) {
			bow.addAll(extractDocLinkedBOW(rootAnnotation.asInstanceOfDocumentLinkedAnnotation()));
		}
		typedBOW.add(new TypedBOW(bow, null));

		final Map<SlotType, Set<AbstractAnnotation>> propertyAnnotations = annotation.filter().docLinkedAnnoation()
				.multiSlots().singleSlots().merge().nonEmpty().build().getMergedAnnotations();

		for (Entry<SlotType, Set<AbstractAnnotation>> prop : propertyAnnotations.entrySet()) {

			Set<String> tmp = extractEntityTypeBOW(rootAnnotation.getEntityType());

			for (AbstractAnnotation val : prop.getValue()) {
				tmp.addAll(extractDocLinkedBOW(val.asInstanceOfDocumentLinkedAnnotation()));
			}

			typedBOW.add(new TypedBOW(tmp, prop.getKey()));

		}

		/*
		 * Recursive search for terms.
		 */
		for (Set<AbstractAnnotation> props : annotation.filter().entityTemplateAnnoation().multiSlots().singleSlots()
				.merge().nonEmpty().build().getMergedAnnotations().values()) {
			for (AbstractAnnotation prop : props) {
				typedBOW.addAll(extractTypedBOW(prop.asInstanceOfEntityTemplate()));
			}
		}

		return typedBOW;
	}

	public static Set<String> extractDocLinkedBOW(DocumentLinkedAnnotation docLinkedAnnotation) {
		Set<String> bow = new HashSet<>();
		for (DocumentToken docToken : docLinkedAnnotation.relatedTokens) {
			bow.add(docToken.getText());
		}
		for (String docToken : docLinkedAnnotation.textualContent.surfaceForm.split("\\W")) {
			bow.add(docToken);
		}
		return bow;

	}

	public static Set<String> extractEntityTypeBOW(EntityType entityType) {
		Set<String> bow = new HashSet<>();
		if (!entityType.isLiteral) {
			for (String part : camelCaseSplit(entityType.entityName)) {
				bow.add(part);
			}
		}

		return bow;

	}

	private static String[] camelCaseSplit(String entityName) {
		return entityName.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
	}

	public static Set<String> getExpGroupBOW(EntityTemplate experimentalGroup) {

		final AbstractAnnotation rootAnnotation = experimentalGroup.getRootAnnotation();

		final Set<String> bow = extractEntityTypeBOW(rootAnnotation.getEntityType());

		if (rootAnnotation.isInstanceOfDocumentLinkedAnnotation())
			bow.addAll(extractDocLinkedBOW(rootAnnotation.asInstanceOfDocumentLinkedAnnotation()));

		for (AbstractAnnotation groupName : experimentalGroup.getMultiFillerSlot("hasGroupName").getSlotFiller()) {
			if (groupName.isInstanceOfDocumentLinkedAnnotation())
				bow.addAll(BOWExtractor.extractDocLinkedBOW(groupName.asInstanceOfDocumentLinkedAnnotation()));
		}

		return bow;
	}
	
	
}
