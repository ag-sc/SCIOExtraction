package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.type_based;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.type_based.TB_TreatmentPriorTemplate.TreatmentPriorScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.wrapper.DefinedExperimentalGroup;

/**
 * 
 * Prior of assigned treatments for each exp group.
 * 
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class TB_TreatmentPriorTemplate extends AbstractFeatureTemplate<TreatmentPriorScope> {

	static class TreatmentPriorScope extends AbstractFactorScope {

		final Set<EntityType> entityTypes;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((entityTypes == null) ? 0 : entityTypes.hashCode());
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
			if (entityTypes == null) {
				if (other.entityTypes != null)
					return false;
			} else if (!entityTypes.equals(other.entityTypes))
				return false;
			return true;
		}

		public TreatmentPriorScope(AbstractFeatureTemplate<?> template, Set<EntityType> entityType) {
			super(template);
			this.entityTypes = entityType;
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

		if (SCIOSlotTypes.hasTreatmentType.isExcluded())
			return Collections.emptyList();

		List<TreatmentPriorScope> factors = new ArrayList<>();

		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
				continue;

			DefinedExperimentalGroup definedExpGroup = new DefinedExperimentalGroup(experimentalGroup);

			Set<EntityType> types = definedExpGroup.getRelevantTreatments().stream().map(a -> a.getEntityType())
					.collect(Collectors.toSet());

			factors.add(new TreatmentPriorScope(this, types));
		}

		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<TreatmentPriorScope> factor) {

		List<EntityType> sorted = new ArrayList<>(factor.getFactorScope().entityTypes);

		Collections.sort(sorted);

		for (int i = 0; i < sorted.size(); i++) {
			factor.getFeatureVector().set(PREFIX + sorted.get(i).name + " + " + (sorted.size() - 1), true);
			for (int j = i + 1; j < sorted.size(); j++) {
				factor.getFeatureVector().set(
						PREFIX + sorted.get(i).name + "--" + sorted.get(j).name + " + " + (sorted.size() - 2), true);
			}
		}
	}

}
