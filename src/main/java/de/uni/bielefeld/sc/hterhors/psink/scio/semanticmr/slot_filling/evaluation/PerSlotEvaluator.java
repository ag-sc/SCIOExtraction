package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;

public class PerSlotEvaluator {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	public static void evalCardinality(EScoreType scoreType, Map<Instance, State> finalStates,
			Map<Instance, State> coverageStates, Map<String, Score> scoreMap) {
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

		log.info(scoreType.name() + "\t" + "Cardinality = " + cardinality.toTSVString() + "\t"
				+ Score.SCORE_FORMAT.format(
						coverageCardinality.getF1() == 0D ? 0 : (cardinality.getF1() / coverageCardinality.getF1()))
				+ "\t"
				+ Score.SCORE_FORMAT.format(coverageCardinality.getPrecision() == 0D ? 0
						: (cardinality.getPrecision() / coverageCardinality.getPrecision()))
				+ "\t"
				+ Score.SCORE_FORMAT.format(coverageCardinality.getRecall() == 0D ? 0
						: (cardinality.getRecall() / coverageCardinality.getRecall()))
				+ "\t" + Score.SCORE_FORMAT.format(coverageCardinality.getF1()) + "\t"
				+ Score.SCORE_FORMAT.format(coverageCardinality.getPrecision()) + "\t"
				+ Score.SCORE_FORMAT.format(coverageCardinality.getRecall())

		);
		scoreMap.putIfAbsent("Cardinality-Absolute", new Score(scoreType));
		scoreMap.putIfAbsent("Cardinality-Relative", new Score(scoreType));
		scoreMap.putIfAbsent("Cardinality-Coverage", new Score(scoreType));
		scoreMap.get("Cardinality-Absolute").add(cardinality);
		scoreMap.get(
				"Cardinality-Relative").add(
						new Score(
								coverageCardinality.getF1() == 0D
										? 0
										: (cardinality.getF1()
												/ Math.max(cardinality.getF1(), coverageCardinality.getF1())),
								coverageCardinality.getPrecision() == 0D ? 0
										: (cardinality.getPrecision()
												/ Math.max(cardinality.getPrecision(), coverageCardinality.getPrecision())),
								coverageCardinality.getRecall() == 0D ? 0
										: (cardinality.getRecall()
												/ Math.max(cardinality.getRecall(), coverageCardinality.getRecall()))));
		scoreMap.get("Cardinality-Coverage").add(coverageCardinality);
	}

	public static void evalProperties(EScoreType scoreType, Map<Instance, State> finalStates,
			Map<Instance, State> coverageStates, Set<SlotType> slotTypesToConsider, AbstractEvaluator evaluator,
			Map<String, Score> scoreMap) {
		for (SlotType consideredSlotType : slotTypesToConsider) {
			Map<SlotType, Boolean> storage = SlotType.storeExcludance();
			SlotType.excludeAll();
			consideredSlotType.includeRec();
			Score score = new Score(scoreType);

			Score coverageRootScore = new Score(scoreType);

			for (Instance instance : finalStates.keySet()) {

				State finalState = finalStates.get(instance);
				State coverageState = coverageStates.get(instance);

				Map<SlotType, Boolean> rootStorage = SlotType.storeExcludance();
				SlotType.excludeAll();
//				-------------------------------------------

				Score coverageRootSub = coverageState.getGoldAnnotations().evaluate(evaluator,
						coverageState.getCurrentPredictions(), EScoreType.MICRO);

				Score rootSub = finalState.getGoldAnnotations().evaluate(evaluator, finalState.getCurrentPredictions(),
						EScoreType.MICRO);

				SlotType.restoreExcludance(rootStorage);

//				-------------------------------------------

				Score coverageAdder = coverageState.getGoldAnnotations().evaluate(evaluator,
						coverageState.getCurrentPredictions(), EScoreType.MICRO);

				coverageAdder.sub(coverageRootSub);

				if (scoreType == EScoreType.MACRO)
					coverageAdder.toMacro();

				coverageRootScore.add(coverageAdder);

				Score adder = finalState.getGoldAnnotations().evaluate(evaluator, finalState.getCurrentPredictions(),
						EScoreType.MICRO);

				adder.sub(rootSub);

				if (scoreType == EScoreType.MACRO)
					adder.toMacro();

				score.add(adder);

			}

			log.info(scoreType.name() + "\t" + consideredSlotType.name + " = " + score.toTSVString() + "\t"
					+ Score.SCORE_FORMAT.format(coverageRootScore.getF1() == 0D ? 0
							: (score.getF1() / Math.max(score.getF1(), coverageRootScore.getF1())))
					+ "\t"
					+ Score.SCORE_FORMAT.format(coverageRootScore.getPrecision() == 0D ? 0
							: (score.getPrecision() / Math.max(score.getPrecision(), coverageRootScore.getPrecision())))
					+ "\t"
					+ Score.SCORE_FORMAT.format(coverageRootScore.getRecall() == 0D ? 0
							: (score.getRecall() / Math.max(score.getRecall(), coverageRootScore.getRecall())))
					+ "\t" + Score.SCORE_FORMAT.format(Math.max(score.getF1(), coverageRootScore.getF1())) + "\t"
					+ Score.SCORE_FORMAT.format(Math.max(score.getPrecision(), coverageRootScore.getPrecision())) + "\t"
					+ Score.SCORE_FORMAT.format(Math.max(score.getRecall(), coverageRootScore.getRecall())));

			scoreMap.putIfAbsent(consideredSlotType.name + "-Absolute", new Score(scoreType));
			scoreMap.putIfAbsent(consideredSlotType.name + "-Relative", new Score(scoreType));
			scoreMap.putIfAbsent(consideredSlotType.name + "-Coverage", new Score(scoreType));
			scoreMap.get(consideredSlotType.name + "-Absolute").add(score);
			scoreMap.get(consideredSlotType.name + "-Relative").add(new Score(
					coverageRootScore.getF1() == 0D ? 0
							: (score.getF1() / Math.max(score.getF1(), coverageRootScore.getF1())),
					coverageRootScore.getPrecision() == 0D ? 0
							: (score.getPrecision() / Math.max(score.getPrecision(), coverageRootScore.getPrecision())),
					coverageRootScore.getRecall() == 0D ? 0
							: (score.getRecall() / Math.max(score.getRecall(), coverageRootScore.getRecall()))));
			scoreMap.get(consideredSlotType.name + "-Coverage").add(coverageRootScore);

			SlotType.restoreExcludance(storage);

		}
	}

