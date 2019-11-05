package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.TreatmentPriorTemplate.TreatmentPriorScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class TreatmentPriorTemplate extends AbstractFeatureTemplate<TreatmentPriorScope> {

	static class TreatmentPriorScope extends AbstractFactorScope {

		final EntityType entityType;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
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
			TreatmentPriorScope other = (TreatmentPriorScope) obj;
			if (entityType == null) {
				if (other.entityType != null)
					return false;
			} else if (!entityType.equals(other.entityType))
				return false;
			return true;
		}

		public TreatmentPriorScope(AbstractFeatureTemplate<?> template, EntityType entityType) {
			super(template);
			this.entityType = entityType;
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
	public List<TreatmentPriorScope> generateFactorScopes(State state) {
		List<TreatmentPriorScope> factors = new ArrayList<>();

		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != EntityType.get("DefinedExperimentalGroup"))
				continue;

			experimentalGroup.getMultiFillerSlot("hasTreatmentType").getSlotFiller().stream()
					.filter(a -> a.getEntityType() == EntityType.get("CompoundTreatment"))
					.map(a -> a.asInstanceOfEntityTemplate().getSingleFillerSlot("hasCompound"))
					.filter(s -> s.containsSlotFiller()).forEach(a -> {
						factors.add(new TreatmentPriorScope(this, a.getSlotFiller().getEntityType()));
					});

			experimentalGroup.getMultiFillerSlot("hasTreatmentType").getSlotFiller().stream()
					.filter(a -> a.getEntityType() != EntityType.get("CompoundTreatment")).forEach(a -> {
						factors.add(new TreatmentPriorScope(this, a.getEntityType()));
					});

		}

		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<TreatmentPriorScope> factor) {

		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().entityType.entityName, true);

	}

}
