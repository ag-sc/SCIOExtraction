package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.evaluation;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.crf.of.IObjectiveFunction;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.STDEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.wrapper.DefinedExperimentalGroup;

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
public class CoarseGrainedExpGroupEvaluation {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	public static Score evaluateCoarsGrained(PrintStream ps, IObjectiveFunction objectiveFunction,
			Map<Instance, State> results) {

		Map<Instance, List<EntityTemplate>> goldResults = new HashMap<>();
		Map<Instance, List<EntityTemplate>> predictedResults = new HashMap<>();
		Score overall = new Score(EScoreType.MACRO);
		Score overallOrgModel = new Score(EScoreType.MACRO);
		Score overallInjury = new Score(EScoreType.MACRO);
		Score overallTreatment = new Score(EScoreType.MACRO);
		for (State finalState : results.values()) {

//			System.out.println(finalState.getInstance().getName());

			goldResults.put(finalState.getInstance(), new ArrayList<>());
			predictedResults.put(finalState.getInstance(), new ArrayList<>());
			/*
			 * Sort experimental groups of results
			 */
			for (AbstractAnnotation result : finalState.getGoldAnnotations().getAnnotations()) {
				goldResults.get(finalState.getInstance()).add(result.asInstanceOfEntityTemplate());

			}
			for (AbstractAnnotation result : finalState.getCurrentPredictions().getAnnotations()) {
				predictedResults.get(finalState.getInstance()).add(result.asInstanceOfEntityTemplate());

			}

			List<EntityTemplate> goldAnnotations = goldResults.get(finalState.getInstance());
			List<EntityTemplate> predictedAnnotations = predictedResults.get(finalState.getInstance());

			Map<EntityTemplate, String> mapGoldGroupToID = new HashMap<>();

			Map<EntityTemplate, String> mapPredictedGroupToID = new HashMap<>();

			getBestOrgModelMappings(objectiveFunction, goldAnnotations, predictedAnnotations, mapGoldGroupToID,
					mapPredictedGroupToID);
			getBestInjuryMappings(objectiveFunction, goldAnnotations, predictedAnnotations, mapGoldGroupToID,
					mapPredictedGroupToID);
			getBestTreatmentMappings(objectiveFunction, goldAnnotations, predictedAnnotations, mapGoldGroupToID,
					mapPredictedGroupToID);

			scoreCoarsGrainedProperty(overall, overallOrgModel, overallInjury, overallTreatment, objectiveFunction,
					goldAnnotations, predictedAnnotations, mapGoldGroupToID, mapPredictedGroupToID);

		}

		log.info("MACRO CoarseGrained overallOrgModel: " + overallOrgModel);
		log.info("MACRO CoarseGrained overallInjury: " + overallInjury);
		log.info("MACRO CoarseGrained overallTreatment: " + overallTreatment);
		log.info("MACRO CoarseGrained overall: " + overall);
		
		ps.println("MACRO CoarseGrained overallOrgModel: " + overallOrgModel);
		ps.println("MACRO CoarseGrained overallInjury: " + overallInjury);
		ps.println("MACRO CoarseGrained overallTreatment: " + overallTreatment);
		ps.println("MACRO CoarseGrained overall: " + overall);
		return overall;
	}

	private static void getBestOrgModelMappings(IObjectiveFunction objectiveFunction,
			List<EntityTemplate> goldAnnotations, List<EntityTemplate> predictedAnnotations,
			Map<EntityTemplate, String> mapGoldGroupToID, Map<EntityTemplate, String> mapPredictedGroupToID) {

		/*
		 * Get experimental groups and their assignments to get IDs
		 */
		List<EntityTemplate> goldData = new ArrayList<>(
				goldAnnotations.stream().map(g -> new DefinedExperimentalGroup(g).getOrganismModel())
						.filter(a -> a != null).distinct().collect(Collectors.toSet()));
		List<EntityTemplate> predictedData = new ArrayList<>(
				predictedAnnotations.stream().map(g -> new DefinedExperimentalGroup(g).getOrganismModel())
						.filter(a -> a != null).collect(Collectors.toSet()));

		x: for (EntityTemplate gold : goldData) {

			for (EntityTemplate entityTemplate : mapGoldGroupToID.keySet()) {
				if (entityTemplate.evaluateEquals(objectiveFunction.getEvaluator(), gold))
					continue x;
			}

			mapGoldGroupToID.put(gold, "OrganismModel_" + mapGoldGroupToID.size());
		}

		for (EntityTemplate prediction : predictedData) {
//			System.out.println("Predict: ");
//			System.out.println(predictedGroup.toPrettyString());
			Score maxScore = new Score();

			for (EntityTemplate goldGroup : goldData) {

//				System.out.println("Gold: ");
//				System.out.println(goldGroup.toPrettyString());
				Score score = objectiveFunction.getEvaluator().scoreSingle(goldGroup, prediction);
//				System.out.println(score);

				if (score.getF1() > maxScore.getF1()) {
					maxScore = score;
					mapPredictedGroupToID.put(prediction, mapGoldGroupToID.get(goldGroup));
				}
			}
		}
	}

