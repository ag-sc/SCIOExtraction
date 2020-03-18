package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.helper.bow.TB_BOWExtractor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.helper.bow.TB_TypedBOW;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.helper.bow.TypedBOW;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based.TB_ExGrBOWInverseTemplate.BOWScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class TB_ExGrBOWInverseTemplate extends AbstractFeatureTemplate<BOWScope> {

	private static final Set<String> SPECIAL_KEEPWORDS = new HashSet<>(
			Arrays.asList("media", "single", "alone", "only", "control", "sham", "low", "high"));

	private static final Set<String> ADDITIONAL_STOPWORDS = new HashSet<>(Arrays.asList("transplantation", "untreated",
			"is", "untrained", "blank", "transplanted", "transection", "grafts", "normal", "injection", "injections",
			"cultured", "cords", "uninfected", "injected", "additional", "ca", "observed", "grafted", "graft", "cells",
			"are", "effects", "gray", "cord", "spinal", "identifi", "cation", "n", "treated", "treatment", "",
			"received", "the", "injured", "all", "lesioned", "fi", "rst", "first", "second", "third", "fourth", "group",
			"animals", "rats", "in", "same", "individual", "groups", "were"));
	private static Set<String> ALL_STOPWORDS;

	static {
		try {
			ALL_STOPWORDS = new HashSet<>(Files.readAllLines(new File("src/main/resources/top1000.csv").toPath()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		ALL_STOPWORDS.addAll(ADDITIONAL_STOPWORDS);
		ALL_STOPWORDS.removeAll(SPECIAL_KEEPWORDS);
	}

	static class BOWScope extends AbstractFactorScope {

		public final Set<String> expGroupBOW;

		public final Set<TB_TypedBOW> propertyBOW;
		public final SlotType slotTypeContext;

		public BOWScope(AbstractFeatureTemplate<?> template, SlotType slotType, Set<String> expGroupBOW,
				Set<TB_TypedBOW> popertyBOW) {
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

			if (experimentalGroup.getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
				continue;

			final Set<String> expGroupBOW = TB_BOWExtractor.getExpGroupPlusNameBOW(experimentalGroup);

			for (EntityTemplate experimentalGroup2 : super.<EntityTemplate>getPredictedAnnotations(state)) {

				if (experimentalGroup2 == experimentalGroup)
					continue;

				addSFSFactor(factors, state.getInstance(), expGroupBOW, experimentalGroup2,
						SCIOSlotTypes.hasOrganismModel);
				addSFSFactor(factors, state.getInstance(), expGroupBOW, experimentalGroup2,
						SCIOSlotTypes.hasInjuryModel);
				addMFSFactor(factors, state.getInstance(), expGroupBOW, experimentalGroup2,
						SCIOSlotTypes.hasTreatmentType);

			}
		}

		return factors;
	}

	private void addMFSFactor(List<BOWScope> factors, Instance instance, Set<String> expGroupBOW,
			EntityTemplate experimentalGroup, SlotType slotType) {
		if (slotType.isExcluded())
			return;

		final MultiFillerSlot mfs = experimentalGroup.getMultiFillerSlot(slotType);

		if (!mfs.containsSlotFiller())
			return;

		for (AbstractAnnotation slotFillerAnnotation : mfs.getSlotFiller()) {

			final EntityTemplate property = slotFillerAnnotation.asInstanceOfEntityTemplate();

			final Set<TB_TypedBOW> propertyBOW = TB_BOWExtractor.extractTypedBOW(instance, property);

			factors.add(new BOWScope(this, slotType, expGroupBOW, propertyBOW));

		}
	}

	private void addSFSFactor(List<BOWScope> factors, Instance instance, Set<String> expGroupBOW,
			EntityTemplate experimentalGroup, SlotType slotType) {
		if (slotType.isExcluded())
			return;

		final SingleFillerSlot sfs = experimentalGroup.getSingleFillerSlot(slotType);

		if (!sfs.containsSlotFiller())
			return;

		final EntityTemplate property = sfs.getSlotFiller().asInstanceOfEntityTemplate();

		final Set<TB_TypedBOW> propertyBOW = TB_BOWExtractor.extractTypedBOW(instance, property);

		factors.add(new BOWScope(this, slotType, expGroupBOW, propertyBOW));
	}

	@Override
	public void generateFeatureVector(Factor<BOWScope> factor) {

		/*
		 * Pair All
		 */
		for (String expBOWTerm : factor.getFactorScope().expGroupBOW) {
			/*
			 * Worse performance.
			 */
			expBOWTerm = normalize(expBOWTerm);

			if (ALL_STOPWORDS.contains(expBOWTerm))
				continue;
			List<String> nGrams1 = getNGrams(expBOWTerm);

			for (TB_TypedBOW typedBOW : factor.getFactorScope().propertyBOW) {
				for (String typedBOWTerm : typedBOW.bow) {

					typedBOWTerm = normalize(typedBOWTerm);

					if (ALL_STOPWORDS.contains(typedBOWTerm))
						continue;

					List<String> nGrams2 = getNGrams(typedBOWTerm);
					if (expBOWTerm.compareTo(typedBOWTerm) < 0) {
						termPair(factor, expBOWTerm, typedBOWTerm);
						nGramPair(factor, nGrams1, nGrams2);
					} else {
						termPair(factor, typedBOWTerm, expBOWTerm);
						nGramPair(factor, nGrams2, nGrams1);
					}
//					
//					if (expBOWTerm.compareTo(typedBOWTerm) < 0)
//						factor.getFeatureVector().set("INVERSE Context: " + factor.getFactorScope().slotTypeContext.name
//								+ ", sorted TermPair: " + typedBOWTerm + "\t" + expBOWTerm, true);
//					else
//						factor.getFeatureVector().set("INVERSE Context: " + factor.getFactorScope().slotTypeContext.name
//								+ ", sorted TermPair: " + expBOWTerm + "\t" + typedBOWTerm, true);

				}
			}
		}
	}

	private void nGramPair(Factor<BOWScope> factor, List<String> expBOWTerm, List<String> typedBOWTerm) {

		for (int i = 0; i < expBOWTerm.size(); i++) {
			for (int j = 0; j < typedBOWTerm.size(); j++) {
				termPair(factor, expBOWTerm.get(i), typedBOWTerm.get(j));
			}
		}

	}

	/**
	 * Return tokenized version of a string.
	 *
	 * @param input
	 * @return tokenized version of a string
	 */
	public final ArrayList<String> getNGrams(final String input) {
		final ArrayList<String> returnVect = new ArrayList<String>();
		final StringBuffer adjustedString = new StringBuffer();
		adjustedString.append("#");
		adjustedString.append("#");
		adjustedString.append(input);
		adjustedString.append("#");
		adjustedString.append("#");
		int curPos = 0;
		final int length = adjustedString.length() - 2;
		while (curPos < length) {
			final String term = adjustedString.substring(curPos, curPos + 3);
			returnVect.add(term);
			curPos++;
		}

		return returnVect;
	}

	public void termPair(Factor<BOWScope> factor, String expBOWTerm, String typedBOWTerm) {
		factor.getFeatureVector().set("INVERSE Context: " + factor.getFactorScope().slotTypeContext.name
				+ ", sorted TermPair: " + expBOWTerm + "\t" + typedBOWTerm, true);
	}

	private String normalize(String expBOWTerm) {
		return toSingular(toLowerIfNotUpper(expBOWTerm));
	}

	private String toSingular(String finding) {
		if (finding.endsWith("s"))
			return finding.substring(0, finding.length() - 1);
		if (finding.endsWith("ies"))
			return finding.substring(0, finding.length() - 3) + "y";
		return finding;
	}

	/**
	 * Converts a string to lowercase if it is not in uppercase
	 * 
	 * @param s
	 * @return
	 */
	private String toLowerIfNotUpper(String s) {
		if (s.matches("[a-z]?[A-Z\\d\\W]+s?"))
			return s;

		return s.toLowerCase();
	}

	private String getOrigin(Factor<BOWScope> factor, TypedBOW typedBOW) {
		return typedBOW.slotType != null ? typedBOW.slotType.name : factor.getFactorScope().slotTypeContext.name;
	}
}
