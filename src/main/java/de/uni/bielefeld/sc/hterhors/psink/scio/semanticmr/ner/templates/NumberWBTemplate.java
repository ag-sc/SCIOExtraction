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
 * checks the number of words between two mentions
 */
public class NumberWBTemplate extends AbstractFeatureTemplate<NumberWBTemplate.NumberWBScope> {

	static class NumberWBScope
			extends AbstractFactorScope {

		public EntityType typeOne;
		public EntityType typeTwo;

		public DocumentToken lastTokenOne;
		public DocumentToken firstTokenTwo;


		public NumberWBScope(
                AbstractFeatureTemplate<NumberWBScope> template, Document document, DocumentToken lastTokenOne, DocumentToken firstTokenTwo,
                EntityType typeOne, EntityType typeTwo) {
			super(template);
			this.lastTokenOne = lastTokenOne;
			this.firstTokenTwo = firstTokenTwo;
			this.typeOne = typeOne;
			this.typeTwo = typeTwo;
		}

		@Override
		public int implementHashCode() {
			return 0;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			NumberWBScope that = (NumberWBScope) o;
			return Objects.equals(typeOne, that.typeOne) &&
					Objects.equals(typeTwo, that.typeTwo) &&
					Objects.equals(lastTokenOne, that.lastTokenOne) &&
					Objects.equals(firstTokenTwo, that.firstTokenTwo);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), typeOne, typeTwo, lastTokenOne, firstTokenTwo);
		}

		@Override
		public boolean implementEquals(Object obj) {
			return false;
		}
	}

	@Override
	public List<NumberWBScope> generateFactorScopes(State state) {

		List<NumberWBScope> factors = new ArrayList<>();

		Document document = state.getInstance().getDocument();

		for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
			for (DocumentLinkedAnnotation annotation2 :super.<DocumentLinkedAnnotation>getPredictedAnnotations(state))
				if(!annotation.equals(annotation2)){
					DocumentToken lastTokenOne = annotation.relatedTokens.get(annotation.relatedTokens.size()-1);
					DocumentToken firstTokenTwo = annotation2.relatedTokens.get(0);
					EntityType typeOne = annotation.entityType;
					EntityType typeTwo = annotation2.entityType;
					if(lastTokenOne.getDocTokenIndex() < firstTokenTwo.getDocTokenIndex())
							factors.add(new NumberWBScope(this, document, lastTokenOne, firstTokenTwo, typeOne, typeTwo));
				}

		}
		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<NumberWBScope> factor) {

			int wordsBetween = factor.getFactorScope().firstTokenTwo.getDocTokenIndex() -
					factor.getFactorScope().lastTokenOne.getDocTokenIndex() - 1;

			factor.getFeatureVector().set(factor.getFactorScope().typeOne.name+" "+factor.getFactorScope().typeTwo.name+" "+wordsBetween, true);

	}
}