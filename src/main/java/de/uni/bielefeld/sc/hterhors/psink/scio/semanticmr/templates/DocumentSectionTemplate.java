package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.DocumentSectionTemplate.DocumentSectionScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.tools.SCIOAutomatedSectionifcation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.tools.SCIOAutomatedSectionifcation.ESection;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class DocumentSectionTemplate extends AbstractFeatureTemplate<DocumentSectionScope> {

	static class DocumentSectionScope extends AbstractFactorScope {

		public final ESection section;
		public final SlotType slotAnnotation;

		public DocumentSectionScope(AbstractFeatureTemplate<?> template, ESection section, SlotType slotAnnotation) {
			super(template);
			this.section = section;
			this.slotAnnotation = slotAnnotation;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((section == null) ? 0 : section.hashCode());
			result = prime * result + ((slotAnnotation == null) ? 0 : slotAnnotation.hashCode());
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
			DocumentSectionScope other = (DocumentSectionScope) obj;
			if (section != other.section)
				return false;
			if (slotAnnotation == null) {
				if (other.slotAnnotation != null)
					return false;
			} else if (!slotAnnotation.equals(other.slotAnnotation))
				return false;
			return true;
		}

		@Override
		public int implementHashCode() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean implementEquals(Object obj) {
			// TODO Auto-generated method stub
			return false;
		}

	}

	private static final String PREFIX = "DST\t";

	@Override
	public List<DocumentSectionScope> generateFactorScopes(State state) {

		List<DocumentSectionScope> factors = new ArrayList<>();

		SCIOAutomatedSectionifcation sectionification = SCIOAutomatedSectionifcation.getInstance(state.getInstance());

		for (EntityTemplate annotation : super.<EntityTemplate>getPredictedAnnotations(state)) {

			Map<SlotType, Set<AbstractAnnotation>> slotAnnotations = annotation.filter().docLinkedAnnoation()
					.singleSlots().nonEmpty().multiSlots().merge().build().getMergedAnnotations();

			if (annotation.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()) {

				final int rootSenIndex = annotation.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation()
						.getSentenceIndex();

				factors.add(new DocumentSectionScope(this, sectionification.getSection(rootSenIndex), null));
			}
			for (Entry<SlotType, Set<AbstractAnnotation>> slotFillerAnnotations : slotAnnotations.entrySet()) {
				for (AbstractAnnotation slotFillerAnnotation : slotFillerAnnotations.getValue()) {
					final int slotFillerSenIndex = slotFillerAnnotation.asInstanceOfDocumentLinkedAnnotation()
							.getSentenceIndex();
					factors.add(new DocumentSectionScope(this, sectionification.getSection(slotFillerSenIndex),
							slotFillerAnnotations.getKey()));
				}
			}

		}

		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<DocumentSectionScope> factor) {
		// No impact
//		for (ESection other : ESection.values()) {
//			if (factor.getFactorScope().section == other)
//				continue;
//			factor.getFeatureVector().set(PREFIX + (factor.getFactorScope().slotAnnotation == null ? "root"
//					: factor.getFactorScope().slotAnnotation.name) + "\tNOT-" + other.name(), true);
//		}
		factor.getFeatureVector().set(
				PREFIX + (factor.getFactorScope().slotAnnotation == null ? "root"
						: factor.getFactorScope().slotAnnotation.name) + "\t" + factor.getFactorScope().section.name(),
				true);
	}

}
