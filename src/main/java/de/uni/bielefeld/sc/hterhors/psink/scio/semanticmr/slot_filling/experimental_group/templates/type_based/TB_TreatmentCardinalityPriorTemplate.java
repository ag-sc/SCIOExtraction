package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.type_based;

import java.util.ArrayList;
import java.util.Arrays;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.type_based.TB_TreatmentCardinalityPriorTemplate.TreatmentCardinalityPriorScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.wrapper.DefinedExperimentalGroup;
import edu.stanford.nlp.pipeline.CoreNLPProtos.Entity;

/**
 * 
 * Combines treatments of different exp groups pairwise.
 * 
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class TB_TreatmentCardinalityPriorTemplate extends AbstractFeatureTemplate<TreatmentCardinalityPriorScope> {

	static class TreatmentCardinalityPriorScope extends AbstractFactorScope {

		final EntityType entityType;
		final int appearanceInNumberOfGroups;
		final int maxNumberOfGroups;

		public TreatmentCardinalityPriorScope(AbstractFeatureTemplate<?> template, EntityType entityType,
				int appearanceInNumberOfGroups, int maxNumberOfGroups) {
			super(template);
			this.entityType = entityType;
			this.appearanceInNumberOfGroups = appearanceInNumberOfGroups;
			this.maxNumberOfGroups = maxNumberOfGroups;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + appearanceInNumberOfGroups;
			result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
			result = prime * result + maxNumberOfGroups;
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
			TreatmentCardinalityPriorScope other = (TreatmentCardinalityPriorScope) obj;
			if (appearanceInNumberOfGroups != other.appearanceInNumberOfGroups)
				return false;
			if (entityType == null) {
				if (other.entityType != null)
					return false;
			} else if (!entityType.equals(other.entityType))
				return false;
			if (maxNumberOfGroups != other.maxNumberOfGroups)
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

	private static final String PREFIX = "TrtPr_INVERSE\t";

	@Override
	public List<TreatmentCardinalityPriorScope> generateFactorScopes(State state) {

		if (SCIOSlotTypes.hasTreatmentType.isExcluded())
			return Collections.emptyList();

		List<TreatmentCardinalityPriorScope> factors = new ArrayList<>();
		List<Set<EntityType>> listOfTypes = new ArrayList<>();
		Set<EntityType> allTypes = new HashSet<>();
		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
				continue;

			DefinedExperimentalGroup definedExpGroup = new DefinedExperimentalGroup(experimentalGroup);

			Set<EntityType> types = definedExpGroup.getRelevantTreatments().stream().map(a -> a.getEntityType())
					.collect(Collectors.toSet());

			allTypes.addAll(types);

			listOfTypes.add(types);

		}

		for (EntityType entityType : allTypes) {
			int appearanceinNumberOfGroups = 0;

			for (Set<EntityType> entityTypes : listOfTypes) {
				if (entityTypes.contains(entityType))
					appearanceinNumberOfGroups++;
			}
			factors.add(
					new TreatmentCardinalityPriorScope(this, entityType, appearanceinNumberOfGroups, allTypes.size()));
		}

		return factors;

	}

	@Override
	public void generateFeatureVector(Factor<TreatmentCardinalityPriorScope> factor) {

		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().entityType.name + " appears in all = "
				+ (factor.getFactorScope().appearanceInNumberOfGroups == factor.getFactorScope().maxNumberOfGroups),
				true);
		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().entityType.name + " appears in all = "
				+ (factor.getFactorScope().appearanceInNumberOfGroups == factor.getFactorScope().maxNumberOfGroups),
				true);
		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().entityType.name + " appears in just 1 = "
				+ (factor.getFactorScope().appearanceInNumberOfGroups == 1), true);
		factor.getFeatureVector()
				.set(PREFIX + factor.getFactorScope().entityType.name + " appears in "
						+ factor.getFactorScope().appearanceInNumberOfGroups + " of "
						+ factor.getFactorScope().maxNumberOfGroups, true);

	}

}
