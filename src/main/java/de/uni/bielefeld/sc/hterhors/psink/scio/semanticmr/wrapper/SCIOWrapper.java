package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.wrapper;

import java.util.List;

import org.apache.jena.sparql.function.library.collation;

import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;

public class SCIOWrapper {

	public final EntityTemplate et;

	public SCIOWrapper(EntityTemplate organismModel) {
		this.et = organismModel;
	}

	protected DocumentLinkedAnnotation getDocumentLinkedAnnotation(SlotType slotType) {
		SingleFillerSlot sfs = et.getSingleFillerSlot(slotType);
		if (sfs.containsSlotFiller())
			return sfs.getSlotFiller().isInstanceOfDocumentLinkedAnnotation()
					? sfs.getSlotFiller().asInstanceOfDocumentLinkedAnnotation()
					: null;
		return null;
	}

	protected void collectDLA(List<DocumentLinkedAnnotation> annotations) {
		collectDLA(annotations, et);
	}

	protected void collectDLA(List<DocumentLinkedAnnotation> annotations, EntityTemplate et) {

		if (et.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()) {
			annotations.add(et.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation());
		}

		et.streamSingleFillerSlotValues().forEach(a -> {

			if (a.isInstanceOfEntityTemplate()) {
				collectDLA(annotations, a.asInstanceOfEntityTemplate());
			} else if (a.isInstanceOfDocumentLinkedAnnotation()) {
				annotations.add(a.asInstanceOfDocumentLinkedAnnotation());
			}
		});
		et.flatStreamMultiFillerSlotValues().forEach(a -> {

			if (a.isInstanceOfEntityTemplate()) {
				collectDLA(annotations, a.asInstanceOfEntityTemplate());
			} else if (a.isInstanceOfDocumentLinkedAnnotation()) {
				annotations.add(a.asInstanceOfDocumentLinkedAnnotation());
			}
		});
	}
}
