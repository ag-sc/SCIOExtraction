package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.OlfactoryContextTemplate.EntityTypeContextScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class OlfactoryContextTemplate extends AbstractFeatureTemplate<EntityTypeContextScope> {

	static class EntityTypeContextScope extends AbstractFactorScope {

		public final Instance instance;
		public final int sentenceIndex;
		public final EntityType entityType;

		public EntityTypeContextScope(AbstractFeatureTemplate<EntityTypeContextScope> template, Instance instance,
				int sentenceIndex, EntityType entityType) {
			super(template);
			this.instance = instance;
			this.sentenceIndex = sentenceIndex;
			this.entityType = entityType;
		}

		@Override
		public int implementHashCode() {
			return 0;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
			result = prime * result + ((instance == null) ? 0 : instance.hashCode());
			result = prime * result + sentenceIndex;
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
			if (entityType == null) {
				if (other.entityType != null)
					return false;
			} else if (!entityType.equals(other.entityType))
				return false;
			if (instance == null) {
				if (other.instance != null)
					return false;
			} else if (!instance.equals(other.instance))
				return false;
			if (sentenceIndex != other.sentenceIndex)
				return false;
			return true;
		}

		@Override
		public boolean implementEquals(Object obj) {
			return false;
		}

	}

	private static final String PREFIX = "OCT\t";

	@Override
	public List<EntityTypeContextScope> generateFactorScopes(State state) {
		List<EntityTypeContextScope> factors = new ArrayList<>();

		for (EntityTemplate annotation : super.<EntityTemplate>getPredictedAnnotations(state)) {

			Map<SlotType, Set<AbstractAnnotation>> slotAnnotations = annotation.filter().docLinkedAnnoation()
					.singleSlots().nonEmpty().multiSlots().merge().build().getMergedAnnotations();

			for (Set<AbstractAnnotation> slotFillerAnnotations : slotAnnotations.values()) {
				for (AbstractAnnotation slotFillerAnnotation : slotFillerAnnotations) {
					final EntityType slotFillerEntityType = slotFillerAnnotation.getEntityType();
					final int slotFillerSenIndex = slotFillerAnnotation
							.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0).getSentenceIndex();
					final Instance instance = state.getInstance();

					factors.add(new EntityTypeContextScope(this, instance, slotFillerSenIndex, slotFillerEntityType));
				}
			}

		}

		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<EntityTypeContextScope> factor) {

		boolean olfactory = false;

		for (DocumentToken token : factor.getFactorScope().instance.getDocument()
				.getSentenceByIndex(factor.getFactorScope().sentenceIndex)) {

			if (token.getText().toLowerCase().startsWith("olfact")) {
				olfactory = true;
				break;
			}

		}

		if (!olfactory)
			return;

		factor.getFeatureVector().set(PREFIX + "ContextOfOlfactory " + factor.getFactorScope().entityType.entityName,
				true);
		for (EntityType superET : factor.getFactorScope().entityType.getDirectSuperEntityTypes()) {
			factor.getFeatureVector().set(PREFIX + "ContextOfOlfactory " + superET.entityName, true);
		}

	}

}
