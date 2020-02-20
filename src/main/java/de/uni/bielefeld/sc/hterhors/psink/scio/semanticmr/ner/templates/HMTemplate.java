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
public class HMTemplate extends AbstractFeatureTemplate<HMTemplate.HMScope> {

	public HMTemplate (boolean cache){
		super(cache);
	}

	public HMTemplate() {
		super();
	}

	static class HMScope
			extends AbstractFactorScope {

		EntityType type;
		DocumentToken headword;


		public HMScope(
                AbstractFeatureTemplate<HMScope> template, EntityType type, DocumentToken token) {
			super(template);
			this.type = type;
			this.headword = token;
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
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			HMScope hmScope = (HMScope) o;
			return Objects.equals(type, hmScope.type) &&
					Objects.equals(headword, hmScope.headword);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), type, headword);
		}
	}

	@Override
	public List<HMScope> generateFactorScopes(State state) {
		List<HMScope> factors = new ArrayList<>();

		for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {

			factors.add(new HMScope(this, annotation.entityType, annotation.relatedTokens.get(0)));

		}
		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<HMScope> factor) {
		factor.getFeatureVector().set("First Word of Annotation <"+factor.getFactorScope().type.name + ">: " + factor.getFactorScope().headword.getText(), true);

	}

}
