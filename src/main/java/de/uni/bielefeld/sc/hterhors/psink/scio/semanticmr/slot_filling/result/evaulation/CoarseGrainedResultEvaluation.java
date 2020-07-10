package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.evaulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.of.IObjectiveFunction;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.wrapper.DefinedExperimentalGroup;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Result;

/**
 * 
 * Conver Result to tuple. Evaluates tuples.
 * 
 * (GroupID,Trend,InvestigationMethod))
 * 
 * 
 * (ID25, ID26, increase, BBBTest)
 * 
 * 
 * Mapping um IDs zu bekommen.
 * 
 * @author hterhors
 *
 */
public class CoarseGrainedResultEvaluation {

	public static void evaluateCoarsGrained(IObjectiveFunction objectiveFunction, Map<Instance, State> results) {

		Map<Instance, List<EntityTemplate>> goldResults = new HashMap<>();
		Map<Instance, List<EntityTemplate>> predictedResults = new HashMap<>();
		Score overall = new Score(EScoreType.MACRO);
		for (State finalState : results.values()) {
			goldResults.put(finalState.getInstance(), new ArrayList<>());
			predictedResults.put(finalState.getInstance(), new ArrayList<>());
			/*
			 * Sort experimental groups of results
			 */
			for (AbstractAnnotation result : finalState.getGoldAnnotations().getAnnotations()) {
				goldResults.get(finalState.getInstance()).add(sortResult(result));

			}
			for (AbstractAnnotation result : finalState.getCurrentPredictions().getAnnotations()) {
				predictedResults.get(finalState.getInstance()).add(sortResult(result));

			}

			List<EntityTemplate> goldAnnotations = goldResults.get(finalState.getInstance());
			List<EntityTemplate> predictedAnnotations = predictedResults.get(finalState.getInstance());

			Map<EntityTemplate, String> mapGoldGroupToID = new HashMap<>();

			Map<EntityTemplate, String> mapPredictedGroupToID = new HashMap<>();

			getBestMappings(objectiveFunction, goldAnnotations, predictedAnnotations, mapGoldGroupToID,
					mapPredictedGroupToID);

			Score adder = scoreCoarsGrainedProperty(objectiveFunction, goldAnnotations, predictedAnnotations,
					mapGoldGroupToID, mapPredictedGroupToID);

			overall.add(adder);

			 break;
		}
System.out.println(overall);
	}

	private static void getBestMappings(IObjectiveFunction objectiveFunction, List<EntityTemplate> goldAnnotations,
			List<EntityTemplate> predictedAnnotations, Map<EntityTemplate, String> mapGoldGroupToID,
			Map<EntityTemplate, String> mapPredictedGroupToID) {

		/*
		 * Get experimental groups and their assignments to get IDs
		 */
		List<EntityTemplate> goldExperimentalGroups = new ArrayList<>(
				goldAnnotations.stream().flatMap(g -> new Result(g).getDefinedExperimentalGroups().stream())
						.map(g -> g.get()).distinct().collect(Collectors.toSet()));
		List<EntityTemplate> predictedExperimentalGroups = new ArrayList<>(
				predictedAnnotations.stream().flatMap(g -> new Result(g).getDefinedExperimentalGroups().stream())
						.map(g -> g.get()).collect(Collectors.toSet()));

		x: for (EntityTemplate goldGroup : goldExperimentalGroups) {

			for (EntityTemplate entityTemplate : mapGoldGroupToID.keySet()) {
				if (entityTemplate.evaluateEquals(objectiveFunction.getEvaluator(), goldGroup))
					continue x;
			}

			mapGoldGroupToID.put(goldGroup, "ExperimentalGroup_" + mapGoldGroupToID.size());
		}

		for (EntityTemplate predictedGroup : predictedExperimentalGroups) {
//			System.out.println("Predict: ");
//			System.out.println(predictedGroup.toPrettyString());
			Score maxScore = new Score();

			for (EntityTemplate goldGroup : goldExperimentalGroups) {

//				System.out.println("Gold: ");
//				System.out.println(goldGroup.toPrettyString());
				Score score = objectiveFunction.getEvaluator().scoreSingle(goldGroup, predictedGroup);
//				System.out.println(score);

				if (score.getF1() > maxScore.getF1()) {
					maxScore = score;
					mapPredictedGroupToID.put(predictedGroup, mapGoldGroupToID.get(goldGroup));
				}
			}
			System.out.println(maxScore);
		}
	}

