package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.helper.bow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer.EExplorationMode;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTypeAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class TB_BOWExtractor {

	public static Set<TypedBOW> extractTypedBOW(Instance instance, EntityTemplate annotation) {
		Set<TypedBOW> typedBOW;
		fillTypedBOW(instance, typedBOW = new HashSet<>(), annotation);
		return typedBOW;
	}

	/**
	 * Recursive walk through annotation and its properties.
	 * 
	 * @param typedBOW
	 * @param annotation
	 */
	private static void fillTypedBOW(Instance instance, Set<TypedBOW> typedBOW, EntityTemplate annotation) {

		AbstractAnnotation rootAnnotation = annotation.getRootAnnotation();

		final Set<String> bow = extractEntityTypeBOW(instance, rootAnnotation.getEntityType());

		typedBOW.add(new TypedBOW(bow, null));

		final Map<SlotType, Set<AbstractAnnotation>> propertyAnnotations = annotation.filter().entityTypeAnnoation()
				.multiSlots().singleSlots().merge().nonEmpty().build().getMergedAnnotations();

		for (Entry<SlotType, Set<AbstractAnnotation>> prop : propertyAnnotations.entrySet()) {

//			if (!(prop.getKey() == SlotType.get("hasDeliveryMethod") || prop.getKey() == SCIOSlotTypes.hasCompound
//					|| prop.getKey() == SlotType.get("hasDosage")))
//				continue;

			Set<String> tmp = extractEntityTypeBOW(instance, rootAnnotation.getEntityType());

			for (AbstractAnnotation val : prop.getValue()) {
//				tmp.addAll(extractEntityTypeBOW(val.getEntityType()));
//				tmp.addAll(extractDocLinkedBOW(val.asInstanceOfDocumentLinkedAnnotation()));
				for (EntityTypeAnnotation eta : instance.getEntityTypeCandidates(EExplorationMode.ANNOTATION_BASED,
						val.getEntityType())) {

					if (eta.isInstanceOfDocumentLinkedAnnotation()) {
						tmp.addAll(extractDocLinkedBOW(eta.asInstanceOfDocumentLinkedAnnotation()));
					}

				}
			}

			typedBOW.add(new TypedBOW(tmp, prop.getKey()));

		}

		/*
		 * Recursive search for terms.
		 */
		for (Set<AbstractAnnotation> props : annotation.filter().entityTemplateAnnoation().multiSlots().singleSlots()
				.merge().nonEmpty().build().getMergedAnnotations().values()) {
			for (AbstractAnnotation prop : props) {
				fillTypedBOW(instance, typedBOW, prop.asInstanceOfEntityTemplate());
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

	public static Set<String> extractEntityTypeBOW(Instance instance, EntityType entityType) {
		Set<String> bow = extractEntityTypeBOW(entityType);

		if (!entityType.isLiteral)
			for (EntityTypeAnnotation eta : instance.getEntityTypeCandidates(EExplorationMode.ANNOTATION_BASED,
					entityType)) {

				if (eta.isInstanceOfDocumentLinkedAnnotation()) {
					bow.addAll(extractDocLinkedBOW(eta.asInstanceOfDocumentLinkedAnnotation()));
				}

			}

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

	/**
	 * Returns a bag of tokens of the given experimental group including its group
	 * names.
	 * 
	 * @param experimentalGroup
	 * @return
	 */
	public static Set<String> getExpGroupPlusNameBOW(EntityTemplate experimentalGroup) {

		final AbstractAnnotation rootAnnotation = experimentalGroup.getRootAnnotation();

		final Set<String> bow = extractEntityTypeBOW(rootAnnotation.getEntityType());

		if (rootAnnotation.isInstanceOfDocumentLinkedAnnotation())
			bow.addAll(extractDocLinkedBOW(rootAnnotation.asInstanceOfDocumentLinkedAnnotation()));

		if (SCIOSlotTypes.hasGroupName.isExcluded())
			return bow;

		for (AbstractAnnotation groupName : experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasGroupName)
				.getSlotFiller()) {

			if (groupName.isInstanceOfDocumentLinkedAnnotation())
				bow.addAll(extractDocLinkedBOW(groupName.asInstanceOfDocumentLinkedAnnotation()));
		}

		return bow;
	}

}
