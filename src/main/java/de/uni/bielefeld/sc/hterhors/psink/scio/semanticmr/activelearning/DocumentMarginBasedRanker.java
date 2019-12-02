package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.activelearning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.activelearning.IActiveLearningDocumentRanker;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;

/**
 * 
 * @author hterhors
 *
 */
public class DocumentMarginBasedRanker implements IActiveLearningDocumentRanker {

	final Logger log = LogManager.getRootLogger();

	final private AbstractSlotFillingPredictor runner;

	public DocumentMarginBasedRanker(AbstractSlotFillingPredictor runner) {
		this.runner = runner;
	}

	@Override
	public List<Instance> rank(List<Instance> remainingInstances) {

		List<SmallestFirst> marginInstances = new ArrayList<>();

		log.info("Compute variations for margin...");
		Map<Instance, List<State>> results = runner.crf.collectNBestStates(remainingInstances, 2, runner.maxStepCrit);

		log.info("Compute margin...");
		for (Entry<Instance, List<State>> predictedInstance : results.entrySet()) {

			double margin;

			if (predictedInstance.getValue().size() != 2) {
				// Highest Value =
				margin = Double.MAX_VALUE;
			} else {
				// Always positive
				margin = Math.abs(predictedInstance.getValue().get(0).getModelScore()
						- predictedInstance.getValue().get(1).getModelScore());
			}

			/*
			 * Sorted by smallest first
			 */
			marginInstances.add(new SmallestFirst(predictedInstance.getKey(), margin));

		}

		log.info("Sort...");

		Collections.sort(marginInstances);

		return marginInstances.stream().map(e -> e.instance).collect(Collectors.toList());

	}

}
