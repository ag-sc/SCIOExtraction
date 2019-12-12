package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea.templates;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea.templates.SlotIsFilledTemplate.SlotIsFilledScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class SlotIsFilledTemplate extends AbstractFeatureTemplate<SlotIsFilledScope> {

	static class SlotIsFilledScope extends AbstractFactorScope {

		boolean hasLowerFilled;
		boolean hasUpperFilled;

		public SlotIsFilledScope(AbstractFeatureTemplate<?> template, boolean hasLowerFilled, boolean hasUpperFilled) {
			super(template);
			this.hasUpperFilled = hasUpperFilled;
			this.hasLowerFilled = hasLowerFilled;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + (hasLowerFilled ? 1231 : 1237);
			result = prime * result + (hasUpperFilled ? 1231 : 1237);
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
			SlotIsFilledScope other = (SlotIsFilledScope) obj;
			if (hasLowerFilled != other.hasLowerFilled)
				return false;
			if (hasUpperFilled != other.hasUpperFilled)
				return false;
			return true;
		}

		@Override
		public int implementHashCode() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean implementEquals(Object obj) {
			// TODO Auto-generated method stub
			return false;
		}

	}

	private static final String PREFIX = "SIFT\t";

	@Override
	public List<SlotIsFilledScope> generateFactorScopes(State state) {
		List<SlotIsFilledScope> factors = new ArrayList<>();

		for (EntityTemplate vertebralArea : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (vertebralArea.getEntityType() != EntityType.get("VertebralArea"))
				continue;

			SingleFillerSlot lower = vertebralArea.getSingleFillerSlotOfName("hasLowerVertebrae");
			SingleFillerSlot upper = vertebralArea.getSingleFillerSlotOfName("hasUpperVertebrae");

			factors.add(new SlotIsFilledScope(this, upper.containsSlotFiller(), lower.containsSlotFiller()));

		}

		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<SlotIsFilledScope> factor) {

		factor.getFeatureVector().set(PREFIX + "none",
				(!factor.getFactorScope().hasLowerFilled && !factor.getFactorScope().hasUpperFilled));
		factor.getFeatureVector().set(PREFIX + "and",
				factor.getFactorScope().hasLowerFilled && factor.getFactorScope().hasUpperFilled);
		factor.getFeatureVector().set(PREFIX + "or",
				factor.getFactorScope().hasLowerFilled || factor.getFactorScope().hasUpperFilled);
		factor.getFeatureVector().set(PREFIX + "lower", factor.getFactorScope().hasLowerFilled);
		factor.getFeatureVector().set(PREFIX + "upper", factor.getFactorScope().hasUpperFilled);

		factor.getFeatureVector().set(PREFIX + "!none",
				!(!factor.getFactorScope().hasLowerFilled && !factor.getFactorScope().hasUpperFilled));
		factor.getFeatureVector().set(PREFIX + "!and",
				!(factor.getFactorScope().hasLowerFilled && factor.getFactorScope().hasUpperFilled));
		factor.getFeatureVector().set(PREFIX + "!or",
				!(factor.getFactorScope().hasLowerFilled || factor.getFactorScope().hasUpperFilled));
		factor.getFeatureVector().set(PREFIX + "!lower", !factor.getFactorScope().hasLowerFilled);
		factor.getFeatureVector().set(PREFIX + "!upper", !factor.getFactorScope().hasUpperFilled);

	}

}
