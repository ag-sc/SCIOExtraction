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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.MultiValueSlotSizeTemplate.MultiValueSlotSizeScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class MultiValueSlotSizeTemplate extends AbstractFeatureTemplate<MultiValueSlotSizeScope> {

	static class MultiValueSlotSizeScope extends AbstractFactorScope {

		public final SlotType slotTypeContext;
		public final int num;

		public MultiValueSlotSizeScope(AbstractFeatureTemplate<MultiValueSlotSizeScope> template,
				SlotType slotTypeContext, int num) {
			super(template);
			this.slotTypeContext = slotTypeContext;
			this.num = num;
		}

		@Override
		public int implementHashCode() {
			return 0;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + num;
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
			MultiValueSlotSizeScope other = (MultiValueSlotSizeScope) obj;
			if (num != other.num)
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
	public List<MultiValueSlotSizeScope> generateFactorScopes(State state) {
		List<MultiValueSlotSizeScope> factors = new ArrayList<>();
		for (EntityTemplate annotation : super.<EntityTemplate>getPredictedAnnotations(state)) {
			Map<SlotType, Set<AbstractAnnotation>> slotAnnotations = annotation.filter().docLinkedAnnoation()
					.literalAnnoation().entityTypeAnnoation().entityTemplateAnnoation().nonEmpty().multiSlots().build()
					.getMultiAnnotations();

			for (Entry<SlotType, Set<AbstractAnnotation>> slotAnnotation : slotAnnotations.entrySet()) {
				factors.add(
						new MultiValueSlotSizeScope(this, slotAnnotation.getKey(), slotAnnotation.getValue().size()));
			}
		}

		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<MultiValueSlotSizeScope> factor) {

		factor.getFeatureVector()
				.set(factor.getFactorScope().slotTypeContext.name + " size = " + factor.getFactorScope().num, true);

	}

}
