package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.type_based;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.type_based.TB_SameOrganismModelTemplate.SlotIsFilledScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class TB_SameOrganismModelTemplate extends AbstractFeatureTemplate<SlotIsFilledScope> {

	static class SlotIsFilledScope extends AbstractFactorScope {

		final SlotType slot;
		final int numberOfSlotFiller;

		public SlotIsFilledScope(AbstractFeatureTemplate<SlotIsFilledScope> template, SlotType slot,
				int numberOfSlotFiller) {
			super(template);
			this.numberOfSlotFiller = numberOfSlotFiller;
			this.slot = slot;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + numberOfSlotFiller;
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
			SlotIsFilledScope other = (SlotIsFilledScope) obj;
			if (numberOfSlotFiller != other.numberOfSlotFiller)
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

	private static final String PREFIX = "SOMT\t";

	@Override
	public List<SlotIsFilledScope> generateFactorScopes(State state) {
		List<SlotIsFilledScope> factors = new ArrayList<>();

		Map<SlotType, Set<EntityType>> collect = new HashMap<>();

		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
				continue;

			if (SCIOSlotTypes.hasOrganismModel.isIncluded()) {
				AbstractAnnotation orgModel = experimentalGroup.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel)
						.getSlotFiller();

				if (orgModel != null)
					for (Entry<SlotType, SingleFillerSlot> f : orgModel.asInstanceOfEntityTemplate()
							.getSingleFillerSlots().entrySet()) {
						collect.putIfAbsent(f.getKey(), new HashSet<>());
						collect.get(f.getKey()).add(f.getValue().getSlotFiller().getEntityType());
					}

			}

		}
		for (Entry<SlotType, Set<EntityType>> f : collect.entrySet()) {

			factors.add(new SlotIsFilledScope(this, f.getKey(), f.getValue().size()));
		}

		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<SlotIsFilledScope> factor) {

		final boolean slotIsFilled = factor.getFactorScope().numberOfSlotFiller == 1;

		final String slotName = factor.getFactorScope().slot.name;

		factor.getFeatureVector().set(PREFIX + slotName + " all same", slotIsFilled);
		factor.getFeatureVector().set(PREFIX + slotName + " different", !slotIsFilled);
		factor.getFeatureVector().set(PREFIX + slotName + " num diff " + factor.getFactorScope().numberOfSlotFiller,
				true);

	}

}
