package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.evaluation;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import de.hterhors.semanticmr.eval.AbstractEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.modes.Modes.ESimpleEvaluationMode;

public class ExperimentalGroupEvaluation {

	private final IObjectiveFunction predictionObjectiveFunction;
	private static Logger log = LogManager.getFormatterLogger("SlotFilling");
	private final AbstractEvaluator evaluator;

	public ExperimentalGroupEvaluation(IObjectiveFunction predictionObjectiveFunction, AbstractEvaluator evaluator) {
		this.predictionObjectiveFunction = predictionObjectiveFunction;
		this.evaluator = evaluator;
	}

	public Score evaluate(PrintStream ps, Map<Instance, State> results, EScoreType scoreType) {

		double cardinalityRMSE = 0;

		Map<Integer, Score> intervallCardinality = new HashMap<>();

		Score experimentalCardinality = new Score(scoreType);
		Score experimentalGroupComponentScore = new Score(scoreType);
		Score experimentalGroupOverallScore = new Score(scoreType);
		Score organismModelScore = new Score(scoreType);
		Score injuryModelScore = new Score(scoreType);
		Score vehicleScore = new Score(scoreType);
		Score nonVehicleScore = new Score(scoreType);
		Score bothS = new Score(scoreType);
//		double macroF1 = 0;
//		double macroPrecision = 0;
//		double macroRecall = 0;
//		int i = 0;

		for (Entry<Instance, State> e : results.entrySet()) {

			List<EntityTemplate> goldAnnotations = e.getValue().getGoldAnnotations().getAnnotations();
			List<EntityTemplate> predictedAnnotations = e.getValue().getCurrentPredictions().getAnnotations();

//			System.out.println(goldAnnotations.size());
//			System.out.println(predictedAnnotations.size());

			/*
			 * 
			 * Evaluate clustering of Treatments
			 */

//			i++;
//			log.info(e.getKey().getName());

			cardinalityRMSE += Math.pow(predictedAnnotations.size() - goldAnnotations.size(), 2);

//			System.out.println(cardinalityRMSE);

			for (int spread = 0; spread < 4; spread++) {
				intervallCardinality.putIfAbsent(spread, new Score(scoreType));

				int tp = Math.abs(goldAnnotations.size() - predictedAnnotations.size()) <= spread ? 1 : 0;
				int fn = tp == 1 ? 0 : 1;

				Score s = new Score(tp, 0, fn);

				if (scoreType == EScoreType.MACRO)
					s.toMacro();

//				System.out.println(s);

				intervallCardinality.get(spread).add(s);
			}
//			intervallCardinality.entrySet().forEach(System.out::println);

			int tp = Math.min(goldAnnotations.size(), predictedAnnotations.size());
			int fp = predictedAnnotations.size() > goldAnnotations.size()
					? predictedAnnotations.size() - goldAnnotations.size()
					: 0;
			int fn = predictedAnnotations.size() < goldAnnotations.size()
					? goldAnnotations.size() - predictedAnnotations.size()
					: 0;

			Score sC = new Score(tp, fp, fn);

			if (scoreType == EScoreType.MACRO)
				sC.toMacro();
//			System.out.println(sC);
//			System.out.println();
			experimentalCardinality.add(sC);

			List<Integer> bestAssignment = predictionObjectiveFunction.getEvaluator().getBestAssignment(goldAnnotations,
					predictedAnnotations, scoreType);
			if (SCIOSlotTypes.hasTreatmentType.isIncluded()) {

				Score both = treatmentsInExpGroupEvaluate(false, bestAssignment, goldAnnotations, predictedAnnotations,
						ESimpleEvaluationMode.BOTH, scoreType);
//				log.info(scoreType + "Both: " + both);

				Score vs = treatmentsInExpGroupEvaluate(false, bestAssignment, goldAnnotations, predictedAnnotations,
						ESimpleEvaluationMode.VEHICLE, scoreType);
//				log.info("Vehicles: " + vs);

				Score nvs = treatmentsInExpGroupEvaluate(false, bestAssignment, goldAnnotations, predictedAnnotations,
						ESimpleEvaluationMode.NON_VEHICLE, scoreType);
//				log.info("Non Vehicles: " + nvs);

				vehicleScore.add(vs);
				nonVehicleScore.add(nvs);
				bothS.add(both);
			}
			if (SCIOSlotTypes.hasOrganismModel.isIncluded()) {
				organismModelScore.add(organismModelInExpGroupEvaluate(bestAssignment, goldAnnotations,
						predictedAnnotations, scoreType));
			}
			if (SCIOSlotTypes.hasInjuryModel.isIncluded()) {
				injuryModelScore.add(injuryModelInExpGroupEvaluate(bestAssignment, goldAnnotations,
						predictedAnnotations, scoreType));
			}
//			macroF1 += score.getF1();
//			macroPrecision += score.getPrecision();
//			macroRecall += score.getRecall();
//			log.info("EXP GROUP INTERMEDIATE MACRO: F1 = " + macroF1 / i + ", P = " + macroPrecision / i + ", R = "
//					+ macroRecall / i);
//			log.info("EXP GROUP INTERMEDIATE MICRO: " + bothS);
//			log.info("");
			Score overall = predictionObjectiveFunction.getEvaluator().scoreMultiValues(goldAnnotations,
					predictedAnnotations, scoreType);
			experimentalGroupOverallScore.add(overall);
		}
		if (SCIOSlotTypes.hasTreatmentType.isIncluded()) {
			experimentalGroupComponentScore.add(bothS);
		}
		if (SCIOSlotTypes.hasOrganismModel.isIncluded()) {
			experimentalGroupComponentScore.add(organismModelScore);
		}
		if (SCIOSlotTypes.hasInjuryModel.isIncluded()) {
			experimentalGroupComponentScore.add(injuryModelScore);
		}

//		macroF1 /= results.entrySet().size();
//		macroPrecision /= results.entrySet().size();
//		macroRecall /= results.entrySet().size();
//		log.info("EXP GROUP MACRO: F1 = " + macroF1 + ", P = " + macroPrecision + ", R = " + macroRecall);
		log.info("EXP GROUP " + scoreType + "  CARDINALITY = " + experimentalCardinality);
		ps.println("EXP GROUP " + scoreType + "CARDINALITY = " + experimentalCardinality);
		final StringBuffer cardString = new StringBuffer();
		intervallCardinality.entrySet()
				.forEach(e -> cardString.append(e.getKey() + ":" + e.getValue().getRecall() + "\t"));
		log.info("EXP GROUP " + scoreType + "  INTERVALL CARDINALITY = " + cardString.toString().trim());
		ps.println("EXP GROUP " + scoreType + "  INTERVALL CARDINALITY = " + cardString.toString().trim());
		log.info("EXP GROUP " + scoreType + "  CARDINALITY RMSE = "
				+ Math.sqrt(cardinalityRMSE / results.entrySet().size()));
		ps.println("EXP GROUP " + scoreType + "  CARDINALITY RMSE = "
				+ Math.sqrt(cardinalityRMSE / results.entrySet().size()));
		log.info("EXP GROUP " + scoreType + "  OVERALL SCORE = " + experimentalGroupOverallScore);
		ps.println("EXP GROUP " + scoreType + "  OVERALL SCORE = " + experimentalGroupOverallScore);
		log.info("EXP GROUP " + scoreType + "  COMPONENTS SCORE = " + experimentalGroupComponentScore);
		ps.println("EXP GROUP " + scoreType + " COMPONENTS SCORE = " + experimentalGroupComponentScore);
		if (SCIOSlotTypes.hasTreatmentType.isIncluded()) {
			log.info("EXP GROUP " + scoreType + ": TREATMENT BOTH = " + bothS);
			ps.println("EXP GROUP " + scoreType + ": TREATMENT BOTH = " + bothS);
			log.info("EXP GROUP " + scoreType + ": TREATMENT Vehicle = " + vehicleScore);
			ps.println("EXP GROUP " + scoreType + ": TREATMENT Vehicle = " + vehicleScore);
			log.info("EXP GROUP " + scoreType + ": TREATMENT Non Vehicle = " + nonVehicleScore);
			ps.println("EXP GROUP " + scoreType + ": TREATMENT Non Vehicle = " + nonVehicleScore);
		}
		if (SCIOSlotTypes.hasOrganismModel.isIncluded()) {
			log.info("EXP GROUP " + scoreType + ": ORG MODEL = " + organismModelScore);
			ps.println("EXP GROUP " + scoreType + ": ORG MODEL = " + organismModelScore);
		}
		if (SCIOSlotTypes.hasInjuryModel.isIncluded()) {
			log.info("EXP GROUP " + scoreType + ": INJURY MODEL = " + injuryModelScore);
			ps.println("EXP GROUP " + scoreType + ": INJURY MODEL = " + injuryModelScore);
		}
		return experimentalGroupComponentScore;
	}

