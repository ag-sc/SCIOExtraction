package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.crf.of.IObjectiveFunction;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.eval.NerlaEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.ESimpleEvaluationMode;

public class ExperimentalGroupEvaluation {

	private final IObjectiveFunction predictionObjectiveFunction;
	private static Logger log = LogManager.getFormatterLogger("SlotFilling");
	private final AbstractEvaluator evaluator;

	public ExperimentalGroupEvaluation(IObjectiveFunction predictionObjectiveFunction, AbstractEvaluator evaluator) {
		this.predictionObjectiveFunction = predictionObjectiveFunction;
		this.evaluator = evaluator;
	}

	public Score evaluate(Map<Instance, State> results) {

		Score experimentalGroupScore = new Score();
		Score organismModelScore = new Score();
		Score injuryModelScore = new Score();
		Score vehicleScore = new Score();
		Score nonVehicleScore = new Score();
		Score bothS = new Score();
//		double macroF1 = 0;
//		double macroPrecision = 0;
//		double macroRecall = 0;
//		int i = 0;
		for (Entry<Instance, State> e : results.entrySet()) {
			/*
			 * 
			 * Evaluate clustering of Treatments
			 */

//			i++;
//			log.info(e.getKey().getName());

			List<EntityTemplate> goldAnnotations = e.getValue().getGoldAnnotations().getAnnotations();
			List<EntityTemplate> predictedAnnotations = e.getValue().getCurrentPredictions().getAnnotations();

			List<Integer> bestAssignment = ((CartesianEvaluator) predictionObjectiveFunction.getEvaluator())
					.getBestAssignment(goldAnnotations, predictedAnnotations);

			Score both = treatmentsInExpGroupEvaluate(false, bestAssignment, goldAnnotations, predictedAnnotations,
					ESimpleEvaluationMode.BOTH);
//			log.info("Both: " + both);

			Score vs = treatmentsInExpGroupEvaluate(false, bestAssignment, goldAnnotations, predictedAnnotations,
					ESimpleEvaluationMode.VEHICLE);
//			log.info("Vehicles: " + vs);

			Score nvs = treatmentsInExpGroupEvaluate(false, bestAssignment, goldAnnotations, predictedAnnotations,
					ESimpleEvaluationMode.NON_VEHICLE);
//			log.info("Non Vehicles: " + nvs);

			vehicleScore.add(vs);
			nonVehicleScore.add(nvs);
			bothS.add(both);

			organismModelScore
					.add(organismModelInExpGroupEvaluate(bestAssignment, goldAnnotations, predictedAnnotations));
			injuryModelScore.add(injuryModelInExpGroupEvaluate(bestAssignment, goldAnnotations, predictedAnnotations));

//			macroF1 += score.getF1();
//			macroPrecision += score.getPrecision();
//			macroRecall += score.getRecall();
//			log.info("EXP GROUP INTERMEDIATE MACRO: F1 = " + macroF1 / i + ", P = " + macroPrecision / i + ", R = "
//					+ macroRecall / i);
//			log.info("EXP GROUP INTERMEDIATE MICRO: " + bothS);
//			log.info("");
		}

		experimentalGroupScore.add(bothS);
		experimentalGroupScore.add(organismModelScore);
		experimentalGroupScore.add(injuryModelScore);

//		macroF1 /= results.entrySet().size();
//		macroPrecision /= results.entrySet().size();
//		macroRecall /= results.entrySet().size();
//		log.info("EXP GROUP MACRO: F1 = " + macroF1 + ", P = " + macroPrecision + ", R = " + macroRecall);
		log.info("EXP GROUP MICRO  SCORE = " + experimentalGroupScore);
		log.info("EXP GROUP MICRO: TREATMENT BOTH = " + bothS);
		log.info("EXP GROUP MICRO: TREATMENT Vehicle = " + vehicleScore);
		log.info("EXP GROUP MICRO: TREATMENT Non Vehicle = " + nonVehicleScore);
		log.info("EXP GROUP MICRO: ORG MODEL = " + organismModelScore);
		log.info("EXP GROUP MICRO: INJURY MODEL = " + injuryModelScore);
		return experimentalGroupScore;
	}

	private Score treatmentsInExpGroupEvaluate(boolean print, List<Integer> bestAssignment,
			List<EntityTemplate> goldAnnotations, List<EntityTemplate> predictedAnnotations,
			ESimpleEvaluationMode mode) {

		Score simpleScore = new Score();

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
			if (goldTreatments.isEmpty() && predictTreatments.isEmpty()) {
				if (mode == ESimpleEvaluationMode.BOTH) {
					s = new Score(1, 0, 0);
				} else {
					if (goldTreatmentsCompare.isEmpty() && predictTreatmentsCompare.isEmpty()
							&& mode == ESimpleEvaluationMode.NON_VEHICLE) {
						s = new Score(1, 0, 0);
					} else {
						s = new Score(0, 0, 0);
					}
				}
			} else
				s = evaluator.scoreMultiValues(goldTreatments, predictTreatments);

			if (print) {
				log.info("Compare: g" + goldIndex);
				goldTreatments.forEach(g -> log.info(g.toPrettyString()));
				log.info("With: p" + predictIndex);
				predictTreatments.forEach(p -> log.info(p.toPrettyString()));
				log.info("Score: " + s);
				log.info("-----");
			}

			simpleScore.add(s);

		}

		return simpleScore;
	}

	private Score organismModelInExpGroupEvaluate(List<Integer> bestAssignment, List<EntityTemplate> goldAnnotations,
			List<EntityTemplate> predictedAnnotations) {

		Score simpleScore = new Score();

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

				simpleScore.add(evaluator.scoreMultiValues(goldOrganismModel, predictOrganismModel));
			}

		}

		return simpleScore;
	}

	private Score injuryModelInExpGroupEvaluate(List<Integer> bestAssignment, List<EntityTemplate> goldAnnotations,
			List<EntityTemplate> predictedAnnotations) {

		Score simpleScore = new Score();

		for (int goldIndex = 0; goldIndex < bestAssignment.size(); goldIndex++) {
			final int predictIndex = bestAssignment.get(goldIndex);

			/*
			 * InjuryModel
			 */
			if (SCIOSlotTypes.hasInjuryModel.isIncluded()) {
				List<AbstractAnnotation> goldInjuryModel;
				if (goldAnnotations.size() > goldIndex)
					goldInjuryModel = Arrays.asList(goldAnnotations.get(goldIndex)
							.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).getSlotFiller()).stream()
							.filter(a -> a != null).collect(Collectors.toList());
				else
					goldInjuryModel = Collections.emptyList();

				List<AbstractAnnotation> predictInjuryModel;

				if (predictedAnnotations.size() > predictIndex)
					predictInjuryModel = Arrays
							.asList(predictedAnnotations.get(predictIndex)
									.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).getSlotFiller())
							.stream().filter(a -> a != null).collect(Collectors.toList());
				else
					predictInjuryModel = Collections.emptyList();

				simpleScore.add(evaluator.scoreMultiValues(goldInjuryModel, predictInjuryModel));
			}

		}

		return simpleScore;
	}

}
