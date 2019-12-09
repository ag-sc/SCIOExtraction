package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.TreatmentCardinalityTemplate.TreatmentCardinalityScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class TreatmentCardinalityTemplate extends AbstractFeatureTemplate<TreatmentCardinalityScope> {

	static class TreatmentCardinalityScope extends AbstractFactorScope {

		final EntityType entityType;
		final int numOfTreatments;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
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
			TreatmentCardinalityScope other = (TreatmentCardinalityScope) obj;
			if (entityType == null) {
				if (other.entityType != null)
					return false;
			} else if (!entityType.equals(other.entityType))
				return false;
			if (numOfTreatments != other.numOfTreatments)
				return false;
			return true;
		}

		public TreatmentCardinalityScope(AbstractFeatureTemplate<?> template, EntityType entityType,
				int numOfTreatments) {
			super(template);
			this.entityType = entityType;
			this.numOfTreatments = numOfTreatments;
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

	private static final String PREFIX = "TrtPr\t";

	@Override
	public List<TreatmentCardinalityScope> generateFactorScopes(State state) {
		List<TreatmentCardinalityScope> factors = new ArrayList<>();

		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
				continue;

			experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller().stream()
					.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
					.map(a -> a.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound))
					.filter(s -> s.containsSlotFiller()).forEach(a -> {
						factors.add(
								new TreatmentCardinalityScope(this, a.getSlotFiller().getEntityType(), experimentalGroup
										.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller().size()));
					});

			experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller().stream()
					.filter(a -> a.getEntityType() != SCIOEntityTypes.compoundTreatment).forEach(a -> {
						factors.add(new TreatmentCardinalityScope(this, a.getEntityType(), experimentalGroup
								.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller().size()));
					});

		}

		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<TreatmentCardinalityScope> factor) {

		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().entityType.name + ", num > 1",
				factor.getFactorScope().numOfTreatments > 1);
		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().entityType.name + ", num != 1",
				factor.getFactorScope().numOfTreatments != 1);
		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().entityType.name + ", num == "
				+ factor.getFactorScope().numOfTreatments, true);

	}

}
