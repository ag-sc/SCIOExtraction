package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer.EExplorationMode;
import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTypeAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.DoubleVector;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based.TB_SingleTokenContextTemplate.SingleTokenContextScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.wrapper.DefinedExperimentalGroup;

public class TB_SingleTokenContextTemplate extends AbstractFeatureTemplate<SingleTokenContextScope> {

	private static final int DEFAULT_MAX_TOKEN_CONTEXT_LEFT = 10;
	private static final int DEFAULT_MAX_TOKEN_CONTEXT_RIGHT = 10;
	private static final String BOF = "BOF";
	private static final String EOF = "EOF";
	private static final String PREFIX = "STCT\t";

	public TB_SingleTokenContextTemplate() {
		super(false);
	}

	static class SingleTokenContextScope extends AbstractFactorScope {

		public final Instance instance;

		public final EntityType entityType;

		public SingleTokenContextScope(AbstractFeatureTemplate<SingleTokenContextScope> template, Instance instance,
				EntityType entityType) {
			super(template);
			this.instance = instance;
			this.entityType = entityType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
			result = prime * result + ((instance == null) ? 0 : instance.hashCode());
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
			SingleTokenContextScope other = (SingleTokenContextScope) obj;
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
			return true;
		}

		@Override
		public int implementHashCode() {
			return hashCode();
		}

		@Override
		public boolean implementEquals(Object obj) {
			return equals(obj);
		}

	}

	@Override
	public List<SingleTokenContextScope> generateFactorScopes(State state) {
		List<SingleTokenContextScope> factors = new ArrayList<>();

		for (EntityTemplate experimentalGroupET : super.<EntityTemplate>getPredictedAnnotations(state)) {

			final DefinedExperimentalGroup experimentalGroup = new DefinedExperimentalGroup(experimentalGroupET);

			final EntityTypeAnnotation species = experimentalGroup.getOrganismSpecies();

			if (species != null)
				factors.add(new SingleTokenContextScope(this, state.getInstance(), species.getEntityType()));

			final EntityTemplate injury = experimentalGroup.getInjury();

			if (injury != null)
				factors.add(new SingleTokenContextScope(this, state.getInstance(), injury.getEntityType()));

			final Set<EntityTemplate> treatments = experimentalGroup.getRelevantTreatments();

			for (EntityTemplate treatment : treatments) {
				factors.add(new SingleTokenContextScope(this, state.getInstance(), treatment.getEntityType()));
			}

		}
		return factors;

	}

	@Override
	public void generateFeatureVector(Factor<SingleTokenContextScope> factor) {
		DoubleVector featureVector = factor.getFeatureVector();

		for (EntityTypeAnnotation annotation : factor.getFactorScope().instance
				.getEntityTypeCandidates(EExplorationMode.ANNOTATION_BASED, factor.getFactorScope().entityType)) {
			DocumentToken beginToken = annotation.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0);
			DocumentToken endToken = annotation.asInstanceOfDocumentLinkedAnnotation().relatedTokens
					.get(annotation.asInstanceOfDocumentLinkedAnnotation().relatedTokens.size() - 1);

			Set<EntityType> entityTypes = new HashSet<>();
			entityTypes.add(factor.getFactorScope().entityType);
			entityTypes.addAll(factor.getFactorScope().entityType.getTransitiveClosureSuperEntityTypes());

			int distance = 0;
			for (int i = Math.max(0, beginToken.getDocTokenIndex() - DEFAULT_MAX_TOKEN_CONTEXT_LEFT) - 1; i < beginToken
					.getDocTokenIndex(); i++) {

				if (i < 0)
					for (EntityType entityType : entityTypes) {
						featureVector.set(PREFIX + BOF + " " + entityType.name, true);
					}
				else {
					final String text = factor.getFactorScope().instance.getDocument().tokenList.get(i).getText();
					if (Document.getStopWords().contains(text))
						continue;
					for (EntityType entityType : entityTypes) {
						featureVector.set(PREFIX + text + " " + entityType.name, true);
						featureVector.set(PREFIX + text + "... " + entityType.name, true);
						featureVector.set(PREFIX + text + "... -" + distance + "... " + entityType.name, true);
					}
				}
				distance++;
			}
			distance = 0;
			for (int i = beginToken.getDocTokenIndex(); i <= Math.min(
					factor.getFactorScope().instance.getDocument().tokenList.size(),
					endToken.getDocTokenIndex() + DEFAULT_MAX_TOKEN_CONTEXT_RIGHT); i++) {

				if (i == factor.getFactorScope().instance.getDocument().tokenList.size())
					for (EntityType entityType : entityTypes) {
						featureVector.set(PREFIX + EOF + " " + entityType.name, true);
					}
				else {
					final String text = factor.getFactorScope().instance.getDocument().tokenList.get(i).getText();
					if (Document.getStopWords().contains(text))
						continue;
					for (EntityType entityType : entityTypes) {
						featureVector.set(PREFIX + text + " " + entityType.name, true);
						featureVector.set(PREFIX + entityType.name + "... " + text, true);
						featureVector.set(PREFIX + entityType.name + "... +" + distance + "... " + text, true);
					}
				}
				distance++;
			}
		}

	}

}
