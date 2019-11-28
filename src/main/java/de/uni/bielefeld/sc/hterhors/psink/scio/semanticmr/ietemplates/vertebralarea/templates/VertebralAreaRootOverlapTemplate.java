package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea.templates;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.DoubleVector;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea.templates.VertebralAreaRootOverlapTemplate.VertebralLocationScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class VertebralAreaRootOverlapTemplate extends AbstractFeatureTemplate<VertebralLocationScope> {

	static class VertebralLocationScope extends AbstractFactorScope {

		public final int countOverlap;

		public VertebralLocationScope(AbstractFeatureTemplate<?> template, int countOverlap) {
			super(template);
			this.countOverlap = countOverlap;
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
			VertebralLocationScope other = (VertebralLocationScope) obj;
			if (countOverlap != other.countOverlap)
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

	private static final String PREFIX = "VALU\t";

	@Override
	public List<VertebralLocationScope> generateFactorScopes(State state) {
		List<VertebralLocationScope> factors = new ArrayList<>();
		int countOverlap = 0;
		Set<String> overlapNames = new HashSet<>();
		for (EntityTemplate annotation : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (annotation.getEntityType() != EntityType.get("VertebralArea"))
				continue;

			if (!annotation.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
				continue;

			if (!overlapNames
					.add(annotation.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation().getSurfaceForm())) {
				countOverlap++;
			}

		}
		factors.add(new VertebralLocationScope(this, countOverlap));
		return factors;

	}

	@Override
	public void generateFeatureVector(Factor<VertebralLocationScope> factor) {

		DoubleVector featureVector = factor.getFeatureVector();

		featureVector.set(PREFIX + "countOverlap = " + factor.getFactorScope().countOverlap, true);

	}

}
