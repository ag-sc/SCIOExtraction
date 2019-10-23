package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.slots.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.slots.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.ExGrAllUsedTemplate.AllUsedScope;
import edu.stanford.nlp.ling.HasOriginalText;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class ExGrAllUsedTemplate extends AbstractFeatureTemplate<AllUsedScope> {

	private Map<Instance, Map<SlotType, Integer>> numToPredictMap;

	@Override
	public void initalize(Object[] parameter) {
		this.numToPredictMap = (Map<Instance, Map<SlotType, Integer>>) parameter[0];
	}

	static class AllUsedScope extends AbstractFactorScope {

		final public boolean allUsed;

		final public SlotType slotTypeContext;

		public AllUsedScope(ExGrAllUsedTemplate exGrAllUsedTemplate, SlotType slotType, boolean allUsed) {
			super(exGrAllUsedTemplate);
			this.allUsed = allUsed;
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
			result = prime * result + (allUsed ? 1231 : 1237);
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
			AllUsedScope other = (AllUsedScope) obj;
			if (allUsed != other.allUsed)
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
	public List<AllUsedScope> generateFactorScopes(State state) {
		List<AllUsedScope> factors = new ArrayList<>();

		Set<EntityTemplate> predictedOrganismModels = new HashSet<>();
		Set<EntityTemplate> predictedInjuryModels = new HashSet<>();
		Set<EntityTemplate> predictedTreatmentTypes = new HashSet<>();

		/*
		 * global count
		 */
		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != EntityType.get("DefinedExperimentalGroup"))
				continue;

			EntityTemplate orgM = collectSFS(experimentalGroup, SlotType.get("hasOrganismModel"));
			if (orgM != null)
				predictedOrganismModels.add(orgM);

			EntityTemplate injuryM = collectSFS(experimentalGroup, SlotType.get("hasInjuryModel"));
			if (injuryM != null)
				predictedInjuryModels.add(injuryM);

			predictedTreatmentTypes.addAll(collectMFS(experimentalGroup, SlotType.get("hasTreatmentType")));

		}

		addFactor(factors, state, SlotType.get("hasOrganismModel"), predictedOrganismModels.size());
		addFactor(factors, state, SlotType.get("hasInjuryModel"), predictedInjuryModels.size());
		addFactor(factors, state, SlotType.get("hasTreatmentType"), predictedTreatmentTypes.size());

		return factors;
	}

	public void addFactor(List<AllUsedScope> factors, State state, SlotType slotType, int numOfPredicted) {
		final int numToPredict = numToPredictMap.get(state.getInstance()).get(slotType).intValue();
		factors.add(new AllUsedScope(this, slotType, numToPredict == numOfPredicted));
	}

	public EntityTemplate collectSFS(EntityTemplate experimentalGroup, SlotType slotType) {
		final SingleFillerSlot sfs = experimentalGroup.getSingleFillerSlot(slotType);

		if (sfs.containsSlotFiller())
			return sfs.getSlotFiller().asInstanceOfEntityTemplate();

		return null;
	}

	public Set<EntityTemplate> collectMFS(EntityTemplate experimentalGroup, SlotType slotType) {
		return experimentalGroup.getMultiFillerSlot(slotType).getSlotFiller().stream()
				.map(e -> e.asInstanceOfEntityTemplate()).collect(Collectors.toSet());
	}

	@Override
	public void generateFeatureVector(Factor<AllUsedScope> factor) {

		factor.getFeatureVector().set("Cntxt: " + factor.getFactorScope().slotTypeContext.slotName,
				factor.getFactorScope().allUsed);
	}

}
