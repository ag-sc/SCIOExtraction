package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.groupname.templates;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;

/**
 * checks if two mentions belong to the same sentence
 */
public class GroupNamesInSameSentenceTemplate_FAST
		extends AbstractFeatureTemplate<GroupNamesInSameSentenceTemplate_FAST.MentionsInSentenceScope> {

	static class MentionsInSentenceScope extends AbstractFactorScope {

		public String tokenOne;
		public String tokenTwo;

		public MentionsInSentenceScope(AbstractFeatureTemplate<?> template, String tokenOne, String tokenTwo) {
			super(template);
			this.tokenOne = tokenOne;
			this.tokenTwo = tokenTwo;
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
			MentionsInSentenceScope other = (MentionsInSentenceScope) obj;
			if (tokenOne == null) {
				if (other.tokenOne != null)
					return false;
			} else if (!tokenOne.equals(other.tokenOne))
				return false;
			if (tokenTwo == null) {
				if (other.tokenTwo != null)
					return false;
			} else if (!tokenTwo.equals(other.tokenTwo))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((tokenOne == null) ? 0 : tokenOne.hashCode());
			result = prime * result + ((tokenTwo == null) ? 0 : tokenTwo.hashCode());
			return result;
		}
	}

	@Override
	public List<MentionsInSentenceScope> generateFactorScopes(State state) {
		List<MentionsInSentenceScope> factors = new ArrayList<>();

		List<DocumentLinkedAnnotation> annotations = super.<DocumentLinkedAnnotation>getPredictedAnnotations(state);

		for (int i = 0; i < annotations.size(); i++) {

			final int a1Index = annotations.get(i).getSentenceIndex();

			for (int j = i + 1; j < annotations.size(); j++) {

				final int a2Index = annotations.get(j).getSentenceIndex();

				if (a1Index != a2Index)
					continue;
				factors.add(new MentionsInSentenceScope(this, annotations.get(i).getSurfaceForm(),
						annotations.get(j).getSurfaceForm()));

			}
		}

		return factors;

	}

	@Override
	public void generateFeatureVector(Factor<MentionsInSentenceScope> factor) {


		factor.getFeatureVector()
				.set("GroupNames in Same Sentence: " + factor.getFactorScope().tokenOne + ", " + factor.getFactorScope().tokenTwo, true);
	}

}
