package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrNameOverlapTemplate.OverlapScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class ExGrNameOverlapTemplate extends AbstractFeatureTemplate<OverlapScope> {

	static class OverlapScope extends AbstractFactorScope {

		public final int countOverlap;

		public OverlapScope(AbstractFeatureTemplate<?> template, int isUnique) {
			super(template);
			this.countOverlap = isUnique;
		}

		@Override
		public int implementHashCode() {
			return 0;
		}

		@Override
		public boolean implementEquals(Object obj) {
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + countOverlap;
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
			OverlapScope other = (OverlapScope) obj;
			if (countOverlap != other.countOverlap)
				return false;
			return true;
		}

	}

	@Override
	public List<OverlapScope> generateFactorScopes(State state) {
		List<OverlapScope> factors = new ArrayList<>();

		/**
		 * Unique over all templates
		 */
		Set<String> contains = new HashSet<>();
		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != EntityType.get("DefinedExperimentalGroup"))
				continue;

			int count = 0;
			for (AbstractAnnotation groupNameAnnotation : experimentalGroup.getMultiFillerSlot("hasGroupName")
					.getSlotFiller()) {
				if (!contains.add(groupNameAnnotation.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm())) {
					count++;
				}
			}

			factors.add(new OverlapScope(this, count));

		}

		return factors;

	}

	@Override
	public void generateFeatureVector(Factor<OverlapScope> factor) {

		factor.getFeatureVector().set("Overlap: " + (factor.getFactorScope().countOverlap), true);
	}

}
