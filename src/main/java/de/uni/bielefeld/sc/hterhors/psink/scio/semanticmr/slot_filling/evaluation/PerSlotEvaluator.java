package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;

public class PerSlotEvaluator {

	public static void evalCardinality(EScoreType scoreType, Map<Instance, State> finalStates,
			Map<Instance, State> coverageStates) {
		Score cardinality = new Score(scoreType);
		Score coverageCardinality = new Score(scoreType);

		for (Instance instance : finalStates.keySet()) {
			State coverageState = coverageStates.get(instance);

			List<EntityTemplate> coverageGoldAnnotations = coverageState.getGoldAnnotations().getAnnotations();
			List<EntityTemplate> coveragePredictedAnnotations = coverageState.getCurrentPredictions().getAnnotations();

			int ctp = Math.min(coverageGoldAnnotations.size(), coveragePredictedAnnotations.size());
			int cfp = coveragePredictedAnnotations.size() > coverageGoldAnnotations.size()
					? coveragePredictedAnnotations.size() - coverageGoldAnnotations.size()
					: 0;
			int cfn = coveragePredictedAnnotations.size() < coverageGoldAnnotations.size()
					? coverageGoldAnnotations.size() - coveragePredictedAnnotations.size()
					: 0;

			Score coverageSC = new Score(ctp, cfp, cfn);
			if (scoreType == EScoreType.MACRO)
				coverageSC.toMacro();
			coverageCardinality.add(coverageSC);

			State finalState = finalStates.get(instance);

			List<EntityTemplate> goldAnnotations = finalState.getGoldAnnotations().getAnnotations();
			List<EntityTemplate> predictedAnnotations = finalState.getCurrentPredictions().getAnnotations();

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
			cardinality.add(sC);

		}
		System.out.println(scoreType.name() + "\t" + "Cardinality = " + cardinality.toTSVString() + "\t"
				+ Score.SCORE_FORMAT.format(cardinality.getF1() / coverageCardinality.getF1()) + "\t"
				+ Score.SCORE_FORMAT.format(cardinality.getPrecision() / coverageCardinality.getPrecision()) + "\t"
				+ Score.SCORE_FORMAT.format(cardinality.getRecall() / coverageCardinality.getRecall()));
	}

	public static void evalProperties(EScoreType scoreType, Map<Instance, State> finalStates,
			Map<Instance, State> coverageStates, Set<SlotType> slotTypesToConsider, AbstractEvaluator evaluator) {
		for (SlotType consideredSlotType : slotTypesToConsider) {

			Map<SlotType, Boolean> storage = SlotType.storeExcludance();
			SlotType.excludeAll();
			consideredSlotType.include();
			Score score = new Score(scoreType);

			Score coverageRootScore = new Score(scoreType);

			for (Instance instance : finalStates.keySet()) {

				State coverageState = coverageStates.get(instance);

				Score coverageAdder = coverageState.getGoldAnnotations().evaluate(evaluator,
						coverageState.getCurrentPredictions(), EScoreType.MICRO);

				if (scoreType == EScoreType.MACRO)
					coverageAdder.toMacro();

				coverageRootScore.add(coverageAdder);

				State finalState = finalStates.get(instance);
				Score adder = finalState.getGoldAnnotations().evaluate(evaluator, finalState.getCurrentPredictions(),
						EScoreType.MICRO);

				if (scoreType == EScoreType.MACRO)
					adder.toMacro();

				score.add(adder);

			}
			System.out.println(scoreType.name() + "\t" + consideredSlotType.name + " = " + score.toTSVString() + "\t"
					+ Score.SCORE_FORMAT.format(score.getF1() / coverageRootScore.getF1()) + "\t"
					+ Score.SCORE_FORMAT.format(score.getPrecision() / coverageRootScore.getPrecision()) + "\t"
					+ Score.SCORE_FORMAT.format(score.getRecall() / coverageRootScore.getRecall()));
			SlotType.restoreExcludance(storage);

		}
	}

	public static void evalRoot(EScoreType scoreType, Map<Instance, State> finalStates,
			Map<Instance, State> coverageStates, AbstractEvaluator evaluator) {

		Score coverageRootScore = new Score(scoreType);
		Score rootScore = new Score(scoreType);
		{
			Map<SlotType, Boolean> storage = SlotType.storeExcludance();
			SlotType.excludeAll();

			for (Instance instance : finalStates.keySet()) {

				State coverageState = coverageStates.get(instance);

				Score coverageAdder = coverageState.getGoldAnnotations().evaluate(evaluator,
						coverageState.getCurrentPredictions(), EScoreType.MICRO);

				if (scoreType == EScoreType.MACRO)
					coverageAdder.toMacro();

				coverageRootScore.add(coverageAdder);

				State finalState = finalStates.get(instance);

				Score adder = finalState.getGoldAnnotations().evaluate(evaluator, finalState.getCurrentPredictions(),
						EScoreType.MICRO);

				if (scoreType == EScoreType.MACRO)
					adder.toMacro();

				rootScore.add(adder);
			}

			SlotType.restoreExcludance(storage);

		}

		System.out.println(scoreType.name() + "\t" + "Root = " + rootScore.toTSVString() + "\t"
				+ Score.SCORE_FORMAT.format(rootScore.getF1() / coverageRootScore.getF1()) + "\t"
				+ Score.SCORE_FORMAT.format(rootScore.getPrecision() / coverageRootScore.getPrecision()) + "\t"
				+ Score.SCORE_FORMAT.format(rootScore.getRecall() / coverageRootScore.getRecall()));
	}

	public static void evalOverall(EScoreType scoreType, Map<Instance, State> finalStates,
			Map<Instance, State> coverageStates, AbstractEvaluator evaluator) {

		Score coverageRootScore = new Score(scoreType);
		Score rootScore = new Score(scoreType);
		{

			for (Instance instance : finalStates.keySet()) {

				State coverageState = coverageStates.get(instance);

				Score coverageAdder = coverageState.getGoldAnnotations().evaluate(evaluator,
						coverageState.getCurrentPredictions(), EScoreType.MICRO);

				if (scoreType == EScoreType.MACRO)
					coverageAdder.toMacro();

				coverageRootScore.add(coverageAdder);

				State finalState = finalStates.get(instance);

				Score adder = finalState.getGoldAnnotations().evaluate(evaluator, finalState.getCurrentPredictions(),
						EScoreType.MICRO);

				if (scoreType == EScoreType.MACRO)
					adder.toMacro();

				rootScore.add(adder);
			}

		}

		System.out.println(scoreType.name() + "\t" + "Overall = " + rootScore.toTSVString() + "\t"
				+ Score.SCORE_FORMAT.format(rootScore.getF1() / coverageRootScore.getF1()) + "\t"
				+ Score.SCORE_FORMAT.format(rootScore.getPrecision() / coverageRootScore.getPrecision()) + "\t"
				+ Score.SCORE_FORMAT.format(rootScore.getRecall() / coverageRootScore.getRecall()));
	}

}
