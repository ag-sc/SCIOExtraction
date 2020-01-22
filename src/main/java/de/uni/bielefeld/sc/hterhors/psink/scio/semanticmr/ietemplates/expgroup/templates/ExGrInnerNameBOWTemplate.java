package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrInnerNameBOWTemplate.ExGrInnerNaBOWScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.helper.bow.BOWExtractor;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class ExGrInnerNameBOWTemplate extends AbstractFeatureTemplate<ExGrInnerNaBOWScope> {

	static class ExGrInnerNaBOWScope extends AbstractFactorScope {

		public final List<Set<String>> expGroupBOWs;

		public ExGrInnerNaBOWScope(AbstractFeatureTemplate<?> template, List<Set<String>> expGroupBOWs) {
			super(template);
			this.expGroupBOWs = expGroupBOWs;
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
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((expGroupBOWs == null) ? 0 : expGroupBOWs.hashCode());
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
			ExGrInnerNaBOWScope other = (ExGrInnerNaBOWScope) obj;
			if (expGroupBOWs == null) {
				if (other.expGroupBOWs != null)
					return false;
			} else if (!expGroupBOWs.equals(other.expGroupBOWs))
				return false;
			return true;
		}

	}

	@Override
	public List<ExGrInnerNaBOWScope> generateFactorScopes(State state) {
		List<ExGrInnerNaBOWScope> factors = new ArrayList<>();

		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
				continue;

			List<Set<String>> bows = new ArrayList<>();

			final AbstractAnnotation rootAnnotation = experimentalGroup.getRootAnnotation();

			Set<String> groupBow;
			if (rootAnnotation.isInstanceOfDocumentLinkedAnnotation())
				groupBow = BOWExtractor.extractDocLinkedBOW(rootAnnotation.asInstanceOfDocumentLinkedAnnotation());
			else
				groupBow = Collections.emptySet();

			bows.add(groupBow);

			if (SCIOSlotTypes.hasGroupName.isIncluded()) {
				for (AbstractAnnotation groupName : experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasGroupName)
						.getSlotFiller()) {

					Set<String> groupNameBow = BOWExtractor
							.extractDocLinkedBOW(groupName.asInstanceOfDocumentLinkedAnnotation());

					bows.add(groupNameBow);
				}
			}

			factors.add(new ExGrInnerNaBOWScope(this, bows));

		}

		return factors;

	}

	@Override
	public void generateFeatureVector(Factor<ExGrInnerNaBOWScope> factor) {

		final List<Set<String>> termsList = new ArrayList<>(factor.getFactorScope().expGroupBOWs);

		for (int i = 0; i < termsList.size(); i++) {
			Set<String> terms1 = termsList.get(i);
			int countOverlap = 0;
			for (int j = i + 1; j < termsList.size(); j++) {
				Set<String> terms2 = termsList.get(j);

				/**
				 * Terms features
				 */

				for (String term1 : terms1) {
					for (String term2 : terms2) {
						countOverlap += term1.equals(term2) ? 1 : 0;
						factor.getFeatureVector().set(term1 + "\t" + term2, true);

					}
				}

				factor.getFeatureVector().set("Overlap = " + countOverlap, true);
				factor.getFeatureVector().set("Overlap == 1 ", countOverlap == 1);
				factor.getFeatureVector().set("Overlap > 1 ", countOverlap > 1);

			}

		}
	}

}
