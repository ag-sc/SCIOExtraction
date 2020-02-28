package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer.EExplorationMode;
import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTypeAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.structure.annotations.filter.EntityTemplateAnnotationFilter;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.DoubleVector;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based.TB_NGramTokenContextTemplate.NGramTokenContextScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.type_based.TB_SingleTokenContextTemplate.SingleTokenContextScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.wrapper.DefinedExperimentalGroup;

public class TB_NGramTokenContextTemplate extends AbstractFeatureTemplate<NGramTokenContextScope> {

	private static final int DEFAULT_MAX_TOKEN_CONTEXT_LEFT = 3;
	private static final int DEFAULT_MAX_TOKEN_CONTEXT_RIGHT = 3;
	private static final int MAX_TOKEN_CONTEXT_LEFT = DEFAULT_MAX_TOKEN_CONTEXT_LEFT;
	private static final int MAX_TOKEN_CONTEXT_RIGHT = DEFAULT_MAX_TOKEN_CONTEXT_RIGHT;

	private static final char SPLITTER = ' ';
	private static final char RIGHT = '>';
	private static final char LEFT = '<';
	private static final String BOF = "BOF";
	private static final String EOF = "EOF";
	private static final String PREFIX = "NGTCT\t";

	static class NGramTokenContextScope extends AbstractFactorScope {

		public final Instance instance;

		public final EntityType entityType;

		public NGramTokenContextScope(AbstractFeatureTemplate<NGramTokenContextScope> template, Instance instance,
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
			NGramTokenContextScope other = (NGramTokenContextScope) obj;
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
	public List<NGramTokenContextScope> generateFactorScopes(State state) {

		List<NGramTokenContextScope> factors = new ArrayList<>();

		for (EntityTemplate experimentalGroupET : super.<EntityTemplate>getPredictedAnnotations(state)) {

			final DefinedExperimentalGroup experimentalGroup = new DefinedExperimentalGroup(experimentalGroupET);

			final EntityTypeAnnotation species = experimentalGroup.getOrganismSpecies();

			if (species != null)
				factors.add(new NGramTokenContextScope(this, state.getInstance(), species.getEntityType()));

			final EntityTemplate injury = experimentalGroup.getInjury();

			if (injury != null)
				factors.add(new NGramTokenContextScope(this, state.getInstance(), injury.getEntityType()));

			final Set<EntityTemplate> treatments = experimentalGroup.getRelevantTreatments();

			for (EntityTemplate treatment : treatments) {
				factors.add(new NGramTokenContextScope(this, state.getInstance(), treatment.getEntityType()));
			}

		}
		return factors;

	}

	@Override
	public void generateFeatureVector(Factor<NGramTokenContextScope> factor) {
		DoubleVector featureVector = factor.getFeatureVector();

		for (EntityTypeAnnotation annotation : factor.getFactorScope().instance
				.getEntityTypeCandidates(EExplorationMode.ANNOTATION_BASED, factor.getFactorScope().entityType)) {

			DocumentToken beginToken = annotation.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0);
			DocumentToken endToken = annotation.asInstanceOfDocumentLinkedAnnotation().relatedTokens
					.get(annotation.asInstanceOfDocumentLinkedAnnotation().relatedTokens.size() - 1);

			final String[] leftContext = extractLeftContext(factor.getFactorScope().instance.getDocument().tokenList,
					beginToken.getDocTokenIndex());

			final String[] rightContext = extractRightContext(factor.getFactorScope().instance.getDocument().tokenList,
					endToken.getDocTokenIndex());

			Set<EntityType> entityTypes = new HashSet<>();

			entityTypes.add(factor.getFactorScope().entityType);
			entityTypes.addAll(factor.getFactorScope().entityType.getTransitiveClosureSuperEntityTypes());

			getContextFeatures(featureVector, entityTypes, leftContext, rightContext);

		}
	}

	private String[] extractLeftContext(List<DocumentToken> tokens, int beginTokenIndex) {
		final String[] leftContext = new String[MAX_TOKEN_CONTEXT_LEFT];

		for (int i = 1; i < 1 + MAX_TOKEN_CONTEXT_LEFT; i++) {
			if (beginTokenIndex - i >= 0) {
				leftContext[i - 1] = tokens.get(beginTokenIndex - i).getText();
			} else {
				break;
			}
		}
		return leftContext;
	}

	private String[] extractRightContext(List<DocumentToken> tokens, int endTokenIndex) {
		final String[] rightContext = new String[MAX_TOKEN_CONTEXT_RIGHT];

		for (int i = 1; i < 1 + MAX_TOKEN_CONTEXT_RIGHT; i++) {
			if (endTokenIndex + i < tokens.size()) {
				rightContext[i - 1] = tokens.get(endTokenIndex + i).getText();
			} else {
				break;
			}
		}
		return rightContext;

	}

	private void getContextFeatures(DoubleVector featureVector, final Set<EntityType> entityTypes,
			final String[] leftContext, final String[] rightContext) {

		final StringBuffer lCs = new StringBuffer();
		final StringBuffer rCs = new StringBuffer();
		String context;
		boolean bof;
		boolean eof;
		for (int i = 0; i < leftContext.length; i++) {
			bof = leftContext[i] == null;
			if (bof)
				context = BOF;
			else
				context = leftContext[i];

			rCs.setLength(0);
			lCs.insert(0, context + SPLITTER);

			for (EntityType entityType : entityTypes) {
				featureVector.set(PREFIX + new StringBuffer(lCs).append(LEFT).append(entityType.name).append(RIGHT)
						.append(rCs).toString().trim(), true);
			}

			for (int j = 0; j < rightContext.length; j++) {
				eof = rightContext[j] == null;
				if (eof)
					context = EOF;
				else
					context = rightContext[j];
				rCs.append(SPLITTER).append(context);
				for (EntityType entityType : entityTypes) {
					featureVector.set(PREFIX + new StringBuffer(lCs).append(LEFT).append(entityType.name).append(RIGHT)
							.append(rCs).toString().trim(), true);
				}
			}
			if (bof)
				break;
		}

		rCs.setLength(0);
		lCs.setLength(0);

		for (int i = 0; i < rightContext.length; i++) {
			eof = rightContext[i] == null;
			if (eof)
				context = EOF;
			else
				context = rightContext[i];
			lCs.setLength(0);
			rCs.append(SPLITTER).append(context);
			for (EntityType entityType : entityTypes) {
				featureVector.set(PREFIX + new StringBuffer(lCs).append(LEFT).append(entityType.name).append(RIGHT)
						.append(rCs).toString().trim(), true);
			}

			for (int j = 0; j < leftContext.length; j++) {
				bof = leftContext[j] == null;
				if (bof)
					context = BOF;
				else
					context = leftContext[j];
				lCs.insert(0, context + SPLITTER);
				for (EntityType entityType : entityTypes) {
					featureVector.set(PREFIX + new StringBuffer(lCs).append(LEFT).append(entityType.name).append(RIGHT)
							.append(rCs).toString().trim(), true);
				}
				if (bof)
					break;
			}

			if (eof)
				break;
		}
	}
}
