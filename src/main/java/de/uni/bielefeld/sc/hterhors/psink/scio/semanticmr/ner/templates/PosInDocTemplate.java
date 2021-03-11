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

public class PosInDocTemplate extends AbstractFeatureTemplate<PosInDocTemplate.PosInDocScope> {

	public PosInDocTemplate() {
		super();
	}

	public PosInDocTemplate(boolean cache) {
		super(cache);
	}

	static class PosInDocScope extends AbstractFactorScope {
		EntityType t;
		double x;

		public PosInDocScope(AbstractFeatureTemplate<?> template, EntityType t, double x) {
			super(template);
			this.t = t;
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
			PosInDocScope other = (PosInDocScope) obj;
			if (t == null) {
				if (other.t != null)
					return false;
			} else if (!t.equals(other.t))
				return false;
			if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((t == null) ? 0 : t.hashCode());
			long temp;
			temp = Double.doubleToLongBits(x);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}
	}

	@Override
	public List<PosInDocScope> generateFactorScopes(State state) {
		List<PosInDocScope> factors = new ArrayList<>();

		Document document = state.getInstance().getDocument();

		for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
			DocumentToken token = annotation.relatedTokens.get(annotation.relatedTokens.size() - 1);

			int docLength = document.documentContent.length();

			double x = token.getDocCharOffset() / docLength;
			factors.add(new PosInDocScope(this, annotation.getEntityType(), x));
		}
		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<PosInDocScope> factor) {
		double x = factor.getFactorScope().x;

		StringBuilder sb = new StringBuilder();

		if (x < 0.33) {
			factor.getFeatureVector().set("Im ersten Drittel des Docs: " + sb.toString().trim(), true);
		} else if (x > 0.66) {
			factor.getFeatureVector().set("Im letzten Drittel des Docs: " + sb.toString().trim(), true);
		} else {
			factor.getFeatureVector().set("im zweiten Drittel des Docs:" + sb.toString().trim(), true);
		}
	}
}
