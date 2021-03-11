package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;

public class PairwiseMentionLabelTemplate extends AbstractFeatureTemplate<PairwiseMentionLabelTemplate.RootTypeScope> {

	static class RootTypeScope extends AbstractFactorScope {

		final String mentionOne;
		final String mentionTwo;
		
		final EntityType typeOne;
		final EntityType typeTwo;

		public RootTypeScope(AbstractFeatureTemplate<?> template, String mentionOne, String mentionTwo,
				EntityType typeOne, EntityType typeTwo) {
			super(template);
			this.mentionOne = mentionOne;
			this.mentionTwo = mentionTwo;
			this.typeOne = typeOne;
			this.typeTwo = typeTwo;
		}

		@Override
		public int implementHashCode() {
			return 0;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			RootTypeScope other = (RootTypeScope) obj;
			if (mentionOne == null) {
				if (other.mentionOne != null)
					return false;
			} else if (!mentionOne.equals(other.mentionOne))
				return false;
			if (mentionTwo == null) {
				if (other.mentionTwo != null)
					return false;
			} else if (!mentionTwo.equals(other.mentionTwo))
				return false;
			if (typeOne == null) {
				if (other.typeOne != null)
					return false;
			} else if (!typeOne.equals(other.typeOne))
				return false;
			if (typeTwo == null) {
				if (other.typeTwo != null)
					return false;
			} else if (!typeTwo.equals(other.typeTwo))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((mentionOne == null) ? 0 : mentionOne.hashCode());
			result = prime * result + ((mentionTwo == null) ? 0 : mentionTwo.hashCode());
			result = prime * result + ((typeOne == null) ? 0 : typeOne.hashCode());
			result = prime * result + ((typeTwo == null) ? 0 : typeTwo.hashCode());
			return result;
		}

		@Override
		public boolean implementEquals(Object obj) {
			return false;
		}

	}

	@Override
	public List<RootTypeScope> generateFactorScopes(State state) {
		List<RootTypeScope> factors = new ArrayList<>();

		for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
			for (DocumentLinkedAnnotation annotation2 : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state))
				if (!annotation.equals(annotation2))
					factors.add(new RootTypeScope(this, annotation.getSurfaceForm(), annotation2.getSurfaceForm(),
							annotation.getEntityType(), annotation2.getEntityType()));
		}
		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<RootTypeScope> factor) {
		factor.getFeatureVector().set(factor.getFactorScope().mentionOne + "-" + factor.getFactorScope().mentionTwo
				+ "--" + factor.getFactorScope().typeOne.name + "-" + factor.getFactorScope().typeTwo.name, true);
	}
}
