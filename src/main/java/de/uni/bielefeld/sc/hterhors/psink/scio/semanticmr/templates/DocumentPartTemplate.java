package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.DocumentPartTemplate.DocumentPartScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class DocumentPartTemplate extends AbstractFeatureTemplate<DocumentPartScope> {

	static class DocumentPartScope extends AbstractFactorScope {

		public final int[] numOfAnnotationsInQuarter;

		public DocumentPartScope(AbstractFeatureTemplate<?> template, int[] numOfAnnotationsInQuarter) {
			super(template);
			this.numOfAnnotationsInQuarter = numOfAnnotationsInQuarter;
		}

		@Override
		public int implementHashCode() {
			return 0;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Arrays.hashCode(numOfAnnotationsInQuarter);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			DocumentPartScope other = (DocumentPartScope) obj;
			if (!Arrays.equals(numOfAnnotationsInQuarter, other.numOfAnnotationsInQuarter))
				return false;
			return true;
		}

		@Override
		public boolean implementEquals(Object obj) {
			return false;
		}

	}

	private static final String PREFIX = "DPT\t";
	private static final int PARTS = 3;

	@Override
	public List<DocumentPartScope> generateFactorScopes(State state) {
		List<DocumentPartScope> factors = new ArrayList<>();

		final int numOfSentences = state.getInstance().getDocument().tokenList
				.get(state.getInstance().getDocument().tokenList.size() - 1).getSentenceIndex();

		for (EntityTemplate annotation : super.<EntityTemplate>getPredictedAnnotations(state)) {

			Map<SlotType, Set<AbstractAnnotation>> slotAnnotations = annotation.filter().docLinkedAnnoation()
					.singleSlots().nonEmpty().multiSlots().merge().build().getMergedAnnotations();

			int[] numOfAnnotationsInQuarter = new int[PARTS];

			if (annotation.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()) {

				final int rootSenIndex = annotation.getRootAnnotation()
						.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0).getSentenceIndex();

				final int rootQuarter = getPart(numOfSentences, rootSenIndex);

				numOfAnnotationsInQuarter[rootQuarter]++;
			}
			for (Set<AbstractAnnotation> slotFillerAnnotations : slotAnnotations.values()) {
				for (AbstractAnnotation slotFillerAnnotation : slotFillerAnnotations) {
					final int slotFillerSenIndex = slotFillerAnnotation
							.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0).getSentenceIndex();
					final int quarter = getPart(numOfSentences, slotFillerSenIndex);
					numOfAnnotationsInQuarter[quarter]++;
				}
			}

			factors.add(new DocumentPartScope(this, numOfAnnotationsInQuarter));
		}

		return factors;
	}

	public int getPart(int numOfSentences, final int rootSenIndex) {
		int numOfSenPerQuarter = numOfSentences / PARTS;

		for (int i = 0; i < PARTS; i++) {
			if (rootSenIndex < numOfSenPerQuarter * (i + 1))
				return i;
		}
		return PARTS - 1;
	}

	@Override
	public void generateFeatureVector(Factor<DocumentPartScope> factor) {

		int[] numOfAnnotationsInQuarter = factor.getFactorScope().numOfAnnotationsInQuarter;

		for (int i = 0; i < numOfAnnotationsInQuarter.length; i++) {
			factor.getFeatureVector()
					.set(PREFIX + "Num Of Annotations In Quarter" + i + " = " + numOfAnnotationsInQuarter[i], true);
		}
	}

}
