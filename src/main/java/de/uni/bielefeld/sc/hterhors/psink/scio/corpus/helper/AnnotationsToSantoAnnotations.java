package de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.ResolvePairwiseComparedGroups.SantoAnnotations;

public class AnnotationsToSantoAnnotations {

	static public void collectRDF(AbstractAnnotation annotation, SantoAnnotations collectData, String dataNamespace,
			String classNamespace) {
		toRDFrec(collectData, annotation, annotation, null, dataNamespace, classNamespace);
	}

	static private SantoAnnotations toRDFrec(SantoAnnotations collectData, AbstractAnnotation parent,
			AbstractAnnotation child, SlotType origin, String dataNamespace, String classNamespace) {
		String rdf;
		if (child.isInstanceOfEntityTemplate()) {

			String rdfType = new StringBuilder("<").append(dataNamespace).append(child.getEntityType().name).append("_")
					.append(getID(child.asInstanceOfEntityTemplate())).append("> <")
					.append("http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <").append(classNamespace)
					.append(child.getEntityType().name).append("> .").toString();

			if (child.asInstanceOfEntityTemplate().getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()) {
				DocumentLinkedAnnotation docLinkedAnn = child.asInstanceOfEntityTemplate().getRootAnnotation()
						.asInstanceOfDocumentLinkedAnnotation();
				collectData.addInstanceToAnnotation(toAnnotation(docLinkedAnn), rdfType);
			}
			collectData.getRdf().add(rdfType);

			if (origin != null) {
				rdf = new StringBuilder("<").append(dataNamespace).append(parent.getEntityType().name).append("_")
						.append(getID(parent)).append("> <").append(classNamespace).append(origin.name).append("> <")
						.append(dataNamespace).append(child.getEntityType().name).append("_")
						.append(getID(child.asInstanceOfEntityTemplate())).append("> .").toString();
				if (child.asInstanceOfEntityTemplate().getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()) {
					DocumentLinkedAnnotation docLinkedAnn = child.asInstanceOfEntityTemplate().getRootAnnotation()
							.asInstanceOfDocumentLinkedAnnotation();
					collectData.addInstanceToAnnotation(toAnnotation(docLinkedAnn), rdf);
				}
				collectData.getRdf().add(rdf);
			}
		} else {
			if (child.getEntityType().isLiteral) {
				rdf = new StringBuilder("<").append(dataNamespace).append(parent.getEntityType().name).append("_")
						.append(getID(parent)).append("> <").append(classNamespace).append(origin.name).append("> \"")
						.append(child.asInstanceOfLiteralAnnotation().getSurfaceForm()).append("\" .").toString();
				if (child.isInstanceOfDocumentLinkedAnnotation()) {
					DocumentLinkedAnnotation docLinkedAnn = child.asInstanceOfDocumentLinkedAnnotation();
					collectData.addInstanceToAnnotation(toAnnotation(docLinkedAnn), rdf);
				}
				collectData.getRdf().add(rdf);

			} else {
				rdf = new StringBuilder("<").append(dataNamespace).append(parent.getEntityType().name).append("_")
						.append(getID(parent)).append("> <").append(classNamespace).append(origin.name).append("> <")
						.append(classNamespace).append(child.getEntityType().name).append("> .").toString();
				if (child.isInstanceOfDocumentLinkedAnnotation()) {
					DocumentLinkedAnnotation docLinkedAnn = child.asInstanceOfDocumentLinkedAnnotation();
					collectData.addInstanceToAnnotation(toAnnotation(docLinkedAnn), rdf);
				}
				collectData.getRdf().add(rdf);

			}

		}

		if (child.isInstanceOfEntityTemplate()) {

			final Map<SlotType, Set<AbstractAnnotation>> slots = child.asInstanceOfEntityTemplate().filter()
					.docLinkedAnnoation().entityTemplateAnnoation().entityTypeAnnoation().literalAnnoation().nonEmpty()
					.multiSlots().singleSlots().merge().build().getMergedAnnotations();

			for (Entry<SlotType, Set<AbstractAnnotation>> slotFillerSlot : slots.entrySet()) {
				for (AbstractAnnotation childAnnotation : slotFillerSlot.getValue()) {
					toRDFrec(collectData, child, childAnnotation, slotFillerSlot.getKey(), dataNamespace,
							classNamespace);
				}
			}
		}
		return collectData;
	}

	static public String toAnnotation(DocumentLinkedAnnotation docLinkedAnn) {
		String annotation = new StringBuilder(docLinkedAnn.getEntityType().name).append(", ")
				.append(docLinkedAnn.getStartDocCharOffset()).append(", ").append(docLinkedAnn.getEndDocCharOffset())
				.append(", \"").append(docLinkedAnn.getSurfaceForm()).append("\", \"\", ").toString();
		return annotation;
	}

	private static Map<AbstractAnnotation, Integer> rdfIDMap = new HashMap<AbstractAnnotation, Integer>();

	public static int getID(AbstractAnnotation from) {
		Integer id;
		if ((id = rdfIDMap.get(from)) == null) {
			id = new Integer(rdfIDMap.size());
			rdfIDMap.put(from, id);
		}
		return id.intValue();
	}

}
