package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates_typebased;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer.ESamplingMode;
import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTypeAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates_typebased.RemainingTypesTemplate.RemainingTypesScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.AutomatedSectionifcation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.preprocessing.AutomatedSectionifcation.ESection;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class RemainingTypesTemplate extends AbstractFeatureTemplate<RemainingTypesScope> {

	static class RemainingTypesScope extends AbstractFactorScope {

		final public Instance instance;

		final public Set<EntityType> assignedTypes;

		public RemainingTypesScope(AbstractFeatureTemplate<?> template, Instance instance,
				Set<EntityType> assignedTypes) {
			super(template);
			this.instance = instance;
			this.assignedTypes = assignedTypes;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((assignedTypes == null) ? 0 : assignedTypes.hashCode());
			result = prime * result + ((instance == null) ? 0 : instance.hashCode());
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
			RemainingTypesScope other = (RemainingTypesScope) obj;
			if (assignedTypes == null) {
				if (other.assignedTypes != null)
					return false;
			} else if (!assignedTypes.equals(other.assignedTypes))
				return false;
			if (instance == null) {
				if (other.instance != null)
					return false;
			} else if (!instance.equals(other.instance))
				return false;
			return true;
		}

		@Override
		public int implementHashCode() {
			return 0;
		}

		@Override
		public boolean implementEquals(Object obj) {
			return false;
		}

	}

	@Override
	public List<RemainingTypesScope> generateFactorScopes(State state) {

		List<RemainingTypesScope> factors = new ArrayList<>();

		Set<EntityType> assignedTypes = getAssignedTypes(state);

		factors.add(new RemainingTypesScope(this, state.getInstance(), assignedTypes));

		return factors;
	}

	private Set<DocumentLinkedAnnotation> getRemainingAnnotations(Instance instance, Set<EntityType> assignedTypes) {
		Set<DocumentLinkedAnnotation> remainingTypeAnnotations = new HashSet<>();

		for (EntityType entityType : EntityType.getEntityTypes()) {

			if (assignedTypes.contains(entityType))
				continue;

			for (EntityTypeAnnotation documentLinkedAnnotation : instance
					.getEntityTypeCandidates(ESamplingMode.ANNOTATION_BASED, entityType)) {

				if (documentLinkedAnnotation.isInstanceOfDocumentLinkedAnnotation())
					remainingTypeAnnotations.add(documentLinkedAnnotation.asInstanceOfDocumentLinkedAnnotation());

			}
		}
		return remainingTypeAnnotations;
	}

	private Set<EntityType> getAssignedTypes(State state) {
		Set<EntityType> assignedTypes = new HashSet<>();

		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
				continue;

			EntityTemplate orgM = collectSFS(experimentalGroup, SCIOSlotTypes.hasOrganismModel);
			if (orgM != null)
				assignedTypes.add(orgM.getEntityType());

			EntityTemplate injuryM = collectSFS(experimentalGroup, SCIOSlotTypes.hasInjuryModel);
			if (injuryM != null)
				assignedTypes.add(injuryM.getEntityType());

			for (EntityTemplate documentLinkedAnnotation : collectMFS(experimentalGroup,
					SCIOSlotTypes.hasTreatmentType)) {
				assignedTypes.add(documentLinkedAnnotation.getEntityType());
			}

		}
		return assignedTypes;
	}

	private EntityTemplate collectSFS(EntityTemplate experimentalGroup, SlotType slotType) {
		if (slotType.isExcluded())
			return null;

		final SingleFillerSlot sfs = experimentalGroup.getSingleFillerSlot(slotType);

		if (sfs.containsSlotFiller())
			return sfs.getSlotFiller().asInstanceOfEntityTemplate();

		return null;
	}

	private Set<EntityTemplate> collectMFS(EntityTemplate experimentalGroup, SlotType slotType) {

		if (slotType.isExcluded())
			Collections.emptySet();

		return experimentalGroup.getMultiFillerSlot(slotType).getSlotFiller().stream()
				.map(e -> e.asInstanceOfEntityTemplate()).collect(Collectors.toSet());
	}

	@Override
	public void generateFeatureVector(Factor<RemainingTypesScope> factor) {

		Instance instance = factor.getFactorScope().instance;
		Set<EntityType> assignedTypes = factor.getFactorScope().assignedTypes;

		Set<DocumentLinkedAnnotation> remainingTypeAnnotations = getRemainingAnnotations(instance, assignedTypes);

		AutomatedSectionifcation sectionification = AutomatedSectionifcation.getInstance(instance);

		Map<EntityType, Set<ESection>> sectionsPerType = new HashMap<>();
		for (DocumentLinkedAnnotation documentLinkedAnnotation : remainingTypeAnnotations) {
			sectionsPerType.putIfAbsent(documentLinkedAnnotation.entityType, new HashSet<>());
			sectionsPerType.get(documentLinkedAnnotation.entityType)
					.add(sectionification.getSection(documentLinkedAnnotation.getSentenceIndex()));
		}

		for (Entry<EntityType, Set<ESection>> feature : sectionsPerType.entrySet()) {
			for (ESection section : feature.getValue()) {

				factor.getFeatureVector()
						.set("RTT: " + feature.getKey().name + " is missing in section " + section.name(), true);
			}
		}

	}

}