	public static void evalRoot(EScoreType scoreType, Map<Instance, State> finalStates,
			Map<Instance, State> coverageStates, AbstractEvaluator evaluator, Map<String, Score> scoreMap) {

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

		log.info(scoreType.name() + "\t" + "Root = " + rootScore.toTSVString() + "\t"
				+ Score.SCORE_FORMAT.format(coverageRootScore.getF1() == 0D ? 0
						: (rootScore.getF1() / Math.max(rootScore.getF1(), coverageRootScore.getF1())))
				+ "\t"
				+ Score.SCORE_FORMAT.format(coverageRootScore.getPrecision() == 0D ? 0
						: (rootScore.getPrecision()
								/ Math.max(rootScore.getPrecision(), coverageRootScore.getPrecision())))
				+ "\t"
				+ Score.SCORE_FORMAT.format(coverageRootScore.getRecall() == 0D ? 0
						: (rootScore.getRecall() / Math.max(rootScore.getRecall(), coverageRootScore.getRecall())))
				+ "\t" + Score.SCORE_FORMAT.format(Math.max(rootScore.getF1(), coverageRootScore.getF1())) + "\t"
				+ Score.SCORE_FORMAT.format(Math.max(rootScore.getPrecision(), coverageRootScore.getPrecision())) + "\t"
				+ Score.SCORE_FORMAT.format(Math.max(rootScore.getRecall(), coverageRootScore.getRecall())));

		scoreMap.putIfAbsent("Root-Absolute", new Score(scoreType));
		scoreMap.putIfAbsent("Root-Relative", new Score(scoreType));
		scoreMap.putIfAbsent("Root-Coverage", new Score(scoreType));
		scoreMap.get("Root-Absolute").add(rootScore);
		scoreMap.get("Root-Relative").add(new Score(
				coverageRootScore.getF1() == 0D ? 0
						: (rootScore.getF1() / Math.max(rootScore.getF1(), coverageRootScore.getF1())),
				coverageRootScore.getPrecision() == 0D ? 0
						: (rootScore.getPrecision()
								/ Math.max(rootScore.getPrecision(), coverageRootScore.getPrecision())),
				coverageRootScore.getRecall() == 0D ? 0
						: (rootScore.getRecall() / Math.max(rootScore.getRecall(), coverageRootScore.getRecall()))));
		scoreMap.get("Root-Coverage").add(coverageRootScore);

	}

	public static void evalOverall(EScoreType scoreType, Map<Instance, State> finalStates,
			Map<Instance, State> coverageStates, AbstractEvaluator evaluator, Map<String, Score> scoreMap) {

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

		log.info(scoreType.name() + "\t" + "Overall = " + rootScore.toTSVString() + "\t"
				+ Score.SCORE_FORMAT.format(coverageRootScore.getF1() == 0D ? 0
						: (rootScore.getF1() / Math.max(rootScore.getF1(), coverageRootScore.getF1())))
				+ "\t"
				+ Score.SCORE_FORMAT.format(coverageRootScore.getPrecision() == 0D ? 0
						: (rootScore.getPrecision()
								/ Math.max(rootScore.getPrecision(), coverageRootScore.getPrecision())))
				+ "\t"
				+ Score.SCORE_FORMAT.format(coverageRootScore.getRecall() == 0D ? 0
						: (rootScore.getRecall() / Math.max(rootScore.getRecall(), coverageRootScore.getRecall())))
				+ "\t" + Score.SCORE_FORMAT.format(Math.max(rootScore.getF1(), coverageRootScore.getF1())) + "\t"
				+ Score.SCORE_FORMAT.format(Math.max(rootScore.getPrecision(), coverageRootScore.getPrecision())) + "\t"
				+ Score.SCORE_FORMAT.format(Math.max(rootScore.getRecall(), coverageRootScore.getRecall())));

		scoreMap.putIfAbsent("Overall-Absolute", new Score(scoreType));
		scoreMap.putIfAbsent("Overall-Relative", new Score(scoreType));
		scoreMap.putIfAbsent("Overall-Coverage", new Score(scoreType));
		scoreMap.get("Overall-Absolute").add(rootScore);
		scoreMap.get("Overall-Relative").add(new Score(
				coverageRootScore.getF1() == 0D ? 0
						: (rootScore.getF1() / Math.max(rootScore.getF1(), coverageRootScore.getF1())),
				coverageRootScore.getPrecision() == 0D ? 0
						: (rootScore.getPrecision()
								/ Math.max(rootScore.getPrecision(), coverageRootScore.getPrecision())),
				coverageRootScore.getRecall() == 0D ? 0
						: (rootScore.getRecall() / Math.max(rootScore.getRecall(), coverageRootScore.getRecall()))));
		scoreMap.get("Overall-Coverage").add(coverageRootScore);

	}

}
