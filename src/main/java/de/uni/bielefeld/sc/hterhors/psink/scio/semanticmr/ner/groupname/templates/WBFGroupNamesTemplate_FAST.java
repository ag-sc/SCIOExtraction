package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.groupname.templates;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.State;

/**
 * WBF: first word in between if more than two words in between WBL: last word
 * in between if more than two words in between
 */
public class WBFGroupNamesTemplate_FAST extends AbstractFeatureTemplate<WBFGroupNamesTemplate_FAST.WBScope> {

	private static boolean WBFACTIVE = true;
	private static boolean WBLACTIVE = false;

	static class WBScope extends AbstractFactorScope {

		public final String annoOne;
		public final String annoTwo;

		public final String word;

		public final boolean first;

		public WBScope(AbstractFeatureTemplate<?> template, String annoOne, String annoTwo, String word,
				boolean first) {
			super(template);
			this.annoOne = annoOne;
			this.annoTwo = annoTwo;
			this.word = word;
			this.first = first;
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
			WBScope other = (WBScope) obj;
			if (annoOne == null) {
				if (other.annoOne != null)
					return false;
			} else if (!annoOne.equals(other.annoOne))
				return false;
			if (annoTwo == null) {
				if (other.annoTwo != null)
					return false;
			} else if (!annoTwo.equals(other.annoTwo))
				return false;
			if (first != other.first)
				return false;
			if (word == null) {
				if (other.word != null)
					return false;
			} else if (!word.equals(other.word))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((annoOne == null) ? 0 : annoOne.hashCode());
			result = prime * result + ((annoTwo == null) ? 0 : annoTwo.hashCode());
			result = prime * result + (first ? 1231 : 1237);
			result = prime * result + ((word == null) ? 0 : word.hashCode());
			return result;
		}
	}

	@Override
	public List<WBScope> generateFactorScopes(State state) {
		List<WBScope> factors = new ArrayList<>();
		Document document = state.getInstance().getDocument();

		List<DocumentLinkedAnnotation> annotations = super.<DocumentLinkedAnnotation>getPredictedAnnotations(state);

		for (int i = 0; i < annotations.size(); i++) {

			final int a1Index = annotations.get(i).relatedTokens.get(annotations.get(i).relatedTokens.size() - 1)
					.getDocTokenIndex();

			for (int j = i + 1; j < annotations.size(); j++) {

				final int a2Index = annotations.get(j).relatedTokens.get(0).getDocTokenIndex();

				if (Math.abs(a1Index - a2Index) < 3 || Math.abs(a1Index - a2Index) > 20)
					continue;

				if (a2Index < a1Index) {
					if (WBFACTIVE) {
						factors.add(new WBScope(this, annotations.get(i).getSurfaceForm(),
								annotations.get(j).getSurfaceForm(), document.tokenList.get(a2Index + 1).getText(),
								true));
					}
					if (WBLACTIVE) {
						factors.add(new WBScope(this, annotations.get(i).getSurfaceForm(),
								annotations.get(j).getSurfaceForm(), document.tokenList.get(a1Index - 1).getText(),
								false));
					}
				} else {
					// swap annotations
					if (WBFACTIVE) {
						factors.add(new WBScope(this, annotations.get(j).getSurfaceForm(),
								annotations.get(i).getSurfaceForm(), document.tokenList.get(a1Index + 1).getText(),
								true));
					}
					if (WBLACTIVE) {
						factors.add(new WBScope(this, annotations.get(j).getSurfaceForm(),
								annotations.get(i).getSurfaceForm(), document.tokenList.get(a2Index - 1).getText(),
								false));
					}
				}

			}
		}

		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<WBScope> factor) {

		factor.getFeatureVector()
				.set((factor.getFactorScope().first ? "First" : "Last") + " word inbetween: <\""
						+ factor.getFactorScope().annoOne + "\", \"" + factor.getFactorScope().annoTwo + "\"> "
						+ factor.getFactorScope().word, true);

	}
}
