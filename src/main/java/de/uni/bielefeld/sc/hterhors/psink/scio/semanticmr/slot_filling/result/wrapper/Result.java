package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.wrapper.DefinedExperimentalGroup;

public class Result {

	final private EntityTemplate result;

	public Result(AbstractAnnotation result) {
		if (result.getEntityType() != SCIOEntityTypes.result)
			throw new IllegalArgumentException("Argument is not of type Result: " + result.getEntityType());

		this.result = result.asInstanceOfEntityTemplate();
	}

	public List<DefinedExperimentalGroup> getDefinedExperimentalGroups() {

		List<DefinedExperimentalGroup> groups = new ArrayList<>();

		if (!SCIOSlotTypes.hasTargetGroup.isExcluded()) {
			AbstractAnnotation target = result.getSingleFillerSlot(SCIOSlotTypes.hasTargetGroup).getSlotFiller();

			if (target != null)
				groups.add(new DefinedExperimentalGroup(target.asInstanceOfEntityTemplate()));
		}

		if (!SCIOSlotTypes.hasReferenceGroup.isExcluded()) {

			AbstractAnnotation reference = result.getSingleFillerSlot(SCIOSlotTypes.hasReferenceGroup).getSlotFiller();

			if (reference != null)
				groups.add(new DefinedExperimentalGroup(reference.asInstanceOfEntityTemplate()));
		}

		return groups;
	}

	public EntityTemplate getTrend() {

		SingleFillerSlot trendSlot = result.getSingleFillerSlot(SCIOSlotTypes.hasTrend);

		if (trendSlot.containsSlotFiller())
			return trendSlot.getSlotFiller().asInstanceOfEntityTemplate();

		return null;
	}

	public EntityTemplate getInvestigationMethod() {

		SingleFillerSlot investSlot = result.getSingleFillerSlot(SCIOSlotTypes.hasInvestigationMethod);

		if (investSlot.containsSlotFiller())
			return investSlot.getSlotFiller().isInstanceOfEntityTemplate()
					? investSlot.getSlotFiller().asInstanceOfEntityTemplate()
					: null;

		return null;
	}

}
