package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.type_based;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.DoubleVector;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.templates.type_based.TB_ContextCardinalityTemplate.ContextBetweenScope;

/**
 * This template creates feature in form of an in between context. Each feature
 * contains the parent class annotations and its property slot annotation and
 * the text in between. Further we capture the
 * 
 * @author hterhors
 *
 * @date Jan 15, 2018
 */
public class TB_ContextCardinalityTemplate extends AbstractFeatureTemplate<ContextBetweenScope> {

	private static final int DEFAULT_MAX_TOKEN_CONTEXT_LEFT = 4;
	private static final int DEFAULT_MAX_TOKEN_CONTEXT_RIGHT = 4;
	private static final int MAX_TOKEN_CONTEXT_LEFT = DEFAULT_MAX_TOKEN_CONTEXT_LEFT;
	private static final int MAX_TOKEN_CONTEXT_RIGHT = DEFAULT_MAX_TOKEN_CONTEXT_RIGHT;

	private static final char SPLITTER = ' ';
	private static final char RIGHT = '>';
	private static final char LEFT = '<';
	private static final String BOF = "BOF";
	private static final String EOF = "EOF";
	private static final String PREFIX = "NGTCT\t";

	static class ContextBetweenScope extends AbstractFactorScope {

		public final Instance instance;
		public final EntityType entity;
		public final int startTokenIndex;
		public final int endTokenIndex;
		public final int cardinality;

		public ContextBetweenScope(AbstractFeatureTemplate<ContextBetweenScope> template, Instance instance,
				EntityType entity, int startTokenIndex, int endTokenIndex, int cardinality) {
			super(template);
			this.instance = instance;
			this.entity = entity;
			this.startTokenIndex = startTokenIndex;
			this.endTokenIndex = endTokenIndex;
			this.cardinality = cardinality;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + cardinality;
			result = prime * result + endTokenIndex;
			result = prime * result + ((entity == null) ? 0 : entity.hashCode());
			result = prime * result + ((instance == null) ? 0 : instance.hashCode());
			result = prime * result + startTokenIndex;
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
			if (cardinality != other.cardinality)
				return false;
			if (endTokenIndex != other.endTokenIndex)
				return false;
			if (entity == null) {
				if (other.entity != null)
					return false;
			} else if (!entity.equals(other.entity))
				return false;
			if (instance == null) {
				if (other.instance != null)
					return false;
			} else if (!instance.equals(other.instance))
				return false;
			if (startTokenIndex != other.startTokenIndex)
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

			int cardinality = experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).size();

//			if (cardinality <= 1)
//				continue;

			final Set<List<DocumentToken>> rootSentenceIndicies = collectExpGroupTokenIndicies(experimentalGroup);
			for (List<DocumentToken> entry : rootSentenceIndicies) {
				factors.add(new ContextBetweenScope(this, state.getInstance(), experimentalGroup.getEntityType(),
						entry.get(0).getDocTokenIndex(), entry.get(entry.size() - 1).getDocTokenIndex(), cardinality));
			}
		}
		return factors;

	}

	public Set<List<DocumentToken>> collectExpGroupTokenIndicies(EntityTemplate experimentalGroup) {
		Set<List<DocumentToken>> tokens = new HashSet<>();
		if (experimentalGroup.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()) {

			DocumentLinkedAnnotation docLinkedRootAnnotation = experimentalGroup.getRootAnnotation()
					.asInstanceOfDocumentLinkedAnnotation();

			tokens.add(docLinkedRootAnnotation.relatedTokens);

		}
		if (SCIOSlotTypes.hasGroupName.isIncluded()) {

			for (AbstractAnnotation groupName : experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasGroupName)
					.getSlotFiller()) {

				if (groupName.isInstanceOfDocumentLinkedAnnotation())
					tokens.add(groupName.asInstanceOfDocumentLinkedAnnotation().relatedTokens);
			}
		}
		return tokens;
	}

	@Override
	public void generateFeatureVector(Factor<ContextBetweenScope> factor) {

		final DoubleVector featureVector = factor.getFeatureVector();
		final List<DocumentToken> tokens = factor.getFactorScope().instance.getDocument().tokenList;
		final EntityType entity = factor.getFactorScope().entity;
		final int beginTokenIndex = factor.getFactorScope().startTokenIndex;
		final int endTokenIndex = factor.getFactorScope().endTokenIndex;
		final int cardinality = factor.getFactorScope().cardinality;

		final String[] leftContext = extractLeftContext(tokens, beginTokenIndex);

		final String[] rightContext = extractRightContext(tokens, endTokenIndex);

		getContextFeatures(featureVector, entity.name, leftContext, rightContext, cardinality);

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

	private void getContextFeatures(DoubleVector featureVector, final String entityName, final String[] leftContext,
			final String[] rightContext, final int cardinality) {

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
			featureVector.set(PREFIX
					+ new StringBuffer(lCs).append(LEFT).append(entityName).append(RIGHT).append(rCs).toString().trim()
					+ ", cardinality = " + (cardinality), true);

			for (int j = 0; j < rightContext.length; j++) {
				eof = rightContext[j] == null;
				if (eof)
					context = EOF;
				else
					context = rightContext[j];
				rCs.append(SPLITTER).append(context);
				featureVector.set(PREFIX + new StringBuffer(lCs).append(LEFT).append(entityName).append(RIGHT)
						.append(rCs).toString().trim() + ", cardinality = " + (cardinality), true);

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
			featureVector.set(PREFIX
					+ new StringBuffer(lCs).append(LEFT).append(entityName).append(RIGHT).append(rCs).toString().trim()
					+ ", cardinality = " + (cardinality), true);

			for (int j = 0; j < leftContext.length; j++) {
				bof = leftContext[j] == null;
				if (bof)
					context = BOF;
				else
					context = leftContext[j];
				lCs.insert(0, context + SPLITTER);
				featureVector.set(PREFIX + new StringBuffer(lCs).append(LEFT).append(entityName).append(RIGHT)
						.append(rCs).toString().trim() + ", cardinality = " + (cardinality), true);

				if (bof)
					break;
			}

			if (eof)
				break;
		}
	}

	private String simplify(String trim) {
//		trim = trim.replaceAll("\\d", "#");
//		trim = trim.toLowerCase();
		return trim;
	}

}