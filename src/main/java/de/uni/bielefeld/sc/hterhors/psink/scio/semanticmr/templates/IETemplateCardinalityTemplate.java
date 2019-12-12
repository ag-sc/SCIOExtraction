package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.IETemplateCardinalityTemplate.IETemplateCardinalityScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class IETemplateCardinalityTemplate extends AbstractFeatureTemplate<IETemplateCardinalityScope> {
	/**
	 * Captures cardinality of evidences in text.
	 */
	final private static String TEMPLATE_0 = "Textual_evidence_for_%s_in_%s = %d && %s_cardinality = %d";

	/**
	 * Captures cardinality of different types of a super type (e.g. gender) in
	 * text.
	 */
	final private static String TEMPLATE_1 = "Number_of_%s_in_%s = %d && %s_cardinality = %d";

	/**
	 * Sets num of types of class with number of entities in relation. Measures how
	 * many types are already used.
	 */
	final private static String TEMPLATE_2 = "Unused_%s in %s = %d";

	static class IETemplateCardinalityScope extends AbstractFactorScope {

		final Instance document;
		final int rootCardinality;
		final EntityType rootClass;
		final EntityType propertyClass;

		public IETemplateCardinalityScope(AbstractFeatureTemplate<IETemplateCardinalityScope> template,
				Instance document, int rootCardinality, EntityType rootClass, EntityType propertyClass) {
			super(template);
			this.document = document;
			this.rootCardinality = rootCardinality;
			this.rootClass = rootClass;
			this.propertyClass = propertyClass;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((document == null) ? 0 : document.hashCode());
			result = prime * result + ((propertyClass == null) ? 0 : propertyClass.hashCode());
			result = prime * result + rootCardinality;
			result = prime * result + ((rootClass == null) ? 0 : rootClass.hashCode());
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
			IETemplateCardinalityScope other = (IETemplateCardinalityScope) obj;
			if (document == null) {
				if (other.document != null)
					return false;
			} else if (!document.equals(other.document))
				return false;
			if (propertyClass == null) {
				if (other.propertyClass != null)
					return false;
			} else if (!propertyClass.equals(other.propertyClass))
				return false;
			if (rootCardinality != other.rootCardinality)
				return false;
			if (rootClass == null) {
				if (other.rootClass != null)
					return false;
			} else if (!rootClass.equals(other.rootClass))
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
	public List<IETemplateCardinalityScope> generateFactorScopes(State state) {
		List<IETemplateCardinalityScope> factors = new ArrayList<>();

		final Map<EntityType, Integer> countRootClasses = new HashMap<>();

		/*
		 * If there is only one rootClass (e.g. OrganismModel) the entry of the map for
		 * that class should be equal to state.getPredictedResult.getEntities().size();
		 */
		state.getCurrentPredictions().getAnnotations().stream().map(a -> a.getEntityType())
				.forEach(s -> countRootClasses.put(s, 1 + countRootClasses.getOrDefault(s, 0)));

		for (EntityTemplate annotation : super.<EntityTemplate>getPredictedAnnotations(state)) {

			final int rootCardinality = countRootClasses.get(annotation.getEntityType());

			for (Entry<SlotType, SingleFillerSlot> singleFillerSlotAnnotation : annotation.getSingleFillerSlots()
					.entrySet()) {

				if (!singleFillerSlotAnnotation.getValue().containsSlotFiller())
					continue;
				
				factors.add(new IETemplateCardinalityScope(this, state.getInstance(), rootCardinality,
						annotation.getEntityType(),
						singleFillerSlotAnnotation.getValue().getSlotFiller().getEntityType()));

			}

			for (Entry<SlotType, MultiFillerSlot> multiFillerSlotAnnotation : annotation.getMultiFillerSlots()
					.entrySet()) {

				if (!multiFillerSlotAnnotation.getValue().containsSlotFiller())
					continue;

				for (AbstractAnnotation msf : multiFillerSlotAnnotation.getValue().getSlotFiller()) {

					factors.add(new IETemplateCardinalityScope(this, state.getInstance(), rootCardinality,
							annotation.getEntityType(), msf.getEntityType()));

				}
			}
		}

		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<IETemplateCardinalityScope> factor) {

//		DoubleVector featureVector = factor.getFeatureVector();
//
//		final Set<NERLClassAnnotation> evidenceMentions = factor.getFactorScope().document.getEntityAnnotations()
//				.getClassAnnotations(factor.getFactorScope().propertyClass);
//		int propertyEvidence = evidenceMentions == null ? 0 : evidenceMentions.size();
//
//		featureVector.set("#OfRootClasses = " + factor.getFactorScope().rootCardinality, true);
//		featureVector.set("#OfRootClasses < 6", factor.getFactorScope().rootCardinality < 6);
//
//		featureVector.set(String.format(TEMPLATE_0, factor.getFactorScope().propertyClass.getSimpleName(),
//				factor.getFactorScope().rootClass, propertyEvidence, factor.getFactorScope().rootClass,
//				factor.getFactorScope().rootCardinality), true);
//
//		/*
//		 * Add type of the field which is the root of the actual class. (More general)
//		 */
//		Set<Class<? extends IOBIEThing>> propertyRootClassTypes = ReflectionUtils
//				.getSuperRootClasses(factor.getFactorScope().propertyClass);
//
//		for (Class<? extends IOBIEThing> propertyRootClassType : propertyRootClassTypes) {
//
//			featureVector.set(String.format(TEMPLATE_0, propertyRootClassType.getSimpleName(),
//					factor.getFactorScope().rootClass, propertyEvidence, factor.getFactorScope().rootClass,
//					factor.getFactorScope().rootCardinality), true);
//
//			if (ReflectionUtils.getAssignableSubClasses(propertyRootClassType) == null
//					|| ReflectionUtils.getAssignableSubClasses(propertyRootClassType).isEmpty())
//				return;
//
//			int countDifferentSubclassEvidences = 0;
//			for (Class<? extends IOBIEThing> subClass : ReflectionUtils
//					.getAssignableSubClasses(propertyRootClassType)) {
//				final Set<NERLClassAnnotation> evidenceList = factor.getFactorScope().document.getEntityAnnotations()
//						.getClassAnnotations(subClass);
//				countDifferentSubclassEvidences += evidenceList == null || evidenceList.isEmpty() ? 0 : 1;
//			}
//
//			featureVector.set(String.format(TEMPLATE_1, propertyRootClassType.getSimpleName(),
//					factor.getFactorScope().rootClass, countDifferentSubclassEvidences,
//					factor.getFactorScope().rootClass, factor.getFactorScope().rootCardinality), true);
//
//			int unusedCandidates = countDifferentSubclassEvidences - factor.getFactorScope().rootCardinality;
//
//			featureVector.set(String.format(TEMPLATE_2, propertyRootClassType.getSimpleName(),
//					factor.getFactorScope().rootClass, unusedCandidates), true);
//
//		}
	}

}