	private static void getBestInjuryMappings(IObjectiveFunction objectiveFunction,
			List<EntityTemplate> goldAnnotations, List<EntityTemplate> predictedAnnotations,
			Map<EntityTemplate, String> mapGoldGroupToID, Map<EntityTemplate, String> mapPredictedGroupToID) {

		/*
		 * Get experimental groups and their assignments to get IDs
		 */
		List<EntityTemplate> goldData = new ArrayList<>(
				goldAnnotations.stream().map(g -> new DefinedExperimentalGroup(g).getInjury()).filter(a -> a != null)
						.distinct().collect(Collectors.toSet()));
		List<EntityTemplate> predictedData = new ArrayList<>(
				predictedAnnotations.stream().map(g -> new DefinedExperimentalGroup(g).getInjury())
						.filter(a -> a != null).collect(Collectors.toSet()));

		x: for (EntityTemplate gold : goldData) {

			for (EntityTemplate entityTemplate : mapGoldGroupToID.keySet()) {
				if (entityTemplate.evaluateEquals(objectiveFunction.getEvaluator(), gold))
					continue x;
			}

			mapGoldGroupToID.put(gold, "Injury_" + mapGoldGroupToID.size());
		}

		for (EntityTemplate prediction : predictedData) {
//			System.out.println("Predict: ");
//			System.out.println(predictedGroup.toPrettyString());
			Score maxScore = new Score();

			for (EntityTemplate goldGroup : goldData) {

//				System.out.println("Gold: ");
//				System.out.println(goldGroup.toPrettyString());
				Score score = objectiveFunction.getEvaluator().scoreSingle(goldGroup, prediction);
//				System.out.println(score);

				if (score.getF1() > maxScore.getF1()) {
					maxScore = score;
					mapPredictedGroupToID.put(prediction, mapGoldGroupToID.get(goldGroup));
				}
			}
		}
	}

	private static void getBestTreatmentMappings(IObjectiveFunction objectiveFunction,
			List<EntityTemplate> goldAnnotations, List<EntityTemplate> predictedAnnotations,
			Map<EntityTemplate, String> mapGoldGroupToID, Map<EntityTemplate, String> mapPredictedGroupToID) {

		/*
		 * Get experimental groups and their assignments to get IDs
		 */
		List<EntityTemplate> goldData = new ArrayList<>(
				goldAnnotations.stream().flatMap(g -> new DefinedExperimentalGroup(g).getTreatments().stream())
						.filter(a -> a != null).distinct().collect(Collectors.toSet()));

		List<EntityTemplate> predictedData = new ArrayList<>(
				predictedAnnotations.stream().flatMap(g -> new DefinedExperimentalGroup(g).getTreatments().stream())
						.filter(a -> a != null).distinct().collect(Collectors.toSet()));

		x: for (EntityTemplate gold : goldData) {

			for (EntityTemplate entityTemplate : mapGoldGroupToID.keySet()) {
				if (entityTemplate.evaluateEquals(objectiveFunction.getEvaluator(), gold))
					continue x;
			}

			mapGoldGroupToID.put(gold, "Treatment_" + mapGoldGroupToID.size());
		}

		for (EntityTemplate prediction : predictedData) {
//			System.out.println("Predict: ");
//			System.out.println(predictedGroup.toPrettyString());
			Score maxScore = new Score();

			for (EntityTemplate goldGroup : goldData) {

//				System.out.println("Gold: ");
//				System.out.println(goldGroup.toPrettyString());
				Score score = objectiveFunction.getEvaluator().scoreSingle(goldGroup, prediction);
//				System.out.println(score);

				if (score.getF1() > maxScore.getF1()) {
					maxScore = score;
					mapPredictedGroupToID.put(prediction, mapGoldGroupToID.get(goldGroup));
				}
			}
		}
	}

