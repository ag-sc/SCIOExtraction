package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.templates;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.DoubleVector;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.templates.VertebralAreaRootMatchTemplate.VertebralAreaRMScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class VertebralAreaRootMatchTemplate extends AbstractFeatureTemplate<VertebralAreaRMScope> {

	static class VertebralAreaRMScope extends AbstractFactorScope {

		public final boolean equalsNumber;
		public final boolean containsLowerNum;
		public final boolean containsUpperNum;

		public final boolean lowerIndexLessUpperIndex;
		public final boolean containsLowerID;
		public final boolean containsUpperID;

		public VertebralAreaRMScope(AbstractFeatureTemplate<?> template, boolean equalsNumber, boolean containsLowerNum,
				boolean containsUpperNum, boolean lowerIndexLessUpperIndex, boolean containsLowerID,
				boolean containsUpperID) {
			super(template);
			this.equalsNumber = equalsNumber;
			this.containsLowerNum = containsLowerNum;
			this.containsUpperNum = containsUpperNum;
			this.lowerIndexLessUpperIndex = lowerIndexLessUpperIndex;
			this.containsLowerID = containsLowerID;
			this.containsUpperID = containsUpperID;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + (containsLowerID ? 1231 : 1237);
			result = prime * result + (containsLowerNum ? 1231 : 1237);
			result = prime * result + (containsUpperID ? 1231 : 1237);
			result = prime * result + (containsUpperNum ? 1231 : 1237);
			result = prime * result + (lowerIndexLessUpperIndex ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			VertebralAreaRMScope other = (VertebralAreaRMScope) obj;
			if (containsLowerID != other.containsLowerID)
				return false;
			if (containsLowerNum != other.containsLowerNum)
				return false;
			if (containsUpperID != other.containsUpperID)
				return false;
			if (containsUpperNum != other.containsUpperNum)
				return false;
			if (lowerIndexLessUpperIndex != other.lowerIndexLessUpperIndex)
				return false;
			return true;
		}

		@Override
		public int implementHashCode() {
			return 0;
		}

		@Override
		public boolean implementEquals(Object obj) {
			return false;
		}

	}

	private static final String PREFIX = "VLRM\t";

	@Override
	public List<VertebralAreaRMScope> generateFactorScopes(State state) {
		List<VertebralAreaRMScope> factors = new ArrayList<>();
		for (EntityTemplate vertebralArea : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (vertebralArea.getEntityType() != EntityType.get("VertebralArea"))
				continue;

			if (!vertebralArea.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
				continue;

			if (SCIOSlotTypes.hasLowerVertebrae.isExcluded() || SCIOSlotTypes.hasUpperVertebrae.isExcluded())
				continue;

			SingleFillerSlot lower = vertebralArea.getSingleFillerSlot(SCIOSlotTypes.hasLowerVertebrae);
			SingleFillerSlot upper = vertebralArea.getSingleFillerSlot(SCIOSlotTypes.hasUpperVertebrae);

			if (!(lower.containsSlotFiller() && upper.containsSlotFiller()))
				continue;

			String lowerVertebraeNum = lower.getSlotFiller().getEntityType().name.replaceAll("T|C|L", "");
			String upperVertebraeNum = upper.getSlotFiller().getEntityType().name.replaceAll("T|C|L", "");

			String lowerVertebraeID = lower.getSlotFiller().getEntityType().name.replaceAll("\\d+", "");
			String upperVertebraeID = upper.getSlotFiller().getEntityType().name.replaceAll("\\d+", "");

			String surfaceForm = vertebralArea.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation()
					.getSurfaceForm();

			int lowerIndex = surfaceForm.indexOf(lowerVertebraeNum);
			int upperIndex = surfaceForm.indexOf(upperVertebraeNum);

			boolean containsLowerNum = surfaceForm.matches(".*" + lowerVertebraeNum + "(?!\\d).*");
			boolean containsUpperNum = surfaceForm.matches(".*" + upperVertebraeNum + "(?!\\d).*");

			boolean containsLowerID = surfaceForm.contains(lowerVertebraeID);
			boolean containsUpperID = surfaceForm.contains(upperVertebraeID);

			factors.add(new VertebralAreaRMScope(this,
					lower.getSlotFiller().getEntityType() == upper.getSlotFiller().getEntityType(), containsLowerNum,
					containsUpperNum, lowerIndex > upperIndex, containsLowerID, containsUpperID));
		}
		return factors;

	}

	@Override
	public void generateFeatureVector(Factor<VertebralAreaRMScope> factor) {

		DoubleVector featureVector = factor.getFeatureVector();

		featureVector.set(PREFIX + "equalsNumber", factor.getFactorScope().equalsNumber);
		featureVector.set(PREFIX + "containsLowerNum", factor.getFactorScope().containsLowerNum);
		featureVector.set(PREFIX + "containsUpperNum", factor.getFactorScope().containsUpperNum);
		featureVector.set(PREFIX + "containsUpperID", factor.getFactorScope().containsUpperID);
		featureVector.set(PREFIX + "containsLowerID", factor.getFactorScope().containsLowerID);
		featureVector.set(PREFIX + "!lowerIndexLessUpperIndex", !factor.getFactorScope().lowerIndexLessUpperIndex);
		featureVector.set(PREFIX + "lowerIndexLessUpperIndex", factor.getFactorScope().lowerIndexLessUpperIndex);

	}

}
