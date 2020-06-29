package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.helper.bow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

	public static Set<TB_TypedBOW> extractTypedBOW(Instance instance, EntityTemplate annotation) {
		Set<TB_TypedBOW> TB_TypedBOW;
		fillTypedBOW(instance, TB_TypedBOW = new HashSet<>(), annotation);
		return TB_TypedBOW;
	}

	/**
	 * Recursive walk through annotation and its properties.
	 * 
	 * @param TB_TypedBOW
	 * @param annotation
	 */
	private static void fillTypedBOW(Instance instance, Set<TB_TypedBOW> TB_TypedBOW, EntityTemplate annotation) {

		AbstractAnnotation rootAnnotation = annotation.getRootAnnotation();

		final Set<String> bow = extractEntityTypeBOW(instance, rootAnnotation.getEntityType());

		TB_TypedBOW.add(new TB_TypedBOW(instance, bow, null));

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

			TB_TypedBOW.add(new TB_TypedBOW(instance, tmp, prop.getKey()));

		}

		/*
		 * Recursive search for terms.
		 */
		for (Set<AbstractAnnotation> props : annotation.filter().entityTemplateAnnoation().multiSlots().singleSlots()
				.merge().nonEmpty().build().getMergedAnnotations().values()) {
			for (AbstractAnnotation prop : props) {
				fillTypedBOW(instance, TB_TypedBOW, prop.asInstanceOfEntityTemplate());
			}
		}

	}

	private static Set<String> extractDocLinkedBOW(DocumentLinkedAnnotation docLinkedAnnotation) {
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

	private static Map<Instance, Map<EntityType, Set<String>>> bowCache = new ConcurrentHashMap<>();

	private synchronized static Set<String> extractEntityTypeBOW(Instance instance, EntityType entityType) {

		Set<String> bow;

		if (!bowCache.containsKey(instance))
			bowCache.put(instance, new HashMap<>());

		if ((bow = bowCache.get(instance).get(entityType)) == null) {

			bow = extractEntityTypeBOW(entityType);

			if (!entityType.isLiteral)
				for (EntityTypeAnnotation eta : instance.getEntityTypeCandidates(EExplorationMode.ANNOTATION_BASED,
						entityType)) {

					if (eta.isInstanceOfDocumentLinkedAnnotation()) {
						bow.addAll(extractDocLinkedBOW(eta.asInstanceOfDocumentLinkedAnnotation()));
					}

				}
			bowCache.get(instance).put(entityType, bow);

		}

		return bow;

	}

	private static Set<String> extractEntityTypeBOW(EntityType entityType) {
		Set<String> bow = new HashSet<>();
		if (!entityType.isLiteral) {
			for (String part : camelCaseSplit(entityType.name)) {
				bow.add(part);
			}
		}

		return bow;

	}

	private static Map<String, String[]> camelCaseSplitCache = new ConcurrentHashMap<>();

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
		final Set<String> bow = new HashSet<>();

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
