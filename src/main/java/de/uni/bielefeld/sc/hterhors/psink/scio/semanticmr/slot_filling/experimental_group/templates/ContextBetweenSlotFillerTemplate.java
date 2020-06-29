package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.DoubleVector;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.ContextBetweenSlotFillerTemplate.ContextBetweenScope;

/**
 * This template creates feature in form of an in between context. Each feature
 * contains the parent class annotations and its property slot annotation and
 * the text in between. Further we capture the
 * 
 * @author hterhors
 *
 * @date Jan 15, 2018
 */
public class ContextBetweenSlotFillerTemplate extends AbstractFeatureTemplate<ContextBetweenScope> {

	private static final String LEFT = "<";

	private static final String RIGHT = ">";

	private static final String END_DOLLAR = "$";

	private static final String START_CIRCUMFLEX = "^";

	private static final int MIN_TOKEN_LENGTH = 2;

	private static final int MAX_TOKEN_DIST = 10;

	private static final int MIN_TOKEN_DIST = 2;

	private static final String PREFIX = "CBSFT\t";

	static class ContextBetweenScope extends AbstractFactorScope {

		public final Instance instance;
		public final EntityType fromEntity;
		public final Integer fromTokenIndex;
		public final EntityType toEntity;
		public final Integer toTokenIndex;

		public ContextBetweenScope(AbstractFeatureTemplate<ContextBetweenScope> template, Instance instance,
				EntityType fromEntity, Integer fromEntityCharacterOnset, EntityType toEntity,
				Integer toEntityCharacterOnset) {
			super(template);
			this.instance = instance;
			this.fromEntity = fromEntity;
			this.fromTokenIndex = fromEntityCharacterOnset;
			this.toEntity = toEntity;
			this.toTokenIndex = toEntityCharacterOnset;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((fromEntity == null) ? 0 : fromEntity.hashCode());
			result = prime * result + ((fromTokenIndex == null) ? 0 : fromTokenIndex.hashCode());
			result = prime * result + ((instance == null) ? 0 : instance.hashCode());
			result = prime * result + ((toEntity == null) ? 0 : toEntity.hashCode());
			result = prime * result + ((toTokenIndex == null) ? 0 : toTokenIndex.hashCode());
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
			ContextBetweenScope other = (ContextBetweenScope) obj;
			if (fromEntity == null) {
				if (other.fromEntity != null)
					return false;
			} else if (!fromEntity.equals(other.fromEntity))
				return false;
			if (fromTokenIndex == null) {
				if (other.fromTokenIndex != null)
					return false;
			} else if (!fromTokenIndex.equals(other.fromTokenIndex))
				return false;
			if (instance == null) {
				if (other.instance != null)
					return false;
			} else if (!instance.equals(other.instance))
				return false;
			if (toEntity == null) {
				if (other.toEntity != null)
					return false;
			} else if (!toEntity.equals(other.toEntity))
				return false;
			if (toTokenIndex == null) {
				if (other.toTokenIndex != null)
					return false;
			} else if (!toTokenIndex.equals(other.toTokenIndex))
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

		@Override
		public String toString() {
			return "ContextBetweenScope [instance=" + instance + ", fromEntity=" + fromEntity
					+ ", fromEntityCharacterOnset=" + fromTokenIndex + ", toEntity=" + toEntity
					+ ", toEntityCharacterOnset=" + toTokenIndex + "]";
		}

	}

	@Override
	public List<ContextBetweenScope> generateFactorScopes(State state) {

		final List<ContextBetweenScope> factors = new ArrayList<>();

		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
				continue;

			EntityType rootEntityType = experimentalGroup.getEntityType();
			final Set<Integer> rootSentenceIndicies = collectExpGroupTokenIndicies(experimentalGroup);

			getMultiFllerSlotSentenceIndicies(factors, rootEntityType, rootSentenceIndicies, state, experimentalGroup,
					SCIOSlotTypes.hasTreatmentType);
		}
		return factors;

	}

