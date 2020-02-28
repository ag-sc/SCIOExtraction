package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.wrapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

	public DefinedExperimentalGroup(EntityTemplate experimentalGroup) {
		if (experimentalGroup.getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
			throw new IllegalArgumentException(
					"Argument is not of type DefinedExperimentalGroup: " + experimentalGroup.getEntityType());

		this.experimentalGroup = experimentalGroup;
	}

	/**
	 * Returns the group name annotation of the organism model of that experimental
	 * group if any, else null.
	 * 
	 * @return the annotation of the species or null
	 */
	public Set<DocumentLinkedAnnotation> getUnFrozenGroupNames() {

		if (SCIOSlotTypes.hasGroupName.isIncluded() && !SCIOSlotTypes.hasGroupName.isFrozen())
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
	public EntityTemplate getInjury() {

		if (SCIOSlotTypes.hasInjuryModel.isIncluded()) {
			return (EntityTemplate) experimentalGroup.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).getSlotFiller();
		}

		return null;
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

}
