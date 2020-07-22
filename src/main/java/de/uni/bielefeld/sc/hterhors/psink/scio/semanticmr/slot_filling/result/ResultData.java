package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result;

import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class ResultData {
	public EntityTemplate group1;
	public EntityTemplate group2;
	public DocumentLinkedAnnotation trend;
	public DocumentLinkedAnnotation difference;
	public DocumentLinkedAnnotation significance;
	public DocumentLinkedAnnotation pValue;
	public DocumentLinkedAnnotation invMethod;

	public ResultData() {
	}

	public ResultData(ResultData resultData) {

		this.trend = resultData.trend;
		this.difference = resultData.difference;
		this.significance = resultData.significance;
		this.pValue = resultData.pValue;
		this.invMethod = resultData.invMethod;
		this.group1 = resultData.group1;
		this.group2 = resultData.group2;

	}

	public EntityTemplate toResult() {
		return toResult(false);
	}

	public EntityTemplate toResult(boolean coverage) {

		if (!coverage)
			if (SCIOSlotTypes.hasTargetGroup.isIncluded() && group1 == null && group2 == null)
				return null;

		EntityTemplate result = new EntityTemplate(SCIOEntityTypes.result);

		if (invMethod != null || !coverage)
			result.setSingleSlotFiller(SCIOSlotTypes.hasInvestigationMethod, new EntityTemplate(invMethod));

		EntityTemplate tr = null;

		if (trend == null && difference == null && significance == null && pValue == null) {
			if (!coverage)
				throw new IllegalStateException("Trend is null");
		} else {

			if (trend != null)
				tr = new EntityTemplate(trend);

			if (difference != null) {

				if (tr == null)
					tr = new EntityTemplate(SCIOEntityTypes.trend);

				tr.setSingleSlotFiller(SCIOSlotTypes.hasDifference, difference);
			}

			EntityTemplate sig = null;
			if (significance != null) {

				if (tr == null)
					tr = new EntityTemplate(SCIOEntityTypes.trend);

				sig = new EntityTemplate(significance);
			}

			if (pValue != null) {

				if (tr == null)
					tr = new EntityTemplate(SCIOEntityTypes.trend);
				if (sig == null)
					sig = new EntityTemplate(SCIOEntityTypes.significance);

				sig.setSingleSlotFiller(SCIOSlotTypes.hasPValue, pValue);
			}

			if (sig != null)
				tr.setSingleSlotFiller(SCIOSlotTypes.hasSignificance, sig);
		}

		if (tr != null)
			result.setSingleSlotFiller(SCIOSlotTypes.hasTrend, tr);

		if (group1 != null)
			result.setSingleSlotFiller(SCIOSlotTypes.hasTargetGroup, group1);

		if (group2 != null)
			result.setSingleSlotFiller(SCIOSlotTypes.hasReferenceGroup, group2);

		return result;
	}

	@Override
	public String toString() {
		return "ResultData [trend=" + trend + ", difference=" + difference + ", significance=" + significance
				+ ", pValue=" + pValue + ", invMethod=" + invMethod + "]";
	}
}
