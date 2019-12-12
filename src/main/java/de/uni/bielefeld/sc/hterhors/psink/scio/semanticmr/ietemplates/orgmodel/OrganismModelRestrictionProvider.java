package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.orgmodel;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;

public class OrganismModelRestrictionProvider {

	enum EOrgModelModifications {
		SPECIES, SPECIES_GENDER, SPECIES_GENDER_WEIGHT, SPECIES_GENDER_WEIGHT_AGE_CATEGORY,
		SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE;
	}

	private static final SlotType speciesSlot = SlotType.get("hasOrganismSpecies");
	private static final SlotType genderSlot = SlotType.get("hasGender");
	private static final SlotType ageCategorySlot = SlotType.get("hasAgeCategory");
	private static final SlotType ageSlot = SlotType.get("hasAge");
	private static final SlotType weightSlot = SlotType.get("hasWeight");

	public static List<GoldModificationRule> getRule(EOrgModelModifications modelModifications) {

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

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						AnnotationBuilder.toAnnotation(goldAnnotation.getEntityType()));
				newGold.setSingleSlotFiller(speciesSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(speciesSlot).getSlotFiller());

				if (newGold.asInstanceOfEntityTemplate().isEmpty())
					return null;

				return newGold;
			}
		});
		return rules;

	}

	public static List<GoldModificationRule> getPlusGender() {
		List<GoldModificationRule> rules = new ArrayList<>();

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						AnnotationBuilder.toAnnotation(goldAnnotation.getEntityType()));
				newGold.setSingleSlotFiller(speciesSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(speciesSlot).getSlotFiller());
				newGold.setSingleSlotFiller(genderSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(genderSlot).getSlotFiller());

				if (newGold.asInstanceOfEntityTemplate().isEmpty())
					return null;
				return newGold;
			}
		});
		return rules;

	}

	public static List<GoldModificationRule> getPlusWeight() {
		List<GoldModificationRule> rules = new ArrayList<>();

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						AnnotationBuilder.toAnnotation(goldAnnotation.getEntityType()));
				newGold.setSingleSlotFiller(speciesSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(speciesSlot).getSlotFiller());
				newGold.setSingleSlotFiller(genderSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(genderSlot).getSlotFiller());
				newGold.setSingleSlotFiller(weightSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(weightSlot).getSlotFiller());

				if (newGold.asInstanceOfEntityTemplate().isEmpty())
					return null;
				return newGold;
			}
		});
		return rules;

	}

	public static List<GoldModificationRule> getPlusAgeCategory() {
		List<GoldModificationRule> rules = new ArrayList<>();

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						AnnotationBuilder.toAnnotation(goldAnnotation.getEntityType()));
				newGold.setSingleSlotFiller(speciesSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(speciesSlot).getSlotFiller());
				newGold.setSingleSlotFiller(genderSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(genderSlot).getSlotFiller());
				newGold.setSingleSlotFiller(weightSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(weightSlot).getSlotFiller());
				newGold.setSingleSlotFiller(ageCategorySlot, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(ageCategorySlot).getSlotFiller());

				if (newGold.asInstanceOfEntityTemplate().isEmpty())
					return null;
				return newGold;
			}
		});
		return rules;

	}

	public static List<GoldModificationRule> getPlusAge() {
		List<GoldModificationRule> rules = new ArrayList<>();

		rules.add(new GoldModificationRule() {

			@Override
			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {

				EntityTemplate newGold = new EntityTemplate(
						AnnotationBuilder.toAnnotation(goldAnnotation.getEntityType()));
				newGold.setSingleSlotFiller(speciesSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(speciesSlot).getSlotFiller());
				newGold.setSingleSlotFiller(genderSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(genderSlot).getSlotFiller());
				newGold.setSingleSlotFiller(weightSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(weightSlot).getSlotFiller());
				newGold.setSingleSlotFiller(ageCategorySlot, goldAnnotation.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(ageCategorySlot).getSlotFiller());
				newGold.setSingleSlotFiller(ageSlot,
						goldAnnotation.asInstanceOfEntityTemplate().getSingleFillerSlot(ageSlot).getSlotFiller());

				if (newGold.asInstanceOfEntityTemplate().isEmpty())
					return null;
				return newGold;
			}
		});
		return rules;

	}

}