	private static void scoreCoarsGrainedProperty(Score overallAll, Score overallOrgModelAll, Score overallInjuryAll,
			Score overallTreatmentAll, IObjectiveFunction objectiveFunction, List<EntityTemplate> goldAnnotations,
			List<EntityTemplate> predictedAnnotations, Map<EntityTemplate, String> mapGoldGroupToID,
			Map<EntityTemplate, String> mapPredictedGroupToID) {

		List<Integer> bestAssignment = (objectiveFunction.getEvaluator()).getBestAssignment(goldAnnotations,
				predictedAnnotations, EScoreType.MACRO);

		Score overall = new Score(EScoreType.MICRO);
		Score overallOrgModel = new Score(EScoreType.MICRO);
		Score overallInjury = new Score(EScoreType.MICRO);
		Score overallTreatment = new Score(EScoreType.MICRO);

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

			Score orgModel = new Score();
			Score injury = new Score();
			Score treatments = new Score();

			if (goldTuple.orgModel == predictedTuple.orgModel)
				orgModel.increaseTruePositive();
			else if (goldTuple.orgModel == null)
				orgModel.increaseFalsePositive();
			else
				orgModel.increaseFalseNegative();

			if (goldTuple.injury == predictedTuple.injury)
				injury.increaseTruePositive();
			else if (goldTuple.injury == null)
				injury.increaseFalsePositive();
			else
				injury.increaseFalseNegative();

			Score s = new Score(STDEvaluator.getTruePositives(goldTuple.treatments, predictedTuple.treatments),
					STDEvaluator.getFalsePositives(goldTuple.treatments, predictedTuple.treatments),
					STDEvaluator.getFalseNegatives(goldTuple.treatments, predictedTuple.treatments));
			treatments.add(s);
//			System.out.println(adder);

			overallOrgModel.add(orgModel);
			overallInjury.add(injury);
			overallTreatment.add(treatments);

			overall.add(orgModel);
			overall.add(injury);
			overall.add(treatments);
		}
		overallAll.add(overall.toMacro());
		overallOrgModelAll.add(overallOrgModel.toMacro());
		overallInjuryAll.add(overallInjury.toMacro());
		overallTreatmentAll.add(overallTreatment.toMacro());
	}

	private static CoarseGrainedTuple convertToTuple(Map<EntityTemplate, String> mapPredictedGroupToID,
			EntityTemplate predictedAnnotation) {

		AbstractAnnotation predictedOrgModel = null;
		AbstractAnnotation predictedInjury = null;
		Set<AbstractAnnotation> predictedTreatments = null;
//		AbstractAnnotation predictedInvestigationMethod = null;

		if (predictedAnnotation != null) {
			predictedOrgModel = predictedAnnotation.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel).getSlotFiller();
		}
		if (predictedAnnotation != null) {
			predictedInjury = predictedAnnotation.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).getSlotFiller();
		}
		if (predictedAnnotation != null) {
			predictedTreatments = predictedAnnotation.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType)
					.getSlotFiller();
		}

		String orgModel = null;
		String injury = null;
		Set<String> treatments = new HashSet<>();

		if (predictedOrgModel != null) {
			orgModel = mapPredictedGroupToID.get(predictedOrgModel);
		}
		if (predictedInjury != null) {
			injury = mapPredictedGroupToID.get(predictedInjury);
		}
		if (predictedTreatments != null) {
			for (AbstractAnnotation abstractAnnotation : predictedTreatments) {
				treatments.add(mapPredictedGroupToID.get(abstractAnnotation));
			}
		}

		return new CoarseGrainedTuple(orgModel, injury, treatments);
	}

	static class CoarseGrainedTuple {

		public final String orgModel;
		public final String injury;
		public final Set<String> treatments;

		public CoarseGrainedTuple(String referenceGroupID, String targetGroupID, Set<String> trend) {
			super();
			this.orgModel = referenceGroupID;
			this.injury = targetGroupID;
			this.treatments = trend;
		}

		@Override
		public String toString() {
			return "CoarseGrainedTuple [orgModel=" + orgModel + ", injury=" + injury + ", treatments=" + treatments
					+ "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((injury == null) ? 0 : injury.hashCode());
			result = prime * result + ((orgModel == null) ? 0 : orgModel.hashCode());
			result = prime * result + ((treatments == null) ? 0 : treatments.hashCode());
			return result;
		}

		public boolean isNull() {
			return orgModel == null && injury == null && treatments == null;
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
			if (injury == null) {
				if (other.injury != null)
					return false;
			} else if (!injury.equals(other.injury))
				return false;
			if (orgModel == null) {
				if (other.orgModel != null)
					return false;
			} else if (!orgModel.equals(other.orgModel))
				return false;
			if (treatments == null) {
				if (other.treatments != null)
					return false;
			} else if (!treatments.equals(other.treatments))
				return false;
			return true;
		}

	}

}
