package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.orgmodel;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class OrganismModelRestrictionProvider {

	public enum EOrgModelModifications {
		SPECIES, SPECIES_GENDER, SPECIES_GENDER_WEIGHT, SPECIES_GENDER_WEIGHT_AGE_CATEGORY,
		SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE;
	}

	public static List<GoldModificationRule> getByRule(EOrgModelModifications modelModifications) {

		switch (modelModifications) {
		case SPECIES:
			return getSpecies();
		case SPECIES_GENDER:
			return getPlusGender();
		case SPECIES_GENDER_WEIGHT:
			return getPlusWeight();
		case SPECIES_GENDER_WEIGHT_AGE_CATEGORY:
			return getPlusAgeCategory();
		case SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE:
			return getPlusAge();
		}
		return null;

	}

	public static List<GoldModificationRule> getSpecies() {
		List<GoldModificationRule> rules = new ArrayList<>();
		SlotType.excludeAll();
		SCIOSlotTypes.hasOrganismSpecies.include();
		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						AnnotationBuilder.toAnnotation(goldAnnotation.getEntityType()));
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasOrganismSpecies,
						goldAnnotation.asInstanceOfEntityTemplate()
								.getSingleFillerSlot(SCIOSlotTypes.hasOrganismSpecies).getSlotFiller());

				if (newGold.asInstanceOfEntityTemplate().isEmpty())
					return null;

				return newGold;
			}
		});
		return rules;

	}

	public static List<GoldModificationRule> getPlusGender() {
		List<GoldModificationRule> rules = new ArrayList<>();
		SlotType.excludeAll();
		SCIOSlotTypes.hasOrganismSpecies.include();
		SCIOSlotTypes.hasGender.include();
		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						AnnotationBuilder.toAnnotation(goldAnnotation.getEntityType()));
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasOrganismSpecies,
						goldAnnotation.asInstanceOfEntityTemplate()
								.getSingleFillerSlot(SCIOSlotTypes.hasOrganismSpecies).getSlotFiller());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasGender, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasGender).getSlotFiller());

				if (newGold.asInstanceOfEntityTemplate().isEmpty())
					return null;
				return newGold;
			}
		});
		return rules;

	}

	public static List<GoldModificationRule> getPlusWeight() {
		List<GoldModificationRule> rules = new ArrayList<>();
		SlotType.excludeAll();
		SCIOSlotTypes.hasOrganismSpecies.include();
		SCIOSlotTypes.hasGender.include();
		SCIOSlotTypes.hasWeight.include();
		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						AnnotationBuilder.toAnnotation(goldAnnotation.getEntityType()));
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasOrganismSpecies,
						goldAnnotation.asInstanceOfEntityTemplate()
								.getSingleFillerSlot(SCIOSlotTypes.hasOrganismSpecies).getSlotFiller());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasGender, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasGender).getSlotFiller());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasWeight, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasWeight).getSlotFiller());

				if (newGold.asInstanceOfEntityTemplate().isEmpty())
					return null;
				return newGold;
			}
		});
		return rules;

	}

	public static List<GoldModificationRule> getPlusAgeCategory() {
		List<GoldModificationRule> rules = new ArrayList<>();
		SlotType.excludeAll();
		SCIOSlotTypes.hasOrganismSpecies.include();
		SCIOSlotTypes.hasGender.include();
		SCIOSlotTypes.hasWeight.include();
		SCIOSlotTypes.hasAgeCategory.include();
		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						AnnotationBuilder.toAnnotation(goldAnnotation.getEntityType()));
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasOrganismSpecies,
						goldAnnotation.asInstanceOfEntityTemplate()
								.getSingleFillerSlot(SCIOSlotTypes.hasOrganismSpecies).getSlotFiller());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasGender, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasGender).getSlotFiller());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasWeight, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasWeight).getSlotFiller());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasAgeCategory, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasAgeCategory).getSlotFiller());

				if (newGold.asInstanceOfEntityTemplate().isEmpty())
					return null;
				return newGold;
			}
		});
		return rules;

	}

	public static List<GoldModificationRule> getPlusAge() {
		List<GoldModificationRule> rules = new ArrayList<>();
		SlotType.excludeAll();
		SCIOSlotTypes.hasOrganismSpecies.include();
		SCIOSlotTypes.hasGender.include();
		SCIOSlotTypes.hasWeight.include();
		SCIOSlotTypes.hasAgeCategory.include();
		SCIOSlotTypes.hasAge.include();
		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						AnnotationBuilder.toAnnotation(goldAnnotation.getEntityType()));
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasOrganismSpecies,
						goldAnnotation.asInstanceOfEntityTemplate()
								.getSingleFillerSlot(SCIOSlotTypes.hasOrganismSpecies).getSlotFiller());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasGender, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasGender).getSlotFiller());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasWeight, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasWeight).getSlotFiller());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasAgeCategory, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasAgeCategory).getSlotFiller());
				newGold.setSingleSlotFiller(SCIOSlotTypes.hasAge, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasAge).getSlotFiller());

				if (newGold.asInstanceOfEntityTemplate().isEmpty())
					return null;
				return newGold;
			}
		});
		return rules;

	}

}
