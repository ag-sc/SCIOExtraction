package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.slots.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.slots.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrBOWTemplate.BOWScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.helper.bow.BOWExtractor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.helper.bow.TypedBOW;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class ExGrBOWTemplate extends AbstractFeatureTemplate<BOWScope> {

	static class BOWScope extends AbstractFactorScope {

		public final Set<String> expGroupBOW;

		public final Set<TypedBOW> propertyBOW;
		public final SlotType slotTypeContext;

		public BOWScope(AbstractFeatureTemplate<?> template, SlotType slotType, Set<String> expGroupBOW,
				Set<TypedBOW> popertyBOW) {
			super(template);
			this.expGroupBOW = expGroupBOW;
			this.propertyBOW = popertyBOW;
			this.slotTypeContext = slotType;
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
			result = prime * result + ((expGroupBOW == null) ? 0 : expGroupBOW.hashCode());
			result = prime * result + ((propertyBOW == null) ? 0 : propertyBOW.hashCode());
			result = prime * result + ((slotTypeContext == null) ? 0 : slotTypeContext.hashCode());
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
			BOWScope other = (BOWScope) obj;
			if (expGroupBOW == null) {
				if (other.expGroupBOW != null)
					return false;
			} else if (!expGroupBOW.equals(other.expGroupBOW))
				return false;
			if (propertyBOW == null) {
				if (other.propertyBOW != null)
					return false;
			} else if (!propertyBOW.equals(other.propertyBOW))
				return false;
			if (slotTypeContext == null) {
				if (other.slotTypeContext != null)
					return false;
			} else if (!slotTypeContext.equals(other.slotTypeContext))
				return false;
			return true;
		}

	}

	@Override
	public List<BOWScope> generateFactorScopes(State state) {
		List<BOWScope> factors = new ArrayList<>();

		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != EntityType.get("DefinedExperimentalGroup"))
				continue;

			addSFSFactor(factors, experimentalGroup, SlotType.get("hasOrganismModel"));
			addSFSFactor(factors, experimentalGroup, SlotType.get("hasInjuryModel"));
			addMFSFactor(factors, experimentalGroup, SlotType.get("hasTreatmentType"));

		}

		return factors;
	}

	private void addMFSFactor(List<BOWScope> factors, EntityTemplate experimentalGroup, SlotType slotType) {

		final MultiFillerSlot mfs = experimentalGroup.getMultiFillerSlot(slotType);

		if (!mfs.containsSlotFiller())
			return;

		for (AbstractAnnotation slotFillerAnnotation : mfs.getSlotFiller()) {

			final EntityTemplate property = slotFillerAnnotation.asInstanceOfEntityTemplate();

			final Set<String> expGroupBOW = BOWExtractor.getExpGroupBOW(experimentalGroup);

			final Set<TypedBOW> propertyBOW = BOWExtractor.extractTypedBOW(property);

			factors.add(new BOWScope(this, slotType, expGroupBOW, propertyBOW));
		}
	}

	private void addSFSFactor(List<BOWScope> factors, EntityTemplate experimentalGroup, SlotType slotType) {

		final SingleFillerSlot sfs = experimentalGroup.getSingleFillerSlot(slotType);

		if (!sfs.containsSlotFiller())
			return;

		final EntityTemplate property = sfs.getSlotFiller().asInstanceOfEntityTemplate();

		final Set<String> expGroupBOW = BOWExtractor.getExpGroupBOW(experimentalGroup);

		final Set<TypedBOW> propertyBOW = BOWExtractor.extractTypedBOW(property);

		factors.add(new BOWScope(this, slotType, expGroupBOW, propertyBOW));
	}

	@Override
	public void generateFeatureVector(Factor<BOWScope> factor) {

		/*
		 * Overlap
		 */
		for (TypedBOW typedBOW : factor.getFactorScope().propertyBOW) {
			int countOverlap = 0;
			for (String expBOWTerm : factor.getFactorScope().expGroupBOW) {
				if (typedBOW.bow.contains(expBOWTerm)) {
//					factor.getFeatureVector().set("Cntxt: " + factor.getFactorScope().slotTypeContext.slotName
//							+ ", OvrlpTrm: " + expBOWTerm + "-", true);
//					factor.getFeatureVector().set("Cntxt: " + factor.getFactorScope().slotTypeContext.slotName
//							+ ", Orgn: " + getOrigin(factor, typedBOW) + ", OvrlpTrm: " + expBOWTerm, true);
					countOverlap++;
				}
			}

			factor.getFeatureVector().set(
					"Cntxt: " + factor.getFactorScope().slotTypeContext.slotName + ", CntOvrlp: " + countOverlap, true);

		}

		/*
		 * Pair All
		 */
		for (TypedBOW typedBOW : factor.getFactorScope().propertyBOW) {
			for (String typedBOWTerm : typedBOW.bow) {
				for (String expBOWTerm : factor.getFactorScope().expGroupBOW) {
					factor.getFeatureVector().set("Cntxt: " + factor.getFactorScope().slotTypeContext.slotName
							+ ", TrmPr: " + expBOWTerm + "-" + typedBOWTerm, true);

					factor.getFeatureVector()
							.set("Cntxt: " + factor.getFactorScope().slotTypeContext.slotName + ", Orgn: "
									+ getOrigin(factor, typedBOW) + ", TrmPr: (Ex)" + expBOWTerm + "\t" + typedBOWTerm,
									true);
				}
			}
		}

	}

	public String getOrigin(Factor<BOWScope> factor, TypedBOW typedBOW) {
		return typedBOW.slotType != null ? typedBOW.slotType.slotName
				: factor.getFactorScope().slotTypeContext.slotName;
	}

}
