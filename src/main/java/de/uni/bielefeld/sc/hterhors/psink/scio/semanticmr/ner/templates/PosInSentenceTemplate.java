package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.State;

/**
 * looks for one to a set number of words after a mention
 */

public class PosInSentenceTemplate extends AbstractFeatureTemplate<PosInSentenceTemplate.PosInSentenceScope> {

	public PosInSentenceTemplate() {
		super();
	}

	public PosInSentenceTemplate(boolean cache) {
		super(cache);
	}

	static class PosInSentenceScope extends AbstractFactorScope {

		EntityType annotation;
		double x;

		public PosInSentenceScope(AbstractFeatureTemplate<?> template, EntityType annotation, double x) {
			super(template);
			this.annotation = annotation;
			this.x = x;
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
			PosInSentenceScope other = (PosInSentenceScope) obj;
			if (annotation == null) {
				if (other.annotation != null)
					return false;
			} else if (!annotation.equals(other.annotation))
				return false;
			if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
			long temp;
			temp = Double.doubleToLongBits(x);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}
	}

	@Override
	public List<PosInSentenceScope> generateFactorScopes(State state) {
		List<PosInSentenceScope> factors = new ArrayList<>();

		for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
			DocumentToken token = annotation.relatedTokens.get(annotation.relatedTokens.size() - 1);
			int senLength = annotation.getSentenceOfAnnotation().length();
			double x = token.getDocCharOffset() / senLength;
			factors.add(new PosInSentenceScope(this, annotation.entityType, x));
		}
		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<PosInSentenceScope> factor) {
		double x = factor.getFactorScope().x;
		for (double i = 0; i < 1; i+=0.1) {
			if(x>i) {
				factor.getFeatureVector().set(i+": " + factor.getFactorScope().annotation.name, true);
				break;
			}
			
		}
//		
//		if (x < 0.33) {
//		} else if (x > 0.66) {
//			factor.getFeatureVector().set("Im letzten Drittel: " + factor.getFactorScope().annotation.name, true);
//		} else {
//			factor.getFeatureVector().set("im zweiten Drittel:" + factor.getFactorScope().annotation.name, true);
//		}

	}
}
