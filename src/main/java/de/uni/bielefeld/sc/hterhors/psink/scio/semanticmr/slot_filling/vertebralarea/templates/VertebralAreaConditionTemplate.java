package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.templates;

import java.util.ArrayList;
import java.util.List;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.DoubleVector;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.templates.VertebralAreaConditionTemplate.VertebralLocationScope;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class VertebralAreaConditionTemplate extends AbstractFeatureTemplate<VertebralLocationScope> {

	private static final String VERTEBRAL_AREA_VALID_ORDINAL = "VertebralAreaValidOrdnial";

	private static final String VERTEBRAL_AREA_SAME_SEGMENT = "VertebralAreaSameSegment";

	private static final String VERTEBRAL_AREA_ORDINAL_DISTANCE_EQ0 = "VertebralAreaOrdinalDistance==0";
	private static final String VERTEBRAL_AREA_ORDINAL_DISTANCE_EQ1 = "VertebralAreaOrdinalDistance==1";
	private static final String VERTEBRAL_AREA_ORDINAL_DISTANCE_EQ2 = "VertebralAreaOrdinalDistance==2";
	private static final String VERTEBRAL_AREA_ORDINAL_DISTANCE_EQ3 = "VertebralAreaOrdinalDistance==3";
	private static final String VERTEBRAL_AREA_ORDINAL_DISTANCE_GR3 = "VertebralAreaOrdinalDistance>3";

	static class VertebralLocationScope extends AbstractFactorScope {

		public final String condition;
		public final boolean matchesCondition;

		public VertebralLocationScope(AbstractFeatureTemplate<?> template, String condition, boolean matchesCondition) {
			super(template);
			this.condition = condition;
			this.matchesCondition = matchesCondition;
		}

		@Override
		public int implementHashCode() {
			return 0;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((condition == null) ? 0 : condition.hashCode());
			result = prime * result + (matchesCondition ? 1231 : 1237);
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
			VertebralLocationScope other = (VertebralLocationScope) obj;
			if (condition == null) {
				if (other.condition != null)
					return false;
			} else if (!condition.equals(other.condition))
				return false;
			if (matchesCondition != other.matchesCondition)
				return false;
			return true;
		}

		@Override
		public boolean implementEquals(Object obj) {
			return false;
		}

	}

	private static final String PREFIX = "VLCT\t";

	@Override
	public List<VertebralLocationScope> generateFactorScopes(State state) {
		List<VertebralLocationScope> factors = new ArrayList<>();

		for (EntityTemplate vertebralArea : super.<EntityTemplate>getPredictedAnnotations(state)) {

			if (vertebralArea.getEntityType() != SCIOEntityTypes.vertebralArea)
				continue;
			if (SCIOSlotTypes.hasLowerVertebrae.isExcluded() || SCIOSlotTypes.hasUpperVertebrae.isExcluded())
				continue;
			
			SingleFillerSlot lower = vertebralArea.getSingleFillerSlot(SCIOSlotTypes.hasLowerVertebrae);
			SingleFillerSlot upper = vertebralArea.getSingleFillerSlot(SCIOSlotTypes.hasUpperVertebrae);

			if (!(lower.containsSlotFiller() && upper.containsSlotFiller()))
				continue;

			EntityType lowerVertebrae = lower.getSlotFiller().getEntityType();
			EntityType upperVertebrae = upper.getSlotFiller().getEntityType();

			final int lowerVertebraeOrdinal = getOrdinal(lowerVertebrae);
			final int upperVertebraeOrdinal = getOrdinal(upperVertebrae);

			addFactor(factors, VERTEBRAL_AREA_VALID_ORDINAL, lowerVertebraeOrdinal > upperVertebraeOrdinal);

			addFactor(factors, VERTEBRAL_AREA_SAME_SEGMENT, sameSegment(lowerVertebraeOrdinal, upperVertebraeOrdinal));

			addFactor(factors, VERTEBRAL_AREA_ORDINAL_DISTANCE_EQ0,
					(upperVertebraeOrdinal - lowerVertebraeOrdinal) == 0);
			addFactor(factors, VERTEBRAL_AREA_ORDINAL_DISTANCE_EQ1,
					(upperVertebraeOrdinal - lowerVertebraeOrdinal) == 1);
			addFactor(factors, VERTEBRAL_AREA_ORDINAL_DISTANCE_EQ2,
					(upperVertebraeOrdinal - lowerVertebraeOrdinal) == 2);
			addFactor(factors, VERTEBRAL_AREA_ORDINAL_DISTANCE_EQ3,
					(upperVertebraeOrdinal - lowerVertebraeOrdinal) == 3);
			addFactor(factors, VERTEBRAL_AREA_ORDINAL_DISTANCE_GR3,
					(upperVertebraeOrdinal - lowerVertebraeOrdinal) > 3);

		}

		return factors;

	}

	private boolean sameSegment(int lowerVertebraeOrdinal, int upperVertebraeOrdinal) {

		if (lowerVertebraeOrdinal < 4 && upperVertebraeOrdinal < 4)
			return true;

		if (lowerVertebraeOrdinal >= 4 && upperVertebraeOrdinal >= 4 && lowerVertebraeOrdinal < 15
				&& upperVertebraeOrdinal < 15)
			return true;

		if (lowerVertebraeOrdinal >= 15 && upperVertebraeOrdinal >= 15)
			return true;

		return false;
	}

	private int getOrdinal(EntityType entityType) {

		if (entityType.name.equals("L1"))
			return 0;
		if (entityType.name.equals("L2"))
			return 1;
		if (entityType.name.equals("L3"))
			return 2;
		if (entityType.name.equals("L4"))
			return 3;
		if (entityType.name.equals("T1"))
			return 4;
		if (entityType.name.equals("T2"))
			return 5;
		if (entityType.name.equals("T3"))
			return 6;
		if (entityType.name.equals("T4"))
			return 7;
		if (entityType.name.equals("T5"))
			return 8;
		if (entityType.name.equals("T6"))
			return 9;
		if (entityType.name.equals("T7"))
			return 10;
		if (entityType.name.equals("T8"))
			return 11;
		if (entityType.name.equals("T9"))
			return 12;
		if (entityType.name.equals("T10"))
			return 13;
		if (entityType.name.equals("T11"))
			return 14;
		if (entityType.name.equals("C1"))
			return 15;
		if (entityType.name.equals("C2"))
			return 16;
		if (entityType.name.equals("C3"))
			return 17;
		if (entityType.name.equals("C4"))
			return 18;
		if (entityType.name.equals("C5"))
			return 19;
		if (entityType.name.equals("C6"))
			return 20;
		if (entityType.name.equals("C7"))
			return 21;
		return -1;
	}

	private void addFactor(List<VertebralLocationScope> factors, String condition, boolean matchesCondition) {
		factors.add(new VertebralLocationScope(this, condition, matchesCondition));
	}

	@Override
	public void generateFeatureVector(Factor<VertebralLocationScope> factor) {

		DoubleVector featureVector = factor.getFeatureVector();

		featureVector.set(PREFIX + factor.getFactorScope().condition, factor.getFactorScope().matchesCondition);
//		featureVector.set(PREFIX + "NOT_" + factor.getFactorScope().condition,
//				!factor.getFactorScope().matchesCondition);

	}

}
