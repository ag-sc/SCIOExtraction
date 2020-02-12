package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.DoubleVector;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.templates.IntraTokenCardinalityTemplate.IntraTokenScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class IntraTokenCardinalityTemplate extends AbstractFeatureTemplate<IntraTokenScope> {

	/**
	 * 
	 */

	private static final String TOKEN_SPLITTER_SPACE = " ";

	private static final String END_SIGN = "$";

	private static final String START_SIGN = "^";

	private static final int MIN_TOKEN_LENGTH = 2;

	private static final String LEFT = "<";

	private static final String RIGHT = ">";

	private static final String PREFIX = "ITT\t";

	static class IntraTokenScope extends AbstractFactorScope {

		public EntityType entityType;
		public final String surfaceForm;
		public final int cardinality;

		public IntraTokenScope(AbstractFeatureTemplate<IntraTokenScope> template, EntityType entityType,
				String surfaceForm, int cardinality) {
			super(template);
			this.cardinality = cardinality;
			this.entityType = entityType;
			this.surfaceForm = surfaceForm;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + cardinality;
			result = prime * result + ((entityType == null) ? 0 : entityType.hashCode());
			result = prime * result + ((surfaceForm == null) ? 0 : surfaceForm.hashCode());
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
			IntraTokenScope other = (IntraTokenScope) obj;
			if (cardinality != other.cardinality)
				return false;
			if (entityType == null) {
				if (other.entityType != null)
					return false;
			} else if (!entityType.equals(other.entityType))
				return false;
			if (surfaceForm == null) {
				if (other.surfaceForm != null)
					return false;
			} else if (!surfaceForm.equals(other.surfaceForm))
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
			return "IntraTokenScope [entityType=" + entityType + ", surfaceForm=" + surfaceForm + "]";
		}

	}

	@Override
	public List<IntraTokenScope> generateFactorScopes(State state) {
		List<IntraTokenScope> factors = new ArrayList<>();

		for (EntityTemplate experimentalGroup : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (experimentalGroup.getEntityType() != SCIOEntityTypes.definedExperimentalGroup)
				continue;
		
			if(SCIOSlotTypes.hasTreatmentType.isExcluded())
				continue;
			
			int cardinality = experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).size();

			if (experimentalGroup.asInstanceOfEntityTemplate().getRootAnnotation().isInstanceOfLiteralAnnotation())
				factors.add(
						new IntraTokenScope(
								this, experimentalGroup.getEntityType(), experimentalGroup.asInstanceOfEntityTemplate()
										.getRootAnnotation().asInstanceOfLiteralAnnotation().getSurfaceForm(),
								cardinality));
			if (SCIOSlotTypes.hasGroupName.isIncluded()) {

				for (AbstractAnnotation groupName : experimentalGroup.getMultiFillerSlot(SCIOSlotTypes.hasGroupName)
						.getSlotFiller()) {

					factors.add(new IntraTokenScope(this, experimentalGroup.getEntityType(),
							groupName.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm(), cardinality));

				}
			}
		}
		return factors;
	}

	@Override
	public void generateFeatureVector(Factor<IntraTokenScope> factor) {

		getTokenNgrams(factor.getFeatureVector(), factor.getFactorScope().entityType.name,
				factor.getFactorScope().surfaceForm, factor.getFactorScope().cardinality);

//		for (EntityType e : factor.getFactorScope().entityType.getDirectSuperEntityTypes()) {
//
//			getTokenNgrams(factor.getFeatureVector(), e.entityName, factor.getFactorScope().surfaceForm);
//		}

	}

	private void getTokenNgrams(DoubleVector featureVector, String name, String surfaceForm, final int cardinality) {

		final String cM = START_SIGN + TOKEN_SPLITTER_SPACE + surfaceForm + TOKEN_SPLITTER_SPACE + END_SIGN;

		final String[] tokens = cM.split(TOKEN_SPLITTER_SPACE);

		final int maxNgramSize = tokens.length;

		featureVector.set(PREFIX + LEFT + name + RIGHT + TOKEN_SPLITTER_SPACE + cM + ", cardinality " + cardinality,
				true);

		for (int ngram = 1; ngram < maxNgramSize; ngram++) {
			for (int i = 0; i < maxNgramSize - 1; i++) {

				/*
				 * Do not include start symbol.
				 */
				if (i + ngram == 1)
					continue;

				/*
				 * Break if size exceeds token length
				 */
				if (i + ngram > maxNgramSize)
					break;

				final StringBuffer fBuffer = new StringBuffer();
				for (int t = i; t < i + ngram; t++) {

					if (tokens[t].isEmpty())
						continue;

//					if (Document.getStopWords().contains(tokens[t].toLowerCase()))
//						continue;

					fBuffer.append(tokens[t]).append(TOKEN_SPLITTER_SPACE);

				}

				final String featureName = fBuffer.toString().trim();

				if (featureName.length() < MIN_TOKEN_LENGTH)
					continue;

				if (featureName.isEmpty())
					continue;

				featureVector.set(PREFIX + LEFT + name + RIGHT + TOKEN_SPLITTER_SPACE + featureName + ", cardinality "
						+ cardinality, true);

			}
		}

	}

}
