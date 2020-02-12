package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.type_based;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer.EExplorationMode;
import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTypeAnnotation;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.templates.type_based.TB_ContextBetweenSlotFillerTemplate.ContextBetweenScope;

/**
 * This template creates feature in form of an in between context. Each feature
 * contains the parent class annotations and its property slot annotation and
 * the text in between. Further we capture the
 * 
 * @author hterhors
 *
 * @date Jan 15, 2018
 */
public class TB_ContextBetweenSlotFillerTemplate extends AbstractFeatureTemplate<ContextBetweenScope> {

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
		Map<EntityType, Set<Integer>> map;

		public ContextBetweenScope(AbstractFeatureTemplate<?> template, Instance instance,
				Map<EntityType, Set<Integer>> map) {
			super(template);
			this.instance = instance;
			this.map = map;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((instance == null) ? 0 : instance.hashCode());
			result = prime * result + ((map == null) ? 0 : map.hashCode());
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
			if (instance == null) {
				if (other.instance != null)
					return false;
			} else if (!instance.equals(other.instance))
				return false;
			if (map == null) {
				if (other.map != null)
					return false;
			} else if (!map.equals(other.map))
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
	public List<ContextBetweenScope> generateFactorScopes(State state) {

		final List<ContextBetweenScope> factors = new ArrayList<>();

		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
				continue;

			if (SCIOSlotTypes.hasTreatmentType.isExcluded())
				continue;

			final MultiFillerSlot mfs = experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType);

			if (mfs.containsSlotFiller()) {

				EntityType rootEntityType = experimentalGroup.getEntityType();
				final Set<Integer> rootSentenceIndicies = collectExpGroupTokenIndicies(experimentalGroup);
				Map<EntityType, Set<Integer>> map = new HashMap<>();

				map.put(rootEntityType, rootSentenceIndicies);

				for (AbstractAnnotation treatment : mfs.getSlotFiller()) {
					final Set<Integer> treatmentIndicies = new HashSet<>();

					EntityType mainAnnotation = null;
					if (treatment.getEntityType() == SCIOEntityTypes.compoundTreatment) {
						SingleFillerSlot sfs = treatment.asInstanceOfEntityTemplate()
								.getSingleFillerSlot(SCIOSlotTypes.hasCompound);
						if (sfs.containsSlotFiller())
							mainAnnotation = sfs.getSlotFiller().asInstanceOfEntityTemplate().getRootAnnotation()
									.getEntityType();
					}

					if (mainAnnotation == null)
						mainAnnotation = treatment.asInstanceOfEntityTemplate().getRootAnnotation().getEntityType();

					for (EntityTypeAnnotation tretamentAnnotation : state.getInstance()
							.getEntityTypeCandidates(EExplorationMode.ANNOTATION_BASED, mainAnnotation)) {

						if (tretamentAnnotation.isInstanceOfDocumentLinkedAnnotation()) {
							treatmentIndicies
									.add(tretamentAnnotation.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0)
											.getDocTokenIndex());
							//
//							Pattern project = Pattern.compile(Pattern
//									.quote(mainAnnotation.asInstanceOfDocumentLinkedAnnotation().textualContent.surfaceForm));
//							Matcher m = project.matcher(state.getInstance().getDocument().documentContent);
							//
//							while (m.find()) {
//								try {
//									treatmentIndicies.add(state.getInstance().getDocument().getTokenByCharStartOffset(m.start())
//											.getDocTokenIndex());
//								} catch (DocumentLinkedAnnotationMismatchException e) {
//								}
//							}

						}
						map.put(mainAnnotation, treatmentIndicies);
					}
				}

				factors.add(new ContextBetweenScope(this, state.getInstance(), map));
			}

		}
		return factors;

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
		DoubleVector featureVector = factor.getFeatureVector();

		List<EntityType> types = new ArrayList<>(factor.getFactorScope().map.keySet());

		Collections.sort(types);

		for (int i = 0; i < types.size(); i++) {
			EntityType fromType = types.get(i);
			for (Integer fromTokenIndex : factor.getFactorScope().map.get(types.get(i))) {

				for (int j = i + 1; j < types.size(); j++) {
					EntityType toType = types.get(j);
					for (Integer toTokenIndex : factor.getFactorScope().map.get(types.get(j))) {

						if (Math.abs(toTokenIndex - fromTokenIndex) > MAX_TOKEN_DIST)
							continue;

						if (Math.abs(toTokenIndex - fromTokenIndex) < MIN_TOKEN_DIST)
							continue;

						final List<DocumentToken> tokens;

						if (fromTokenIndex < toTokenIndex)
							tokens = factor.getFactorScope().instance.getDocument().tokenList
									.subList(fromTokenIndex + 1, toTokenIndex); // exclude
						else
							tokens = factor.getFactorScope().instance.getDocument().tokenList.subList(toTokenIndex + 1,
									fromTokenIndex); // exclude

						if (tokens.size() > 2) {

							getTokenNgrams(featureVector, fromType.name, toType.name, tokens);

//										for (EntityType fe : fromEntity.getTransitiveClosureSuperEntityTypes()) {
//											for (EntityType te : toEntity.getTransitiveClosureSuperEntityTypes()) {
							//
//												if (tokens.size() > 2)
//													getTokenNgrams(featureVector, fe.entityName, te.entityName, tokens);
//											}
							//
//										}
						}
					}
				}

			}
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