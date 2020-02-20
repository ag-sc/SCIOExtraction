package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;

/**
 * Counts the number of words in a Mention
 */
public class WordCountTemplate extends AbstractFeatureTemplate<WordCountTemplate.EmptyScope> {



	static class EmptyScope
			extends AbstractFactorScope {


		int numberOfWords;
		EntityType type;

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			EmptyScope that = (EmptyScope) o;
			return numberOfWords == that.numberOfWords &&
					Objects.equals(type, that.type);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), numberOfWords, type);
		}

		public EmptyScope(AbstractFeatureTemplate template, int numberOfWords, EntityType type) {
			super(template);
			this.numberOfWords = numberOfWords;
			this.type = type;
		}

		public EmptyScope(
                AbstractFeatureTemplate<EmptyScope> template) {
			super(template);
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

	@Override
	public List<EmptyScope> generateFactorScopes(State state) {
		List<EmptyScope> factors = new ArrayList<>();

		for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {

			factors.add(new EmptyScope(this, annotation.relatedTokens.size()-1, annotation.getEntityType()));

		}
		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<EmptyScope> factor) {

		factor.getFeatureVector().set("Number of Words in <" + factor.getFactorScope().type.name + ">: " + factor.getFactorScope().numberOfWords, true);

	}
}
