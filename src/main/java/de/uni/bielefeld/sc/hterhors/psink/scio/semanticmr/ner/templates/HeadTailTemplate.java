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
 * first word of a mention
 */
public class HeadTailTemplate extends AbstractFeatureTemplate<HeadTailTemplate.HMScope> {

	public HeadTailTemplate(boolean cache) {
		super(cache);
	}

	public HeadTailTemplate() {
		super();
	}

	static class HMScope extends AbstractFactorScope {

		final EntityType type;
		final String headword;
		final String tailword;

		public HMScope(AbstractFeatureTemplate<?> template, EntityType type, String headword, String tailword) {
			super(template);
			this.type = type;
			this.headword = headword;
			this.tailword = tailword;
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
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			HMScope other = (HMScope) obj;
			if (headword == null) {
				if (other.headword != null)
					return false;
			} else if (!headword.equals(other.headword))
				return false;
			if (tailword == null) {
				if (other.tailword != null)
					return false;
			} else if (!tailword.equals(other.tailword))
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((headword == null) ? 0 : headword.hashCode());
			result = prime * result + ((tailword == null) ? 0 : tailword.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}
	}

	@Override
	public List<HMScope> generateFactorScopes(State state) {
		List<HMScope> factors = new ArrayList<>();

		for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {

			factors.add(new HMScope(this, annotation.entityType, annotation.relatedTokens.get(0).getText(),
					annotation.relatedTokens.get(annotation.relatedTokens.size()-1).getText()));

		}
		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<HMScope> factor) {
		factor.getFeatureVector().set(factor.getFactorScope().type.name + " head " + factor.getFactorScope().headword,
				true);
		factor.getFeatureVector().set(factor.getFactorScope().type.name + " tail" + factor.getFactorScope().tailword,
				true);

	}

}