	private void getMultiFllerSlotSentenceIndicies(List<ContextBetweenScope> factors, EntityType rootEntityType,
			Set<Integer> rootSentenceIndicies, State state, EntityTemplate experimentalGroup, SlotType slotType) {

		if (slotType.isExcluded())
			return;

		final MultiFillerSlot mfs = experimentalGroup.getMultiFillerSlot(slotType);

		if (mfs.containsSlotFiller()) {

			Map<EntityType, Set<Integer>> map = new HashMap<>();

			map.put(rootEntityType, rootSentenceIndicies);

			for (AbstractAnnotation treatment : mfs.getSlotFiller()) {
				final Set<Integer> treatmentIndicies = new HashSet<>();

				AbstractAnnotation mainAnnotation = null;
				if (treatment.getEntityType() == SCIOEntityTypes.compoundTreatment) {
					SingleFillerSlot sfs = treatment.asInstanceOfEntityTemplate()
							.getSingleFillerSlot(SCIOSlotTypes.hasCompound);
					if (sfs.containsSlotFiller())
						mainAnnotation = sfs.getSlotFiller().asInstanceOfEntityTemplate().getRootAnnotation();
				}

				if (mainAnnotation == null)
					mainAnnotation = treatment.asInstanceOfEntityTemplate().getRootAnnotation();

				if (mainAnnotation.isInstanceOfDocumentLinkedAnnotation()) {
					treatmentIndicies.add(mainAnnotation.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0)
							.getDocTokenIndex());
//
//					Pattern project = Pattern.compile(Pattern
//							.quote(mainAnnotation.asInstanceOfDocumentLinkedAnnotation().textualContent.surfaceForm));
//					Matcher m = project.matcher(state.getInstance().getDocument().documentContent);
//
//					while (m.find()) {
//						try {
//							treatmentIndicies.add(state.getInstance().getDocument().getTokenByCharStartOffset(m.start())
//									.getDocTokenIndex());
//						} catch (DocumentLinkedAnnotationMismatchException e) {
//						}
//					}

				}
				map.put(mainAnnotation.getEntityType(), treatmentIndicies);

			}

			List<EntityType> types = new ArrayList<>(map.keySet());

			Collections.sort(types);

			for (int i = 0; i < types.size(); i++) {
				EntityType fromType = types.get(i);
				for (Integer fromTokenIndex : map.get(types.get(i))) {

					for (int j = i + 1; j < types.size(); j++) {
						EntityType toType = types.get(j);
						for (Integer toTokenIndex : map.get(types.get(j))) {

							if (Math.abs(toTokenIndex - fromTokenIndex) > MAX_TOKEN_DIST)
								continue;

							if (Math.abs(toTokenIndex - fromTokenIndex) < MIN_TOKEN_DIST)
								continue;

							if (fromTokenIndex < toTokenIndex)
								factors.add(new ContextBetweenScope(this, state.getInstance(), fromType, fromTokenIndex,
										toType, toTokenIndex));
							else
								factors.add(new ContextBetweenScope(this, state.getInstance(), toType, toTokenIndex,
										fromType, fromTokenIndex));
						}
					}

				}
			}

		}
	}

	public Set<Integer> collectExpGroupTokenIndicies(EntityTemplate experimentalGroup) {
		Set<Integer> sentenceIndicies = new HashSet<>();
		if (experimentalGroup.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()) {

			DocumentLinkedAnnotation docLinkedRootAnnotation = experimentalGroup.getRootAnnotation()
					.asInstanceOfDocumentLinkedAnnotation();

			sentenceIndicies.add(docLinkedRootAnnotation.relatedTokens.get(0).getDocTokenIndex());

		}
		if (SCIOSlotTypes.hasGroupName.isIncluded()) {

			for (AbstractAnnotation groupName : experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasGroupName)
					.getSlotFiller()) {

				if (groupName.isInstanceOfDocumentLinkedAnnotation())
					sentenceIndicies.add(
							groupName.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0).getDocTokenIndex());
			}
		}
		return sentenceIndicies;
	}

	@Override
	public void generateFeatureVector(Factor<ContextBetweenScope> factor) {

		EntityType fromEntity = factor.getFactorScope().fromEntity;
		EntityType toEntity = factor.getFactorScope().toEntity;

		DoubleVector featureVector = factor.getFeatureVector();

		final List<DocumentToken> tokens = factor.getFactorScope().instance.getDocument().tokenList
				.subList(factor.getFactorScope().fromTokenIndex + 1, factor.getFactorScope().toTokenIndex); // exclude
																											// start
																											// token.

		if (tokens.size() > 2) {

			getTokenNgrams(featureVector, fromEntity.name, toEntity.name, tokens);

//			for (EntityType fe : fromEntity.getTransitiveClosureSuperEntityTypes()) {
//				for (EntityType te : toEntity.getTransitiveClosureSuperEntityTypes()) {
//
//					if (tokens.size() > 2)
//						getTokenNgrams(featureVector, fe.entityName, te.entityName, tokens);
//				}
//
//			}
		}
	}

	private void getTokenNgrams(DoubleVector featureVector, String fromClassName, String toClassName,
			List<DocumentToken> tokens) {

		final int maxNgramSize = tokens.size() + 2;
		for (int ngram = 1; ngram < maxNgramSize; ngram++) {
			for (int i = 0; i < maxNgramSize - 1; i++) {

				/*
				 * Break if size exceeds token length
				 */
				if (i + ngram > maxNgramSize)
					break;

				final StringBuffer fBuffer = new StringBuffer();
				for (int t = i; t < i + ngram; t++) {

					final String text;
					if (t == 0)
						text = START_CIRCUMFLEX;
					else if (t == tokens.size() + 1)
						text = END_DOLLAR;
					else {

						final DocumentToken token = tokens.get(t - 1);

						if (token.getText().isEmpty())
							continue;

						if (token.isStopWord())
							continue;
						text = token.getText();
					}

					fBuffer.append(text);
					fBuffer.append(Document.TOKEN_SPLITTER);

				}

				final String featureName = simplify(fBuffer.toString().trim());

				if (featureName.length() < MIN_TOKEN_LENGTH)
					continue;

				if (featureName.isEmpty())
					continue;

				featureVector.set(PREFIX + LEFT + fromClassName + RIGHT + Document.TOKEN_SPLITTER + featureName
						+ Document.TOKEN_SPLITTER + LEFT + toClassName + RIGHT, true);

			}
		}

	}

	private String simplify(String trim) {
//		trim = trim.replaceAll("\\d", "#");
//		trim = trim.toLowerCase();
		return trim;
	}

}