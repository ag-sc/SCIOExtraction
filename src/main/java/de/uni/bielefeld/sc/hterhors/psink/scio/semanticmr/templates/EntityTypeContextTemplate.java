package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.EntityTypeContextTemplate.EntityTypeContextScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class EntityTypeContextTemplate extends AbstractFeatureTemplate<EntityTypeContextScope> {

	public EntityTypeContextTemplate() {
//		super(false);
	}

	static class EntityTypeContextScope extends AbstractFactorScope {

		public final List<EntityType> leftContext;
		public final List<EntityType> rightContext;
		public final SlotType slotTypeContext;

		public EntityTypeContextScope(AbstractFeatureTemplate<EntityTypeContextScope> template,
				List<EntityType> leftContext, List<EntityType> rightContext, SlotType slotTypeContext) {
			super(template);
			this.leftContext = leftContext;
			this.rightContext = rightContext;
			this.slotTypeContext = slotTypeContext;
		}

		@Override
		public int implementHashCode() {
			return 0;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((leftContext == null) ? 0 : leftContext.hashCode());
			result = prime * result + ((rightContext == null) ? 0 : rightContext.hashCode());
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
			EntityTypeContextScope other = (EntityTypeContextScope) obj;
			if (leftContext == null) {
				if (other.leftContext != null)
					return false;
			} else if (!leftContext.equals(other.leftContext))
				return false;
			if (rightContext == null) {
				if (other.rightContext != null)
					return false;
			} else if (!rightContext.equals(other.rightContext))
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

	private static final String PREFIX = "ETCT\t";

	@Override
	public List<EntityTypeContextScope> generateFactorScopes(State state) {
		List<EntityTypeContextScope> factors = new ArrayList<>();

		for (EntityTemplate annotation : super.<EntityTemplate>getPredictedAnnotations(state)) {

			List<AbstractAnnotation> annotations = new ArrayList<>();

			Map<SlotType, Set<AbstractAnnotation>> slotAnnotations = annotation.filter().docLinkedAnnoation()
					.singleSlots().nonEmpty().multiSlots().merge().build().getMergedAnnotations();

			for (Entry<SlotType, Set<AbstractAnnotation>> slotAnnotation : slotAnnotations.entrySet()) {
				annotations.addAll(slotAnnotation.getValue());
			}

			annotations.sort(new Comparator<AbstractAnnotation>() {

				@Override
				public int compare(AbstractAnnotation o1, AbstractAnnotation o2) {
					return Integer.compare(o1.asInstanceOfDocumentLinkedAnnotation().getStartDocCharOffset(),
							o2.asInstanceOfDocumentLinkedAnnotation().getStartDocCharOffset());
				}
			});

			for (Entry<SlotType, Set<AbstractAnnotation>> slotAnnotation : slotAnnotations.entrySet()) {
				for (AbstractAnnotation slotFiller : slotAnnotation.getValue()) {
					List<EntityType> leftContext = new ArrayList<>();
					List<EntityType> rightContext = new ArrayList<>();

					boolean found = false;

					for (AbstractAnnotation contextAnnotation : annotations) {
						if (contextAnnotation == slotFiller) {
							found = true;
						} else {
							if (!found)
								leftContext.add(contextAnnotation.getEntityType());
							else
								rightContext.add(contextAnnotation.getEntityType());
						}

					}
					factors.add(new EntityTypeContextScope(this, leftContext, rightContext, slotAnnotation.getKey()));
				}
			}

		}

		return factors;
	}

//	Mean Score: Score [getF1()=0.755, getPrecision()=0.883, getRecall()=0.660, tp=128, fp=17, fn=66, tn=0]
//			CRFStatistics [context=Train, getTotalDuration()=16088]
//			CRFStatistics [context=Test, getTotalDuration()=722]
//			OrgModel860371

	@Override
	public void generateFeatureVector(Factor<EntityTypeContextScope> factor) {

		List<EntityType> superLeftContext = factor.getFactorScope().leftContext.stream()
				.flatMap(e -> e.getDirectSuperEntityTypes().stream()).sorted().collect(Collectors.toList());
		List<EntityType> superRightContext = factor.getFactorScope().rightContext.stream()
				.flatMap(e -> e.getDirectSuperEntityTypes().stream()).sorted().collect(Collectors.toList());

		factor.getFeatureVector()
				.set(PREFIX + superLeftContext.stream().map(e -> " " + e.name).reduce("", String::concat).trim() + "<->"
						+ factor.getFactorScope().slotTypeContext.name + "<->"
						+ superRightContext.stream().map(e -> " " + e.name).reduce("", String::concat).trim(), true);

//		factor.getFeatureVector()
//				.set(factor.getFactorScope().leftContext.stream().map(e -> " " + e.entityName)
//						.reduce("", String::concat).trim() + "<->" + factor.getFactorScope().slotTypeContext.slotName
//						+ "<->" + factor.getFactorScope().rightContext.stream().map(e -> " " + e.entityName)
//								.reduce("", String::concat).trim(),
//						true);

	}

}
