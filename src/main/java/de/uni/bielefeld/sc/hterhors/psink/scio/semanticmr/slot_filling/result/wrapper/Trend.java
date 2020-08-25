package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class Trend {

	final private EntityTemplate trend;

	public EntityTemplate get() {
		return trend;
	}

	public Trend(EntityTemplate trend) {
		this.trend = trend;
	}

	public DocumentLinkedAnnotation getRootAnntoationAsDocumentLinkedAnnotation() {
		return getRootAnntoationAsDocumentLinkedAnnotation(trend);
	}

	public DocumentLinkedAnnotation getRootAnntoationAsDocumentLinkedAnnotation(EntityTemplate et) {
		return et.getRootAnnotation() != null && et.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()
				? et.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation()
				: null;
	}

	public DocumentLinkedAnnotation getDifferenceAsDocumentLinkedAnnotation() {
		SingleFillerSlot sfs = trend.getSingleFillerSlot(SCIOSlotTypes.hasDifference);
		if (sfs.containsSlotFiller())
			return sfs.getSlotFiller().isInstanceOfDocumentLinkedAnnotation()
					? sfs.getSlotFiller().asInstanceOfDocumentLinkedAnnotation()
					: null;
		return null;
	}

	public static DocumentLinkedAnnotation getDocumentLinkedAnnotation(EntityTemplate et, SlotType slotType) {
		SingleFillerSlot sfs = et.getSingleFillerSlot(slotType);
		if (sfs.containsSlotFiller())
			return sfs.getSlotFiller().isInstanceOfDocumentLinkedAnnotation()
					? sfs.getSlotFiller().asInstanceOfDocumentLinkedAnnotation()
					: null;
		return null;
	}

	public List<Integer> getRelevantSentenceIndexes() {

		List<Integer> relIds = new ArrayList<>();

		DocumentLinkedAnnotation difference = getDifferenceAsDocumentLinkedAnnotation();
		if (difference != null)
			relIds.add(difference.getSentenceIndex());

		DocumentLinkedAnnotation root = getRootAnntoationAsDocumentLinkedAnnotation();
		if (root != null)
			relIds.add(root.getSentenceIndex());

		EntityTemplate significance = getSignificance();

		if (significance != null) {

			DocumentLinkedAnnotation rootSig = getRootAnntoationAsDocumentLinkedAnnotation(significance);
			if (rootSig != null)
				relIds.add(rootSig.getSentenceIndex());

			DocumentLinkedAnnotation pvalue = getDocumentLinkedAnnotation(significance, SCIOSlotTypes.hasPValue);
			if (pvalue != null)
				relIds.add(pvalue.getSentenceIndex());

			DocumentLinkedAnnotation alphaSignificanceNiveau = getDocumentLinkedAnnotation(significance,
					SCIOSlotTypes.hasAlphaSignificanceNiveau);
			if (alphaSignificanceNiveau != null)
				relIds.add(alphaSignificanceNiveau.getSentenceIndex());

		}

		return relIds;

	}

	public List<DocumentLinkedAnnotation> getRelevantDocumentLinkedAnnotations() {

		List<DocumentLinkedAnnotation> relDocs = new ArrayList<>();

		DocumentLinkedAnnotation difference = getDifferenceAsDocumentLinkedAnnotation();
		if (difference != null)
			relDocs.add(difference);

		DocumentLinkedAnnotation root = getRootAnntoationAsDocumentLinkedAnnotation();
		if (root != null)
			relDocs.add(root);

		EntityTemplate significance = getSignificance();

		if (significance != null) {

			DocumentLinkedAnnotation rootSig = getRootAnntoationAsDocumentLinkedAnnotation(significance);
			if (rootSig != null)
				relDocs.add(rootSig);

			DocumentLinkedAnnotation pvalue = getDocumentLinkedAnnotation(significance, SCIOSlotTypes.hasPValue);
			if (pvalue != null)
				relDocs.add(pvalue);

			DocumentLinkedAnnotation alphaSignificanceNiveau = getDocumentLinkedAnnotation(significance,
					SCIOSlotTypes.hasAlphaSignificanceNiveau);
			if (alphaSignificanceNiveau != null)
				relDocs.add(alphaSignificanceNiveau);

		}

		return relDocs;

	}

	public DocumentLinkedAnnotation getSignificanceRootAsDocumentLinkedAnnotation() {
		EntityTemplate significance = getSignificance();

		if (significance != null) {

			DocumentLinkedAnnotation rootSig = getRootAnntoationAsDocumentLinkedAnnotation(significance);

			return rootSig;
		}
		return null;
	}

	public DocumentLinkedAnnotation getPValueAsDocumentLinkedAnnotation() {
		EntityTemplate significance = getSignificance();

		if (significance != null) {

			DocumentLinkedAnnotation pvalue = getDocumentLinkedAnnotation(significance, SCIOSlotTypes.hasPValue);

			return pvalue;
		}
		return null;
	}

	public DocumentLinkedAnnotation getAlphaSigNivAsDocumentLinkedAnnotation() {
		EntityTemplate significance = getSignificance();

		if (significance != null) {

			DocumentLinkedAnnotation pvalue = getDocumentLinkedAnnotation(significance,
					SCIOSlotTypes.hasAlphaSignificanceNiveau);

			return pvalue;
		}
		return null;
	}

	public EntityTemplate getSignificance() {
		SingleFillerSlot sfs = trend.getSingleFillerSlot(SCIOSlotTypes.hasSignificance);
		if (sfs.containsSlotFiller())
			return sfs.getSlotFiller().asInstanceOfEntityTemplate();
		return null;
	}

}
