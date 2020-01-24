package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.evaluation;

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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.modes.Modes.ESimpleEvaluationMode;

public class InjuryEvaluation {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private final AbstractEvaluator evaluator;

	public InjuryEvaluation(AbstractEvaluator evaluator) {
		this.evaluator = evaluator;
	}

	public Score evaluate(Map<Instance, State> results) {

		Score score = new Score();
//		double macroF1 = 0;
//		double macroPrecision = 0;
//		double macroRecall = 0;
//		int i = 0;
		for (Entry<Instance, State> e : results.entrySet()) {

			/*
			 * Evaluate organismModel
			 */

			List<EntityTemplate> goldAnnotations = new ArrayList<>(
					e.getValue().getGoldAnnotations().getAnnotations().stream()
							.map(ex -> ex.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel)
									.getSlotFiller())
							.filter(x -> x != null).map(t -> t.asInstanceOfEntityTemplate())
							.collect(Collectors.toSet()));

			List<EntityTemplate> predictedAnnotations = new ArrayList<>(
					e.getValue().getCurrentPredictions().getAnnotations().stream()
							.map(ex -> ex.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel)
									.getSlotFiller())
							.filter(x -> x != null).map(t -> t.asInstanceOfEntityTemplate())
							.collect(Collectors.toSet()));

//			i++;
//			log.info(e.getKey().getName());

			Score s = evaluator.scoreMultiValues(goldAnnotations, predictedAnnotations);
//			log.info("nerl based score: " + s);

			score.add(s);
//			macroF1 += s.getF1();
//			macroPrecision += s.getPrecision();
//			macroRecall += s.getRecall();
//			log.info("INJURY MODEL INTERMEDIATE MACRO: F1 = " + macroF1 / i + ", P = " + macroPrecision / i + ", R = "
//					+ macroRecall / i);
//			log.info("INJURY MODEL INTERMEDIATE MICRO: " + score);
//			log.info("");
		}
//		macroF1 /= results.entrySet().size();
//		macroPrecision /= results.entrySet().size();
//		macroRecall /= results.entrySet().size();
//		log.info("INJURY MODEL MACRO: F1 = " + macroF1 + ", P = " + macroPrecision + ", R = " + macroRecall);
		log.info("INJURY MODEL MICRO: SCORE = " + score);
		return score;
	}

}