	private Score treatmentsInExpGroupEvaluate(boolean print, List<Integer> bestAssignment,
			List<EntityTemplate> goldAnnotations, List<EntityTemplate> predictedAnnotations, ESimpleEvaluationMode mode,
			EScoreType scoreType) {

		Score simpleScore = new Score(scoreType);
		if (SCIOSlotTypes.hasTreatmentType.isExcluded())
			return simpleScore;

		for (int goldIndex = 0; goldIndex < bestAssignment.size(); goldIndex++) {
			final int predictIndex = bestAssignment.get(goldIndex);
			/*
			 * Treatments
			 */
			List<AbstractAnnotation> goldTreatments = new ArrayList<>();
			if (goldAnnotations.size() > goldIndex)
				goldTreatments.addAll(goldAnnotations.get(goldIndex).getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType)
						.getSlotFiller());

			List<AbstractAnnotation> predictTreatments = new ArrayList<>();
			if (predictedAnnotations.size() > predictIndex)
				predictTreatments.addAll(predictedAnnotations.get(predictIndex)
						.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller());

			/*
			 * NOTE: Compare objects are used to tell whether a tp should be given for an
			 * empty prediction or not. If gold and predicted is empty AND the compare
			 * objects are also empty then a +1 tp is given
			 *
			 * E.g. g:Vehicle p:Vehicle would result in non vehicle mode to add 1 tp cause g
			 * and p are empty, however if we check the compare list (vehicle list) then
			 * they are not empty thus we do not add +1 We add only +1 for non vehicle,
			 * otherwise +1 would be added twice.
			 * 
			 *
			 */

			List<AbstractAnnotation> goldTreatmentsCompare = new ArrayList<>();
			if (goldAnnotations.size() > goldIndex)
				goldTreatmentsCompare.addAll(goldAnnotations.get(goldIndex)
						.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller());

			List<AbstractAnnotation> predictTreatmentsCompare = new ArrayList<>();
			if (predictedAnnotations.size() > predictIndex)
				predictTreatmentsCompare.addAll(predictedAnnotations.get(predictIndex)
						.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller());

			switch (mode) {
			case BOTH: {
				// Do nothing
				break;
			}
			case VEHICLE: {
				/*
				 * Filter for vehicles
				 */
				goldTreatments = goldTreatments.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == SCIOEntityTypes.vehicle
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(SCIOEntityTypes.vehicle))
						.collect(Collectors.toList());
				predictTreatments = predictTreatments.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == SCIOEntityTypes.vehicle
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(SCIOEntityTypes.vehicle))
						.collect(Collectors.toList());
				goldTreatmentsCompare.removeAll(goldTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == SCIOEntityTypes.vehicle
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(SCIOEntityTypes.vehicle))
						.collect(Collectors.toList()));
				predictTreatmentsCompare.removeAll(predictTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == SCIOEntityTypes.vehicle
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(SCIOEntityTypes.vehicle))
						.collect(Collectors.toList()));
				break;
			}
			case NON_VEHICLE: {
				goldTreatments.removeAll(goldTreatments.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == SCIOEntityTypes.vehicle
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(SCIOEntityTypes.vehicle))
						.collect(Collectors.toList()));
				predictTreatments.removeAll(predictTreatments.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == SCIOEntityTypes.vehicle
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(SCIOEntityTypes.vehicle))
						.collect(Collectors.toList()));
				goldTreatmentsCompare = goldTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == SCIOEntityTypes.vehicle
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(SCIOEntityTypes.vehicle))
						.collect(Collectors.toList());
				predictTreatmentsCompare = predictTreatmentsCompare.stream()
						.filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.containsSlotFiller())
						.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
								.getSlotFiller().getEntityType() == SCIOEntityTypes.vehicle
								|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
										.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
										.contains(SCIOEntityTypes.vehicle))
						.collect(Collectors.toList());
				break;
			}
			}

			Score s;
			if (goldTreatments.isEmpty() && predictTreatments.isEmpty())
				if (mode == ESimpleEvaluationMode.BOTH) {
					s = new Score(1, 0, 0);
				} else {
					if (goldTreatmentsCompare.isEmpty() && predictTreatmentsCompare.isEmpty()
							&& mode == ESimpleEvaluationMode.NON_VEHICLE) {
						s = new Score(1, 0, 0);
					} else {
						// No influence on micro score
						s = Score.getZero(scoreType);
					}
				}
			else
				s = evaluator.scoreMultiValues(goldTreatments, predictTreatments, scoreType);

			if (print) {
				log.info("Compare: g" + goldIndex);
				goldTreatments.forEach(g -> log.info(g.toPrettyString()));
				log.info("With: p" + predictIndex);
				predictTreatments.forEach(p -> log.info(p.toPrettyString()));
				log.info("Score: " + s);
				log.info("-----");
			}

			if (scoreType == EScoreType.MACRO)
				s.toMacro();

			simpleScore.add(s);

		}

		return simpleScore;
	}

	private Score organismModelInExpGroupEvaluate(List<Integer> bestAssignment, List<EntityTemplate> goldAnnotations,
			List<EntityTemplate> predictedAnnotations, EScoreType scoreType) {

		Score simpleScore = new Score(scoreType);

		for (int goldIndex = 0; goldIndex < bestAssignment.size(); goldIndex++) {
			final int predictIndex = bestAssignment.get(goldIndex);
			/*
			 * Treatments
			 */
			/*
			 * OrganismModel
			 */
			if (SCIOSlotTypes.hasOrganismModel.isIncluded()) {
				List<AbstractAnnotation> goldOrganismModel;
				if (goldAnnotations.size() > goldIndex)
					goldOrganismModel = Arrays.asList(goldAnnotations.get(goldIndex)
							.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel).getSlotFiller()).stream()
							.filter(a -> a != null).collect(Collectors.toList());

				else
					goldOrganismModel = Collections.emptyList();

				List<AbstractAnnotation> predictOrganismModel;

				if (predictedAnnotations.size() > predictIndex)
					predictOrganismModel = Arrays
							.asList(predictedAnnotations.get(predictIndex)
									.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel).getSlotFiller())
							.stream().filter(a -> a != null).collect(Collectors.toList());
				else
					predictOrganismModel = Collections.emptyList();

				simpleScore.add(evaluator.scoreMultiValues(goldOrganismModel, predictOrganismModel, scoreType));
			}

		}

		return simpleScore;
	}

	private Score injuryModelInExpGroupEvaluate(List<Integer> bestAssignment, List<EntityTemplate> goldAnnotations,
			List<EntityTemplate> predictedAnnotations, EScoreType scoreType) {

		Score simpleScore = new Score(scoreType);
		if (SCIOSlotTypes.hasInjuryModel.isExcluded())
			return simpleScore;
		for (int goldIndex = 0; goldIndex < bestAssignment.size(); goldIndex++) {
			final int predictIndex = bestAssignment.get(goldIndex);

			/*
			 * InjuryModel
			 */
			List<AbstractAnnotation> goldInjuryModel;
			if (goldAnnotations.size() > goldIndex)
				goldInjuryModel = Arrays.asList(goldAnnotations.get(goldIndex)
						.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).getSlotFiller()).stream()
						.filter(a -> a != null).collect(Collectors.toList());
			else
				goldInjuryModel = Collections.emptyList();

			List<AbstractAnnotation> predictInjuryModel;

			if (predictedAnnotations.size() > predictIndex)
				predictInjuryModel = Arrays.asList(predictedAnnotations.get(predictIndex)
						.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).getSlotFiller()).stream()
						.filter(a -> a != null).collect(Collectors.toList());
			else
				predictInjuryModel = Collections.emptyList();

			simpleScore.add(evaluator.scoreMultiValues(goldInjuryModel, predictInjuryModel, scoreType));

		}

		return simpleScore;
	}

}
