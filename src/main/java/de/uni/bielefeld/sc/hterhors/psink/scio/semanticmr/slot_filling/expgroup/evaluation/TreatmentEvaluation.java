package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.evaluation;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.crf.of.IObjectiveFunction;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.modes.Modes.ESimpleEvaluationMode;

public class TreatmentEvaluation {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");
	private final AbstractEvaluator evaluator;

	public TreatmentEvaluation(AbstractEvaluator evaluator) {
		this.evaluator = evaluator;
	}

	public Score evaluate(PrintStream ps, Map<Instance, State> results) {
		
		Score vehicleScore = new Score();
		Score nonVehicleScore = new Score();
		Score bothS = new Score();
//		double macroF1 = 0;
//		double macroPrecision = 0;
//		double macroRecall = 0;
//		int i = 0;
		for (Entry<Instance, State> e : results.entrySet()) {

			/*
			 * Evaluate treatments
			 */

			List<EntityTemplate> goldAnnotations = new ArrayList<>(
					e.getValue().getGoldAnnotations().getAnnotations().stream()
							.flatMap(ex -> ex.asInstanceOfEntityTemplate()
									.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller().stream())
							.map(t -> t.asInstanceOfEntityTemplate()).collect(Collectors.toSet()));

			List<EntityTemplate> predictedAnnotations = new ArrayList<>(
					e.getValue().getCurrentPredictions().getAnnotations().stream()
							.flatMap(ex -> ex.asInstanceOfEntityTemplate()
									.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller().stream())
							.map(t -> t.asInstanceOfEntityTemplate()).collect(Collectors.toSet()));

//			i++;
//			log.info(e.getKey().getName());

			Score both = simpleTreatmentEvaluate(goldAnnotations, predictedAnnotations, ESimpleEvaluationMode.BOTH);
//			log.info("Both: " + both);

			Score vs = simpleTreatmentEvaluate(goldAnnotations, predictedAnnotations, ESimpleEvaluationMode.VEHICLE);
//			log.info("Vehicles: " + vs);

			Score nvs = simpleTreatmentEvaluate(goldAnnotations, predictedAnnotations,
					ESimpleEvaluationMode.NON_VEHICLE);
//			log.info("Non Vehicles: " + nvs);

			vehicleScore.add(vs);
			nonVehicleScore.add(nvs);
			bothS.add(both);
//			macroF1 += both.getF1();
//			macroPrecision += both.getPrecision();
//			macroRecall += both.getRecall();
//			log.info("TREATMENTS INTERMEDIATE MACRO: F1 = " + macroF1 / i + ", P = " + macroPrecision / i + ", R = "
//					+ macroRecall / i);
//			log.info("TREATMENTS INTERMEDIATE MICRO: " + bothS);
//			log.info("");
		}
//		macroF1 /= results.entrySet().size();
//		macroPrecision /= results.entrySet().size();
//		macroRecall /= results.entrySet().size();
//		log.info("TREATMENTS MACRO: F1 = " + macroF1 + ", P = " + macroPrecision + ", R = " + macroRecall);
		log.info("TREATMENTS MICRO: BOTH = " + bothS);
		log.info("TREATMENTS MICRO: Vehicle = " + vehicleScore);
		log.info("TREATMENTS MICRO: Non Vehicle = " + nonVehicleScore);
		ps.println("TREATMENTS MICRO: BOTH = " + bothS);
		ps.println("TREATMENTS MICRO: Vehicle = " + vehicleScore);
		ps.println("TREATMENTS MICRO: Non Vehicle = " + nonVehicleScore);
		return bothS;
	}

