package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.wrapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTypeAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

/**
 * Wrapper class for EntityTemplates that are ExperimentalGroups.
 * 
 * This class provides methods to work with experimental groups.
 * 
 * @author hterhors
 *
 */
public class DefinedExperimentalGroup {

	final private EntityTemplate experimentalGroup;

	public EntityTemplate get() {
		return experimentalGroup;
	}

	public DefinedExperimentalGroup(EntityTemplate experimentalGroup) {
		if (experimentalGroup.getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
			throw new IllegalArgumentException(
					"Argument is not of type DefinedExperimentalGroup: " + experimentalGroup.getEntityType());

		this.experimentalGroup = experimentalGroup;
	}

	/**
	 * Returns the group name annotations of that experimental group if hasGroupName
	 * slot is not frozen or excluded.
	 * 
	 * @return the annotation of the species or null
	 */
	public Set<DocumentLinkedAnnotation> getGroupNamesIfNotFrozen() {

		if (SCIOSlotTypes.hasGroupName.isIncluded() && !SCIOSlotTypes.hasGroupName.isFrozen())
			return Collections.emptySet();

		return experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasGroupName).getAutoCastSlotFiller();

	}

	/**
	 * /** Returns the group name annotations of that experimental group if
	 * hasGroupName slot is not excluded.
	 * 
	 * @return the annotation of the species or null
	 */
	public Set<DocumentLinkedAnnotation> getGroupNames() {

		if (SCIOSlotTypes.hasGroupName.isExcluded())
			return Collections.emptySet();

		return experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasGroupName).getAutoCastSlotFiller();
	}

	/**
	 * Returns the species annotation of the organism model of that experimental
	 * group if any, else null.
	 * 
	 * @return the annotation of the species or null
	 */
	public EntityTypeAnnotation getOrganismSpecies() {

		if (SCIOSlotTypes.hasOrganismModel.isIncluded()) {
			AbstractAnnotation orgModel = experimentalGroup.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel)
					.getSlotFiller();
			if (orgModel != null) {
				SingleFillerSlot species = orgModel.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasOrganismSpecies);
				if (species.containsSlotFiller())
					return (EntityTypeAnnotation) species.getSlotFiller();
			}
		}
		return null;
	}

	/**
	 * Returns the injury annotation of that experimental group if any, else null.
	 * 
	 * @return the annotation of the species or null
	 */
	public EntityTemplate getOrganisModel() {

		if (SCIOSlotTypes.hasOrganismModel.isIncluded()) {
			return (EntityTemplate) experimentalGroup.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel)
					.getSlotFiller();
		}

		return null;
	}

	/**
	 * Returns the injury annotation of that experimental group if any, else null.
	 * 
	 * @return the annotation of the species or null
	 */
	public EntityTemplate getInjury() {

		if (SCIOSlotTypes.hasInjuryModel.isIncluded()) {
			return (EntityTemplate) experimentalGroup.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).getSlotFiller();
		}

		return null;
	}

	public Set<EntityTemplate> getTreatments() {

		if (SCIOSlotTypes.hasTreatmentType.isIncluded()) {
			return experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller().stream()
					.map(a -> a.asInstanceOfEntityTemplate()).collect(Collectors.toSet());
		}

		return null;
	}

	public boolean isEmpty() {
		return getOrganisModel() == null
				|| getInjury() == null && (getTreatments() == null || getTreatments().isEmpty());
	}

	/**
	 * Returns the compound annotations from this experimental group if any, else an
	 * empty set.
	 * 
	 * @return
	 */
	public Set<EntityTemplate> getCompounds() {
		Set<EntityTemplate> compounds = null;
		if (SCIOSlotTypes.hasTreatmentType.isIncluded()) {
			Set<AbstractAnnotation> treatmentTypes = experimentalGroup
					.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller();

			for (AbstractAnnotation treatment : treatmentTypes) {

				if (treatment.getEntityType() == SCIOEntityTypes.compoundTreatment) {

					SingleFillerSlot compound = treatment.asInstanceOfEntityTemplate()
							.getSingleFillerSlot(SCIOSlotTypes.hasCompound);
					if (compound.containsSlotFiller()) {
						if (compounds == null)
							compounds = new HashSet<>();
						compounds.add((EntityTemplate) compound.getSlotFiller());
					}

				}
			}
		}
		if (compounds == null)
			compounds = Collections.emptySet();
		return compounds;
	}

	/**
	 * Returns non compound based treatments from this experimental group.
	 * 
	 * @return
	 */
	public Set<EntityTemplate> getNonCompoundTreatments() {
		Set<EntityTemplate> treatments = null;
		if (SCIOSlotTypes.hasTreatmentType.isIncluded()) {
			Set<AbstractAnnotation> treatmentTypes = experimentalGroup
					.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller();

			for (AbstractAnnotation treatment : treatmentTypes) {

				if (treatment.getEntityType() != SCIOEntityTypes.compoundTreatment) {
					if (treatments == null)
						treatments = new HashSet<>();
					treatments.add((EntityTemplate) treatment);
				}
			}
		}
		if (treatments == null)
			treatments = Collections.emptySet();
		return treatments;
	}

	/**
	 * Returns non compound based treatments and for compound treatments the
	 * compound from this experimental group.
	 * 
	 * @return
	 */
	public Set<EntityTemplate> getRelevantTreatments() {
		Set<EntityTemplate> treatments = null;
		if (SCIOSlotTypes.hasTreatmentType.isIncluded()) {
			Set<AbstractAnnotation> treatmentTypes = experimentalGroup
					.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller();

			for (AbstractAnnotation treatment : treatmentTypes) {
				if (treatment.getEntityType() == SCIOEntityTypes.compoundTreatment) {
					SingleFillerSlot compound = treatment.asInstanceOfEntityTemplate()
							.getSingleFillerSlot(SCIOSlotTypes.hasCompound);
					if (compound.containsSlotFiller()) {
						if (treatments == null)
							treatments = new HashSet<>();
						treatments.add((EntityTemplate) compound.getSlotFiller());
					}
				} else {
					if (treatments == null)
						treatments = new HashSet<>();
					treatments.add((EntityTemplate) treatment);
				}
			}
		}
		if (treatments == null)
			treatments = Collections.emptySet();
		return treatments;
	}

	/**
	 * Returns the DocumentLinkedAnnotation of the root annotation if any, else
	 * null.
	 * 
	 * @return
	 */
	public DocumentLinkedAnnotation getGroupRootName() {

		AbstractAnnotation rootAnnotation = get().getRootAnnotation();
		if (rootAnnotation.isInstanceOfDocumentLinkedAnnotation())
			return rootAnnotation.asInstanceOfDocumentLinkedAnnotation();

		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((experimentalGroup == null) ? 0 : experimentalGroup.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefinedExperimentalGroup other = (DefinedExperimentalGroup) obj;
		if (experimentalGroup == null) {
			if (other.experimentalGroup != null)
				return false;
		} else if (!experimentalGroup.equals(other.experimentalGroup))
			return false;
		return true;
	}

}