	private static Score scoreCoarsGrainedProperty(IObjectiveFunction objectiveFunction,
			List<EntityTemplate> goldAnnotations, List<EntityTemplate> predictedAnnotations,
			Map<EntityTemplate, String> mapGoldGroupToID, Map<EntityTemplate, String> mapPredictedGroupToID) {

		List<Integer> bestAssignment = (objectiveFunction.getEvaluator()).getBestAssignment(goldAnnotations,
				predictedAnnotations, EScoreType.MACRO);
		Score score = new Score(EScoreType.MACRO);

		for (int goldIndex = 0; goldIndex < bestAssignment.size(); goldIndex++) {
			final int predictionIndex = bestAssignment.get(goldIndex);

			EntityTemplate goldAnnotation = goldAnnotations.size() > goldIndex ? goldAnnotations.get(goldIndex) : null;

//			System.out.println("GOLD:" + goldAnnotation.toPrettyString());

			CoarseGrainedTuple goldTuple = convertToTuple(mapGoldGroupToID, goldAnnotation);

			EntityTemplate predictedAnnotation = predictedAnnotations.size() > predictionIndex
					? predictedAnnotations.get(predictionIndex)
					: null;

//			if (predictedAnnotation != null)
//				System.out.println("PREDICT:" + predictedAnnotation.toPrettyString());

			CoarseGrainedTuple predictedTuple = convertToTuple(mapPredictedGroupToID, predictedAnnotation);

//			System.out.println("Compare GOLD: ");
//			System.out.println(goldTuple);
//			System.out.println("With PRED: ");
//			System.out.println(predictedTuple);
//			System.out.println(goldTuple.equals(predictedTuple));

			Score adder = new Score();

			if (goldTuple.equals(predictedTuple))
				adder.increaseTruePositive();
			else
				adder.increaseFalseNegative();

			score.add(adder.toMacro());
		}

		return score.toMacro();
	}

	private static CoarseGrainedTuple convertToTuple(Map<EntityTemplate, String> mapPredictedGroupToID,
			EntityTemplate predictedAnnotation) {

		AbstractAnnotation predictedReferenceGroup = null;
		AbstractAnnotation predictedTargetGroup = null;
		AbstractAnnotation predictedTrend = null;
		AbstractAnnotation predictedInvestigationMethod = null;

		if (predictedAnnotation != null) {
			predictedReferenceGroup = predictedAnnotation.getSingleFillerSlot(SCIOSlotTypes.hasReferenceGroup)
					.getSlotFiller();
		}
		if (predictedAnnotation != null) {
			predictedTargetGroup = predictedAnnotation.getSingleFillerSlot(SCIOSlotTypes.hasTargetGroup)
					.getSlotFiller();
		}
		if (predictedAnnotation != null) {
			predictedTrend = predictedAnnotation.getSingleFillerSlot(SCIOSlotTypes.hasReferenceGroup).getSlotFiller();
		}
		if (predictedAnnotation != null) {
			predictedInvestigationMethod = predictedAnnotation.getSingleFillerSlot(SCIOSlotTypes.hasReferenceGroup)
					.getSlotFiller();
		}

		String referenceGroupID = null;
		String targetGroupID = null;
		String investigationMethod = null;
		String trend = null;

		if (predictedReferenceGroup != null) {
			referenceGroupID = mapPredictedGroupToID.get(predictedReferenceGroup);
		}
		if (predictedTargetGroup != null) {
			targetGroupID = mapPredictedGroupToID.get(predictedTargetGroup);
		}
		if (predictedTrend != null) {
			trend = getTrend(predictedTrend);
		}
		if (predictedTrend != null) {
			investigationMethod = getInvestigationMethod(predictedInvestigationMethod);
		}

		return new CoarseGrainedTuple(referenceGroupID, targetGroupID, investigationMethod, trend);
	}

	private static String getInvestigationMethod(AbstractAnnotation predictedInvestigationMethod) {

		return null;
	}

	private static String getTrend(AbstractAnnotation predictedTrend) {

		return null;
	}

