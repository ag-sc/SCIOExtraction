package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based.TB_TreatmentInGroupCardinalityTemplate.TreatmentGroupCardinalityScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.wrapper.DefinedExperimentalGroup;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class TB_TreatmentInGroupCardinalityTemplate extends AbstractFeatureTemplate<TreatmentGroupCardinalityScope> {

	static class TreatmentGroupCardinalityScope extends AbstractFactorScope {

		final int numOfTreatments;
		final int numOfGroups;

		public TreatmentGroupCardinalityScope(AbstractFeatureTemplate<?> template, int numOfTreatments,
				int numOfGroups) {
			super(template);
			this.numOfTreatments = numOfTreatments;
			this.numOfGroups = numOfGroups;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + numOfGroups;
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
			TreatmentGroupCardinalityScope other = (TreatmentGroupCardinalityScope) obj;
			if (numOfGroups != other.numOfGroups)
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

	private static final String PREFIX = "TrtPr\t";

	@Override
	public List<TreatmentGroupCardinalityScope> generateFactorScopes(State state) {

		if (SCIOSlotTypes.hasTreatmentType.isExcluded())
			return Collections.emptyList();

		List<TreatmentGroupCardinalityScope> factors = new ArrayList<>();
		Set<EntityType> allTypes = new HashSet<>();

		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
				continue;

			DefinedExperimentalGroup definedExpGroup = new DefinedExperimentalGroup(experimentalGroup);

			allTypes.addAll(definedExpGroup.getRelevantTreatments().stream().map(a -> a.getEntityType())
					.collect(Collectors.toSet()));

		}
		factors.add(new TreatmentGroupCardinalityScope(this, allTypes.size(),
				super.<EntityTemplate>getPredictedAnnotations(state).size()));

		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<TreatmentGroupCardinalityScope> factor) {
		factor.getFeatureVector().set(PREFIX + "#Groups: " + factor.getFactorScope().numOfGroups + ", #Treatments: "
				+ factor.getFactorScope().numOfTreatments, true);
	}

}
