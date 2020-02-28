package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.helper.bow.TB_BOWExtractor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based.TB_GroupBOWTreatmentCardinalityTemplate.BOWCardScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class TB_GroupBOWTreatmentCardinalityTemplate extends AbstractFeatureTemplate<BOWCardScope> {

	static class BOWCardScope extends AbstractFactorScope {

		final Set<String> expGroupBOW;
		final int numOfTreatments;

		public BOWCardScope(AbstractFeatureTemplate<?> template, Set<String> experimentalgroupBOW,
				int numOfTreatments) {
			super(template);
			this.expGroupBOW = experimentalgroupBOW;
			this.numOfTreatments = numOfTreatments;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((expGroupBOW == null) ? 0 : expGroupBOW.hashCode());
			result = prime * result + numOfTreatments;
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
			BOWCardScope other = (BOWCardScope) obj;
			if (expGroupBOW == null) {
				if (other.expGroupBOW != null)
					return false;
			} else if (!expGroupBOW.equals(other.expGroupBOW))
				return false;
			if (numOfTreatments != other.numOfTreatments)
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

	private static final String PREFIX = "BOWCardTreat\t";

	@Override
	public List<BOWCardScope> generateFactorScopes(State state) {

		List<BOWCardScope> factors = new ArrayList<>();

		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
				continue;

			if (SCIOSlotTypes.hasTreatmentType.isExcluded())
				continue;

			final Set<String> expGroupBOW = TB_BOWExtractor.getExpGroupPlusNameBOW(experimentalGroup);

//			if (expGroupBOW.contains("non") || expGroupBOW.contains("sham")
//					|| ((expGroupBOW.contains("laminectomy") || expGroupBOW.contains("lesion")
//							|| expGroupBOW.contains("injury"))
//							&& (expGroupBOW.contains("only") || expGroupBOW.contains("alone")))) {
			factors.add(new BOWCardScope(this, expGroupBOW,
					experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller().size()));
//			}

		}

		return factors;

	}

	@Override
	public void generateFeatureVector(Factor<BOWCardScope> factor) {

		for (String expBOWTerm : factor.getFactorScope().expGroupBOW) {
			factor.getFeatureVector().set(PREFIX + expBOWTerm + ", num = " + factor.getFactorScope().numOfTreatments,
					true);
		}

	}

}
