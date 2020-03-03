package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based;

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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based.TB_TreatmentPriorInverseTemplate.TreatmentPriorInverseScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.wrapper.DefinedExperimentalGroup;

/**
 * 
 * Combines treatments of different exp groups pairwise.
 * 
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class TB_TreatmentPriorInverseTemplate extends AbstractFeatureTemplate<TreatmentPriorInverseScope> {

	static class TreatmentPriorInverseScope extends AbstractFactorScope {

		final List<Set<EntityType>> entityTypes;
		final int numberOfGroups;

		public TreatmentPriorInverseScope(AbstractFeatureTemplate<?> template, List<Set<EntityType>> entityTypes,
				int numberOfGroups) {
			super(template);
			this.entityTypes = entityTypes;
			this.numberOfGroups = numberOfGroups;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((entityTypes == null) ? 0 : entityTypes.hashCode());
			result = prime * result + numberOfGroups;
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
			TreatmentPriorInverseScope other = (TreatmentPriorInverseScope) obj;
			if (entityTypes == null) {
				if (other.entityTypes != null)
					return false;
			} else if (!entityTypes.equals(other.entityTypes))
				return false;
			if (numberOfGroups != other.numberOfGroups)
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
	public List<TreatmentPriorInverseScope> generateFactorScopes(State state) {

		if (SCIOSlotTypes.hasTreatmentType.isExcluded())
			return Collections.emptyList();

		List<TreatmentPriorInverseScope> factors = new ArrayList<>();
		List<Set<EntityType>> listOfTypes = new ArrayList<>();

		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
				continue;

			DefinedExperimentalGroup definedExpGroup = new DefinedExperimentalGroup(experimentalGroup);

			Set<EntityType> types = definedExpGroup.getRelevantTreatments().stream().map(a -> a.getEntityType())
					.collect(Collectors.toSet());

			listOfTypes.add(types);

		}

		for (int i = 0; i < listOfTypes.size(); i++) {
			for (int j = i + 1; j < listOfTypes.size(); j++) {
				factors.add(new TreatmentPriorInverseScope(this, Arrays.asList(listOfTypes.get(i), listOfTypes.get(j)),
						listOfTypes.size()));
			}
		}

		return factors;

	}

	@Override
	public void generateFeatureVector(Factor<TreatmentPriorInverseScope> factor) {

		List<List<EntityType>> types = new ArrayList<>();
		for (Set<EntityType> entityTypes : factor.getFactorScope().entityTypes) {
			types.add(new ArrayList<>(entityTypes));
		}

		for (int i = 0; i < types.get(0).size(); i++) {
			final String name1 = types.get(0).get(i).name;
			for (int j = i + 1; j < types.get(1).size(); j++) {
				final String name2 = types.get(1).get(j).name;

				if (name1.compareTo(name2) < 0) {
					factor.getFeatureVector().set(
							PREFIX + name1 + "--" + name2 + " > 2 " + (factor.getFactorScope().numberOfGroups > 2),
							true);
					factor.getFeatureVector()
							.set(PREFIX + name1 + "--" + name2 + " : " + factor.getFactorScope().numberOfGroups, true);
					factor.getFeatureVector().set(PREFIX + name1 + "--" + name2, true);
				} else {
					factor.getFeatureVector().set(
							PREFIX + name2 + "--" + name1 + " > 2 " + (factor.getFactorScope().numberOfGroups > 2),
							true);
					factor.getFeatureVector()
							.set(PREFIX + name2 + "--" + name1 + " : " + factor.getFactorScope().numberOfGroups, true);
					factor.getFeatureVector().set(PREFIX + name2 + "--" + name1, true);

				}

			}
		}
	}

}
