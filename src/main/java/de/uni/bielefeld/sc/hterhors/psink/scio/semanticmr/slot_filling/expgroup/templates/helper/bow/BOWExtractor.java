package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.helper.bow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class BOWExtractor {

	public static Set<TypedBOW> extractTypedBOW(EntityTemplate annotation) {
		Set<TypedBOW> typedBOW;
		fillTypedBOW(typedBOW = new HashSet<>(), annotation);
		return typedBOW;
	}

	/**
	 * Recursive walk through annotation and its properties.
	 * 
	 * @param typedBOW
	 * @param annotation
	 */
	private static void fillTypedBOW(Set<TypedBOW> typedBOW, EntityTemplate annotation) {

		AbstractAnnotation rootAnnotation = annotation.getRootAnnotation();

		final Set<String> bow = extractEntityTypeBOW(rootAnnotation.getEntityType());

		if (rootAnnotation.isInstanceOfDocumentLinkedAnnotation()) {
			bow.addAll(extractDocLinkedBOW(rootAnnotation.asInstanceOfDocumentLinkedAnnotation()));
		}
		typedBOW.add(new TypedBOW(bow, null));

		final Map<SlotType, Set<AbstractAnnotation>> propertyAnnotations = annotation.filter().docLinkedAnnoation()
				.multiSlots().singleSlots().merge().nonEmpty().build().getMergedAnnotations();

		for (Entry<SlotType, Set<AbstractAnnotation>> prop : propertyAnnotations.entrySet()) {

//			if (!(prop.getKey() == SlotType.get("hasDeliveryMethod") || prop.getKey() == SCIOSlotTypes.hasCompound
//					|| prop.getKey() == SlotType.get("hasDosage")))
//				continue;

			Set<String> tmp = extractEntityTypeBOW(rootAnnotation.getEntityType());

			for (AbstractAnnotation val : prop.getValue()) {
//				tmp.addAll(extractEntityTypeBOW(val.getEntityType()));
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
				fillTypedBOW(typedBOW, prop.asInstanceOfEntityTemplate());
			}
		}

	}

	public static Set<String> extractDocLinkedBOW(DocumentLinkedAnnotation docLinkedAnnotation) {
		Set<String> bow = new HashSet<>();
		for (DocumentToken docToken : docLinkedAnnotation.relatedTokens) {
//			if (docToken.isPunctuation() || docToken.isStopWord())
//				continue;
			bow.add(docToken.getText());
		}
//		for (String docToken : docLinkedAnnotation.textualContent.surfaceForm.split("\\W")) {
//			bow.add(docToken);
//		}
		return bow;

	}

	public static Set<String> extractEntityTypeBOW(EntityType entityType) {
		Set<String> bow = new HashSet<>();
		if (!entityType.isLiteral) {
			for (String part : camelCaseSplit(entityType.name)) {
				bow.add(part);
			}
		}

		return bow;

	}

	private static Map<String, String[]> camelCaseSplitCache = new HashMap<>();

	private static String[] camelCaseSplit(String entityName) {
		String[] split;
		if ((split = camelCaseSplitCache.get(entityName)) == null) {
			camelCaseSplitCache.put(entityName,
					split = entityName.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"));
		}
		return split;
	}

	public static Set<String> getExpGroupPlusNameBOW(EntityTemplate experimentalGroup) {

		final AbstractAnnotation rootAnnotation = experimentalGroup.getRootAnnotation();

		final Set<String> bow = extractEntityTypeBOW(rootAnnotation.getEntityType());

		if (rootAnnotation.isInstanceOfDocumentLinkedAnnotation())
			bow.addAll(extractDocLinkedBOW(rootAnnotation.asInstanceOfDocumentLinkedAnnotation()));

		if (SCIOSlotTypes.hasGroupName.isExcluded())
			return bow;

		for (AbstractAnnotation groupName : experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasGroupName)
				.getSlotFiller()) {

			bow.addAll(extractEntityTypeBOW(groupName.getEntityType()));
			if (groupName.isInstanceOfDocumentLinkedAnnotation())
				bow.addAll(BOWExtractor.extractDocLinkedBOW(groupName.asInstanceOfDocumentLinkedAnnotation()));
		}

		return bow;
	}

}
