package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer.EExplorationMode;
import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTypeAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.AnnotationExistsInAbstractTemplate.DocumentSectionScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.tools.SCIOAutomatedSectionifcation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.tools.SCIOAutomatedSectionifcation.ESection;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class AnnotationExistsInAbstractTemplate extends AbstractFeatureTemplate<DocumentSectionScope> {

	static class DocumentSectionScope extends AbstractFactorScope {

		public final SlotType slotAnnotation;

		public DocumentSectionScope(AbstractFeatureTemplate<?> template, SlotType slotAnnotation) {
			super(template);
			this.slotAnnotation = slotAnnotation;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
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

	private static final String PREFIX = "AEIA\t";

	@Override
	public List<DocumentSectionScope> generateFactorScopes(State state) {

		List<DocumentSectionScope> factors = new ArrayList<>();

		SCIOAutomatedSectionifcation sectionification = SCIOAutomatedSectionifcation.getInstance(state.getInstance());

		for (EntityTemplate annotation : super.<EntityTemplate>getPredictedAnnotations(state)) {

			Map<SlotType, Set<AbstractAnnotation>> slotAnnotations = annotation.filter().docLinkedAnnoation()
					.singleSlots().nonEmpty().multiSlots().merge().build().getMergedAnnotations();

			if (annotation.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()) {

				EntityType rootType = annotation.getRootAnnotation().entityType;

				for (EntityTypeAnnotation eta : state.getInstance()
						.getEntityTypeCandidates(EExplorationMode.ANNOTATION_BASED, rootType)) {

					if (!eta.isInstanceOfDocumentLinkedAnnotation())
						continue;
					if (isAbstract(sectionification, eta.asInstanceOfDocumentLinkedAnnotation())) {
						factors.add(new DocumentSectionScope(this, null));
						break;
					}

				}

			}

			for (Entry<SlotType, Set<AbstractAnnotation>> slotFillerAnnotations : slotAnnotations.entrySet()) {

				boolean allTrue = true;

				for (AbstractAnnotation slotFillerAnnotation : slotFillerAnnotations.getValue()) {

					for (EntityTypeAnnotation eta : state.getInstance().getEntityTypeCandidates(
							EExplorationMode.ANNOTATION_BASED, slotFillerAnnotation.getEntityType())) {

						if (!eta.isInstanceOfDocumentLinkedAnnotation())
							continue;

						allTrue |= isAbstract(sectionification, eta.asInstanceOfDocumentLinkedAnnotation());

					}

				}
				if (allTrue)
					factors.add(new DocumentSectionScope(this, slotFillerAnnotations.getKey()));
			}

		}

		return factors;
	}

	public boolean isAbstract(SCIOAutomatedSectionifcation sectionification, DocumentLinkedAnnotation eta) {
		return sectionification.getSection(eta.getSentenceIndex()) == ESection.ABSTRACT;
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
		factor.getFeatureVector().set(PREFIX + ((factor.getFactorScope().slotAnnotation == null ? "root"
				: factor.getFactorScope().slotAnnotation.name)) + " also in ABSTRACT", true);
	}

}
