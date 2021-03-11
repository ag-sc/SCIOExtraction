package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.State;

/**
 * splits the mention into parts of size n
 */

public class BigramTemplate extends AbstractFeatureTemplate<BigramTemplate.BigramScope> {

	public BigramTemplate(boolean cache) {
		super(cache);
	}

	public BigramTemplate() {
		super();
	}

	private static final int MAX_NGRAM_SIZE = 5; // 5 seems to be the most efficient
	private static final int MIN_NGRAM_SIZE = 2;

	/**
	 *
	 */
	static class BigramScope extends AbstractFactorScope {

		final String text;
		final public EntityType entityType;

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			BigramScope other = (BigramScope) obj;
			if (entityType == null) {
				if (other.entityType != null)
					return false;
			} else if (!entityType.equals(other.entityType))
				return false;
			if (text == null) {
				if (other.text != null)
					return false;
			} else if (!text.equals(other.text))
				return false;
			return true;
		}

		public BigramScope(AbstractFeatureTemplate<?> template, String text, EntityType entityType) {
			super(template);
			this.text = text;
			this.entityType = entityType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
			result = prime * result + ((text == null) ? 0 : text.hashCode());
			return result;
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
	public List<BigramScope> generateFactorScopes(State state) {
		final List<BigramScope> factors = new ArrayList<>();
		for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
			EntityType entityType = annotation.getEntityType();
			factors.add(new BigramScope(this, annotation.getSurfaceForm(), entityType));

		}
		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<BigramScope> factor) {
		EntityType et = factor.getFactorScope().entityType;
		String text = factor.getFactorScope().text;

			for (int i = 0; i < text.length(); i++) {
				for (int j = MIN_NGRAM_SIZE; j <= MAX_NGRAM_SIZE; j++) {
					char[] bigramChars = new char[j];
					if (text.length() >= i + j) {
						text.getChars(i, i + j, bigramChars, 0);
						String bigram = new String(bigramChars);
						factor.getFeatureVector().set(et.name + " " + bigram, true);
					}
				}
			}
	}
}