	private Score simpleTreatmentEvaluate(List<EntityTemplate> goldAnnotations,
			List<EntityTemplate> predictedAnnotations, ESimpleEvaluationMode mode) {

		List<EntityTemplate> goldTreatments = new ArrayList<>(goldAnnotations);
		List<EntityTemplate> predictTreatments = new ArrayList<>(predictedAnnotations);

		List<EntityTemplate> goldTreatmentsCompare = new ArrayList<>(goldAnnotations);
		List<EntityTemplate> predictTreatmentsCompare = new ArrayList<>(predictedAnnotations);

		switch (mode) {
		case BOTH: {
			// Do nothing
			break;
		}
		case VEHICLE: {
			/*
			 * Filter for vehicles
			 */
			goldTreatments = goldTreatments.stream().filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
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
			goldTreatmentsCompare.removeAll(
					goldTreatmentsCompare.stream().filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
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
			goldTreatments.removeAll(
					goldTreatments.stream().filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
							.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
									.containsSlotFiller())
							.filter(t -> t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
									.getSlotFiller().getEntityType() == SCIOEntityTypes.vehicle
									|| t.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound)
											.getSlotFiller().getEntityType().getHierarchicalEntityTypes()
											.contains(SCIOEntityTypes.vehicle))
							.collect(Collectors.toList()));
			predictTreatments.removeAll(
					predictTreatments.stream().filter(a -> a.getEntityType() == SCIOEntityTypes.compoundTreatment)
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

		return s;

	}

	public void evaluateComparable(IObjectiveFunction predictionObjectiveFunction, Map<Instance, State> results) {

		Score vehicleScore = new Score();
		Score nonVehicleScore = new Score();
		Score bothS = new Score();
//		double macroF1 = 0;
//		double macroPrecision = 0;
//		double macroRecall = 0;
//		int i = 0;
		for (Entry<Instance, State> e : results.entrySet()) {

			/*
			 * Evaluate treatments
			 */

			List<EntityTemplate> goldAnnotations = new ArrayList<>(
					e.getValue().getGoldAnnotations().getAnnotations().stream()
							.flatMap(ex -> ex.asInstanceOfEntityTemplate()
									.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller().stream())
							.map(t -> t.asInstanceOfEntityTemplate()).collect(Collectors.toSet()));

			List<EntityTemplate> predictedAnnotations = new ArrayList<>(
					e.getValue().getCurrentPredictions().getAnnotations().stream()
							.flatMap(ex -> ex.asInstanceOfEntityTemplate()
									.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller().stream())
							.map(t -> t.asInstanceOfEntityTemplate()).collect(Collectors.toSet()));

//			i++;
//			log.info(e.getKey().getName());

			Score s = evaluator.scoreMultiValues(goldAnnotations, predictedAnnotations);
//			log.info("nerl based score: " + s);

			List<Integer> bestAssignment = ((CartesianEvaluator) predictionObjectiveFunction.getEvaluator())
					.getBestAssignment(goldAnnotations, predictedAnnotations);

			Score both = simpleTreatmentEvaluateComparable(false, bestAssignment, goldAnnotations, predictedAnnotations,
					ESimpleEvaluationMode.BOTH);
//			log.info("Both: " + both);

			Score vs = simpleTreatmentEvaluateComparable(false, bestAssignment, goldAnnotations, predictedAnnotations,
					ESimpleEvaluationMode.VEHICLE);
//			log.info("Vehicles: " + vs);

			Score nvs = simpleTreatmentEvaluateComparable(false, bestAssignment, goldAnnotations, predictedAnnotations,
					ESimpleEvaluationMode.NON_VEHICLE);
//			log.info("Non Vehicles: " + nvs);

			vehicleScore.add(vs);
			nonVehicleScore.add(nvs);
			bothS.add(both);
//			macroF1 += both.getF1();
//			macroPrecision += both.getPrecision();
//			macroRecall += both.getRecall();
//			log.info("TREATMENTS INTERMEDIATE MACRO: F1 = " + macroF1 / i + ", P = " + macroPrecision / i + ", R = "
//					+ macroRecall / i);
//			log.info("TREATMENTS INTERMEDIATE MICRO: " + bothS);
//			log.info("");
		}
//		macroF1 /= results.entrySet().size();
//		macroPrecision /= results.entrySet().size();
//		macroRecall /= results.entrySet().size();
//		log.info("TREATMENTS MACRO: F1 = " + macroF1 + ", P = " + macroPrecision + ", R = " + macroRecall);
		log.info("TREATMENTS MICRO: BOTH = " + bothS);
		log.info("TREATMENTS MICRO: Vehicle = " + vehicleScore);
		log.info("TREATMENTS MICRO: Non Vehicle = " + nonVehicleScore);
	}

	private Score simpleTreatmentEvaluateComparable(boolean print, List<Integer> bestAssignment,
			List<EntityTemplate> goldAnnotations, List<EntityTemplate> predictedAnnotations,
			ESimpleEvaluationMode mode) {

		Score simpleScore = new Score();

		for (int goldIndex = 0; goldIndex < bestAssignment.size(); goldIndex++) {
			final int predictIndex = bestAssignment.get(goldIndex);

			List<EntityTemplate> goldTreatments = new ArrayList<>();
			List<EntityTemplate> predictTreatments = new ArrayList<>();

			if (goldAnnotations.size() > goldIndex)
				goldTreatments.add(goldAnnotations.get(goldIndex));

			if (predictedAnnotations.size() > predictIndex)
				predictTreatments.add(predictedAnnotations.get(predictIndex));
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
			List<EntityTemplate> goldTreatmentsCompare = new ArrayList<>();
			List<EntityTemplate> predictTreatmentsCompare = new ArrayList<>();

			if (goldAnnotations.size() > goldIndex)
				goldTreatmentsCompare.add(goldAnnotations.get(goldIndex));

			if (predictedAnnotations.size() > predictIndex)
				predictTreatmentsCompare.add(predictedAnnotations.get(predictIndex));

//			fillData(mode, goldTreatments, predictTreatments, goldTreatmentsCompare, predictTreatmentsCompare);
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

}
