package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result;

import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class TrendData {
	public DocumentLinkedAnnotation trend;
	public DocumentLinkedAnnotation difference;
	public DocumentLinkedAnnotation significance;
	public DocumentLinkedAnnotation pValue;

	public TrendData() {
	}

	public TrendData(TrendData resultData) {

		this.trend = resultData.trend;
		this.difference = resultData.difference;
		this.significance = resultData.significance;
		this.pValue = resultData.pValue;

	}

	public EntityTemplate toTrend() {
		return toResult(false);
	}

	public EntityTemplate toResult(boolean coverage) {

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

		return tr;
	}

	@Override
	public String toString() {
		return "TrendData [trend=" + trend + ", difference=" + difference + ", significance=" + significance
				+ ", pValue=" + pValue + "]";
	}

}