	private static EntityTemplate sortResult(AbstractAnnotation result) {

		AbstractAnnotation referenceFiller = result.asInstanceOfEntityTemplate()
				.getSingleFillerSlot(SCIOSlotTypes.hasReferenceGroup).getSlotFiller();
		AbstractAnnotation targetFiller = result.asInstanceOfEntityTemplate()
				.getSingleFillerSlot(SCIOSlotTypes.hasTargetGroup).getSlotFiller();

		if (referenceFiller == null && targetFiller == null)
			return result.asInstanceOfEntityTemplate();
		if (referenceFiller == null && targetFiller != null)
			return result.asInstanceOfEntityTemplate();

		DefinedExperimentalGroup def1 = new DefinedExperimentalGroup(referenceFiller.asInstanceOfEntityTemplate());

		DefinedExperimentalGroup def2 = new DefinedExperimentalGroup(targetFiller.asInstanceOfEntityTemplate());

		DefinedExperimentalGroup ref = null;
		DefinedExperimentalGroup target = null;

		boolean change = !doNotSwitchPos(def1, def2);
		if (change) {
			ref = def2;
			target = def1;
		} else {
			ref = def1;
			target = def2;
		}

		EntityTemplate newResult = result.asInstanceOfEntityTemplate().deepCopy();

		if (change) {

			AbstractAnnotation trend = result.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasTrend)
					.getSlotFiller();

			if (trend != null) {

				AbstractAnnotation differenceFiller = trend.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SCIOSlotTypes.hasDifference).getSlotFiller();

				if (differenceFiller != null) {

					EntityType difference = differenceFiller.getEntityType();

					if (difference == EntityType.get("Higher"))
						difference = EntityType.get("Lower");
					else if (difference == EntityType.get("Lower"))
						difference = EntityType.get("Higher");
					else if (difference == EntityType.get("FasterIncrease"))
						difference = EntityType.get("SlowerIncrease");
					else if (difference == EntityType.get("SlowerIncrease"))
						difference = EntityType.get("FasterIncrease");

					newResult.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasTrend).getSlotFiller()
							.asInstanceOfEntityTemplate()
							.setSingleSlotFiller(SCIOSlotTypes.hasDifference, AnnotationBuilder.toAnnotation(
									differenceFiller.asInstanceOfDocumentLinkedAnnotation().document, difference,
									differenceFiller.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm(),
									differenceFiller.asInstanceOfDocumentLinkedAnnotation().getStartDocCharOffset()));
				}

			}
		}

		newResult.setSingleSlotFiller(SCIOSlotTypes.hasReferenceGroup, ref.get());
		newResult.setSingleSlotFiller(SCIOSlotTypes.hasTargetGroup, target.get());
		return newResult;
	}

	private static boolean doNotSwitchPos(DefinedExperimentalGroup toCheck, DefinedExperimentalGroup basedOn) {

		Set<EntityType> referenceTreats = toCheck.getRelevantTreatments().stream().map(e -> e.getEntityType())
				.collect(Collectors.toSet());

		Set<EntityType> targetTreats = basedOn.getRelevantTreatments().stream().map(e -> e.getEntityType())
				.collect(Collectors.toSet());

		boolean referenceContainsVehicle = containsVehicle(referenceTreats);
		boolean targetContainsVehicle = containsVehicle(targetTreats);

		boolean referenceContainsOEC = containsOEC(referenceTreats);
		boolean targetContainsOEC = containsOEC(targetTreats);

		if (targetTreats.containsAll(referenceTreats))
			return true;

		if (referenceContainsOEC && targetContainsOEC)
			return false;

		if (referenceTreats.isEmpty() && !targetTreats.isEmpty())
			return true;

		if (!referenceTreats.isEmpty() && targetTreats.isEmpty())
			return false;

		if (referenceContainsOEC && !targetContainsOEC) {
			return false;
		}

		if (!referenceContainsOEC && targetContainsOEC) {
			return true;
		}

		if (referenceContainsVehicle && !targetContainsVehicle) {
			return true;
		}
		if (!referenceContainsVehicle && targetContainsVehicle) {
			return false;
		}

		if (!referenceContainsVehicle && !referenceContainsOEC && targetContainsOEC) {
			return true;
		}

		if (!referenceContainsOEC && !targetContainsOEC)
			return true;

		throw new IllegalStateException();
	}

	private static boolean containsVehicle(Set<EntityType> toCheckTreats) {
		boolean toCheckContainsVehicle = false;
		for (EntityType entityType : EntityType.get("Vehicle").getRelatedEntityTypes()) {
			toCheckContainsVehicle |= toCheckTreats.contains(entityType);
			if (toCheckContainsVehicle)
				break;
		}
		return toCheckContainsVehicle;
	}

	private static boolean containsOEC(Set<EntityType> r) {
		return r.contains(EntityType.get("OlfactoryEnsheathingGliaCell"));
	}

	static class CoarseGrainedTuple {

		public final String referenceGroupID;
		public final String targetGroupID;
		public final String investigationMethod;
		public final String trend;

		public CoarseGrainedTuple(String referenceGroupID, String targetGroupID, String investigationMethod,
				String trend) {
			super();
			this.referenceGroupID = referenceGroupID;
			this.targetGroupID = targetGroupID;
			this.investigationMethod = investigationMethod;
			this.trend = trend;
		}

		@Override
		public String toString() {
			return "CoarseGrainedTuple [referenceGroupID=" + referenceGroupID + ", targetGroupID=" + targetGroupID
					+ ", investigationMethod=" + investigationMethod + ", trend=" + trend + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((investigationMethod == null) ? 0 : investigationMethod.hashCode());
			result = prime * result + ((referenceGroupID == null) ? 0 : referenceGroupID.hashCode());
			result = prime * result + ((targetGroupID == null) ? 0 : targetGroupID.hashCode());
			result = prime * result + ((trend == null) ? 0 : trend.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CoarseGrainedTuple other = (CoarseGrainedTuple) obj;
			if (investigationMethod == null) {
				if (other.investigationMethod != null)
					return false;
			} else if (!investigationMethod.equals(other.investigationMethod))
				return false;
			if (referenceGroupID == null) {
				if (other.referenceGroupID != null)
					return false;
			} else if (!referenceGroupID.equals(other.referenceGroupID))
				return false;
			if (targetGroupID == null) {
				if (other.targetGroupID != null)
					return false;
			} else if (!targetGroupID.equals(other.targetGroupID))
				return false;
			if (trend == null) {
				if (other.trend != null)
					return false;
			} else if (!trend.equals(other.trend))
				return false;
			return true;
		}

	}

}
