package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.text.translate.LookupTranslator;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.DistinctMultiValueSlotsTemplate.DistinctMultiValueSlotsScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class DistinctMultiValueSlotsTemplate extends AbstractFeatureTemplate<DistinctMultiValueSlotsScope> {

	static class DistinctMultiValueSlotsScope extends AbstractFactorScope {

		public final SlotType slotTypeContext;
		public final boolean isDistinct;

		public DistinctMultiValueSlotsScope(AbstractFeatureTemplate<DistinctMultiValueSlotsScope> template,
				SlotType slotTypeContext, boolean isDistinct) {
			super(template);
			this.slotTypeContext = slotTypeContext;
			this.isDistinct = isDistinct;
		}

		@Override
		public int implementHashCode() {
			return 0;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + (isDistinct ? 1231 : 1237);
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
			DistinctMultiValueSlotsScope other = (DistinctMultiValueSlotsScope) obj;
			if (isDistinct != other.isDistinct)
				return false;
			if (slotTypeContext == null) {
				if (other.slotTypeContext != null)
					return false;
			} else if (!slotTypeContext.equals(other.slotTypeContext))
				return false;
			return true;
		}

		@Override
		public boolean implementEquals(Object obj) {
			return false;
		}

	}

	@Override
	public List<DistinctMultiValueSlotsScope> generateFactorScopes(State state) {
		List<DistinctMultiValueSlotsScope> factors = new ArrayList<>();
		for (EntityTemplate annotation : super.<EntityTemplate>getPredictedAnnotations(state)) {
			Map<SlotType, Set<AbstractAnnotation>> slotAnnotations = annotation.filter().docLinkedAnnoation()
					.literalAnnoation().entityTypeAnnoation().entityTemplateAnnoation().nonEmpty().multiSlots().build()
					.getMultiAnnotations();

			for (Entry<SlotType, Set<AbstractAnnotation>> slotAnnotation : slotAnnotations.entrySet()) {

				final long i = slotAnnotation.getValue().stream().map(aa -> aa.getEntityType()).distinct().count();
				factors.add(new DistinctMultiValueSlotsScope(this, slotAnnotation.getKey(),
						i != slotAnnotation.getValue().size()));

			}
		}

		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<DistinctMultiValueSlotsScope> factor) {

		factor.getFeatureVector().set(
				factor.getFactorScope().slotTypeContext.slotName + "is distinct =" + factor.getFactorScope().isDistinct,
				true);

	}

}
