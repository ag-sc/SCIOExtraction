package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrNameBOWTemplate.ExGrNaBOWScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.helper.bow.BOWExtractor;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class ExGrNameBOWTemplate extends AbstractFeatureTemplate<ExGrNaBOWScope> {

	public ExGrNameBOWTemplate() {
		super(false);
	}

	static class ExGrNaBOWScope extends AbstractFactorScope {

		public final String term1;
		public final String term2;
		public final String context;

		public ExGrNaBOWScope(AbstractFeatureTemplate<?> template, String context, String term1, String term2) {
			super(template);
			this.term1 = term1;
			this.term2 = term2;
			this.context = context;
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
			result = prime * result + ((context == null) ? 0 : context.hashCode());
			result = prime * result + ((term1 == null) ? 0 : term1.hashCode());
			result = prime * result + ((term2 == null) ? 0 : term2.hashCode());
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
			ExGrNaBOWScope other = (ExGrNaBOWScope) obj;
			if (context == null) {
				if (other.context != null)
					return false;
			} else if (!context.equals(other.context))
				return false;
			if (term1 == null) {
				if (other.term1 != null)
					return false;
			} else if (!term1.equals(other.term1))
				return false;
			if (term2 == null) {
				if (other.term2 != null)
					return false;
			} else if (!term2.equals(other.term2))
				return false;
			return true;
		}

	}

	@Override
	public List<ExGrNaBOWScope> generateFactorScopes(State state) {
		List<ExGrNaBOWScope> factors = new ArrayList<>();
		List<List<Set<String>>> x = collectBOWs(state);

//		addFactors(factors, createOuterPairs(x), "Out: ");
		addFactors(factors, createInnerPairs(x), "In: ");

		return factors;

	}

	public void addFactors(List<ExGrNaBOWScope> factors, Map<String, Set<String>> pairs, final String context) {
		for (Entry<String, Set<String>> e : pairs.entrySet()) {
			final String term1 = e.getKey();
			for (String term2 : e.getValue()) {
				factors.add(new ExGrNaBOWScope(this, context, term1, term2));
			}
		}
	}

	public Map<String, Set<String>> createInnerPairs(List<List<Set<String>>> allBows) {
		Map<String, Set<String>> pairs = new HashMap<>();
		/**
		 * pair positive terms
		 */

		for (List<Set<String>> bows : allBows) {
			for (int i = 0; i < bows.size(); i++) {
				for (String term1 : bows.get(i)) {
					if (!Document.getStopWords().contains(term1) && !Document.getPunctuationWords().contains(term1)) {
						Set<String> y = new HashSet<>();
						pairs.putIfAbsent(term1, y);
						for (int j = i + 1; j < bows.size(); j++) {
							for (String term2 : bows.get(j)) {
								y.add(term2);
							}
						}
					}
				}
			}
		}
		return pairs;

	}

	public Map<String, Set<String>> createOuterPairs(List<List<Set<String>>> allBows) {
		Map<String, Set<String>> pairs = new HashMap<>();
		/**
		 * pair negative terms
		 */
		for (int i = 0; i < allBows.size(); i++) {
			for (Set<String> terms1 : allBows.get(i)) {
				for (String term1 : terms1) {
					if (!Document.getStopWords().contains(term1) && !Document.getPunctuationWords().contains(term1)) {
						Set<String> y;
						pairs.put(term1, y = new HashSet<>());
						for (int j = i + 1; j < allBows.size(); j++) {
							for (Set<String> terms2 : allBows.get(j)) {
								for (String term2 : terms2) {
									if (!Document.getStopWords().contains(term2)
											&& !Document.getPunctuationWords().contains(term2)) {
										y.add(term2);
									}
								}
							}
						}
					}
				}
			}
		}
		return pairs;
	}

	public List<List<Set<String>>> collectBOWs(State state) {
		/**
		 * Collect all bows
		 */
		List<List<Set<String>>> allBows = new ArrayList<>();
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

			for (AbstractAnnotation groupName : experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasGroupName).getSlotFiller()) {

				Set<String> groupNameBow = BOWExtractor
						.extractDocLinkedBOW(groupName.asInstanceOfDocumentLinkedAnnotation());

				bows.add(groupNameBow);
			}

			allBows.add(bows);
		}
		return allBows;
	}

	@Override
	public void generateFeatureVector(Factor<ExGrNaBOWScope> factor) {

		factor.getFeatureVector().set(factor.getFactorScope().context + "\t" + factor.getFactorScope().term1 + "\t"
				+ factor.getFactorScope().term2, true);
	}

}
