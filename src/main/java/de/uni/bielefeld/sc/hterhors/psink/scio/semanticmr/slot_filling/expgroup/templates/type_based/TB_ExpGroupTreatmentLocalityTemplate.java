package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer.EExplorationMode;
import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTypeAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based.TB_ExpGroupTreatmentLocalityTemplate.ExpGroupTreatmentLocalityScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class TB_ExpGroupTreatmentLocalityTemplate extends AbstractFeatureTemplate<ExpGroupTreatmentLocalityScope> {

	static class ExpGroupTreatmentLocalityScope extends AbstractFactorScope {

		public final SlotType slot;
		public final int sentenceDistance;
		public final EntityType entityType;

		public ExpGroupTreatmentLocalityScope(AbstractFeatureTemplate<?> template, SlotType slot, EntityType entityType,
				int sentenceDistance) {
			super(template);
			this.slot = slot;
			this.entityType = entityType;
			this.sentenceDistance = sentenceDistance;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
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
			if (entityType == null) {
				if (other.entityType != null)
					return false;
			} else if (!entityType.equals(other.entityType))
				return false;
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

			if (experimentalGroup.getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
				continue;

			if (SCIOSlotTypes.hasTreatmentType.isExcluded())
				continue;

			final Set<Integer> rootSentenceIndicies = collectExpGroupSentenceIndicies(experimentalGroup);

			final MultiFillerSlot mfs = experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType);

			if (mfs.containsSlotFiller()) {

				for (AbstractAnnotation treatment : mfs.getSlotFiller()) {
					final Set<Integer> treatmentIndicies = new HashSet<>();

					EntityType main = null;
					if (treatment.getEntityType() == SCIOEntityTypes.compoundTreatment) {
						SingleFillerSlot sfs = treatment.asInstanceOfEntityTemplate()
								.getSingleFillerSlot(SCIOSlotTypes.hasCompound);
						if (sfs.containsSlotFiller())
							main = sfs.getSlotFiller().asInstanceOfEntityTemplate().getEntityType();
					}

					if (main == null)
						main = treatment.asInstanceOfEntityTemplate().getRootAnnotation().getEntityType();

					for (EntityTypeAnnotation tretamentAnnotation : state.getInstance()
							.getEntityTypeCandidates(EExplorationMode.ANNOTATION_BASED, main)) {

						if (tretamentAnnotation.isInstanceOfDocumentLinkedAnnotation()) {
							treatmentIndicies
									.add(tretamentAnnotation.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0)
											.getSentenceIndex());
							//
//						Pattern project = Pattern.compile(
//								Pattern.quote(main.asInstanceOfDocumentLinkedAnnotation().textualContent.surfaceForm));
//						Matcher m = project.matcher(state.getInstance().getDocument().documentContent);
							//
//						while (m.find()) {
//							try {
							//
//								int sentence = state.getInstance().getDocument().getTokenByCharStartOffset(m.start())
//										.getSentenceIndex();
							//
//								treatmentIndicies.add(sentence);
//							} catch (DocumentLinkedAnnotationMismatchException e) {
//							}
//						}

						}
					}

					int minDist = Integer.MAX_VALUE;
//					int maxDist = Integer.MIN_VALUE;
					for (Integer ri : rootSentenceIndicies) {
						for (Integer ti : treatmentIndicies) {
							final int dist = Math.abs(ri - ti);
							minDist = Math.min(dist, minDist);
//							maxDist = Math.max(dist, maxDist);
						}
					}
					if (minDist == Integer.MAX_VALUE)
						continue;

//					System.out.println(main.getEntityType().entityName + " -> " + minDist);
					factors.add(
							new ExpGroupTreatmentLocalityScope(this, SCIOSlotTypes.hasTreatmentType, main, minDist));
//					factors.add(new ExpGroupTreatmentLocalityScope(this, SCIOSlotTypes.hasTreatmentType, maxDist));

				}
			}
		}
		return factors;
	}

	public Set<Integer> collectExpGroupSentenceIndicies(EntityTemplate experimentalGroup) {
		Set<Integer> sentenceIndicies = new HashSet<>();
		if (experimentalGroup.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()) {

			DocumentLinkedAnnotation docLinkedRootAnnotation = experimentalGroup.getRootAnnotation()
					.asInstanceOfDocumentLinkedAnnotation();

			sentenceIndicies.add(docLinkedRootAnnotation.relatedTokens.get(0).getSentenceIndex());

		}
		if (SCIOSlotTypes.hasGroupName.isIncluded()) {

			for (AbstractAnnotation groupName : experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasGroupName)
					.getSlotFiller()) {

				if (groupName.isInstanceOfDocumentLinkedAnnotation())
					sentenceIndicies.add(
							groupName.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0).getSentenceIndex());
			}
		}
		return sentenceIndicies;
	}

	@Override
	public void generateFeatureVector(Factor<ExpGroupTreatmentLocalityScope> factor) {

//		factor.getFeatureVector().set(
//				PREFIX + factor.getFactorScope().slot.slotName + "->" + factor.getFactorScope().sentenceDistance, true);

//		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().slot.slotName + "-> dist == 0",
//				factor.getFactorScope().sentenceDistance == 0);
//		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().slot.slotName + "-> dist != 0",
//				factor.getFactorScope().sentenceDistance != 0);
//		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().slot.slotName + "-> dist == 1",
//				factor.getFactorScope().sentenceDistance == 1);
//		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().slot.slotName + "-> dist == 2",
//				factor.getFactorScope().sentenceDistance == 2);
//		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().slot.slotName + "-> dist == 3",
//				factor.getFactorScope().sentenceDistance == 3);
//		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().slot.slotName + "-> dist >= 4",
//				factor.getFactorScope().sentenceDistance >= 4);

		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().slot.name + "->"
				+ factor.getFactorScope().entityType.name + " dist == 0",
				factor.getFactorScope().sentenceDistance == 0);
		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().slot.name + "->"
				+ factor.getFactorScope().entityType.name + " dist != 0",
				factor.getFactorScope().sentenceDistance != 0);
		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().slot.name + "->"
				+ factor.getFactorScope().entityType.name + " dist == 1",
				factor.getFactorScope().sentenceDistance == 1);
		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().slot.name + "->"
				+ factor.getFactorScope().entityType.name + " dist != 1",
				factor.getFactorScope().sentenceDistance != 1);
		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().slot.name + "->"
				+ factor.getFactorScope().entityType.name + " dist == 2",
				factor.getFactorScope().sentenceDistance == 2);
		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().slot.name + "->"
				+ factor.getFactorScope().entityType.name + " dist != 2",
				factor.getFactorScope().sentenceDistance != 2);
		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().slot.name + "->"
				+ factor.getFactorScope().entityType.name + " dist == 3",
				factor.getFactorScope().sentenceDistance == 3);
		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().slot.name + "->"
				+ factor.getFactorScope().entityType.name + " dist != 3",
				factor.getFactorScope().sentenceDistance != 3);
		factor.getFeatureVector().set(PREFIX + factor.getFactorScope().slot.name + "->"
				+ factor.getFactorScope().entityType.name + " dist >= 4",
				factor.getFactorScope().sentenceDistance >= 4);
	}

}
