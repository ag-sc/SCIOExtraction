package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.sparql.function.library.min;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.filter.EntityTemplateAnnotationFilter;
import de.hterhors.semanticmr.crf.structure.slots.AbstractSlot;
import de.hterhors.semanticmr.crf.structure.slots.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.slots.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrBOWTemplate.BOWScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExpGroupTreatmentLocalityTemplate.ExpGroupTreatmentLocalityScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.helper.bow.BOWExtractor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.helper.bow.TypedBOW;
import edu.stanford.nlp.ie.crf.FactorTable;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class ExpGroupTreatmentLocalityTemplate extends AbstractFeatureTemplate<ExpGroupTreatmentLocalityScope> {

	static class ExpGroupTreatmentLocalityScope extends AbstractFactorScope {

		public final SlotType slot;
		public final int sentenceDistance;

		public ExpGroupTreatmentLocalityScope(AbstractFeatureTemplate<?> template, SlotType slot,
				int sentenceDistance) {
			super(template);
			this.slot = slot;
			this.sentenceDistance = sentenceDistance;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + sentenceDistance;
			result = prime * result + ((slot == null) ? 0 : slot.hashCode());
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
			ExpGroupTreatmentLocalityScope other = (ExpGroupTreatmentLocalityScope) obj;
			if (sentenceDistance != other.sentenceDistance)
				return false;
			if (slot == null) {
				if (other.slot != null)
					return false;
			} else if (!slot.equals(other.slot))
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

	private static final String PREFIX = "EGTL\t";

	@Override
	public List<ExpGroupTreatmentLocalityScope> generateFactorScopes(State state) {
		List<ExpGroupTreatmentLocalityScope> factors = new ArrayList<>();

		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != EntityType.get("DefinedExperimentalGroup"))
				continue;

			final Set<Integer> rootSentenceIndicies = collectExpGroupSentenceIndicies(experimentalGroup);

			final Set<Integer> treatmentIndicies = getMultiFllerSlotSentenceIndicies(experimentalGroup,
					SlotType.get("hasTreatmentType"));

			int minDist = Integer.MAX_VALUE;
			int maxDist = Integer.MIN_VALUE;
			for (Integer ri : rootSentenceIndicies) {
				for (Integer ti : treatmentIndicies) {
					final int dist = Math.abs(ri - ti);
					minDist = Math.min(dist, minDist);
					maxDist = Math.max(dist, maxDist);
				}
			}
			factors.add(new ExpGroupTreatmentLocalityScope(this, SlotType.get("hasTreatmentType"), -minDist));
			factors.add(new ExpGroupTreatmentLocalityScope(this, SlotType.get("hasTreatmentType"), maxDist));
		}
		return factors;
	}

	private Set<Integer> getMultiFllerSlotSentenceIndicies(EntityTemplate experimentalGroup, SlotType slotType) {
		final Set<Integer> treatmentIndicies = new HashSet<>();

		final MultiFillerSlot mfs = experimentalGroup.getMultiFillerSlot(slotType);

		if (mfs.containsSlotFiller()) {

			for (AbstractAnnotation treatment : mfs.getSlotFiller()) {

				if (treatment.getEntityType() == EntityType.get("CompoundTreatment")) {

					SingleFillerSlot sfs = treatment.asInstanceOfEntityTemplate()
							.getSingleFillerSlot(SlotType.get("hasCompound"));

					AbstractAnnotation compound = sfs.getSlotFiller().asInstanceOfEntityTemplate().getRootAnnotation();

					if (compound.isInstanceOfDocumentLinkedAnnotation()) {
						treatmentIndicies.add(compound.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0)
								.getSentenceIndex());

					}
				}
			}
		}
		return treatmentIndicies;
	}

	public Set<Integer> collectExpGroupSentenceIndicies(EntityTemplate experimentalGroup) {
		Set<Integer> sentenceIndicies = new HashSet<>();
		if (experimentalGroup.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()) {

			DocumentLinkedAnnotation docLinkedRootAnnotation = experimentalGroup.getRootAnnotation()
					.asInstanceOfDocumentLinkedAnnotation();

			sentenceIndicies.add(docLinkedRootAnnotation.relatedTokens.get(0).getSentenceIndex());

		}

		for (AbstractAnnotation groupName : experimentalGroup.getMultiFillerSlot("hasGroupName").getSlotFiller()) {

			if (groupName.isInstanceOfDocumentLinkedAnnotation())
				sentenceIndicies
						.add(groupName.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0).getSentenceIndex());
		}
		return sentenceIndicies;
	}

	@Override
	public void generateFeatureVector(Factor<ExpGroupTreatmentLocalityScope> factor) {

//		factor.getFeatureVector().set(PREFIX + pe.entityName + "->" + ce.entityName + " sentence dist = "
//				+ factor.getFactorScope().sentenceDistance, true);
//		factor.getFeatureVector().set(PREFIX + pe.entityName + "->" + ce.entityName + " sentence dist >= 4",
//				factor.getFactorScope().sentenceDistance >= 4);

		factor.getFeatureVector().set(
				PREFIX + factor.getFactorScope().slot.slotName + "->" + factor.getFactorScope().sentenceDistance, true);
	}

}
